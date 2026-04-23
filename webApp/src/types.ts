export type LanguageCode = "zh-CN" | "zh-TW" | "en" | "ja" | "ko";
export type ThemeMode = "system" | "light" | "dark";
export type StartupAreaMode = "remember-last" | "fixed";
export type AppView = "home" | "now-playing" | "settings";

export interface LocalizedText {
  "zh-CN": string;
  "zh-TW": string;
  en: string;
  ja: string;
  ko: string;
}

export interface FeatureFlags {
  autoPlayOnLaunch: boolean;
  backgroundPlaybackBestEffort: boolean;
  alarms: boolean;
  pushReminder: boolean;
  sleepTimer: boolean;
  wifiOnlyPlayback: boolean;
  confirmMobileDataPlayback: boolean;
}

export interface RegionSummary {
  id: string;
  names: LocalizedText;
}

export interface PrefectureSummary {
  id: string;
  regionId: string;
  areaCode: string;
  names: LocalizedText;
}

export interface StationSummary {
  id: string;
  name: string;
  areaIds: string[];
  logoUrl: string;
}

export interface ProgramEntry {
  stationId: string;
  title: string;
  description: string;
  performer: string | null;
  startAt: string;
  endAt: string;
  info: string | null;
  imageUrl: string | null;
  url: string | null;
}

export interface OnAirSong {
  title: string;
  artist: string;
  imageUrl: string | null;
  stampDate: string;
}

export interface BootstrapResponse {
  defaultAreaId: string;
  supportedLanguages: LanguageCode[];
  featureFlags: FeatureFlags;
  regions: RegionSummary[];
  prefectures: PrefectureSummary[];
  stations: StationSummary[];
}

export interface NowPlayingResponse {
  stationId: string;
  currentProgram: ProgramEntry | null;
  currentSong: OnAirSong | null;
  currentProgramSongs: OnAirSong[];
  recentSongs: OnAirSong[];
}

export interface ScheduleResponse {
  stationId: string;
  todayPrograms: ProgramEntry[];
  weeklyPrograms: ProgramEntry[];
}

export interface LiveSessionRequest {
  stationId: string;
  preferredAreaId: string;
}

export interface LiveSessionResponse {
  sessionId: string;
  stationId: string;
  resolvedAreaId: string;
  streamUrl: string;
  fallbackStreamUrl: string | null;
  currentProgram: ProgramEntry | null;
  expiresAtEpochMillis: number;
}

export interface WebSettings {
  language: LanguageCode;
  themeMode: ThemeMode;
  startupAreaMode: StartupAreaMode;
  fixedAreaId: string;
  lastAreaId: string;
  lastStationId: string | null;
  drivingModeEnabled: boolean;
}

export interface PlaybackState {
  currentStation: StationSummary | null;
  currentProgram: ProgramEntry | null;
  isLoading: boolean;
  isPlaying: boolean;
  resolvedAreaId: string;
  expiresAtEpochMillis: number | null;
  error: string | null;
}
