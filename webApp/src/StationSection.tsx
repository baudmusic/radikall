import { useEffect, useRef } from "react";
import type { ReactNode } from "react";
import { useNearViewport } from "./hooks";
import type { NowOnAirStore } from "./nowOnAir";
import type { StationSummary } from "./types";

/**
 * 首页的一个电台分组。分组接近视口时才为组里的电台请求当前节目，
 * 「全国」总表里还没滚到的大区不会提前请求；之后每次 refreshKey 变化（时钟走一格）再确认一次，有效期内不会真的发请求。
 */
export function StationSection({
  title,
  stations,
  store,
  preferredAreaIds,
  refreshKey,
  eager = false,
  children,
}: {
  title: string | null;
  stations: StationSummary[];
  store: NowOnAirStore | null;
  preferredAreaIds: string[];
  refreshKey: number;
  /** 第一个分组总在页面顶部，不必等可见性检测，直接请求（单地区列表因此完全不依赖 IntersectionObserver）。 */
  eager?: boolean;
  children: ReactNode;
}) {
  const ref = useRef<HTMLElement | null>(null);
  const near = useNearViewport(ref, "600px 0px") || eager;

  useEffect(() => {
    if (near && store != null) {
      void store.ensureStations(stations, preferredAreaIds);
    }
  }, [near, preferredAreaIds, refreshKey, stations, store]);

  return (
    <section ref={ref} className="station-section">
      {title ? (
        <h2 className="station-section-title">
          <span>{title}</span>
          <span className="station-section-count">{stations.length}</span>
        </h2>
      ) : null}
      <div className="station-grid">{children}</div>
    </section>
  );
}
