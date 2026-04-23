import { startTransition, useDeferredValue, useEffect, useMemo, useRef, useState } from "react";
import type { ReactNode } from "react";
import type Hls from "hls.js";
import { api } from "./api/client";
import { languageLabels, localizeText, t } from "./i18n";
import {
  detectInstallGuideKind,
  installGuideCopy,
  INSTALL_GUIDE_DISMISSED_KEY,
  isStandaloneDisplayMode,
  type DeferredInstallPromptEvent,
  unsupportedPlaybackCopy,
} from "./installGuide";
import {
  attachLiveStream,
  buildPlaybackCandidates,
  cleanupAudio,
  playAudioElement,
  UnsupportedPlaybackError,
} from "./player";
import type {
  BootstrapResponse,
  LanguageCode,
  NowPlayingResponse,
  OnAirSong,
  PlaybackState,
  ProgramEntry,
  ScheduleResponse,
  StationSummary,
  ThemeMode,
  WebSettings,
} from "./types";
import {
  applyTheme,
  createDefaultSettings,
  loadSettings,
  persistSettings,
  resolveStartupArea,
} from "./utils";

type OverlayView = "settings" | "now-playing" | null;
type SongHistoryMode = "current-program" | "full-station";

const DEFAULT_PLAYBACK_STATE: PlaybackState = {
  currentStation: null,
  currentProgram: null,
  isLoading: false,
  isPlaying: false,
  resolvedAreaId: "JP13",
  expiresAtEpochMillis: null,
  error: null,
};

const DETAILS_REFRESH_MS = 60_000;

export default function App() {
  const audioRef = useRef<HTMLAudioElement | null>(null);
  const hlsRef = useRef<Hls | null>(null);
  const currentStationRef = useRef<StationSummary | null>(null);
  const currentAreaRef = useRef("JP13");
  const reconnectRef = useRef<() => void>(() => undefined);
  const reconnectingRef = useRef(false);
  const playbackAttemptRef = useRef(0);
  const scheduleListRef = useRef<HTMLDivElement | null>(null);

  const [bootstrap, setBootstrap] = useState<BootstrapResponse | null>(null);
  const [settings, setSettings] = useState<WebSettings | null>(null);
  const [overlay, setOverlay] = useState<OverlayView>(null);
  const [selectedAreaId, setSelectedAreaId] = useState("JP13");
  const [searchInput, setSearchInput] = useState("");
  const [stations, setStations] = useState<StationSummary[]>([]);
  const [stationsLoading, setStationsLoading] = useState(false);
  const [bootstrapError, setBootstrapError] = useState<string | null>(null);
  const [playback, setPlayback] = useState<PlaybackState>(DEFAULT_PLAYBACK_STATE);
  const [nowPlaying, setNowPlaying] = useState<NowPlayingResponse | null>(null);
  const [schedule, setSchedule] = useState<ScheduleResponse | null>(null);
  const [detailsLoading, setDetailsLoading] = useState(false);
  const [detailsError, setDetailsError] = useState<string | null>(null);
  const [songHistoryMode, setSongHistoryMode] = useState<SongHistoryMode>("current-program");
  const [showAllSongs, setShowAllSongs] = useState(false);
  const [selectedScheduleDayIndex, setSelectedScheduleDayIndex] = useState(0);
  const [cacheClearMessage, setCacheClearMessage] = useState<string | null>(null);
  const [installPromptEvent, setInstallPromptEvent] = useState<DeferredInstallPromptEvent | null>(null);
  const [installGuideDismissed, setInstallGuideDismissed] = useState(false);

  const deferredSearch = useDeferredValue(searchInput);
  const language = settings?.language ?? "en";
  const extra = settingsCopy(language);
  const detailStation = playback.currentStation ?? findStation(bootstrap, settings?.lastStationId ?? null);
  const detailRequestStation = overlay === "now-playing" ? detailStation : null;
  const activeProgram = nowPlaying?.currentProgram ?? playback.currentProgram;
  const currentArea = bootstrap?.prefectures.find((item) => item.id === selectedAreaId) ?? null;
  const fixedArea = bootstrap?.prefectures.find((item) => item.id === settings?.fixedAreaId) ?? null;
  const resolvedArea = bootstrap?.prefectures.find((item) => item.id === playback.resolvedAreaId) ?? null;
  const playerProgramTitle = playback.currentStation != null
    ? activeProgram?.title ?? t(language, "playerIdleSubtitle")
    : t(language, "playerIdleSubtitle");

  const programDescriptionBlocks = useMemo(
    () => buildProgramDescriptionBlocks(activeProgram),
    [activeProgram],
  );
  const scheduleDays = useMemo(
    () => groupProgramsByCompactDay(schedule?.weeklyPrograms ?? []),
    [schedule?.weeklyPrograms],
  );
  const selectedSchedulePrograms = scheduleDays[selectedScheduleDayIndex]?.items ?? [];
  const currentSong = nowPlaying?.currentSong ?? nowPlaying?.recentSongs[0] ?? null;
  const historyBaseSongs = songHistoryMode === "current-program"
    ? nowPlaying?.currentProgramSongs ?? []
    : nowPlaying?.recentSongs ?? [];
  const historySongs = useMemo(
    () => historyBaseSongs.filter((song) => !sameSong(song, currentSong)),
    [currentSong, historyBaseSongs],
  );
  const visibleHistorySongs = showAllSongs ? historySongs : historySongs.slice(0, 4);
  const programArtworkUrl = getProgramArtworkUrl(activeProgram, detailStation);
  const installGuideKind = useMemo(
    () => detectInstallGuideKind(installPromptEvent != null),
    [installPromptEvent],
  );
  const installGuide = installGuideKind == null ? null : installGuideCopy(language, installGuideKind);
  const showInstallGuide = installGuide != null && !installGuideDismissed && !isStandaloneDisplayMode();

  useEffect(() => {
    let cancelled = false;

    async function loadBootstrap(): Promise<void> {
      try {
        setBootstrapError(null);
        const response = await api.getBootstrap();
        if (cancelled) {
          return;
        }

        const loadedSettings = loadSettings(response.defaultAreaId);
        const startupAreaId = resolveStartupArea(loadedSettings, response.defaultAreaId);
        setBootstrap(response);
        setSettings(loadedSettings);
        setSelectedAreaId(startupAreaId);
        setPlayback((previous) => ({
          ...previous,
          resolvedAreaId: loadedSettings.lastAreaId || startupAreaId,
        }));
      } catch (error) {
        if (!cancelled) {
          setBootstrapError(getErrorMessage(error));
        }
      }
    }

    void loadBootstrap();

    return () => {
      cancelled = true;
    };
  }, []);

  useEffect(() => {
    try {
      setInstallGuideDismissed(window.localStorage.getItem(INSTALL_GUIDE_DISMISSED_KEY) === "1");
    } catch {
      setInstallGuideDismissed(false);
    }

    const handleBeforeInstallPrompt = (event: Event) => {
      event.preventDefault();
      setInstallPromptEvent(event as DeferredInstallPromptEvent);
    };
    const handleAppInstalled = () => {
      setInstallPromptEvent(null);
      dismissInstallGuide();
    };

    window.addEventListener("beforeinstallprompt", handleBeforeInstallPrompt as EventListener);
    window.addEventListener("appinstalled", handleAppInstalled);
    return () => {
      window.removeEventListener("beforeinstallprompt", handleBeforeInstallPrompt as EventListener);
      window.removeEventListener("appinstalled", handleAppInstalled);
    };
  }, []);

  useEffect(() => {
    currentStationRef.current = playback.currentStation;
  }, [playback.currentStation]);

  useEffect(() => {
    currentAreaRef.current = selectedAreaId;
  }, [selectedAreaId]);

  useEffect(() => {
    if (settings == null) {
      return;
    }
    persistSettings(settings);
    applyTheme(settings.themeMode);
  }, [settings]);

  useEffect(() => {
    if (settings?.startupAreaMode === "fixed" && settings.fixedAreaId !== selectedAreaId) {
      setSelectedAreaId(settings.fixedAreaId);
    }
  }, [selectedAreaId, settings?.fixedAreaId, settings?.startupAreaMode]);

  useEffect(() => {
    if (cacheClearMessage == null) {
      return;
    }

    const timeoutId = window.setTimeout(() => setCacheClearMessage(null), 2400);
    return () => window.clearTimeout(timeoutId);
  }, [cacheClearMessage]);

  useEffect(() => {
    setSongHistoryMode("current-program");
    setShowAllSongs(false);
  }, [detailStation?.id]);

  useEffect(() => {
    if (scheduleDays.length === 0) {
      setSelectedScheduleDayIndex(0);
      return;
    }

    const currentDayIndex = scheduleDays.findIndex((group) =>
      activeProgram != null && activeProgram.startAt.startsWith(group.dayKey),
    );
    if (currentDayIndex >= 0) {
      setSelectedScheduleDayIndex(currentDayIndex);
      return;
    }

    setSelectedScheduleDayIndex((previous) => clamp(previous, 0, scheduleDays.length - 1));
  }, [activeProgram, scheduleDays]);

  useEffect(() => {
    if (overlay !== "now-playing") {
      return;
    }

    const list = scheduleListRef.current;
    if (list == null) {
      return;
    }

    const frameId = window.requestAnimationFrame(() => {
      const currentElement = list.querySelector<HTMLElement>('[data-current-program="true"]');
      if (currentElement != null) {
        const targetElement = currentElement.previousElementSibling instanceof HTMLElement
          ? currentElement.previousElementSibling
          : currentElement;
        const listRect = list.getBoundingClientRect();
        const targetRect = targetElement.getBoundingClientRect();
        const targetTop = Math.max(list.scrollTop + (targetRect.top - listRect.top), 0);
        list.scrollTop = targetTop;
      } else {
        list.scrollTop = 0;
      }
    });

    return () => window.cancelAnimationFrame(frameId);
  }, [overlay, activeProgram, selectedScheduleDayIndex, selectedSchedulePrograms]);

  useEffect(() => {
    const audio = audioRef.current;
    if (audio == null) {
      return;
    }

    const handlePlaying = (): void => {
      setPlayback((previous) => ({
        ...previous,
        isLoading: false,
        isPlaying: true,
        error: null,
      }));
    };
    const handlePause = (): void => {
      setPlayback((previous) => ({
        ...previous,
        isLoading: false,
        isPlaying: false,
      }));
    };
    const handleWaiting = (): void => {
      setPlayback((previous) =>
        previous.currentStation == null
          ? previous
          : {
              ...previous,
              isLoading: true,
            },
      );
    };
    const handleError = (): void => {
      reconnectRef.current();
    };

    audio.addEventListener("playing", handlePlaying);
    audio.addEventListener("pause", handlePause);
    audio.addEventListener("waiting", handleWaiting);
    audio.addEventListener("error", handleError);

    return () => {
      audio.removeEventListener("playing", handlePlaying);
      audio.removeEventListener("pause", handlePause);
      audio.removeEventListener("waiting", handleWaiting);
      audio.removeEventListener("error", handleError);
    };
  }, []);

  useEffect(() => {
    return () => {
      if (audioRef.current != null) {
        hlsRef.current = cleanupAudio(audioRef.current, hlsRef.current);
      }
    };
  }, []);

  useEffect(() => {
    if (bootstrap == null) {
      return;
    }

    let cancelled = false;

    async function loadStations(): Promise<void> {
      try {
        setStationsLoading(true);
        const response = await api.getStations(selectedAreaId, deferredSearch);
        if (!cancelled) {
          setStations(response);
        }
      } catch (error) {
        if (!cancelled) {
          setBootstrapError(getErrorMessage(error));
          setStations([]);
        }
      } finally {
        if (!cancelled) {
          setStationsLoading(false);
        }
      }
    }

    void loadStations();

    return () => {
      cancelled = true;
    };
  }, [bootstrap, deferredSearch, selectedAreaId]);

  useEffect(() => {
    if (detailRequestStation == null) {
      setNowPlaying(null);
      setSchedule(null);
      setDetailsError(null);
      setDetailsLoading(false);
      return;
    }

    const stationId = detailRequestStation.id;
    let cancelled = false;
    const timerId = window.setInterval(() => {
      void loadDetails();
    }, DETAILS_REFRESH_MS);

    async function loadDetails(): Promise<void> {
      try {
        setDetailsLoading(true);
        setDetailsError(null);
        const [nowPlayingResponse, scheduleResponse] = await Promise.all([
          api.getNowPlaying(stationId),
          api.getSchedule(stationId),
        ]);

        if (cancelled) {
          return;
        }

        setNowPlaying(nowPlayingResponse);
        setSchedule(scheduleResponse);
        if (currentStationRef.current?.id === stationId) {
          setPlayback((previous) => ({
            ...previous,
            currentProgram: nowPlayingResponse.currentProgram,
          }));
        }
      } catch (error) {
        if (!cancelled) {
          setDetailsError(getErrorMessage(error));
        }
      } finally {
        if (!cancelled) {
          setDetailsLoading(false);
        }
      }
    }

    void loadDetails();

    return () => {
      cancelled = true;
      window.clearInterval(timerId);
    };
  }, [detailRequestStation?.id]);

  async function startPlayback(station: StationSummary, preferredAreaId: string): Promise<void> {
    const audio = audioRef.current;
    if (audio == null) {
      return;
    }

    const requestId = ++playbackAttemptRef.current;

    setPlayback((previous) => ({
      ...previous,
      currentStation: station,
      currentProgram: previous.currentProgram,
      isLoading: true,
      error: null,
    }));

    try {
      const session = await api.createLiveSession({
        stationId: station.id,
        preferredAreaId,
      });
      if (requestId !== playbackAttemptRef.current) {
        return;
      }

      let attachedPlayer = hlsRef.current;
      let playbackError: unknown = null;
      const candidates = buildPlaybackCandidates(audio, session.streamUrl);

      for (const candidateUrl of candidates) {
        try {
          attachedPlayer = await attachLiveStream(audio, candidateUrl, attachedPlayer, () => {
            reconnectRef.current();
          });

          if (requestId !== playbackAttemptRef.current) {
            return;
          }

          await playAudioElement(audio);
          playbackError = null;
          break;
        } catch (error) {
          playbackError = error;
          attachedPlayer = cleanupAudio(audio, attachedPlayer);
        }
      }

      if (playbackError != null) {
        throw playbackError;
      }

      hlsRef.current = attachedPlayer;

      if (requestId !== playbackAttemptRef.current) {
        return;
      }

      setPlayback({
        currentStation: station,
        currentProgram: session.currentProgram,
        isLoading: false,
        isPlaying: true,
        resolvedAreaId: session.resolvedAreaId,
        expiresAtEpochMillis: session.expiresAtEpochMillis,
        error: null,
      });
      setSettings((previous) =>
        previous == null
          ? previous
          : {
              ...previous,
              lastAreaId: session.resolvedAreaId,
              lastStationId: station.id,
            },
      );
    } catch (error) {
      if (requestId !== playbackAttemptRef.current && isIgnorablePlaybackError(error)) {
        return;
      }
      if (isIgnorablePlaybackError(error)) {
        return;
      }

      hlsRef.current = cleanupAudio(audio, hlsRef.current);
      setPlayback((previous) => ({
        ...previous,
        currentStation: station,
        currentProgram: null,
        isLoading: false,
        isPlaying: false,
        error: error instanceof UnsupportedPlaybackError
          ? unsupportedPlaybackCopy(language)
          : getErrorMessage(error) || t(language, "playbackFailed"),
      }));
    }
  }

  async function reconnectCurrentStation(): Promise<void> {
    const station = currentStationRef.current;
    if (station == null || reconnectingRef.current) {
      return;
    }

    reconnectingRef.current = true;
    setPlayback((previous) => ({
      ...previous,
      currentStation: station,
      isLoading: true,
      error: t(language, "reconnecting"),
    }));

    try {
      await startPlayback(station, currentAreaRef.current);
    } finally {
      reconnectingRef.current = false;
    }
  }

  reconnectRef.current = () => {
    void reconnectCurrentStation();
  };

  function stopPlayback(): void {
    playbackAttemptRef.current += 1;
    reconnectingRef.current = false;
    if (audioRef.current != null) {
      hlsRef.current = cleanupAudio(audioRef.current, hlsRef.current);
    }
    setPlayback((previous) => ({
      ...previous,
      isLoading: false,
      isPlaying: false,
      error: null,
    }));
  }

  function updateSettings(updater: (previous: WebSettings) => WebSettings): void {
    setSettings((previous) => {
      if (previous == null) {
        return previous;
      }
      return updater(previous);
    });
  }

  function changeArea(nextAreaId: string): void {
    setSelectedAreaId(nextAreaId);
    updateSettings((previous) => ({
      ...previous,
      lastAreaId: nextAreaId,
    }));
  }

  function togglePlayback(station: StationSummary): void {
    if (playback.currentStation?.id === station.id && playback.isPlaying) {
      stopPlayback();
      return;
    }
    void startPlayback(station, selectedAreaId);
  }

  function togglePlaybackFromBar(): void {
    if (playback.currentStation == null) {
      return;
    }
    if (playback.isPlaying) {
      stopPlayback();
    } else {
      void startPlayback(playback.currentStation, selectedAreaId);
    }
  }

  function resumeLastStation(): void {
    const station = findStation(bootstrap, settings?.lastStationId ?? null);
    if (station == null) {
      return;
    }
    void startPlayback(station, selectedAreaId);
  }

  async function clearLocalCaches(): Promise<void> {
    try {
      if ("caches" in window) {
        const cacheNames = await window.caches.keys();
        await Promise.all(cacheNames.map((name) => window.caches.delete(name)));
      }
      setCacheClearMessage(extra.cacheCleared);
    } catch {
      setCacheClearMessage(extra.cacheClearFailed);
    }
  }

  function dismissInstallGuide(): void {
    setInstallGuideDismissed(true);
    try {
      window.localStorage.setItem(INSTALL_GUIDE_DISMISSED_KEY, "1");
    } catch {
      // Ignore localStorage write failures and keep the in-memory dismissal.
    }
  }

  async function promptInstall(): Promise<void> {
    if (installPromptEvent == null) {
      return;
    }

    await installPromptEvent.prompt();
    const choice = await installPromptEvent.userChoice;
    if (choice.outcome === "accepted") {
      dismissInstallGuide();
      setInstallPromptEvent(null);
    }
  }

  if (bootstrapError && bootstrap == null) {
    return (
      <div className="boot-error">
        <h1>Radikall Web</h1>
        <p>{bootstrapError}</p>
      </div>
    );
  }

  return (
    <div className={`app-shell ${settings?.drivingModeEnabled ? "driving" : ""}`}>
      <audio ref={audioRef} preload="metadata" />

      <header className="home-header">
        <div className="brand-row">
          <img className="brand-logo" src="/logo3.png" alt="Radikall" />
          <div className="brand-actions">
            <button
              type="button"
              className="icon-circle-button"
              onClick={() => startTransition(() => setOverlay("settings"))}
              aria-label={t(language, "openSettings")}
            >
              <SettingsIcon />
            </button>
            <AreaPicker
              bootstrap={bootstrap}
              currentLabel={currentArea ? localizeText(currentArea.names, language) : selectedAreaId}
              language={language}
              value={selectedAreaId}
              onSelect={changeArea}
            />
          </div>
        </div>

        {!settings?.drivingModeEnabled ? (
          <label className="search-strip">
            <input
              type="search"
              value={searchInput}
              onChange={(event) => setSearchInput(event.target.value)}
              placeholder={t(language, "searchPlaceholder")}
            />
          </label>
        ) : null}
      </header>

      <div className="home-divider" />

      {showInstallGuide && installGuide != null ? (
        <section className="install-guide-card" aria-label={installGuide.title}>
          <div className="install-guide-copy">
            <span className="install-guide-kicker">PWA</span>
            <h2>{installGuide.title}</h2>
            <p>{installGuide.body}</p>
            <ol className="install-guide-steps">
              {installGuide.steps.map((step) => (
                <li key={step}>{step}</li>
              ))}
            </ol>
          </div>
          <div className="install-guide-actions">
            {installGuide.actionLabel && installPromptEvent != null ? (
              <button type="button" className="install-guide-primary" onClick={() => void promptInstall()}>
                {installGuide.actionLabel}
              </button>
            ) : null}
            <button type="button" className="install-guide-secondary" onClick={dismissInstallGuide}>
              {installGuide.dismissLabel}
            </button>
          </div>
        </section>
      ) : null}

      {playback.error ? (
        <div className="status-banner" role="alert">
          <span>{playback.error}</span>
          <button type="button" onClick={() => setPlayback((previous) => ({ ...previous, error: null }))}>
            {t(language, "removeError")}
          </button>
        </div>
      ) : null}

      <main className="station-stage">
        {stationsLoading ? <p className="helper-text">{t(language, "searching")}</p> : null}
        {!stationsLoading && stations.length === 0 ? <p className="helper-text">{t(language, "noStations")}</p> : null}

        <div className="station-grid">
          {stations.map((station) => {
            const isCurrent = playback.currentStation?.id === station.id;
            return (
              <button
                key={station.id}
                type="button"
                className={`station-card ${isCurrent ? "active" : ""}`}
                onClick={() => void startPlayback(station, selectedAreaId)}
              >
                <div className="station-card-logo">
                  <img
                    src={station.logoUrl}
                    alt={station.name}
                    loading="lazy"
                    referrerPolicy="no-referrer"
                  />
                </div>
                <span className="station-card-id">{station.id}</span>
                <strong className="station-card-name">{station.name}</strong>
              </button>
            );
          })}
        </div>
      </main>

      <footer className="player-dock">
        <div className="player-bar">
          <button
            type="button"
            className="player-bar-main"
            disabled={detailStation == null}
            onClick={() => {
              if (detailStation != null) {
                startTransition(() => setOverlay("now-playing"));
              }
            }}
          >
            <span className="player-bar-title">
              {playback.currentStation?.name ?? t(language, "playerIdleTitle")}
            </span>
            <span className="player-bar-subtitle">{playerProgramTitle}</span>
          </button>
          <button
            type="button"
            className="player-bar-action"
            disabled={playback.currentStation == null || playback.isLoading}
            onClick={togglePlaybackFromBar}
          >
            {playback.isLoading ? t(language, "loading") : playback.isPlaying ? t(language, "stop") : t(language, "play")}
          </button>
        </div>
      </footer>

      {overlay === "settings" ? (
        <div className="overlay-shell">
          <div className="overlay-backdrop" onClick={() => setOverlay(null)} />
          <section className="overlay-panel settings-panel">
            <div className="settings-shell">
              <header className="settings-topbar">
                <button type="button" className="icon-circle-button" onClick={() => setOverlay(null)}>
                  <BackArrowIcon />
                </button>
                <div className="settings-heading">
                  <h2>{t(language, "settingsTitle")}</h2>
                  {t(language, "settingsSubtitle") ? <p>{t(language, "settingsSubtitle")}</p> : null}
                </div>
              </header>

              {cacheClearMessage ? <div className="settings-banner">{cacheClearMessage}</div> : null}

              <SettingsSectionCard title={extra.languageSectionTitle} description={extra.languageSectionDescription}>
                <SettingsChoiceGroup title={t(language, "language")} subtitle="">
                  {Object.entries(languageLabels).map(([value, label]) => (
                    <PillButton
                      key={value}
                      selected={settings?.language === value}
                      onClick={() =>
                        updateSettings((previous) => ({
                          ...previous,
                          language: value as LanguageCode,
                        }))}
                    >
                      {label}
                    </PillButton>
                  ))}
                </SettingsChoiceGroup>
              </SettingsSectionCard>

              <SettingsSectionCard title={extra.regionSectionTitle} description={extra.regionSectionDescription}>
                <SettingsChoiceGroup title={t(language, "startupArea")} subtitle="">
                  <PillButton
                    selected={settings?.startupAreaMode === "remember-last"}
                    onClick={() =>
                      updateSettings((previous) => ({
                        ...previous,
                        startupAreaMode: "remember-last",
                      }))}
                  >
                    {t(language, "rememberLastArea")}
                  </PillButton>
                  <PillButton
                    selected={settings?.startupAreaMode === "fixed"}
                    onClick={() =>
                      updateSettings((previous) => ({
                        ...previous,
                        startupAreaMode: "fixed",
                      }))}
                  >
                    {t(language, "fixedArea")}
                  </PillButton>
                </SettingsChoiceGroup>

                {settings?.startupAreaMode === "fixed" ? (
                  <SettingsSelectCard title={t(language, "fixedArea")}>
                    <AreaPicker
                      bootstrap={bootstrap}
                      className="settings-area-picker"
                      currentLabel={fixedArea ? localizeText(fixedArea.names, language) : settings.fixedAreaId}
                      language={language}
                      value={settings.fixedAreaId}
                      onSelect={(nextAreaId) =>
                        updateSettings((previous) => ({
                          ...previous,
                          fixedAreaId: nextAreaId,
                        }))}
                    />
                  </SettingsSelectCard>
                ) : null}
              </SettingsSectionCard>

              <SettingsSectionCard title={extra.appearanceSectionTitle} description={extra.appearanceSectionDescription}>
                <SettingsChoiceGroup title={t(language, "theme")} subtitle="">
                  <PillButton
                    selected={settings?.themeMode === "system"}
                    onClick={() =>
                      updateSettings((previous) => ({
                        ...previous,
                        themeMode: "system" as ThemeMode,
                      }))}
                  >
                    {t(language, "system")}
                  </PillButton>
                  <PillButton
                    selected={settings?.themeMode === "light"}
                    onClick={() =>
                      updateSettings((previous) => ({
                        ...previous,
                        themeMode: "light" as ThemeMode,
                      }))}
                  >
                    {t(language, "light")}
                  </PillButton>
                  <PillButton
                    selected={settings?.themeMode === "dark"}
                    onClick={() =>
                      updateSettings((previous) => ({
                        ...previous,
                        themeMode: "dark" as ThemeMode,
                      }))}
                  >
                    {t(language, "dark")}
                  </PillButton>
                </SettingsChoiceGroup>

                <SettingsToggleCard
                  title={t(language, "drivingMode")}
                  subtitle=""
                  checked={settings?.drivingModeEnabled ?? false}
                  onChange={(checked) =>
                    updateSettings((previous) => ({
                      ...previous,
                      drivingModeEnabled: checked,
                    }))}
                />
              </SettingsSectionCard>

              <SettingsSectionCard title={extra.generalSectionTitle} description={extra.generalSectionDescription}>
                <button type="button" className="settings-action-card" onClick={() => void clearLocalCaches()}>
                  <strong>{extra.clearCacheTitle}</strong>
                </button>
                <article className="settings-about-card">
                  <strong className="settings-about-title">{extra.aboutTitle}</strong>
                  <p className="settings-about-version">{extra.aboutBody}</p>

                  <SettingsAboutBlock
                    title={extra.aboutOpenSourceLabel}
                    body={extra.aboutOpenSourceBody}
                    accent="red"
                  />
                  <SettingsAboutLink href={extra.aboutRepoUrl}>{extra.aboutRepoLabel}</SettingsAboutLink>
                  <SettingsAboutLink href={extra.aboutCheckUpdateUrl}>{extra.aboutCheckUpdate}</SettingsAboutLink>

                  <SettingsAboutBlock
                    title={extra.aboutSiteLabel}
                    body={extra.aboutSiteBody}
                  />
                  <SettingsAboutLink href={extra.aboutSiteUrl}>baudstudio.com</SettingsAboutLink>

                  <SettingsAboutBlock
                    title={extra.aboutCreditsLabel}
                    body={extra.aboutCreditsBody}
                  />
                  <SettingsAboutLink href={extra.aboutCreditsUrl}>jackyzy823/rajiko</SettingsAboutLink>

                  <p className="settings-about-note">{extra.aboutFontNotice}</p>

                  <SettingsAboutBlock
                    title={extra.aboutDisclaimerLabel}
                    body={extra.aboutDisclaimerBody}
                    accent="blue"
                  />
                </article>
              </SettingsSectionCard>
            </div>
          </section>
        </div>
      ) : null}

      {overlay === "now-playing" ? (
        <div className="overlay-shell overlay-shell-blue">
          <div className="overlay-backdrop" onClick={() => setOverlay(null)} />
          <section className="overlay-panel now-playing-panel">
            <div className="now-playing-shell">
              <div className="sheet-handle" />

              <header className="now-playing-topbar">
                <button type="button" className="topbar-circle-button" onClick={() => setOverlay(null)}>
                  <ChevronDownIcon />
                </button>
                <div className="topbar-copy">
                  <span className="topbar-eyebrow">{t(language, "nowPlaying")}</span>
                  <h2>{detailStation?.name ?? t(language, "playerIdleTitle")}</h2>
                </div>
                <div className="topbar-logo-chip">
                  {detailStation ? (
                    <img
                      src={detailStation.logoUrl}
                      alt={detailStation.name}
                      loading="lazy"
                      referrerPolicy="no-referrer"
                    />
                  ) : null}
                </div>
              </header>

              {detailStation == null ? (
                <p className="sheet-helper-text">{t(language, "chooseStation")}</p>
              ) : (
                <div className="now-playing-content">
                  <div className="now-playing-main-grid">
                    <section className="np-section-card np-program-card np-main-program">
                      <h3 className="np-card-title">
                        {activeProgram?.title ?? t(language, "noProgram")}
                      </h3>

                      <div className="np-program-layout">
                        <div className="np-artwork-frame">
                          {programArtworkUrl ? <img src={programArtworkUrl} alt={detailStation.name} /> : null}
                        </div>

                        <div className="np-meta-panel">
                          {activeProgram?.performer ? (
                            <div className="np-meta-block">
                              <span>{extra.performerLabel}</span>
                              <strong>{activeProgram.performer}</strong>
                            </div>
                          ) : null}

                          {activeProgram ? (
                            <div className="np-meta-block">
                              <span>{extra.onAirLabel}</span>
                              <strong>{formatDateTimeRange(activeProgram)}</strong>
                            </div>
                          ) : null}

                          <button
                            type="button"
                            className="player-bar-action np-play-action"
                            disabled={playback.isLoading}
                            onClick={() => togglePlayback(detailStation)}
                          >
                            {playback.isLoading ? t(language, "loading") : playback.isPlaying ? t(language, "stop") : t(language, "play")}
                          </button>
                        </div>
                      </div>
                    </section>

                    <aside className="np-section-card current-song-card np-main-song">
                      <h3>{extra.currentSongTitle}</h3>
                      {detailsLoading && currentSong == null ? (
                        <p className="np-muted-copy">{t(language, "loadingDetails")}</p>
                      ) : currentSong == null ? (
                        <p className="np-muted-copy">{t(language, "noSongs")}</p>
                      ) : (
                        <SongCardContent song={currentSong} compact={false} />
                      )}
                    </aside>

                    <section className="np-section np-main-description">
                      <div className="np-section-heading">
                        <h3>{extra.programDetailsTitle}</h3>
                      </div>
                      <div className="np-section-card np-description-card">
                        {programDescriptionBlocks.length === 0 ? (
                          <p className="np-muted-copy">{extra.noProgramDetails}</p>
                        ) : (
                          programDescriptionBlocks.map((block, index) => (
                            <p key={`${block.slice(0, 20)}-${index}`} className="np-body-copy">{block}</p>
                          ))
                        )}
                      </div>

                      {activeProgram?.url ? (
                        <div className="np-section-card np-link-card">
                          <h4>{t(language, "programWebsite")}</h4>
                          <p className="np-link-url">{activeProgram.url}</p>
                          <a href={activeProgram.url} target="_blank" rel="noreferrer">
                            {t(language, "openWebsite")}
                          </a>
                        </div>
                      ) : null}
                    </section>
                  </div>

                  {detailsError ? <p className="sheet-helper-text">{detailsError}</p> : null}

                  <section className="np-section">
                    <div className="np-section-heading">
                      <h3>{extra.historyTitle}</h3>
                      <p>
                        {songHistoryMode === "current-program" ? extra.currentProgramSongsDescription : extra.recentStationSongsDescription}
                      </p>
                    </div>

                    <div className="np-chip-row">
                      <ChipButton
                        selected={songHistoryMode === "current-program"}
                        onClick={() => {
                          setSongHistoryMode("current-program");
                          setShowAllSongs(false);
                        }}
                      >
                        {extra.currentProgramTab}
                      </ChipButton>
                      <ChipButton
                        selected={songHistoryMode === "full-station"}
                        onClick={() => {
                          setSongHistoryMode("full-station");
                          setShowAllSongs(false);
                        }}
                      >
                        {extra.fullStationTab}
                      </ChipButton>
                    </div>

                    <div className="np-section-card history-card">
                      {detailsLoading && historySongs.length === 0 ? (
                        <p className="np-muted-copy">{t(language, "loadingDetails")}</p>
                      ) : historySongs.length === 0 ? (
                        <p className="np-muted-copy">
                          {songHistoryMode === "current-program" ? extra.noCurrentProgramSongs : extra.noRecentStationSongs}
                        </p>
                      ) : (
                        <>
                          <div className="history-song-grid">
                            {visibleHistorySongs.map((song) => (
                              <article
                                key={`${song.title}-${song.artist}-${song.stampDate}`}
                                className="history-song-card"
                              >
                                <SongCardContent song={song} compact />
                              </article>
                            ))}
                          </div>

                          {historySongs.length > 4 ? (
                            <button
                              type="button"
                              className="text-link-button"
                              onClick={() => setShowAllSongs((previous) => !previous)}
                            >
                              {showAllSongs ? extra.collapseLabel : extra.showMoreLabel}
                            </button>
                          ) : null}
                        </>
                      )}
                    </div>
                  </section>

                  <section className="np-section">
                    <div className="np-section-heading">
                      <h3>{extra.weeklyScheduleTitle}</h3>
                    </div>

                    <div className="np-section-card weekly-schedule-card">
                      <div className="np-chip-row schedule-day-row">
                        {scheduleDays.map((group, index) => (
                          <ChipButton
                            key={group.dayKey}
                            selected={index === selectedScheduleDayIndex}
                            onClick={() => setSelectedScheduleDayIndex(index)}
                          >
                            {formatScheduleDayLabel(group.dayKey, language)}
                          </ChipButton>
                        ))}
                      </div>

                      {detailsLoading && selectedSchedulePrograms.length === 0 ? (
                        <p className="np-muted-copy">{t(language, "loadingDetails")}</p>
                      ) : selectedSchedulePrograms.length === 0 ? (
                        <p className="np-muted-copy">{extra.noScheduleAvailable}</p>
                      ) : (
                        <div ref={scheduleListRef} className="schedule-program-list">
                          {selectedSchedulePrograms.map((program) => {
                            const isCurrent = sameProgram(program, activeProgram) || isProgramLiveNow(program);
                            const isPast = !isCurrent && isProgramPast(program, activeProgram ?? null);
                            return (
                              <article
                                key={`${program.stationId}-${program.startAt}`}
                                className={`schedule-program-row ${isCurrent ? "current" : ""} ${isPast ? "past" : ""}`}
                                data-current-program={isCurrent ? "true" : "false"}
                              >
                                <time>{formatTimeRange(program.startAt, program.endAt)}</time>
                                <div>
                                  <strong>{program.title}</strong>
                                  {program.performer ? <p>{program.performer}</p> : null}
                                </div>
                              </article>
                            );
                          })}
                        </div>
                      )}
                    </div>
                  </section>
                </div>
              )}
            </div>
          </section>
        </div>
      ) : null}
    </div>
  );
}

function SettingsSectionCard({
  title,
  description,
  children,
}: {
  title: string;
  description: string;
  children: ReactNode;
}) {
  return (
    <section className="settings-section-card">
      <h3>{title}</h3>
      {description ? <p>{description}</p> : null}
      <div className="settings-section-body">{children}</div>
    </section>
  );
}

function SettingsChoiceGroup({
  title,
  subtitle,
  children,
}: {
  title: string;
  subtitle: string;
  children: ReactNode;
}) {
  return (
    <div className="settings-choice-group">
      <div className="settings-row-copy">
        <strong>{title}</strong>
        {subtitle ? <span>{subtitle}</span> : null}
      </div>
      <div className="settings-pill-row">{children}</div>
    </div>
  );
}

function SettingsToggleCard({
  title,
  subtitle,
  checked,
  disabled = false,
  note,
  onChange,
}: {
  title: string;
  subtitle: string;
  checked: boolean;
  disabled?: boolean;
  note?: string;
  onChange: (checked: boolean) => void;
}) {
  return (
    <label className={`settings-toggle-card ${disabled ? "disabled" : ""}`}>
      <div className="settings-row-copy">
        <strong>{title}</strong>
        {subtitle ? <span>{subtitle}</span> : null}
        {note ? <em>{note}</em> : null}
      </div>
      <input type="checkbox" checked={checked} disabled={disabled} onChange={(event) => onChange(event.target.checked)} />
    </label>
  );
}

function SettingsNoteCard({
  title,
  body,
}: {
  title: string;
  body: string;
}) {
  return (
    <article className="settings-note-card">
      <strong>{title}</strong>
      <p>{body}</p>
    </article>
  );
}

function SettingsSelectCard({
  title,
  description,
  children,
}: {
  title: string;
  description?: string;
  children: ReactNode;
}) {
  return (
    <div className="settings-select-card">
      <strong>{title}</strong>
      {description ? <span>{description}</span> : null}
      {children}
    </div>
  );
}

function SettingsAboutBlock({
  title,
  body,
  accent,
}: {
  title: string;
  body: string;
  accent?: "blue" | "red";
}) {
  return (
    <div className={`settings-about-block ${accent != null ? `accent-${accent}` : ""}`}>
      <strong>{title}</strong>
      <p>{body}</p>
    </div>
  );
}

function SettingsAboutLink({
  href,
  children,
}: {
  href: string;
  children: ReactNode;
}) {
  return (
    <a className="settings-about-link" href={href} target="_blank" rel="noreferrer">
      {children}
    </a>
  );
}

function AreaPicker({
  bootstrap,
  className,
  currentLabel,
  language,
  value,
  onSelect,
}: {
  bootstrap: BootstrapResponse | null;
  className?: string;
  currentLabel: string;
  language: LanguageCode;
  value: string;
  onSelect: (areaId: string) => void;
}) {
  const [open, setOpen] = useState(false);
  const pickerRef = useRef<HTMLDivElement | null>(null);

  const groupedPrefectures = useMemo(() => {
    if (bootstrap == null) {
      return [];
    }

    return bootstrap.regions
      .map((region) => ({
        id: region.id,
        name: localizeText(region.names, language),
        items: bootstrap.prefectures.filter((prefecture) => prefecture.regionId === region.id),
      }))
      .filter((group) => group.items.length > 0);
  }, [bootstrap, language]);

  useEffect(() => {
    if (!open) {
      return;
    }

    function handlePointerDown(event: MouseEvent): void {
      if (pickerRef.current != null && !pickerRef.current.contains(event.target as Node)) {
        setOpen(false);
      }
    }

    function handleKeyDown(event: KeyboardEvent): void {
      if (event.key === "Escape") {
        setOpen(false);
      }
    }

    document.addEventListener("mousedown", handlePointerDown);
    document.addEventListener("keydown", handleKeyDown);
    return () => {
      document.removeEventListener("mousedown", handlePointerDown);
      document.removeEventListener("keydown", handleKeyDown);
    };
  }, [open]);

  return (
    <div ref={pickerRef} className={`area-picker ${open ? "open" : ""} ${className ?? ""}`.trim()}>
      <button
        type="button"
        className="area-pill area-pill-button"
        aria-haspopup="listbox"
        aria-expanded={open}
        onClick={() => setOpen((previous) => !previous)}
      >
        <span>{currentLabel}</span>
        <span className="area-pill-chevron" aria-hidden="true">
          <ChevronDownIcon compact />
        </span>
      </button>

      {open ? (
        <div className="area-menu-panel" role="listbox" aria-label={t(language, "startupArea")}>
          {groupedPrefectures.map((group) => (
            <section key={group.id} className="area-menu-section">
              <strong className="area-menu-region">{group.name}</strong>
              <div className="area-menu-items">
                {group.items.map((prefecture) => {
                  const selected = prefecture.id === value;
                  return (
                    <button
                      key={prefecture.id}
                      type="button"
                      className={`area-menu-item ${selected ? "selected" : ""}`}
                      onClick={() => {
                        onSelect(prefecture.id);
                        setOpen(false);
                      }}
                    >
                      {localizeText(prefecture.names, language)}
                    </button>
                  );
                })}
              </div>
            </section>
          ))}
        </div>
      ) : null}
    </div>
  );
}

function PillButton({
  children,
  selected,
  disabled = false,
  onClick,
}: {
  children: ReactNode;
  selected: boolean;
  disabled?: boolean;
  onClick: () => void;
}) {
  return (
    <button
      type="button"
      className={`pill-button ${selected ? "selected" : ""} ${disabled ? "disabled" : ""}`}
      disabled={disabled}
      onClick={onClick}
    >
      {children}
    </button>
  );
}

function ChipButton({
  children,
  selected,
  onClick,
}: {
  children: ReactNode;
  selected: boolean;
  onClick: () => void;
}) {
  return (
    <button type="button" className={`chip-button ${selected ? "selected" : ""}`} onClick={onClick}>
      {children}
    </button>
  );
}

function SongCardContent({
  song,
  compact,
}: {
  song: OnAirSong;
  compact: boolean;
}) {
  return (
    <div className={`song-card-content ${compact ? "compact" : ""}`}>
      <div className="song-artwork-frame">
        {song.imageUrl ? <img src={song.imageUrl} alt={song.title} loading="lazy" referrerPolicy="no-referrer" /> : null}
      </div>
      <div className="song-card-copy">
        <span className="song-artist">{song.artist}</span>
        <strong className="song-title">{song.title}</strong>
        <span className="song-date">{formatSongDate(song.stampDate)}</span>
        <span className="song-time">{formatSongTime(song.stampDate)}</span>
      </div>
    </div>
  );
}

function SettingsIcon() {
  return (
    <svg viewBox="0 0 24 24" aria-hidden="true">
      <circle cx="12" cy="12" r="4.2" fill="none" stroke="currentColor" strokeWidth="1.8" />
      <circle cx="12" cy="12" r="8" fill="none" stroke="currentColor" strokeWidth="1.8" />
      {[0, 45, 90, 135, 180, 225, 270, 315].map((angle) => {
        const radians = angle * Math.PI / 180;
        const x1 = 12 + Math.cos(radians) * 8;
        const y1 = 12 + Math.sin(radians) * 8;
        const x2 = 12 + Math.cos(radians) * 10.2;
        const y2 = 12 + Math.sin(radians) * 10.2;
        return (
          <line
            key={angle}
            x1={x1}
            y1={y1}
            x2={x2}
            y2={y2}
            stroke="currentColor"
            strokeWidth="1.8"
            strokeLinecap="round"
          />
        );
      })}
    </svg>
  );
}

function BackArrowIcon() {
  return (
    <svg viewBox="0 0 24 24" aria-hidden="true">
      <path d="M15.5 5.5L8.5 12L15.5 18.5" fill="none" stroke="currentColor" strokeWidth="2.4" strokeLinecap="round" strokeLinejoin="round" />
    </svg>
  );
}

function ChevronDownIcon({ compact = false }: { compact?: boolean }) {
  return (
    <svg viewBox="0 0 24 24" aria-hidden="true" className={compact ? "compact-chevron" : undefined}>
      <path d="M5 8.5L12 15.5L19 8.5" fill="none" stroke="currentColor" strokeWidth={compact ? "2.1" : "2.8"} strokeLinecap="round" strokeLinejoin="round" />
    </svg>
  );
}

function buildProgramDescriptionBlocks(program: ProgramEntry | null | undefined): string[] {
  if (program == null) {
    return [];
  }

  return [program.description, program.info]
    .filter((value): value is string => value != null && value.trim().length > 0)
    .map(stripHtml)
    .filter((value) => value.length > 0);
}

function stripHtml(html: string): string {
  const normalizedHtml = decodeHtmlEntities(html)
    .replace(/<\s*(script|style)\b[^>]*>[\s\S]*?<\/\s*\1\s*>/gi, "")
    .replace(/<\s*br\s*\/?>/gi, "\n")
    .replace(/<\/\s*(p|div|tr|table|tbody|section|article|ul|ol|h[1-6])\s*>/gi, "\n")
    .replace(/<\s*li[^>]*>/gi, "\n- ")
    .replace(/<\s*(td|th)[^>]*>/gi, " ");

  const strippedHtml = typeof DOMParser === "undefined"
    ? normalizedHtml.replace(/<[^>]+>/g, "")
    : new DOMParser().parseFromString(normalizedHtml, "text/html").body.textContent ?? normalizedHtml.replace(/<[^>]+>/g, "");

  return decodeHtmlEntities(strippedHtml)
    .split("\n")
    .map(normalizeProgramDescriptionLine)
    .filter((line) => line.length > 0)
    .join("\n");
}

function normalizeProgramDescriptionLine(line: string): string {
  const urlPattern = /https?:\/\/\S+/giu;
  const hadUrl = urlPattern.test(line);
  let cleaned = line
    .replace(urlPattern, "")
    .replace(/[「」<>]/gu, "")
    .replace(/\s+/gu, " ")
    .trim();

  cleaned = cleaned.replace(/\s*[:：]\s*$/u, "").trim();

  if (cleaned.length === 0) {
    return "";
  }

  if (hadUrl && /(?:Webサイト|website|message form|メッセージフォーム|番組Webサイト|ハッシュタグ|account|アカウント|SNS)/iu.test(cleaned)) {
    return "";
  }

  return cleaned;
}

function decodeHtmlEntities(value: string): string {
  return value
    .replace(/&amp;/g, "&")
    .replace(/&lt;/g, "<")
    .replace(/&gt;/g, ">")
    .replace(/&quot;/g, "\"")
    .replace(/&#39;/g, "'")
    .replace(/&nbsp;/g, " ");
}

function getProgramArtworkUrl(program: ProgramEntry | null | undefined, station: StationSummary | null): string | null {
  if (program?.imageUrl) {
    return program.imageUrl;
  }
  if (station == null) {
    return null;
  }
  return `https://radiko.jp/v2/static/station/logo/${station.id}/224x100.png`;
}

function formatDateTimeRange(program: ProgramEntry): string {
  if (program.startAt.length < 12 || program.endAt.length < 12) {
    return formatTimeRange(program.startAt, program.endAt);
  }

  const month = Number.parseInt(program.startAt.slice(4, 6), 10);
  const day = Number.parseInt(program.startAt.slice(6, 8), 10);
  return `${month}/${day} ${formatTimeRange(program.startAt, program.endAt)}`;
}

function formatTimeRange(startAt: string, endAt: string): string {
  if (startAt.length < 12 || endAt.length < 12) {
    return `${startAt} - ${endAt}`;
  }
  return `${startAt.slice(8, 10)}:${startAt.slice(10, 12)} - ${endAt.slice(8, 10)}:${endAt.slice(10, 12)}`;
}

function formatSongDate(stampDate: string): string {
  const date = new Date(stampDate);
  if (Number.isNaN(date.getTime())) {
    return stampDate;
  }
  const month = date.getMonth() + 1;
  const day = date.getDate();
  return `${date.getFullYear()}/${month}/${day}`;
}

function formatSongTime(stampDate: string): string {
  const date = new Date(stampDate);
  if (Number.isNaN(date.getTime())) {
    return "";
  }
  return `${date.getHours().toString().padStart(2, "0")}:${date.getMinutes().toString().padStart(2, "0")}`;
}

function groupProgramsByCompactDay(programs: ProgramEntry[]): Array<{ dayKey: string; items: ProgramEntry[] }> {
  const map = new Map<string, ProgramEntry[]>();
  for (const program of programs) {
    const dayKey = program.startAt.slice(0, 8);
    const items = map.get(dayKey) ?? [];
    items.push(program);
    map.set(dayKey, items);
  }
  return Array.from(map.entries())
    .sort((a, b) => a[0].localeCompare(b[0]))
    .map(([dayKey, items]) => ({ dayKey, items }));
}

function formatScheduleDayLabel(dayKey: string, language: LanguageCode): string {
  if (dayKey.length < 8) {
    return dayKey;
  }

  const date = new Date(
    Number.parseInt(dayKey.slice(0, 4), 10),
    Number.parseInt(dayKey.slice(4, 6), 10) - 1,
    Number.parseInt(dayKey.slice(6, 8), 10),
  );
  const weekday = new Intl.DateTimeFormat(localeForLanguage(language), { weekday: "short" }).format(date);
  return `${date.getMonth() + 1}/${date.getDate()}(${weekday})`;
}

function sameProgram(program: ProgramEntry | null | undefined, current: ProgramEntry | null | undefined): boolean {
  return program != null && current != null && program.stationId === current.stationId && program.startAt === current.startAt;
}

function isProgramLiveNow(program: ProgramEntry): boolean {
  const start = parseCompactTimestampJst(program.startAt);
  const end = parseCompactTimestampJst(program.endAt);
  if (start == null || end == null) {
    return false;
  }
  const now = Date.now();
  return start.getTime() <= now && now < end.getTime();
}

function isProgramPast(program: ProgramEntry, currentProgram: ProgramEntry | null): boolean {
  return currentProgram != null &&
    program.startAt.slice(0, 8) === currentProgram.startAt.slice(0, 8) &&
    program.endAt <= currentProgram.startAt;
}

function parseCompactTimestampJst(value: string): Date | null {
  if (!/^\d{12}$/.test(value)) {
    return null;
  }

  const year = Number.parseInt(value.slice(0, 4), 10);
  const month = Number.parseInt(value.slice(4, 6), 10);
  const day = Number.parseInt(value.slice(6, 8), 10);
  const hour = Number.parseInt(value.slice(8, 10), 10);
  const minute = Number.parseInt(value.slice(10, 12), 10);
  return new Date(Date.UTC(year, month - 1, day, hour - 9, minute));
}

function sameSong(song: OnAirSong | null | undefined, currentSong: OnAirSong | null | undefined): boolean {
  return song != null &&
    currentSong != null &&
    song.title === currentSong.title &&
    song.artist === currentSong.artist &&
    song.stampDate === currentSong.stampDate;
}

function findStation(
  bootstrap: BootstrapResponse | null,
  stationId: string | null,
): StationSummary | null {
  if (bootstrap == null || stationId == null) {
    return null;
  }
  return bootstrap.stations.find((station) => station.id === stationId) ?? null;
}

function getErrorMessage(error: unknown): string {
  if (error instanceof Error && error.message.trim()) {
    return error.message;
  }
  return "Unexpected error";
}

function isIgnorablePlaybackError(error: unknown): boolean {
  if (!(error instanceof Error)) {
    return false;
  }
  const message = error.message.toLowerCase();
  return message.includes("interrupted by a new load request") || message.includes("aborterror");
}

function clamp(value: number, min: number, max: number): number {
  return Math.min(Math.max(value, min), max);
}

function localeForLanguage(language: LanguageCode): string {
  switch (language) {
    case "zh-CN":
      return "zh-CN";
    case "zh-TW":
      return "zh-TW";
    case "ja":
      return "ja-JP";
    case "ko":
      return "ko-KR";
    default:
      return "en-US";
  }
}

function settingsCopy(language: LanguageCode) {
  const table = {
    "zh-CN": {
      languageSectionTitle: "0. 语言",
      languageSectionDescription: "",
      regionSectionTitle: "1. 地域与解锁",
      regionSectionDescription: "",
      appearanceSectionTitle: "5. 外观与交互",
      appearanceSectionDescription: "",
      generalSectionTitle: "7. 系统常规",
      generalSectionDescription: "",
      clearCacheTitle: "清除缓存",
      cacheCleared: "Web 缓存已清除。",
      cacheClearFailed: "清除 Web 缓存失败。",
      aboutTitle: "关于 / 开源许可",
      aboutBody: "版本 Web 预览版",
      aboutOpenSourceLabel: "完全开源免费",
      aboutOpenSourceBody: "Radikall 完全开源且永久免费。任何以此应用收费的行为均为欺诈，请认准原始仓库。",
      aboutRepoLabel: "原始仓库",
      aboutRepoUrl: "https://github.com/baudmusic/radikall",
      aboutSiteLabel: "关注开发者",
      aboutSiteBody: "如果你喜欢这个应用，欢迎访问 baudstudio.com 并关注开发者。",
      aboutSiteUrl: "https://baudstudio.com",
      aboutCheckUpdate: "检查更新",
      aboutCheckUpdateUrl: "https://github.com/baudmusic/radikall/releases/latest",
      aboutCreditsLabel: "致谢",
      aboutCreditsBody: "本项目受益于 jackyzy823/rajiko 的思路与逆向工程工作。",
      aboutCreditsUrl: "https://github.com/jackyzy823/rajiko",
      aboutFontNotice: "界面使用 Noto Sans JP 字体，并遵循 SIL Open Font License 1.1 (OFL)。",
      aboutDisclaimerLabel: "免责声明",
      aboutDisclaimerBody: "Radikall 是非官方第三方客户端，与 Radiko Co., Ltd. 及其广播合作伙伴没有任何关联。仅供个人学习与技术研究使用。",
      performerLabel: "表演者",
      onAirLabel: "播出时间",
      currentSongTitle: "当前歌曲",
      programDetailsTitle: "节目详情",
      noProgramDetails: "当前没有可用的节目详情。",
      historyTitle: "歌曲记录",
      currentProgramSongsDescription: "当前节目内歌曲",
      recentStationSongsDescription: "全台最近歌曲",
      currentProgramTab: "当前节目",
      fullStationTab: "全台历史",
      noCurrentProgramSongs: "暂无当前节目歌曲记录。",
      noRecentStationSongs: "暂无全台最近歌曲记录。",
      showMoreLabel: "展开更多",
      collapseLabel: "收起",
      weeklyScheduleTitle: "周节目表",
      noScheduleAvailable: "暂无节目表。",
    },
    "zh-TW": {
      languageSectionTitle: "0. 語言",
      languageSectionDescription: "",
      regionSectionTitle: "1. 地域與解鎖",
      regionSectionDescription: "",
      appearanceSectionTitle: "5. 外觀與互動",
      appearanceSectionDescription: "",
      generalSectionTitle: "7. 系統常規",
      generalSectionDescription: "",
      clearCacheTitle: "清除快取",
      cacheCleared: "Web 快取已清除。",
      cacheClearFailed: "清除 Web 快取失敗。",
      aboutTitle: "關於 / 開源授權",
      aboutBody: "版本 Web 預覽版",
      aboutOpenSourceLabel: "完全開源免費",
      aboutOpenSourceBody: "Radikall 完全開源且永久免費。任何以此應用收費的行為均屬詐騙，請認準原始倉庫。",
      aboutRepoLabel: "原始倉庫",
      aboutRepoUrl: "https://github.com/baudmusic/radikall",
      aboutSiteLabel: "關注開發者",
      aboutSiteBody: "如果你喜歡這個應用，歡迎造訪 baudstudio.com 並關注開發者。",
      aboutSiteUrl: "https://baudstudio.com",
      aboutCheckUpdate: "檢查更新",
      aboutCheckUpdateUrl: "https://github.com/baudmusic/radikall/releases/latest",
      aboutCreditsLabel: "致謝",
      aboutCreditsBody: "本專案受益於 jackyzy823/rajiko 的構想與逆向工程工作。",
      aboutCreditsUrl: "https://github.com/jackyzy823/rajiko",
      aboutFontNotice: "介面使用 Noto Sans JP 字體，並遵循 SIL Open Font License 1.1 (OFL)。",
      aboutDisclaimerLabel: "免責聲明",
      aboutDisclaimerBody: "Radikall 是非官方第三方客戶端，與 Radiko Co., Ltd. 及其廣播合作夥伴沒有任何關聯。僅供個人學習與技術研究使用。",
      performerLabel: "演出者",
      onAirLabel: "播出時間",
      currentSongTitle: "目前歌曲",
      programDetailsTitle: "節目詳情",
      noProgramDetails: "目前沒有可用的節目詳情。",
      historyTitle: "歌曲紀錄",
      currentProgramSongsDescription: "目前節目內歌曲",
      recentStationSongsDescription: "全台最近歌曲",
      currentProgramTab: "目前節目",
      fullStationTab: "全台歷史",
      noCurrentProgramSongs: "暫無目前節目歌曲紀錄。",
      noRecentStationSongs: "暫無全台最近歌曲紀錄。",
      showMoreLabel: "展開更多",
      collapseLabel: "收起",
      weeklyScheduleTitle: "週節目表",
      noScheduleAvailable: "暫無節目表。",
    },
    en: {
      languageSectionTitle: "0. Language",
      languageSectionDescription: "",
      regionSectionTitle: "1. Region & Unlock",
      regionSectionDescription: "",
      appearanceSectionTitle: "5. Appearance & Interaction",
      appearanceSectionDescription: "",
      generalSectionTitle: "7. General",
      generalSectionDescription: "",
      clearCacheTitle: "Clear cache",
      cacheCleared: "Web cache cleared.",
      cacheClearFailed: "Unable to clear Web cache.",
      aboutTitle: "About / Open-source notices",
      aboutBody: "Version Web Preview",
      aboutOpenSourceLabel: "Free and Open Source",
      aboutOpenSourceBody: "Radikall is completely free and open source. Any paid version of this app is fraudulent.",
      aboutRepoLabel: "Source Repository",
      aboutRepoUrl: "https://github.com/baudmusic/radikall",
      aboutSiteLabel: "Follow the Developer",
      aboutSiteBody: "If you enjoy this app, please visit baudstudio.com and follow the developer.",
      aboutSiteUrl: "https://baudstudio.com",
      aboutCheckUpdate: "Check for Updates",
      aboutCheckUpdateUrl: "https://github.com/baudmusic/radikall/releases/latest",
      aboutCreditsLabel: "Credits",
      aboutCreditsBody: "This project builds on the ideas and reverse-engineering work of jackyzy823/rajiko.",
      aboutCreditsUrl: "https://github.com/jackyzy823/rajiko",
      aboutFontNotice: "The interface uses Noto Sans JP under the SIL Open Font License 1.1 (OFL).",
      aboutDisclaimerLabel: "Disclaimer",
      aboutDisclaimerBody: "Radikall is an unofficial third-party client, unaffiliated with Radiko Co., Ltd. or its broadcasting partners. For personal study and technical research only.",
      performerLabel: "Performer",
      onAirLabel: "On Air",
      currentSongTitle: "Current Song",
      programDetailsTitle: "Program Details",
      noProgramDetails: "No program details are available right now.",
      historyTitle: "Song History",
      currentProgramSongsDescription: "Songs in the current program",
      recentStationSongsDescription: "Recent songs from the station",
      currentProgramTab: "Current program",
      fullStationTab: "Full station",
      noCurrentProgramSongs: "No songs are listed for the current program yet.",
      noRecentStationSongs: "No recent station songs are available yet.",
      showMoreLabel: "Show more",
      collapseLabel: "Collapse",
      weeklyScheduleTitle: "Weekly Schedule",
      noScheduleAvailable: "No schedule is available.",
    },
    ja: {
      languageSectionTitle: "0. 言語",
      languageSectionDescription: "",
      regionSectionTitle: "1. 地域と解放",
      regionSectionDescription: "",
      appearanceSectionTitle: "5. 外観と操作",
      appearanceSectionDescription: "",
      generalSectionTitle: "7. 一般",
      generalSectionDescription: "",
      clearCacheTitle: "キャッシュを削除",
      cacheCleared: "Web キャッシュを削除しました。",
      cacheClearFailed: "Web キャッシュを削除できませんでした。",
      aboutTitle: "情報 / オープンソース表記",
      aboutBody: "バージョン Webプレビュー",
      aboutOpenSourceLabel: "完全無料・オープンソース",
      aboutOpenSourceBody: "Radikall は完全無料のオープンソースです。このアプリの有料版を名乗るものは詐欺です。必ず元のリポジトリを確認してください。",
      aboutRepoLabel: "元のリポジトリ",
      aboutRepoUrl: "https://github.com/baudmusic/radikall",
      aboutSiteLabel: "開発者をフォロー",
      aboutSiteBody: "このアプリが気に入ったら、baudstudio.com を訪れて開発者をフォローしてください。",
      aboutSiteUrl: "https://baudstudio.com",
      aboutCheckUpdate: "アップデートを確認",
      aboutCheckUpdateUrl: "https://github.com/baudmusic/radikall/releases/latest",
      aboutCreditsLabel: "クレジット",
      aboutCreditsBody: "このプロジェクトは jackyzy823/rajiko の発想とリバースエンジニアリング作業に基づいています。",
      aboutCreditsUrl: "https://github.com/jackyzy823/rajiko",
      aboutFontNotice: "UI では Noto Sans JP を使用し、SIL Open Font License 1.1 (OFL) に従っています。",
      aboutDisclaimerLabel: "免責事項",
      aboutDisclaimerBody: "Radikall は非公式の第三者クライアントであり、Radiko Co., Ltd. およびその放送パートナーとは一切関係ありません。個人利用と技術研究のみを想定しています。",
      performerLabel: "出演者",
      onAirLabel: "放送時間",
      currentSongTitle: "現在の楽曲",
      programDetailsTitle: "番組詳細",
      noProgramDetails: "現在利用できる番組詳細はありません。",
      historyTitle: "楽曲履歴",
      currentProgramSongsDescription: "現在の番組内の楽曲",
      recentStationSongsDescription: "局全体の最近の楽曲",
      currentProgramTab: "現在の番組",
      fullStationTab: "局全体",
      noCurrentProgramSongs: "現在の番組の楽曲履歴はまだありません。",
      noRecentStationSongs: "局全体の最近の楽曲履歴はまだありません。",
      showMoreLabel: "もっと見る",
      collapseLabel: "折りたたむ",
      weeklyScheduleTitle: "週間番組表",
      noScheduleAvailable: "番組表はまだありません。",
    },
    ko: {
      languageSectionTitle: "0. 언어",
      languageSectionDescription: "",
      regionSectionTitle: "1. 지역 및 해제",
      regionSectionDescription: "",
      appearanceSectionTitle: "5. 외관 및 상호작용",
      appearanceSectionDescription: "",
      generalSectionTitle: "7. 일반",
      generalSectionDescription: "",
      clearCacheTitle: "캐시 지우기",
      cacheCleared: "Web 캐시를 지웠습니다.",
      cacheClearFailed: "Web 캐시를 지울 수 없습니다.",
      aboutTitle: "정보 / 오픈소스 안내",
      aboutBody: "버전 Web 프리뷰",
      aboutOpenSourceLabel: "완전 무료 오픈소스",
      aboutOpenSourceBody: "Radikall은 완전 무료 오픈소스입니다. 이 앱의 유료판을 표방하는 것은 사기입니다. 반드시 원본 저장소를 확인하세요.",
      aboutRepoLabel: "원본 저장소",
      aboutRepoUrl: "https://github.com/baudmusic/radikall",
      aboutSiteLabel: "개발자 팔로우",
      aboutSiteBody: "이 앱이 마음에 들면 baudstudio.com을 방문해 개발자를 팔로우해 주세요.",
      aboutSiteUrl: "https://baudstudio.com",
      aboutCheckUpdate: "업데이트 확인",
      aboutCheckUpdateUrl: "https://github.com/baudmusic/radikall/releases/latest",
      aboutCreditsLabel: "크레딧",
      aboutCreditsBody: "이 프로젝트는 jackyzy823/rajiko의 아이디어와 리버스 엔지니어링 작업을 바탕으로 합니다.",
      aboutCreditsUrl: "https://github.com/jackyzy823/rajiko",
      aboutFontNotice: "인터페이스는 Noto Sans JP를 사용하며 SIL Open Font License 1.1 (OFL)을 따릅니다.",
      aboutDisclaimerLabel: "면책 조항",
      aboutDisclaimerBody: "Radikall은 비공식 서드파티 클라이언트이며 Radiko Co., Ltd. 및 방송 파트너와 아무 관련이 없습니다. 개인 학습과 기술 연구 용도만을 전제로 합니다.",
      performerLabel: "출연자",
      onAirLabel: "방송 시간",
      currentSongTitle: "현재 곡",
      programDetailsTitle: "프로그램 상세",
      noProgramDetails: "지금 표시할 수 있는 프로그램 상세가 없습니다.",
      historyTitle: "곡 기록",
      currentProgramSongsDescription: "현재 프로그램 내 곡",
      recentStationSongsDescription: "방송국 전체 최근 곡",
      currentProgramTab: "현재 프로그램",
      fullStationTab: "방송국 전체",
      noCurrentProgramSongs: "현재 프로그램 곡 기록이 아직 없습니다.",
      noRecentStationSongs: "방송국 전체 최근 곡 기록이 아직 없습니다.",
      showMoreLabel: "더 보기",
      collapseLabel: "접기",
      weeklyScheduleTitle: "주간 편성표",
      noScheduleAvailable: "표시할 편성표가 없습니다.",
    },
  } as const;

  return table[language];
}

function legacyExtraCopy(language: LanguageCode) {
  const zh = {
    languageSectionTitle: "0. 语言",
    languageSectionDescription: "与现有 Android / Windows 版一致的语言切换入口。",
    languageChoiceSubtitle: "切换应用文案语言。",
    regionSectionTitle: "1. 地区",
    regionSectionDescription: "保持与原版一致的启动地区与固定地区结构。",
    playbackSectionTitle: "2. 播放",
    playbackSectionDescription: "Web 版保留原版结构，但对浏览器做了能力降级说明。",
    autoPlayTitle: "启动自动播放",
    autoPlayBody: "iPhone PWA 无法在无手势前提下稳定自动播放，因此此项在 Web 版固定为手动继续播放。",
    timerSectionTitle: "3. 定时器",
    timerSectionDescription: "保留与原版一致的分区结构。",
    timerUnsupportedTitle: "睡眠定时与闹钟",
    timerUnsupportedBody: "Web / iOS PWA 目前不提供原生级睡眠定时与闹钟自动开播，只保留结构说明。",
    networkSectionTitle: "4. 网络",
    networkSectionDescription: "保留网络相关结构，当前以说明为主。",
    networkUnsupportedTitle: "Wi‑Fi / 蜂窝限制",
    networkUnsupportedBody: "浏览器环境无法像原生 App 一样稳定识别与拦截网络类型，因此当前只保留结构，不强制执行。",
    appearanceSectionTitle: "5. 外观",
    appearanceSectionDescription: "主题与驾驶模式保持与 Android 端一致。",
    themeChoiceSubtitle: "跟随系统、浅色、深色三档。",
    generalSectionTitle: "6. 通用",
    generalSectionDescription: "清缓存与版本说明。",
    clearCacheTitle: "清除 Web 缓存",
    clearCacheBody: "清除浏览器中的 PWA 缓存资源，不影响服务器端数据。",
    cacheCleared: "Web 缓存已清除。",
    cacheClearFailed: "清除 Web 缓存失败。",
    aboutTitle: "关于",
    aboutBody: "当前为 Radikall PWA 预览版，界面正在按 Android / Windows 版逐项对齐。",
    performerLabel: "表演者",
    onAirLabel: "播出时间",
    currentSongTitle: "当前歌曲",
    programDetailsTitle: "节目详情",
    noProgramDetails: "当前没有可用的节目详情。",
    historyTitle: "节目歌曲",
    currentProgramSongsDescription: "当前节目内歌曲",
    recentStationSongsDescription: "全台最近歌曲",
    currentProgramTab: "当前节目",
    fullStationTab: "全台历史",
    noCurrentProgramSongs: "暂无当前节目歌曲记录。",
    noRecentStationSongs: "暂无全台最近歌曲记录。",
    showMoreLabel: "展开更多",
    collapseLabel: "收起",
    weeklyScheduleTitle: "周节目表",
    noScheduleAvailable: "暂无节目表。",
  };

  const en = {
    languageSectionTitle: "0. Language",
    languageSectionDescription: "Use the same language entry structure as the Android and Windows app.",
    languageChoiceSubtitle: "Switch the UI copy language.",
    regionSectionTitle: "1. Region",
    regionSectionDescription: "Keep the same startup-area and fixed-area structure as the original app.",
    playbackSectionTitle: "2. Playback",
    playbackSectionDescription: "The Web build keeps the original section structure, with browser-specific capability notes.",
    autoPlayTitle: "Auto play on launch",
    autoPlayBody: "iPhone PWAs cannot reliably auto-play without a user gesture, so the Web build keeps this manual.",
    timerSectionTitle: "3. Timer",
    timerSectionDescription: "Keep the same section structure as the original app.",
    timerUnsupportedTitle: "Sleep timer and alarm",
    timerUnsupportedBody: "Web / iOS PWA does not provide native-grade sleep timer or alarm auto-start yet, so this section is explanatory only.",
    networkSectionTitle: "4. Network",
    networkSectionDescription: "Preserve the network section layout while documenting Web limits.",
    networkUnsupportedTitle: "Wi‑Fi and cellular restrictions",
    networkUnsupportedBody: "Browsers cannot enforce network-type rules as reliably as the native app, so this remains informational for now.",
    appearanceSectionTitle: "5. Appearance",
    appearanceSectionDescription: "Theme and driving mode stay aligned with the Android app.",
    themeChoiceSubtitle: "System, light, and dark.",
    generalSectionTitle: "6. General",
    generalSectionDescription: "Cache cleanup and version notes.",
    clearCacheTitle: "Clear Web cache",
    clearCacheBody: "Remove cached PWA assets from the browser without touching server-side data.",
    cacheCleared: "Web cache cleared.",
    cacheClearFailed: "Unable to clear Web cache.",
    aboutTitle: "About",
    aboutBody: "This is the Radikall PWA preview. The UI is being aligned screen-by-screen with the Android and Windows app.",
    performerLabel: "Performer",
    onAirLabel: "On Air",
    currentSongTitle: "Current Song",
    programDetailsTitle: "Program Details",
    noProgramDetails: "No program details are available right now.",
    historyTitle: "Song History",
    currentProgramSongsDescription: "Songs in the current program",
    recentStationSongsDescription: "Recent songs from the station",
    currentProgramTab: "Current program",
    fullStationTab: "Full station",
    noCurrentProgramSongs: "No songs are listed for the current program yet.",
    noRecentStationSongs: "No recent station songs are available yet.",
    showMoreLabel: "Show more",
    collapseLabel: "Collapse",
    weeklyScheduleTitle: "Weekly Schedule",
    noScheduleAvailable: "No schedule is available.",
  };

  return language === "zh-CN" ? zh : en;
}

function brokenExtraCopy(language: LanguageCode) {
  const zh = {
    languageSectionTitle: "0. 语言",
    languageSectionDescription: "保持与 Android / Windows 版一致的语言切换入口。",
    languageChoiceSubtitle: "切换应用文案语言。",
    regionSectionTitle: "1. 地区",
    regionSectionDescription: "保持与原版一致的启动地区与固定地区结构。",
    playbackSectionTitle: "2. 播放",
    playbackSectionDescription: "Web 版保留原版结构，同时标明浏览器环境的能力差异。",
    autoPlayTitle: "启动自动播放",
    autoPlayBody: "iPhone PWA 无法像原生 App 一样在无手势前提下稳定自动播放，所以 Web 版只能改为手动继续播放。",
    audioFocusTitle: "音频焦点",
    audioFocusSubtitle: "保持与原版一致的结构，Web 端当前不接管系统音频焦点策略。",
    duckAudio: "降低其他音量",
    pausePlayback: "暂停播放",
    timerSectionTitle: "3. 定时器",
    timerSectionDescription: "保持与原版一致的分区结构。",
    timerUnsupportedTitle: "睡眠定时与闹钟",
    timerUnsupportedBody: "Web / iOS PWA 目前不提供原生级睡眠定时与闹钟自动开播，这里先保留原版结构说明。",
    sleepTimerTitle: "睡眠定时",
    sleepTimerSubtitle: "保留与原版一致的选项布局。",
    sleepTimerOption: (minutes: number) => `${minutes} 分钟`,
    sleepTimerOff: "关闭",
    alarmEnabledTitle: "启用闹钟",
    alarmEnabledSubtitle: "到点自动开播属于原生能力，Web / PWA 当前无法等价实现。",
    alarmTimeTitle: "闹钟时间",
    alarmStationTitle: "闹钟电台",
    networkSectionTitle: "4. 网络",
    networkSectionDescription: "保留网络相关结构，当前以说明为主。",
    networkUnsupportedTitle: "Wi‑Fi / 蜂窝限制",
    networkUnsupportedBody: "浏览器环境无法像原生 App 一样稳定识别并拦截网络类型，所以当前只保留结构，不强制执行。",
    wifiOnlyTitle: "仅限 Wi‑Fi 播放",
    wifiOnlySubtitle: "浏览器环境无法稳定识别网络类型，所以此项暂不生效。",
    mobileDataConfirmTitle: "蜂窝网络确认",
    mobileDataConfirmSubtitle: "保留结构，但 Web 端当前不做强制弹窗拦截。",
    appearanceSectionTitle: "5. 外观",
    appearanceSectionDescription: "主题与驾驶模式保持与 Android 端一致。",
    themeChoiceSubtitle: "跟随系统、浅色、深色三档。",
    generalSectionTitle: "6. 通用",
    generalSectionDescription: "缓存清理与版本说明。",
    clearCacheTitle: "清除 Web 缓存",
    clearCacheBody: "清除浏览器中的 PWA 缓存资源，不影响服务端数据。",
    cacheCleared: "Web 缓存已清除。",
    cacheClearFailed: "清除 Web 缓存失败。",
    aboutTitle: "关于",
    aboutBody: "当前是 Radikall PWA 预览版，界面正在按 Android / Windows 版逐项对齐。",
    performerLabel: "表演者",
    onAirLabel: "播出时间",
    currentSongTitle: "当前歌曲",
    programDetailsTitle: "节目详情",
    noProgramDetails: "当前没有可用的节目详情。",
    historyTitle: "节目歌曲",
    currentProgramSongsDescription: "当前节目内歌曲",
    recentStationSongsDescription: "全台最近歌曲",
    currentProgramTab: "当前节目",
    fullStationTab: "全台历史",
    noCurrentProgramSongs: "暂无当前节目歌曲记录。",
    noRecentStationSongs: "暂无全台最近歌曲记录。",
    showMoreLabel: "展开更多",
    collapseLabel: "收起",
    weeklyScheduleTitle: "周节目表",
    noScheduleAvailable: "暂无节目表。",
  };

  const en = {
    languageSectionTitle: "0. Language",
    languageSectionDescription: "Use the same language entry structure as the Android and Windows app.",
    languageChoiceSubtitle: "Switch the UI copy language.",
    regionSectionTitle: "1. Region",
    regionSectionDescription: "Keep the same startup-area and fixed-area structure as the original app.",
    playbackSectionTitle: "2. Playback",
    playbackSectionDescription: "The Web build keeps the original section structure, with browser-specific capability notes.",
    autoPlayTitle: "Auto play on launch",
    autoPlayBody: "iPhone PWAs cannot reliably auto-play without a user gesture, so the Web build keeps this manual.",
    audioFocusTitle: "Audio focus",
    audioFocusSubtitle: "Keep the native structure, while leaving browser audio-focus policy unmanaged for now.",
    duckAudio: "Duck audio",
    pausePlayback: "Pause playback",
    timerSectionTitle: "3. Timer",
    timerSectionDescription: "Keep the same section structure as the original app.",
    timerUnsupportedTitle: "Sleep timer and alarm",
    timerUnsupportedBody: "Web / iOS PWA does not provide native-grade sleep timer or alarm auto-start yet, so this section is explanatory only.",
    sleepTimerTitle: "Sleep timer",
    sleepTimerSubtitle: "Keep the original option layout.",
    sleepTimerOption: (minutes: number) => `${minutes} min`,
    sleepTimerOff: "Off",
    alarmEnabledTitle: "Enable alarm",
    alarmEnabledSubtitle: "Auto-start playback at a fixed time is still a native-only behavior in Web / PWA.",
    alarmTimeTitle: "Alarm time",
    alarmStationTitle: "Alarm station",
    networkSectionTitle: "4. Network",
    networkSectionDescription: "Preserve the network section layout while documenting Web limits.",
    networkUnsupportedTitle: "Wi-Fi and cellular restrictions",
    networkUnsupportedBody: "Browsers cannot enforce network-type rules as reliably as the native app, so this remains informational for now.",
    wifiOnlyTitle: "Wi-Fi only playback",
    wifiOnlySubtitle: "Network type cannot be detected reliably enough in browsers, so this is not enforced yet.",
    mobileDataConfirmTitle: "Cellular playback confirmation",
    mobileDataConfirmSubtitle: "The structure is preserved, but Web does not block playback with a native-style confirmation dialog yet.",
    appearanceSectionTitle: "5. Appearance",
    appearanceSectionDescription: "Theme and driving mode stay aligned with the Android app.",
    themeChoiceSubtitle: "System, light, and dark.",
    generalSectionTitle: "6. General",
    generalSectionDescription: "Cache cleanup and version notes.",
    clearCacheTitle: "Clear Web cache",
    clearCacheBody: "Remove cached PWA assets from the browser without touching server-side data.",
    cacheCleared: "Web cache cleared.",
    cacheClearFailed: "Unable to clear Web cache.",
    aboutTitle: "About",
    aboutBody: "This is the Radikall PWA preview. The UI is being aligned screen-by-screen with the Android and Windows app.",
    performerLabel: "Performer",
    onAirLabel: "On Air",
    currentSongTitle: "Current Song",
    programDetailsTitle: "Program Details",
    noProgramDetails: "No program details are available right now.",
    historyTitle: "Song History",
    currentProgramSongsDescription: "Songs in the current program",
    recentStationSongsDescription: "Recent songs from the station",
    currentProgramTab: "Current program",
    fullStationTab: "Full station",
    noCurrentProgramSongs: "No songs are listed for the current program yet.",
    noRecentStationSongs: "No recent station songs are available yet.",
    showMoreLabel: "Show more",
    collapseLabel: "Collapse",
    weeklyScheduleTitle: "Weekly Schedule",
    noScheduleAvailable: "No schedule is available.",
  };

  if (language === "zh-CN") {
    return zh;
  }

  return en;
}

function extraCopy(language: LanguageCode) {
  const zh = {
    languageSectionTitle: "0. 语言",
    languageSectionDescription: "",
    languageChoiceSubtitle: "",
    regionSectionTitle: "1. 地区",
    regionSectionDescription: "",
    playbackSectionTitle: "2. 播放",
    playbackSectionDescription: "",
    autoPlayTitle: "启动自动播放",
    autoPlayBody: "",
    audioFocusTitle: "音频焦点",
    audioFocusSubtitle: "",
    duckAudio: "降低其他音量",
    pausePlayback: "暂停播放",
    timerSectionTitle: "3. 定时器",
    timerSectionDescription: "",
    timerUnsupportedTitle: "",
    timerUnsupportedBody: "",
    sleepTimerTitle: "睡眠定时",
    sleepTimerSubtitle: "",
    sleepTimerOption: (minutes: number) => `${minutes} 分钟`,
    sleepTimerOff: "关闭",
    alarmEnabledTitle: "启用闹钟",
    alarmEnabledSubtitle: "",
    alarmTimeTitle: "闹钟时间",
    alarmStationTitle: "闹钟电台",
    networkSectionTitle: "4. 网络",
    networkSectionDescription: "",
    networkUnsupportedTitle: "",
    networkUnsupportedBody: "",
    wifiOnlyTitle: "仅限 Wi‑Fi 播放",
    wifiOnlySubtitle: "",
    mobileDataConfirmTitle: "蜂窝网络确认",
    mobileDataConfirmSubtitle: "",
    appearanceSectionTitle: "5. 外观",
    appearanceSectionDescription: "",
    themeChoiceSubtitle: "",
    generalSectionTitle: "6. 通用",
    generalSectionDescription: "",
    clearCacheTitle: "清除 Web 缓存",
    clearCacheBody: "",
    cacheCleared: "Web 缓存已清除。",
    cacheClearFailed: "清除 Web 缓存失败。",
    aboutTitle: "关于 / 开源许可",
    aboutBody: "版本 Web Preview",
    aboutOpenSourceLabel: "完全开源免费",
    aboutOpenSourceBody: "Radikall 完全开源且永久免费。任何以此应用收费的行为均为欺诈，请认准原仓库。",
    aboutRepoLabel: "原始仓库",
    aboutRepoUrl: "https://github.com/baudmusic/radikall",
    aboutSiteLabel: "关注开发者",
    aboutSiteBody: "如果你喜欢这个应用，欢迎访问 baudstudio.com 并关注我的社交账号。",
    aboutSiteUrl: "https://baudstudio.com",
    aboutCheckUpdate: "检查更新",
    aboutCheckUpdateUrl: "https://github.com/baudmusic/radikall/releases/latest",
    aboutCreditsLabel: "致谢",
    aboutCreditsBody: "本项目得益于 jackyzy823/rajiko 项目的思路与逆向工程工作。",
    aboutCreditsUrl: "https://github.com/jackyzy823/rajiko",
    aboutFontNotice: "界面使用 Noto Sans JP 字体，遵循 SIL Open Font License 1.1（OFL）授权。",
    aboutDisclaimerLabel: "免责声明",
    aboutDisclaimerBody: "Radikall 是非官方第三方客户端，与 Radiko Co., Ltd. 及其广播合作伙伴无任何关联。仅供个人学习与技术研究使用。",
    performerLabel: "表演者",
    onAirLabel: "播出时间",
    currentSongTitle: "当前歌曲",
    programDetailsTitle: "节目详情",
    noProgramDetails: "当前没有可用的节目详情。",
    historyTitle: "节目歌曲",
    currentProgramSongsDescription: "当前节目内歌曲",
    recentStationSongsDescription: "全台最近歌曲",
    currentProgramTab: "当前节目",
    fullStationTab: "全台历史",
    noCurrentProgramSongs: "暂无当前节目歌曲记录。",
    noRecentStationSongs: "暂无全台最近歌曲记录。",
    showMoreLabel: "展开更多",
    collapseLabel: "收起",
    weeklyScheduleTitle: "周节目表",
    noScheduleAvailable: "暂无节目表。",
  };

  const en = {
    languageSectionTitle: "0. Language",
    languageSectionDescription: "",
    languageChoiceSubtitle: "",
    regionSectionTitle: "1. Region",
    regionSectionDescription: "",
    playbackSectionTitle: "2. Playback",
    playbackSectionDescription: "",
    autoPlayTitle: "Auto play on launch",
    autoPlayBody: "",
    audioFocusTitle: "Audio focus",
    audioFocusSubtitle: "",
    duckAudio: "Duck audio",
    pausePlayback: "Pause playback",
    timerSectionTitle: "3. Timer",
    timerSectionDescription: "",
    timerUnsupportedTitle: "",
    timerUnsupportedBody: "",
    sleepTimerTitle: "Sleep timer",
    sleepTimerSubtitle: "",
    sleepTimerOption: (minutes: number) => `${minutes} min`,
    sleepTimerOff: "Off",
    alarmEnabledTitle: "Enable alarm",
    alarmEnabledSubtitle: "",
    alarmTimeTitle: "Alarm time",
    alarmStationTitle: "Alarm station",
    networkSectionTitle: "4. Network",
    networkSectionDescription: "",
    networkUnsupportedTitle: "",
    networkUnsupportedBody: "",
    wifiOnlyTitle: "Wi‑Fi only playback",
    wifiOnlySubtitle: "",
    mobileDataConfirmTitle: "Cellular playback confirmation",
    mobileDataConfirmSubtitle: "",
    appearanceSectionTitle: "5. Appearance",
    appearanceSectionDescription: "",
    themeChoiceSubtitle: "",
    generalSectionTitle: "6. General",
    generalSectionDescription: "",
    clearCacheTitle: "Clear Web cache",
    clearCacheBody: "",
    cacheCleared: "Web cache cleared.",
    cacheClearFailed: "Unable to clear Web cache.",
    aboutTitle: "About / Open-source notices",
    aboutBody: "Version Web Preview",
    aboutOpenSourceLabel: "Free and Open Source",
    aboutOpenSourceBody: "Radikall is completely free and open source. Any paid version of this app is fraudulent.",
    aboutRepoLabel: "Source Repository",
    aboutRepoUrl: "https://github.com/baudmusic/radikall",
    aboutSiteLabel: "Follow the Developer",
    aboutSiteBody: "If you enjoy this app, please visit baudstudio.com and follow my social accounts.",
    aboutSiteUrl: "https://baudstudio.com",
    aboutCheckUpdate: "Check for Updates",
    aboutCheckUpdateUrl: "https://github.com/baudmusic/radikall/releases/latest",
    aboutCreditsLabel: "Credits",
    aboutCreditsBody: "This project builds on the ideas and reverse-engineering work of jackyzy823/rajiko.",
    aboutCreditsUrl: "https://github.com/jackyzy823/rajiko",
    aboutFontNotice: "The interface uses the Noto Sans JP font under the SIL Open Font License 1.1 (OFL).",
    aboutDisclaimerLabel: "Disclaimer",
    aboutDisclaimerBody: "Radikall is an unofficial third-party client, unaffiliated with Radiko Co., Ltd. or its broadcasting partners.",
    performerLabel: "Performer",
    onAirLabel: "On Air",
    currentSongTitle: "Current Song",
    programDetailsTitle: "Program Details",
    noProgramDetails: "No program details are available right now.",
    historyTitle: "Song History",
    currentProgramSongsDescription: "Songs in the current program",
    recentStationSongsDescription: "Recent songs from the station",
    currentProgramTab: "Current program",
    fullStationTab: "Full station",
    noCurrentProgramSongs: "No songs are listed for the current program yet.",
    noRecentStationSongs: "No recent station songs are available yet.",
    showMoreLabel: "Show more",
    collapseLabel: "Collapse",
    weeklyScheduleTitle: "Weekly Schedule",
    noScheduleAvailable: "No schedule is available.",
  };

  return language === "zh-CN" ? zh : en;
}

function resolvedExtraCopy(language: LanguageCode) {
  const zh = {
    languageSectionTitle: "0. 语言",
    languageSectionDescription: "",
    languageChoiceSubtitle: "",
    regionSectionTitle: "1. 地域与解锁",
    regionSectionDescription: "",
    playbackSectionTitle: "2. 播放与音频设置",
    playbackSectionDescription: "",
    autoPlayTitle: "启动时自动播放",
    autoPlayBody: "",
    audioFocusTitle: "音频焦点",
    audioFocusSubtitle: "",
    duckAudio: "降低其他音量",
    pausePlayback: "暂停播放",
    timerSectionTitle: "3. 定时与闹铃",
    timerSectionDescription: "",
    timerUnsupportedTitle: "",
    timerUnsupportedBody: "",
    sleepTimerTitle: "睡眠定时器",
    sleepTimerSubtitle: "",
    sleepTimerOption: (minutes: number) => `${minutes} 分钟`,
    sleepTimerOff: "关闭",
    alarmEnabledTitle: "启用闹钟",
    alarmEnabledSubtitle: "",
    alarmTimeTitle: "闹钟时间",
    alarmStationTitle: "闹钟电台",
    networkSectionTitle: "4. 网络与流量",
    networkSectionDescription: "",
    networkUnsupportedTitle: "",
    networkUnsupportedBody: "",
    wifiOnlyTitle: "仅限 Wi-Fi 播放",
    wifiOnlySubtitle: "",
    mobileDataConfirmTitle: "蜂窝网络确认",
    mobileDataConfirmSubtitle: "",
    appearanceSectionTitle: "5. 外观与交互",
    appearanceSectionDescription: "",
    themeChoiceSubtitle: "",
    generalSectionTitle: "7. 系统常规",
    generalSectionDescription: "",
    clearCacheTitle: "清除缓存",
    clearCacheBody: "",
    cacheCleared: "Web 缓存已清除。",
    cacheClearFailed: "清除 Web 缓存失败。",
    aboutTitle: "关于 / 开源许可",
    aboutBody: "版本 Web Preview",
    aboutOpenSourceLabel: "完全开源免费",
    aboutOpenSourceBody: "Radikall 完全开源且永久免费。任何以此应用收费的行为均为欺诈，请认准原始仓库。",
    aboutRepoLabel: "原始仓库",
    aboutRepoUrl: "https://github.com/baudmusic/radikall",
    aboutSiteLabel: "关注开发者",
    aboutSiteBody: "如果你喜欢这个应用，欢迎访问 baudstudio.com 并关注我的社交账号。",
    aboutSiteUrl: "https://baudstudio.com",
    aboutCheckUpdate: "检查更新",
    aboutCheckUpdateUrl: "https://github.com/baudmusic/radikall/releases/latest",
    aboutCreditsLabel: "致谢",
    aboutCreditsBody: "本项目得益于 jackyzy823/rajiko 项目的思路与逆向工程工作。",
    aboutCreditsUrl: "https://github.com/jackyzy823/rajiko",
    aboutFontNotice: "界面使用 Noto Sans JP 字体，遵循 SIL Open Font License 1.1（OFL）授权。",
    aboutDisclaimerLabel: "免责声明",
    aboutDisclaimerBody: "Radikall 是非官方第三方客户端，与 Radiko Co., Ltd. 及其广播合作伙伴无任何关联。仅供个人学习与技术研究使用。",
    performerLabel: "表演者",
    onAirLabel: "播出时间",
    currentSongTitle: "当前歌曲",
    programDetailsTitle: "节目详情",
    noProgramDetails: "当前没有可用的节目详情。",
    historyTitle: "歌曲记录",
    currentProgramSongsDescription: "当前节目内歌曲",
    recentStationSongsDescription: "全台最近歌曲",
    currentProgramTab: "当前节目",
    fullStationTab: "全台历史",
    noCurrentProgramSongs: "暂无当前节目歌曲记录。",
    noRecentStationSongs: "暂无全台最近歌曲记录。",
    showMoreLabel: "展开更多",
    collapseLabel: "收起",
    weeklyScheduleTitle: "周节目表",
    noScheduleAvailable: "暂无节目表。",
  };

  const en = {
    languageSectionTitle: "0. Language",
    languageSectionDescription: "",
    languageChoiceSubtitle: "",
    regionSectionTitle: "1. Region & Unlock",
    regionSectionDescription: "",
    playbackSectionTitle: "2. Playback & Audio",
    playbackSectionDescription: "",
    autoPlayTitle: "Auto-play on launch",
    autoPlayBody: "",
    audioFocusTitle: "Audio focus",
    audioFocusSubtitle: "",
    duckAudio: "Duck audio",
    pausePlayback: "Pause playback",
    timerSectionTitle: "3. Timers & Alarm",
    timerSectionDescription: "",
    timerUnsupportedTitle: "",
    timerUnsupportedBody: "",
    sleepTimerTitle: "Sleep timer",
    sleepTimerSubtitle: "",
    sleepTimerOption: (minutes: number) => `${minutes} min`,
    sleepTimerOff: "Off",
    alarmEnabledTitle: "Enable alarm",
    alarmEnabledSubtitle: "",
    alarmTimeTitle: "Alarm time",
    alarmStationTitle: "Alarm station",
    networkSectionTitle: "4. Network & Data",
    networkSectionDescription: "",
    networkUnsupportedTitle: "",
    networkUnsupportedBody: "",
    wifiOnlyTitle: "Wi-Fi only playback",
    wifiOnlySubtitle: "",
    mobileDataConfirmTitle: "Cellular playback confirmation",
    mobileDataConfirmSubtitle: "",
    appearanceSectionTitle: "5. Appearance & Interaction",
    appearanceSectionDescription: "",
    themeChoiceSubtitle: "",
    generalSectionTitle: "7. General",
    generalSectionDescription: "",
    clearCacheTitle: "Clear cache",
    clearCacheBody: "",
    cacheCleared: "Web cache cleared.",
    cacheClearFailed: "Unable to clear Web cache.",
    aboutTitle: "About / Open-source notices",
    aboutBody: "Version Web Preview",
    aboutOpenSourceLabel: "Free and Open Source",
    aboutOpenSourceBody: "Radikall is completely free and open source. Any paid version of this app is fraudulent.",
    aboutRepoLabel: "Source Repository",
    aboutRepoUrl: "https://github.com/baudmusic/radikall",
    aboutSiteLabel: "Follow the Developer",
    aboutSiteBody: "If you enjoy this app, please visit baudstudio.com and follow the developer.",
    aboutSiteUrl: "https://baudstudio.com",
    aboutCheckUpdate: "Check for Updates",
    aboutCheckUpdateUrl: "https://github.com/baudmusic/radikall/releases/latest",
    aboutCreditsLabel: "Credits",
    aboutCreditsBody: "This project builds on the ideas and reverse-engineering work of jackyzy823/rajiko.",
    aboutCreditsUrl: "https://github.com/jackyzy823/rajiko",
    aboutFontNotice: "The interface uses Noto Sans JP under the SIL Open Font License 1.1 (OFL).",
    aboutDisclaimerLabel: "Disclaimer",
    aboutDisclaimerBody: "Radikall is an unofficial third-party client, unaffiliated with Radiko Co., Ltd. or its broadcasting partners.",
    performerLabel: "Performer",
    onAirLabel: "On Air",
    currentSongTitle: "Current Song",
    programDetailsTitle: "Program Details",
    noProgramDetails: "No program details are available right now.",
    historyTitle: "Song History",
    currentProgramSongsDescription: "Songs in the current program",
    recentStationSongsDescription: "Recent songs from the station",
    currentProgramTab: "Current program",
    fullStationTab: "Full station",
    noCurrentProgramSongs: "No songs are listed for the current program yet.",
    noRecentStationSongs: "No recent station songs are available yet.",
    showMoreLabel: "Show more",
    collapseLabel: "Collapse",
    weeklyScheduleTitle: "Weekly Schedule",
    noScheduleAvailable: "No schedule is available.",
  };

  return language === "zh-CN" ? zh : en;
}
