import type {
  LanguageCode,
  ProgramEntry,
  StartupAreaMode,
  ThemeMode,
  WebSettings,
} from "./types";
import { resolveLanguageTag } from "./i18n";

const SETTINGS_KEY = "radikall.web.settings";

export function createDefaultSettings(defaultAreaId: string): WebSettings {
  return {
    language: resolveLanguageTag(window.navigator.language),
    themeMode: "system",
    startupAreaMode: "remember-last",
    fixedAreaId: defaultAreaId,
    lastAreaId: defaultAreaId,
    lastStationId: null,
    drivingModeEnabled: false,
  };
}

export function loadSettings(defaultAreaId: string): WebSettings {
  const fallback = createDefaultSettings(defaultAreaId);
  const raw = window.localStorage.getItem(SETTINGS_KEY);
  if (!raw) {
    return fallback;
  }

  try {
    const parsed = JSON.parse(raw) as Partial<WebSettings>;
    return {
      language: isLanguage(parsed.language) ? parsed.language : fallback.language,
      themeMode: isTheme(parsed.themeMode) ? parsed.themeMode : fallback.themeMode,
      startupAreaMode: isStartupAreaMode(parsed.startupAreaMode) ? parsed.startupAreaMode : fallback.startupAreaMode,
      fixedAreaId: parsed.fixedAreaId || fallback.fixedAreaId,
      lastAreaId: parsed.lastAreaId || fallback.lastAreaId,
      lastStationId: parsed.lastStationId ?? null,
      drivingModeEnabled: parsed.drivingModeEnabled ?? fallback.drivingModeEnabled,
    };
  } catch {
    return fallback;
  }
}

export function persistSettings(settings: WebSettings): void {
  window.localStorage.setItem(SETTINGS_KEY, JSON.stringify(settings));
}

export function resolveStartupArea(settings: WebSettings, defaultAreaId: string): string {
  return settings.startupAreaMode === "fixed"
    ? settings.fixedAreaId || defaultAreaId
    : settings.lastAreaId || defaultAreaId;
}

export function applyTheme(themeMode: ThemeMode): void {
  const root = document.documentElement;
  const resolvedTheme = themeMode === "system"
    ? (window.matchMedia("(prefers-color-scheme: dark)").matches ? "dark" : "light")
    : themeMode;
  root.dataset.theme = resolvedTheme;
}

export function formatProgramTimeRange(program: ProgramEntry): string {
  return `${formatCompactTime(program.startAt)} - ${formatCompactTime(program.endAt)}`;
}

export function groupProgramsByDay(programs: ProgramEntry[]): Array<{ day: string; items: ProgramEntry[] }> {
  const map = new Map<string, ProgramEntry[]>();
  for (const program of programs) {
    const day = formatCompactDay(program.startAt);
    const items = map.get(day) ?? [];
    items.push(program);
    map.set(day, items);
  }
  return Array.from(map.entries()).map(([day, items]) => ({ day, items }));
}

export function formatSongStamp(stamp: string): string {
  const date = new Date(stamp);
  if (Number.isNaN(date.getTime())) {
    return stamp;
  }
  return date.toLocaleString(undefined, {
    month: "short",
    day: "numeric",
    hour: "2-digit",
    minute: "2-digit",
  });
}

function formatCompactTime(compact: string): string {
  if (compact.length < 12) {
    return compact;
  }
  return `${compact.slice(8, 10)}:${compact.slice(10, 12)}`;
}

function formatCompactDay(compact: string): string {
  if (compact.length < 8) {
    return compact;
  }
  return `${compact.slice(0, 4)}-${compact.slice(4, 6)}-${compact.slice(6, 8)}`;
}

function isLanguage(value: unknown): value is LanguageCode {
  return value === "zh-CN" || value === "zh-TW" || value === "en" || value === "ja" || value === "ko";
}

function isTheme(value: unknown): value is ThemeMode {
  return value === "system" || value === "light" || value === "dark";
}

function isStartupAreaMode(value: unknown): value is StartupAreaMode {
  return value === "remember-last" || value === "fixed";
}
