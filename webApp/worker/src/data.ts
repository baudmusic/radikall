import workerData from "../generated/worker-data.json";
import type { BootstrapResponse, WorkerData } from "./types";

const typedWorkerData = workerData as WorkerData;

export const bootstrapData: BootstrapResponse = typedWorkerData.bootstrap;
export const gpsCoordinates = typedWorkerData.gpsCoordinates;
export const fullKeyBase64 = typedWorkerData.fullKeyBase64;

export const stationById = new Map(bootstrapData.stations.map((station) => [station.id, station]));

export const allowedUpstreamHostSuffixes = ["radiko.jp", "smartstream.ne.jp"];

export const supportedLanguages = bootstrapData.supportedLanguages;
