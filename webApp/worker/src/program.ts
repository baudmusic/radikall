import type { NowPlayingResponse, OnAirSong, ProgramEntry, ScheduleResponse } from "../../src/types";

const programRegex = /<prog\b([^>]*)>([\s\S]*?)<\/prog>/gu;
const attributeRegex = /(\w+)="([^"]*)"/gu;

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

export async function findCurrentProgramForStation(stationId: string): Promise<ProgramEntry | null> {
  return findCurrentProgram(await fetchWeeklyPrograms(stationId));
}

async function fetchWeeklyPrograms(stationId: string): Promise<ProgramEntry[]> {
  const response = await fetch(`https://api.radiko.jp/program/v3/weekly/${encodeURIComponent(stationId)}.xml`);
  if (!response.ok) {
    throw new Error(`Failed to fetch weekly programs for ${stationId}: ${response.status}`);
  }

  const xml = await response.text();
  return [...xml.matchAll(programRegex)].map((match) => {
    const attributes = Object.fromEntries(
      [...match[1].matchAll(attributeRegex)].map((attribute) => [attribute[1], attribute[2]]),
    );
    const payload = match[2];

    return {
      stationId,
      title: extractTag(payload, "title") ?? "",
      description: extractTag(payload, "desc") ?? "",
      performer: extractTag(payload, "pfm"),
      startAt: attributes.ft ?? "",
      endAt: attributes.to ?? "",
      info: extractTag(payload, "info"),
      imageUrl: extractTag(payload, "img"),
      url: extractTag(payload, "url"),
    } satisfies ProgramEntry;
  }).filter((program) => program.startAt !== "" && program.endAt !== "");
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
