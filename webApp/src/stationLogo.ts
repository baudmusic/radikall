import { STATION_LOGO_INK } from "./stationLogoInk";

/** 卡片用的台标图：去掉左右留白的 688×160 版本，裁掉上下留白放大后在高密度屏上也清晰。 */
export function wideLogoUrl(stationId: string): string {
  return `https://radiko.jp/v2/static/station/logo/${encodeURIComponent(stationId)}/lrtrim/688x160.png`;
}

/** 台标在卡片上的摆放：外框只框住墨迹（inkW × inkH），里面的整张图按 offset 往左上挪，把透明边藏到框外。 */
export interface LogoBox {
  inkW: number;
  inkH: number;
  imgW: number;
  imgH: number;
  offsetX: number;
  offsetY: number;
}

/**
 * 按「视觉面积」统一台标大小：墨迹面积尽量等于 area，同时不超过 maxW × maxH。
 * 这样细长的字标会更宽更矮、方形徽标会顶到高度上限，看起来分量相近，而不是按图片尺寸一律缩放。
 * 没有测量数据（新加的台）时返回 null，由调用方退回普通缩放。
 * 原生端的 shared/.../station/StationLogo.kt 是同一套算法。
 */
export function fitLogo(stationId: string, maxW: number, maxH: number, area: number): LogoBox | null {
  const ink = STATION_LOGO_INK[stationId];
  if (ink == null) {
    return null;
  }
  const [aspect, left, top, right, bottom] = ink;
  const inkAspect = (aspect * (right - left)) / (bottom - top);
  const inkH = Math.min(maxH, Math.sqrt(area / inkAspect), maxW / inkAspect);
  const inkW = inkH * inkAspect;
  const imgH = inkH / (bottom - top);
  const imgW = imgH * aspect;
  return { inkW, inkH, imgW, imgH, offsetX: left * imgW, offsetY: top * imgH };
}
