import type {
  AreaNowPlayingResponse,
  NowPlayingResponse,
  OnAirSong,
  ProgramEntry,
  ScheduleResponse,
} from "../../src/types";
import { narrowAlphanumerics } from "./text";

const programRegex = /<prog\b([^>]*)>([\s\S]*?)<\/prog>/gu;
const attributeRegex = /(\w+)="([^"]*)"/gu;
const stationBlockRegex = /<station id="([^"]+)">([\s\S]*?)<\/station>/gu;

/** 同一地区的 now 数据在这个 isolate 里复用多久。radiko 建议 300 秒，这里取短一些，节目切换时更新更及时。 */
const AREA_NOW_CACHE_MS = 60_000;

export interface AreaNowFeed {
  areaId: string;
  /** radiko 建议的刷新间隔（<ttl>，秒）。 */
  ttlSeconds: number;
  /** 每个电台按开始时间排好的「当前 + 下一档」节目，字段完整。 */
  programsByStation: Map<string, ProgramEntry[]>;
}

// workers.dev 上 Cache API 不保证可用，所以用 isolate 内存缓存：同一实例里并发和连续请求共用一次上游请求。
const areaNowCache = new Map<string, { expiresAt: number; feed: Promise<AreaNowFeed> }>();

/** radiko 按地区的 now 接口：一次拿到该地区所有电台的「当前 + 下一档」节目。 */
export function getAreaNowFeed(areaId: string): Promise<AreaNowFeed> {
  const cached = areaNowCache.get(areaId);
  if (cached != null && cached.expiresAt > Date.now()) {
    return cached.feed;
  }
  const feed = fetchAreaNowFeed(areaId);
  areaNowCache.set(areaId, { expiresAt: Date.now() + AREA_NOW_CACHE_MS, feed });
  feed.catch(() => areaNowCache.delete(areaId));
  return feed;
}

/** 给首页卡片用的精简版：只留标题、主持人、封面和起止时间，不带大段简介。 */
export function buildAreaNowPlayingResponse(feed: AreaNowFeed): AreaNowPlayingResponse {
  const stations: AreaNowPlayingResponse["stations"] = {};
  for (const [stationId, programs] of feed.programsByStation) {
    stations[stationId] = programs.map((program) => ({
      title: program.title,
      performer: program.performer,
      imageUrl: program.imageUrl,
      startAt: program.startAt,
      endAt: program.endAt,
    }));
  }
  return { areaId: feed.areaId, ttlSeconds: feed.ttlSeconds, stations };
}

async function fetchAreaNowFeed(areaId: string): Promise<AreaNowFeed> {
  // cf.cacheTtl 让 Cloudflare 边缘也缓存这个上游请求（对非 Cloudflare 的源站有效），不同实例之间也能复用。
  const init: RequestInit & { cf?: { cacheTtl: number; cacheEverything: boolean } } = {
    cf: { cacheTtl: 60, cacheEverything: true },
  };
  const response = await fetch(`https://api.radiko.jp/program/v3/now/${encodeURIComponent(areaId)}.xml`, init);
  if (!response.ok) {
    throw new Error(`Failed to fetch now programs for ${areaId}: ${response.status}`);
  }
  return parseAreaNowFeed(areaId, await response.text());
}

export function parseAreaNowFeed(areaId: string, xml: string): AreaNowFeed {
  const ttlSeconds = Number.parseInt(xml.match(/<ttl>(\d+)<\/ttl>/u)?.[1] ?? "", 10);
  const programsByStation = new Map<string, ProgramEntry[]>();
  for (const [, stationId, body] of xml.matchAll(stationBlockRegex)) {
    programsByStation.set(
      stationId,
      parsePrograms(stationId, body).sort((left, right) => left.startAt.localeCompare(right.startAt)),
    );
  }
  return {
    areaId,
    ttlSeconds: Number.isFinite(ttlSeconds) && ttlSeconds > 0 ? ttlSeconds : 300,
    programsByStation,
  };
}

export async function buildNowPlayingResponse(stationId: string): Promise<NowPlayingResponse> {
  const weeklyPrograms = await fetchWeeklyPrograms(stationId);
  const currentProgram = findCurrentProgram(weeklyPrograms);
  const recentSongs = await fetchOnAirSongs(stationId, 50);

  return {
    stationId,
    currentProgram,
    currentSong: recentSongs[0] ?? null,
    currentProgramSongs: filterSongsForProgram(recentSongs, currentProgram),
    recentSongs,
  };
}

export async function buildScheduleResponse(stationId: string): Promise<ScheduleResponse> {
  const weeklyPrograms = filterProgramsFromToday(await fetchWeeklyPrograms(stationId))
    .map(compactScheduleProgram);
  const todayPrograms = weeklyPrograms.filter((program) => program.startAt.startsWith(currentTokyoDate()));
  return {
    stationId,
    todayPrograms,
    weeklyPrograms,
  };
}

/**
 * 优先从认证地区的 now 数据里取（约 60KB，且和首页卡片共用缓存），
 * 取不到再退回该台的周节目表（约 700KB）。
 */
export async function findCurrentProgramForStation(stationId: string, areaId?: string): Promise<ProgramEntry | null> {
  if (areaId != null) {
    try {
      const programs = (await getAreaNowFeed(areaId)).programsByStation.get(stationId);
      const current = programs == null ? null : findCurrentProgram(programs);
      if (current != null) {
        return current;
      }
    } catch {
      // now 接口失败时退回周节目表。
    }
  }
  return findCurrentProgram(await fetchWeeklyPrograms(stationId));
}

async function fetchWeeklyPrograms(stationId: string): Promise<ProgramEntry[]> {
  const response = await fetch(`https://api.radiko.jp/program/v3/weekly/${encodeURIComponent(stationId)}.xml`);
  if (!response.ok) {
    throw new Error(`Failed to fetch weekly programs for ${stationId}: ${response.status}`);
  }

  return parsePrograms(stationId, await response.text());
}

function parsePrograms(stationId: string, xml: string): ProgramEntry[] {
  return [...xml.matchAll(programRegex)].map((match) => {
    const attributes = Object.fromEntries(
      [...match[1].matchAll(attributeRegex)].map((attribute) => [attribute[1], attribute[2]]),
    );
    const payload = match[2];
    const performer = extractTag(payload, "pfm");

    return {
      stationId,
      title: narrowAlphanumerics(decodeXmlText(extractTag(payload, "title") ?? "")),
      description: extractTag(payload, "desc") ?? "",
      performer: performer == null ? null : narrowAlphanumerics(decodeXmlText(performer)),
      startAt: attributes.ft ?? "",
      endAt: attributes.to ?? "",
      info: extractTag(payload, "info"),
      imageUrl: extractTag(payload, "img"),
      url: extractTag(payload, "url"),
    } satisfies ProgramEntry;
  }).filter((program) => program.startAt !== "" && program.endAt !== "");
}

/**
 * 标题和主持人是纯文本，但 radiko 会转义 & 和 '（如 `&amp;`、`&apos;`），这里还原。
 * 简介和 info 本身是转义过的 HTML，交给前端的描述处理逻辑，不在这里动。
 */
function decodeXmlText(value: string): string {
  return value.replace(/&(#x[0-9a-f]+|#\d+|amp|lt|gt|quot|apos);/giu, (entity, body: string) => {
    const lower = body.toLowerCase();
    if (lower.startsWith("#x")) return String.fromCodePoint(Number.parseInt(lower.slice(2), 16));
    if (lower.startsWith("#")) return String.fromCodePoint(Number.parseInt(lower.slice(1), 10));
    return { amp: "&", lt: "<", gt: ">", quot: "\"", apos: "'" }[lower] ?? entity;
  });
}

async function fetchOnAirSongs(stationId: string, size: number): Promise<OnAirSong[]> {
  try {
    const url = new URL(`https://api.radiko.jp/music/api/v1/noas/${encodeURIComponent(stationId)}/latest`);
    url.searchParams.set("size", String(size));
    url.searchParams.set("_", String(Date.now()));

    const response = await fetch(url, {
      headers: {
        "Cache-Control": "no-cache, no-store, max-age=0",
        "Pragma": "no-cache",
      },
    });
    if (!response.ok) {
      return [];
    }

    const payload = (await response.json()) as {
      data?: Array<{
        title?: string;
        artist_name?: string;
        artist?: { name?: string };
        displayed_start_time?: string;
        music?: {
          image?: {
            large?: string;
            medium?: string;
            small?: string;
          };
        };
      }>;
    };

    return (payload.data ?? [])
      .map((item) => {
        const title = item.title;
        const artist = item.artist_name ?? item.artist?.name;
        const stampDate = item.displayed_start_time;
        if (title == null || artist == null || stampDate == null) {
          return null;
        }

        return {
          title,
          artist,
          imageUrl: item.music?.image?.large ?? item.music?.image?.medium ?? item.music?.image?.small ?? null,
          stampDate,
        } satisfies OnAirSong;
      })
      .filter((song): song is OnAirSong => song != null)
      .sort((left, right) => toSongSortKey(right.stampDate).localeCompare(toSongSortKey(left.stampDate)));
  } catch {
    return [];
  }
}

function extractTag(payload: string, tag: string): string | null {
  const match = payload.match(new RegExp(`<${tag}>([\\s\\S]*?)<\\/${tag}>`, "u"));
  return match?.[1]?.trim() ?? null;
}

function filterSongsForProgram(songs: OnAirSong[], currentProgram: ProgramEntry | null): OnAirSong[] {
  if (currentProgram == null) {
    return songs;
  }

  return songs.filter((song) => {
    const compactStamp = song.stampDate.replace(/\D/gu, "").slice(0, 14);
    return compactStamp >= currentProgram.startAt && compactStamp < currentProgram.endAt;
  });
}

function filterProgramsFromToday(programs: ProgramEntry[]): ProgramEntry[] {
  const today = currentTokyoDate();
  return programs.filter((program) => program.startAt.slice(0, 8) >= today);
}

function compactScheduleProgram(program: ProgramEntry): ProgramEntry {
  return {
    ...program,
    description: "",
    info: null,
    imageUrl: null,
    url: null,
  };
}

function findCurrentProgram(programs: ProgramEntry[]): ProgramEntry | null {
  const now = currentTokyoTimestamp();
  return programs.find((program) => now >= program.startAt && now < program.endAt) ?? null;
}

function currentTokyoDate(): string {
  const parts = formatTokyoParts();
  return `${parts.year}${parts.month}${parts.day}`;
}

function currentTokyoTimestamp(): string {
  const parts = formatTokyoParts();
  return `${parts.year}${parts.month}${parts.day}${parts.hour}${parts.minute}${parts.second}`;
}

function formatTokyoParts(date = new Date()): Record<string, string> {
  const formatter = new Intl.DateTimeFormat("en-GB", {
    timeZone: "Asia/Tokyo",
    year: "numeric",
    month: "2-digit",
    day: "2-digit",
    hour: "2-digit",
    minute: "2-digit",
    second: "2-digit",
    hourCycle: "h23",
  });
  return Object.fromEntries(
    formatter.formatToParts(date)
      .filter((part) => part.type !== "literal")
      .map((part) => [part.type, part.value]),
  );
}

function toSongSortKey(stampDate: string): string {
  return stampDate.replace(/\D/gu, "").slice(0, 14).padEnd(14, "0");
}
