import type {
  BootstrapResponse,
  LiveSessionRequest,
  LiveSessionResponse,
  NowPlayingResponse,
  ScheduleResponse,
  StationSummary,
} from "../types";

async function request<T>(input: RequestInfo | URL, init?: RequestInit): Promise<T> {
  const response = await fetch(input, {
    ...init,
    headers: {
      Accept: "application/json",
      ...(init?.body ? { "Content-Type": "application/json" } : {}),
      ...(init?.headers ?? {}),
    },
  });

  if (!response.ok) {
    const message = await extractErrorMessage(response);
    throw new Error(message);
  }

  return response.json() as Promise<T>;
}

async function extractErrorMessage(response: Response): Promise<string> {
  const contentType = response.headers.get("content-type") ?? "";
  if (contentType.includes("application/json")) {
    const payload = (await response.json()) as { message?: string };
    return payload.message ?? `${response.status} ${response.statusText}`;
  }
  const text = await response.text();
  return text || `${response.status} ${response.statusText}`;
}

export const api = {
  getBootstrap(): Promise<BootstrapResponse> {
    return request("/api/bootstrap");
  },
  getStations(areaId: string, query: string): Promise<StationSummary[]> {
    const params = new URLSearchParams();
    params.set("areaId", areaId);
    if (query.trim()) {
      params.set("query", query.trim());
    }
    return request(`/api/stations?${params.toString()}`);
  },
  getNowPlaying(stationId: string): Promise<NowPlayingResponse> {
    return request(`/api/stations/${encodeURIComponent(stationId)}/now-playing`);
  },
  getSchedule(stationId: string): Promise<ScheduleResponse> {
    return request(`/api/stations/${encodeURIComponent(stationId)}/schedule?day=today`);
  },
  createLiveSession(payload: LiveSessionRequest): Promise<LiveSessionResponse> {
    return request("/api/playback/live-session", {
      method: "POST",
      body: JSON.stringify(payload),
    });
  },
};
