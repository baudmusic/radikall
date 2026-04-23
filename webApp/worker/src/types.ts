import type {
  BootstrapResponse,
  LiveSessionRequest,
  LiveSessionResponse,
  NowPlayingResponse,
  ScheduleResponse,
  StationSummary,
} from "../../src/types";

export type {
  BootstrapResponse,
  LiveSessionRequest,
  LiveSessionResponse,
  NowPlayingResponse,
  ScheduleResponse,
  StationSummary,
};

export interface WorkerData {
  generatedAt: string;
  bootstrap: BootstrapResponse;
  gpsCoordinates: Record<string, { latitude: number; longitude: number }>;
  fullKeyBase64: string;
}

export interface AssetBinding {
  fetch(input: RequestInfo | URL, init?: RequestInit): Promise<Response>;
}

export interface Env {
  ASSETS: AssetBinding;
  RADIKALL_SESSION_SECRET: string;
}

export interface PlaybackTokenPayload {
  stationId: string;
  resolvedAreaId: string;
  authToken: string;
  userAgent: string;
  upstreamMasterUrl: string;
  issuedAtEpochMillis: number;
  expiresAtEpochMillis: number;
}

export interface AuthSession {
  token: string;
  areaId: string;
  userAgent: string;
}

export interface WorkerContext {
  request: Request;
  env: Env;
}
