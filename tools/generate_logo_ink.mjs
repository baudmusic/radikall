// 测量每个电台台标实际有墨迹的范围，生成两端共用的数据表：
//   shared/src/commonMain/kotlin/com/radiko/station/StationLogoInk.kt
//   webApp/src/stationLogoInk.ts
//
// radiko 的 lrtrim 台标只裁了左右，上下常留着大片透明边，直接按图片尺寸缩放时，有的台标看着比别的小一半。
// 卡片按这张表只显示墨迹部分，再按视觉面积统一大小。
//
// 用法（在仓库根目录）：node tools/generate_logo_ink.mjs
// radiko 换了台标、或注册表里加了台之后重新跑一次。只用 Node 自带模块。

import { readFileSync, writeFileSync } from "node:fs";
import { dirname, resolve } from "node:path";
import { fileURLToPath } from "node:url";
import { inflateSync } from "node:zlib";

const repoRoot = resolve(dirname(fileURLToPath(import.meta.url)), "..");
const registryPath = resolve(repoRoot, "shared/src/commonMain/kotlin/com/radiko/station/StationRegistry.kt");
const kotlinOut = resolve(repoRoot, "shared/src/commonMain/kotlin/com/radiko/station/StationLogoInk.kt");
const tsOut = resolve(repoRoot, "webApp/src/stationLogoInk.ts");

/** 卡片用的台标图：去掉左右留白的 688×160 版本，裁掉上下留白放大后在高密度屏上也清晰。 */
const logoUrl = (stationId) => `https://radiko.jp/v2/static/station/logo/${stationId}/lrtrim/688x160.png`;
/** alpha 高于这个值才算墨迹，滤掉抗锯齿的半透明毛边。 */
const ALPHA_THRESHOLD = 24;

async function main() {
  const stationIds = [...readFileSync(registryPath, "utf8").matchAll(/Station\(id = "([^"]+)"/gu)].map((m) => m[1]);
  const results = [];
  for (const stationId of stationIds) {
    const response = await fetch(logoUrl(stationId));
    if (!response.ok) {
      throw new Error(`${stationId}: HTTP ${response.status}`);
    }
    const image = decodePng(Buffer.from(await response.arrayBuffer()));
    results.push({ stationId, ...measureInk(image) });
  }

  writeFileSync(kotlinOut, renderKotlin(results));
  writeFileSync(tsOut, renderTypeScript(results));
  console.log(`Measured ${results.length} logos`);
  console.log(`  ${kotlinOut}`);
  console.log(`  ${tsOut}`);
}

function measureInk({ width, height, alphaAt }) {
  let left = width, top = height, right = -1, bottom = -1;
  for (let y = 0; y < height; y++) {
    for (let x = 0; x < width; x++) {
      if (alphaAt(x, y) > ALPHA_THRESHOLD) {
        if (x < left) left = x;
        if (x > right) right = x;
        if (y < top) top = y;
        if (y > bottom) bottom = y;
      }
    }
  }
  if (right < 0) {
    return { aspect: width / height, left: 0, top: 0, right: 1, bottom: 1 };
  }
  return {
    aspect: width / height,
    left: left / width,
    top: top / height,
    right: (right + 1) / width,
    bottom: (bottom + 1) / height,
  };
}

// ---- 最小 PNG 解码：只取 alpha。支持 8/16 位 RGBA、灰度+alpha、调色板（含 tRNS）、RGB、灰度，非隔行。 ----
function decodePng(buffer) {
  const signature = "89504e470d0a1a0a";
  if (buffer.subarray(0, 8).toString("hex") !== signature) {
    throw new Error("Not a PNG");
  }
  let offset = 8;
  let header = null;
  let transparency = null;
  const dataChunks = [];
  while (offset < buffer.length) {
    const length = buffer.readUInt32BE(offset);
    const type = buffer.toString("ascii", offset + 4, offset + 8);
    const data = buffer.subarray(offset + 8, offset + 8 + length);
    if (type === "IHDR") {
      header = {
        width: data.readUInt32BE(0),
        height: data.readUInt32BE(4),
        bitDepth: data[8],
        colorType: data[9],
        interlace: data[12],
      };
    } else if (type === "tRNS") {
      transparency = data;
    } else if (type === "IDAT") {
      dataChunks.push(data);
    } else if (type === "IEND") {
      break;
    }
    offset += 12 + length;
  }
  if (header == null) throw new Error("Missing IHDR");
  if (header.interlace !== 0) throw new Error("Interlaced PNG is not supported");

  const { width, height, bitDepth, colorType } = header;
  const channels = { 0: 1, 2: 3, 3: 1, 4: 2, 6: 4 }[colorType];
  if (channels == null) throw new Error(`Unsupported color type ${colorType}`);
  const bitsPerPixel = channels * bitDepth;
  const bytesPerPixel = Math.max(1, bitsPerPixel / 8);
  const stride = Math.ceil((width * bitsPerPixel) / 8);
  const pixels = unfilter(inflateSync(Buffer.concat(dataChunks)), height, stride, bytesPerPixel);

  const sample = (x, y, channel) => {
    const rowStart = y * stride;
    if (bitDepth === 8) return pixels[rowStart + x * channels + channel];
    if (bitDepth === 16) return pixels[rowStart + (x * channels + channel) * 2]; // 取高字节
    const bitOffset = (x * channels + channel) * bitDepth;
    const byte = pixels[rowStart + (bitOffset >> 3)];
    return (byte >> (8 - bitDepth - (bitOffset & 7))) & ((1 << bitDepth) - 1);
  };

  let alphaAt;
  if (colorType === 6) alphaAt = (x, y) => sample(x, y, 3);
  else if (colorType === 4) alphaAt = (x, y) => sample(x, y, 1);
  else if (colorType === 3) alphaAt = (x, y) => transparency?.[sample(x, y, 0)] ?? 255;
  else alphaAt = () => 255; // RGB / 灰度：按不透明处理（radiko 的台标没有用颜色键透明的）
  return { width, height, alphaAt };
}

function unfilter(raw, height, stride, bytesPerPixel) {
  const out = Buffer.alloc(stride * height);
  for (let y = 0; y < height; y++) {
    const filter = raw[y * (stride + 1)];
    const source = y * (stride + 1) + 1;
    const target = y * stride;
    for (let i = 0; i < stride; i++) {
      const left = i >= bytesPerPixel ? out[target + i - bytesPerPixel] : 0;
      const up = y > 0 ? out[target - stride + i] : 0;
      const upLeft = y > 0 && i >= bytesPerPixel ? out[target - stride + i - bytesPerPixel] : 0;
      let value = raw[source + i];
      if (filter === 1) value += left;
      else if (filter === 2) value += up;
      else if (filter === 3) value += (left + up) >> 1;
      else if (filter === 4) value += paeth(left, up, upLeft);
      else if (filter !== 0) throw new Error(`Unknown filter ${filter}`);
      out[target + i] = value & 0xff;
    }
  }
  return out;
}

function paeth(a, b, c) {
  const p = a + b - c;
  const pa = Math.abs(p - a), pb = Math.abs(p - b), pc = Math.abs(p - c);
  if (pa <= pb && pa <= pc) return a;
  return pb <= pc ? b : c;
}

// ---- 输出 ----
const fixed = (value) => Number(value.toFixed(4)).toString();

function renderKotlin(results) {
  const rows = results
    .map(({ stationId, aspect, left, top, right, bottom }) =>
      `    "${stationId}" to LogoInk(${fixed(aspect)}f, ${fixed(left)}f, ${fixed(top)}f, ${fixed(right)}f, ${fixed(bottom)}f),`)
    .join("\n");
  return `// 由 tools/generate_logo_ink.mjs 生成，不要手改；radiko 换台标或注册表加台后重新生成。
package com.radiko.station

/**
 * 台标图（[wideLogoUrl]）里实际有墨迹的范围，left/top/right/bottom 是占整张图宽高的比例，aspect 是整张图的宽高比。
 */
data class LogoInk(
    val aspect: Float,
    val left: Float,
    val top: Float,
    val right: Float,
    val bottom: Float,
)

internal val stationLogoInk: Map<String, LogoInk> = mapOf(
${rows}
)
`;
}

function renderTypeScript(results) {
  const rows = results
    .map(({ stationId, aspect, left, top, right, bottom }) =>
      `  ${JSON.stringify(stationId)}: [${[aspect, left, top, right, bottom].map(fixed).join(", ")}],`)
    .join("\n");
  return `// 由 tools/generate_logo_ink.mjs 生成，不要手改；radiko 换台标或注册表加台后重新生成。

/** 台标图（wideLogoUrl）里实际有墨迹的范围：[整张图宽高比, left, top, right, bottom]，后四个是占宽高的比例。 */
export type LogoInkTuple = readonly [aspect: number, left: number, top: number, right: number, bottom: number];

export const STATION_LOGO_INK: Readonly<Record<string, LogoInkTuple>> = {
${rows}
};
`;
}

await main();
