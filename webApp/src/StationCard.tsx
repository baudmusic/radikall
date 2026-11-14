import { useState } from "react";
import { fitLogo, wideLogoUrl } from "./stationLogo";
import type { StationProgramSlot, StationSummary } from "./types";

/** ready：有当前节目；loading：还没拿到数据；unavailable：请求过但没有（失败或该台不在数据里）。 */
export type StationProgramState = "ready" | "loading" | "unavailable";

// 台标尺寸：签条里（有封面时）和整块（无封面时）。驾驶模式整体放大 1.2 倍。
const STRIP_LOGO = { maxW: 80, maxH: 18, area: 720 };
const SOLO_LOGO = { maxW: 76, maxH: 46, area: 2000 };

/**
 * 横向卡片：左边是「封面 + 白色台标签条」组成的媒体块；右边从上到下是
 * 台名眉标（小字，地区标签跟在后面）、节目名（最多两行）、「结束时间 · 主持人」，底部 2px 细线是节目进度。
 * 台标很小时（NHK 东京/大阪、联播同一节目的不同台）靠眉标区分电台。
 */
export function StationCard({
  station,
  active,
  playing,
  program,
  programState,
  progress,
  endLabel,
  areaLabel,
  emphasized = false,
  onSelect,
}: {
  station: StationSummary;
  /** 当前选中的台（红框、红色台名）。 */
  active: boolean;
  /** 当前选中且正在出声：台名前的音柱会跳动。 */
  playing: boolean;
  program: StationProgramSlot | null;
  programState: StationProgramState;
  /** 节目已播出的比例（0–1）。 */
  progress: number;
  /** 如「〜12:45」，已按语言格式化。 */
  endLabel: string | null;
  /** 电台不属于当前浏览地区时，标出它的覆盖范围（如「近畿」「全国」）。 */
  areaLabel?: string | null;
  /** 驾驶模式：放大，隐藏主持人。 */
  emphasized?: boolean;
  onSelect: () => void;
}) {
  const [failedCoverUrl, setFailedCoverUrl] = useState<string | null>(null);
  const coverUrl = program?.imageUrl && program.imageUrl !== failedCoverUrl ? program.imageUrl : null;
  const scale = emphasized ? 1.2 : 1;
  const performer = emphasized ? null : program?.performer;
  // 没有节目数据时台名本身就是标题，不再重复放眉标；台号和地区标签放到下面一行。
  const unavailable = programState === "unavailable";
  const meta = program ? [endLabel, performer].filter(Boolean).join(" · ") : unavailable ? station.id : null;

  return (
    <button
      type="button"
      className={`station-card ${active ? "active" : ""}`}
      aria-label={program ? `${station.name}：${program.title}` : station.name}
      onClick={onSelect}
    >
      <span className={`station-media ${coverUrl ? "" : "solo"}`}>
        {coverUrl ? (
          <>
            <img
              className="station-cover"
              src={coverUrl}
              alt=""
              loading="lazy"
              referrerPolicy="no-referrer"
              onError={() => setFailedCoverUrl(coverUrl)}
            />
            <span className="station-logo-strip">
              <StationLogo station={station} size={STRIP_LOGO} scale={scale} />
            </span>
          </>
        ) : (
          <StationLogo station={station} size={SOLO_LOGO} scale={scale} />
        )}
      </span>

      <span className="station-copy">
        {unavailable ? null : (
          <span className="station-eyebrow">
            {active ? <EqualizerIcon playing={playing} /> : null}
            <span className="station-eyebrow-name">{station.name}</span>
            {areaLabel ? <span className="station-card-area">{areaLabel}</span> : null}
          </span>
        )}
        {programState === "loading" ? (
          <span className="station-skeleton" aria-hidden="true">
            <span />
            <span />
          </span>
        ) : (
          <strong className="station-program-title">{program?.title ?? station.name}</strong>
        )}
        {meta || (unavailable && areaLabel) ? (
          <span className="station-program-meta">
            {unavailable && areaLabel ? <span className="station-card-area">{areaLabel}</span> : null}
            {meta ? <span className="station-program-meta-text">{meta}</span> : null}
          </span>
        ) : null}
      </span>

      {program ? (
        <span className="station-progress" style={{ width: `${(progress * 100).toFixed(1)}%` }} aria-hidden="true" />
      ) : null}
    </button>
  );
}

/** 当前台的小音柱：正在出声时跳动，暂停时静止。 */
function EqualizerIcon({ playing }: { playing: boolean }) {
  return (
    <span className={`station-eq ${playing ? "playing" : ""}`} aria-hidden="true">
      <i />
      <i />
      <i />
    </span>
  );
}

/** 按测量好的墨迹范围裁掉台标的透明边；没有测量数据或高清图加载失败时，退回普通缩放的小图。 */
function StationLogo({
  station,
  size,
  scale,
}: {
  station: StationSummary;
  size: { maxW: number; maxH: number; area: number };
  scale: number;
}) {
  const [failed, setFailed] = useState(false);
  const box = failed ? null : fitLogo(station.id, size.maxW * scale, size.maxH * scale, size.area * scale * scale);

  if (box == null) {
    return (
      <img
        className="station-logo-fallback"
        src={station.logoUrl}
        alt=""
        loading="lazy"
        referrerPolicy="no-referrer"
        style={{ maxWidth: size.maxW * scale, maxHeight: size.maxH * scale }}
      />
    );
  }

  return (
    <span className="station-logo-crop" style={{ width: box.inkW, height: box.inkH }}>
      <img
        src={wideLogoUrl(station.id)}
        alt=""
        loading="lazy"
        referrerPolicy="no-referrer"
        onError={() => setFailed(true)}
        style={{ width: box.imgW, height: box.imgH, left: -box.offsetX, top: -box.offsetY }}
      />
    </span>
  );
}
