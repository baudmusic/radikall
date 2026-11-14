import { useSyncExternalStore } from "react";
import { api } from "./api/client";
import type { StationCatalog } from "./stationCatalog";
import type { AreaNowPlayingResponse, StationProgramSlot, StationSummary } from "./types";

export interface NowOnAirSnapshot {
  /** 键是台号；每个电台按开始时间排好的「当前 + 下一档」节目。 */
  programs: ReadonlyMap<string, readonly StationProgramSlot[]>;
  /** 至少请求过一次（成功或失败）的电台。没数据又不在这里，说明还在加载。 */
  settled: ReadonlySet<string>;
}

const EMPTY_SNAPSHOT: NowOnAirSnapshot = { programs: new Map(), settled: new Set() };

/** 请求失败、或地区数据里意外缺了某台时，隔多久再试。 */
const RETRY_AFTER_FAILURE_MS = 30_000;

/**
 * 各电台「正在播 / 下一档」节目的缓存。数据来自 Worker 的 /api/areas/:areaId/now-playing：
 * 一次请求拿到一个地区所有电台的两档节目；有效期（radiko 给的 ttl，通常 300 秒）内不重复请求，
 * 同一地区的并发请求合并成一次。过期后旧数据继续显示，直到新数据回来。
 * 原生端的 shared/.../program/NowOnAirRepository.kt 是同一套规则。
 */
export class NowOnAirStore {
  private snapshot: NowOnAirSnapshot = EMPTY_SNAPSHOT;
  private readonly freshUntil = new Map<string, number>();
  private readonly retryAt = new Map<string, number>();
  private readonly inFlight = new Map<string, Promise<void>>();
  private readonly listeners = new Set<() => void>();

  constructor(
    private readonly catalog: Pick<StationCatalog, "coveringAreas">,
    private readonly fetchArea: (areaId: string) => Promise<AreaNowPlayingResponse> = (areaId) =>
      api.getAreaNowPlaying(areaId),
    private readonly now: () => number = () => Date.now(),
  ) {}

  readonly subscribe = (listener: () => void): (() => void) => {
    this.listeners.add(listener);
    return () => {
      this.listeners.delete(listener);
    };
  };

  /** 给 useSyncExternalStore 用；数据变化时换成新对象，没变化时返回同一个对象。 */
  readonly getSnapshot = (): NowOnAirSnapshot => this.snapshot;

  /**
   * 确保这些电台有有效期内的节目数据：只为过期或缺失的电台请求，并挑最少的地区去请求。
   * 返回时数据已更新；请求失败时保留旧数据，30 秒内不再重试这些电台。
   */
  async ensureStations(stations: readonly StationSummary[], preferredAreaIds: readonly string[] = []): Promise<void> {
    const now = this.now();
    const stale = stations.filter(
      (station) => (this.freshUntil.get(station.id) ?? 0) <= now && (this.retryAt.get(station.id) ?? 0) <= now,
    );
    if (stale.length === 0) {
      return;
    }
    // 正在请求的地区排在最前，能顺带覆盖的就直接等它，不另发请求。
    const areaIds = this.catalog.coveringAreas([...stale], [...this.inFlight.keys(), ...preferredAreaIds]);
    await Promise.all(areaIds.map((areaId) => this.refreshArea(areaId, stale)));
  }

  /** 某台在某一时刻（默认现在）正在播的节目；没有数据时返回 null。数据里有下一档，所以整点切换不需要重新请求。 */
  programAt(stationId: string, at: Date = new Date(this.now())): StationProgramSlot | null {
    return findSlotAt(this.snapshot.programs.get(stationId), tokyoCompactTimestamp(at));
  }

  private refreshArea(areaId: string, requested: readonly StationSummary[]): Promise<void> {
    const existing = this.inFlight.get(areaId);
    if (existing != null) {
      return existing;
    }

    const task = this.fetchArea(areaId)
      .then(
        (response) => {
          const freshUntil = this.now() + response.ttlSeconds * 1000;
          const programs = new Map(this.snapshot.programs);
          const settled = new Set(this.snapshot.settled);
          for (const [stationId, slots] of Object.entries(response.stations)) {
            programs.set(stationId, slots);
            settled.add(stationId);
            this.freshUntil.set(stationId, freshUntil);
            this.retryAt.delete(stationId);
          }
          // 按收听地区本该包含、这次却没返回的电台：等下一个有效期再试，免得每次都重新请求。
          for (const station of requested) {
            if (station.areaIds.includes(areaId) && !(station.id in response.stations)) {
              this.retryAt.set(station.id, freshUntil);
              settled.add(station.id);
            }
          }
          this.publish({ programs, settled });
        },
        () => {
          const retryAt = this.now() + RETRY_AFTER_FAILURE_MS;
          const settled = new Set(this.snapshot.settled);
          for (const station of requested) {
            if (station.areaIds.includes(areaId)) {
              this.retryAt.set(station.id, retryAt);
              settled.add(station.id);
            }
          }
          this.publish({ programs: this.snapshot.programs, settled });
        },
      )
      .finally(() => {
        this.inFlight.delete(areaId);
      });
    this.inFlight.set(areaId, task);
    return task;
  }

  private publish(snapshot: NowOnAirSnapshot): void {
    this.snapshot = snapshot;
    this.listeners.forEach((listener) => listener());
  }
}

const noopSubscribe = () => () => undefined;
const emptySnapshot = () => EMPTY_SNAPSHOT;

/** 订阅节目缓存；store 为 null（目录还没加载）时返回空快照。 */
export function useNowOnAirPrograms(store: NowOnAirStore | null): NowOnAirSnapshot {
  return useSyncExternalStore(store?.subscribe ?? noopSubscribe, store?.getSnapshot ?? emptySnapshot);
}

export function findSlotAt(
  slots: readonly StationProgramSlot[] | undefined,
  timestamp: string,
): StationProgramSlot | null {
  return slots?.find((slot) => timestamp >= slot.startAt && timestamp < slot.endAt) ?? null;
}

/** 节目已播出的比例（0–1），用来画卡片底部的进度细线。 */
export function programProgress(slot: StationProgramSlot, at: Date): number {
  const start = parseTokyoCompactTimestamp(slot.startAt);
  const end = parseTokyoCompactTimestamp(slot.endAt);
  if (start == null || end == null || end <= start) {
    return 0;
  }
  return Math.min(1, Math.max(0, (at.getTime() - start) / (end - start)));
}

/** 东京时间的 yyyyMMddHHmmss，和 radiko 的 ft / to 同格式。日本没有夏令时，固定 +9 小时即可。 */
export function tokyoCompactTimestamp(date: Date): string {
  return new Date(date.getTime() + TOKYO_OFFSET_MS).toISOString().replace(/\D/gu, "").slice(0, 14);
}

/** tokyoCompactTimestamp 的反向：返回毫秒时间戳，格式不对时返回 null。 */
export function parseTokyoCompactTimestamp(value: string): number | null {
  const match = /^(\d{4})(\d{2})(\d{2})(\d{2})(\d{2})(\d{2})$/u.exec(value);
  if (match == null) {
    return null;
  }
  const [, year, month, day, hour, minute, second] = match.map(Number);
  return Date.UTC(year, month - 1, day, hour, minute, second) - TOKYO_OFFSET_MS;
}

const TOKYO_OFFSET_MS = 9 * 60 * 60 * 1000;
