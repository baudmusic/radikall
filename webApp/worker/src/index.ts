import { authenticate, currentEpochMillis } from "./auth";
import { encryptPlaybackToken, decryptPlaybackToken } from "./crypto";
import { allowedUpstreamHostSuffixes, bootstrapData, stationById } from "./data";
import { buildNowPlayingResponse, buildScheduleResponse, findCurrentProgramForStation } from "./program";
import { buildLiveStreamUrl } from "./stream";
import type {
  Env,
  LiveSessionRequest,
  LiveSessionResponse,
  PlaybackTokenPayload,
  StationSummary,
} from "./types";

const SESSION_TTL_MS = 55 * 60 * 1000;

export default {
  async fetch(request: Request, env: Env): Promise<Response> {
    try {
      return await routeRequest(request, env);
    } catch (error) {
      if (error instanceof HttpError) {
        return jsonResponse({ message: error.message }, error.status);
      }
      return jsonResponse(
        {
          message: error instanceof Error ? error.message : "Unexpected worker error",
        },
        500,
      );
    }
  },
};

async function routeRequest(request: Request, env: Env): Promise<Response> {
  const url = new URL(request.url);
  const path = url.pathname;

  if (path === "/health") {
    return jsonResponse({ status: "ok" });
  }

  if (path === "/api/bootstrap" && request.method === "GET") {
    return jsonResponse(bootstrapData);
  }

  if (path === "/api/stations" && request.method === "GET") {
    const areaId = url.searchParams.get("areaId")?.trim() || null;
    const query = url.searchParams.get("query")?.trim().toLowerCase() ?? "";
    const stations = filterStations(areaId, query);
    return jsonResponse(stations);
  }

  const nowPlayingMatch = path.match(/^\/api\/stations\/([^/]+)\/now-playing$/u);
  if (nowPlayingMatch && request.method === "GET") {
    const stationId = decodeURIComponent(nowPlayingMatch[1]);
    assertKnownStation(stationId);
    return jsonResponse(await buildNowPlayingResponse(stationId));
  }

  const scheduleMatch = path.match(/^\/api\/stations\/([^/]+)\/schedule$/u);
  if (scheduleMatch && request.method === "GET") {
    const stationId = decodeURIComponent(scheduleMatch[1]);
    assertKnownStation(stationId);
    return jsonResponse(await buildScheduleResponse(stationId));
  }

  if (path === "/api/playback/live-session" && request.method === "POST") {
    const body = (await request.json()) as LiveSessionRequest;
    const station = assertKnownStation(body.stationId);
    const authSession = await authenticate(body.preferredAreaId);
    const upstreamMasterUrl = await buildLiveStreamUrl(station.id);
    const currentProgram = await findCurrentProgramForStation(station.id);
    const issuedAtEpochMillis = currentEpochMillis();
    const expiresAtEpochMillis = issuedAtEpochMillis + SESSION_TTL_MS;
    const sessionId = await encryptPlaybackToken(env.RADIKALL_SESSION_SECRET, {
      stationId: station.id,
      resolvedAreaId: authSession.areaId,
      authToken: authSession.token,
      userAgent: authSession.userAgent,
      upstreamMasterUrl,
      issuedAtEpochMillis,
      expiresAtEpochMillis,
    });

    const response: LiveSessionResponse = {
      sessionId,
      stationId: station.id,
      resolvedAreaId: authSession.areaId,
      streamUrl: `/api/playback/live/${sessionId}/master.m3u8`,
      fallbackStreamUrl: null,
      currentProgram,
      expiresAtEpochMillis,
    };
    return jsonResponse(response);
  }

  const masterMatch = path.match(/^\/api\/playback\/live\/([^/]+)\/master\.m3u8$/u);
  if (masterMatch && request.method === "GET") {
    const sessionId = decodeURIComponent(masterMatch[1]);
    const session = await resolvePlaybackToken(env, sessionId);
    return proxyUpstream(request, sessionId, session, session.upstreamMasterUrl);
  }

  const proxyMatch = path.match(/^\/api\/playback\/live\/([^/]+)\/proxy$/u);
  if (proxyMatch && request.method === "GET") {
    const sessionId = decodeURIComponent(proxyMatch[1]);
    const session = await resolvePlaybackToken(env, sessionId);
    const targetUrl = url.searchParams.get("target");
    if (targetUrl == null || !isAllowedUpstreamUrl(targetUrl)) {
      return jsonResponse({ message: "Unsupported upstream host" }, 400);
    }
    return proxyUpstream(request, sessionId, session, targetUrl);
  }

  return env.ASSETS.fetch(request);
}

function filterStations(areaId: string | null, query: string): StationSummary[] {
  return bootstrapData.stations.filter((station) => {
    const matchesArea = areaId == null || station.areaIds.includes(areaId);
    if (!matchesArea) {
      return false;
    }
    if (query === "") {
      return true;
    }
    const haystack = `${station.id} ${station.name}`.toLowerCase();
    return haystack.includes(query);
  });
}

function assertKnownStation(stationId: string): StationSummary {
  const station = stationById.get(stationId);
  if (station == null) {
    throw new HttpError(404, `Unknown station: ${stationId}`);
  }
  return station;
}

async function resolvePlaybackToken(env: Env, token: string): Promise<PlaybackTokenPayload> {
  const payload = await decryptPlaybackToken(env.RADIKALL_SESSION_SECRET, token);
  if (payload == null || currentEpochMillis() >= payload.expiresAtEpochMillis) {
    throw new HttpError(410, "Playback session expired");
  }
  return payload;
}

async function proxyUpstream(
  request: Request,
  sessionId: string,
  session: PlaybackTokenPayload,
  targetUrl: string,
): Promise<Response> {
  const upstreamRequest = new Request(targetUrl, {
    headers: buildUpstreamHeaders(request, session),
    method: "GET",
  });
  const upstreamResponse = await fetch(upstreamRequest);

  const contentType = upstreamResponse.headers.get("content-type") ?? "";
  const headers = new Headers();
  headers.set("Cache-Control", "no-store, no-cache, max-age=0");
  const contentRange = upstreamResponse.headers.get("content-range");
  const acceptRanges = upstreamResponse.headers.get("accept-ranges");
  if (contentRange != null) {
    headers.set("Content-Range", contentRange);
  }
  if (acceptRanges != null) {
    headers.set("Accept-Ranges", acceptRanges);
  }

  if (isPlaylistResponse(targetUrl, contentType)) {
    headers.set("Content-Type", contentType || "application/vnd.apple.mpegurl");
    const playlist = await upstreamResponse.text();
    return new Response(rewritePlaylist(sessionId, targetUrl, playlist), {
      status: upstreamResponse.status,
      headers,
    });
  }

  if (contentType !== "") {
    headers.set("Content-Type", contentType);
  }

  return new Response(upstreamResponse.body, {
    status: upstreamResponse.status,
    headers,
  });
}

function buildUpstreamHeaders(request: Request, session: PlaybackTokenPayload): Headers {
  const headers = new Headers({
    "X-Radiko-AuthToken": session.authToken,
    "X-Radiko-AreaId": session.resolvedAreaId,
    "User-Agent": session.userAgent,
  });
  const requestedRange = request.headers.get("range");
  if (requestedRange != null) {
    headers.set("Range", requestedRange);
  }
  return headers;
}

function rewritePlaylist(
  sessionId: string,
  currentTargetUrl: string,
  playlist: string,
): string {
  const currentUrl = new URL(currentTargetUrl);
  return playlist
    .split("\n")
    .map((line) => {
      const trimmed = line.trim();
      if (trimmed === "" || trimmed.startsWith("#")) {
        return line;
      }

      const resolved = new URL(trimmed, currentUrl).toString();
      const target = encodeURIComponent(resolved);
      return `/api/playback/live/${encodeURIComponent(sessionId)}/proxy?target=${target}`;
    })
    .join("\n");
}

function isAllowedUpstreamUrl(value: string): boolean {
  try {
    const url = new URL(value);
    return url.protocol === "https:" && allowedUpstreamHostSuffixes.some((suffix) =>
      url.hostname === suffix || url.hostname.endsWith(`.${suffix}`),
    );
  } catch {
    return false;
  }
}

function isPlaylistResponse(targetUrl: string, contentType: string): boolean {
  const normalizedType = contentType.toLowerCase();
  return targetUrl.split("?")[0]?.endsWith(".m3u8") ||
    normalizedType.includes("mpegurl") ||
    normalizedType.includes("vnd.apple.mpegurl");
}

function jsonResponse(body: unknown, status = 200): Response {
  return new Response(JSON.stringify(body), {
    status,
    headers: {
      "Content-Type": "application/json; charset=utf-8",
      "Cache-Control": "no-store",
    },
  });
}

class HttpError extends Error {
  constructor(
    public readonly status: number,
    message: string,
  ) {
    super(message);
  }
}
