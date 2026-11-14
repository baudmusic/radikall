import workerData from "../generated/worker-data.json";
import { narrowAlphanumerics } from "./text";
import type { BootstrapResponse, WorkerData } from "./types";

const typedWorkerData = workerData as WorkerData;

// 台名照抄 radiko 官方（含「ＨＢＣラジオ」这类全角英数），发给前端前统一转半角。
export const bootstrapData: BootstrapResponse = {
  ...typedWorkerData.bootstrap,
  stations: typedWorkerData.bootstrap.stations.map((station) => ({
    ...station,
    name: narrowAlphanumerics(station.name),
  })),
};
export const gpsCoordinates = typedWorkerData.gpsCoordinates;
export const fullKeyBase64 = typedWorkerData.fullKeyBase64;

export const stationById = new Map(bootstrapData.stations.map((station) => [station.id, station]));

export const prefectureIds = new Set(bootstrapData.prefectures.map((prefecture) => prefecture.id));

export const allowedUpstreamHostSuffixes = ["radiko.jp", "smartstream.ne.jp"];

export const supportedLanguages = bootstrapData.supportedLanguages;
