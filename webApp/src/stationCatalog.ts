import { localizeText } from "./i18n";
import type { BootstrapResponse, LanguageCode, StationSummary } from "./types";

/** 地区选择器里「全国」一项的 ID：浏览全部电台并按大区分组。它不是真实地区，不能拿去认证。 */
export const ALL_AREAS_ID = "ALL";

/** 电台的覆盖范围，用于卡片上的地区标签和「全国」总表的分组。 */
export type StationCoverage =
  | { kind: "nationwide" }
  | { kind: "prefecture"; areaId: string }
  | { kind: "region"; regionId: string };

export type StationSectionKind =
  /** 普通的单地区列表，没有分组标题。 */
  | "area"
  /** 搜索时，当前浏览地区内的结果。 */
  | "current-area"
  /** 搜索时，当前浏览地区以外的结果，卡片上要标出来自哪里。 */
  | "other-areas"
  /** 「全国」总表里覆盖全部 47 个都道府县的电台。 */
  | "nationwide"
  /** 「全国」总表里某个大区的电台。 */
  | "region";

export interface StationSection {
  key: string;
  kind: StationSectionKind;
  /** area / current-area 时是地区 ID，region 时是大区 ID，其余为 null。 */
  groupId: string | null;
  stations: StationSummary[];
}

export interface StationCatalog {
  isNationwide(station: StationSummary): boolean;
  coverage(station: StationSummary): StationCoverage;
  /** 电台的收听地区，按地区选择器里的顺序排好。 */
  sortedAreaIds(station: StationSummary): string[];
  /** 搜索词为空时返回普通列表；有搜索词时永远覆盖全部电台。 */
  sections(areaId: string, query: string): StationSection[];
  /**
   * 要拿到这些电台的当前节目，最少该请求哪几个地区（贪心近似）。
   * 覆盖数相同时优先 preferredAreaIds 里靠前的地区，其次按地区选择器顺序。
   */
  coveringAreas(stations: StationSummary[], preferredAreaIds?: string[]): string[];
}

/**
 * 把全部电台整理成首页要显示的分组：单地区列表、全局搜索结果、「全国」总表。
 * 原生端的 shared/.../station/StationCatalog.kt 是同一套规则，两边改动时要保持一致。
 */
export function createStationCatalog(bootstrap: BootstrapResponse): StationCatalog {
  const { stations, prefectures, regions } = bootstrap;
  const prefectureIndex = new Map(prefectures.map((prefecture, index) => [prefecture.id, index]));
  const regionIndex = new Map(regions.map((region, index) => [region.id, index]));
  const regionByArea = new Map(prefectures.map((prefecture) => [prefecture.id, prefecture.regionId]));

  // 可被搜索的地名（都道府县、大区或大区的一半）及其对应的都道府县。
  const areaSearchGroups: Array<{ names: string[]; areaIds: Set<string> }> = [
    ...prefectures.map((prefecture) => ({
      names: searchNames(Object.values(prefecture.names)),
      areaIds: new Set([prefecture.id]),
    })),
    ...regions.flatMap((region) => {
      const names = Object.values(region.names);
      const parts = COMPOSITE_REGION_PARTS[region.id];
      if (parts == null) {
        const areaIds = prefectures.filter((prefecture) => prefecture.regionId === region.id).map((item) => item.id);
        return [{ names: searchNames(names), areaIds: new Set(areaIds) }];
      }
      return parts.map((areaIds, index) => ({
        names: searchNames(names.map((name) => name.split(REGION_NAME_SEPARATOR)[index] ?? "")),
        areaIds: new Set(areaIds),
      }));
    }),
  ];

  // 搜索词点名的都道府县。单个英文字母太泛（f 会命中 Fukuoka、Fukui……），不当地名用。
  const areasNamed = (query: string): Set<string> => {
    if (query.length < 2 && /^[\x00-\x7f]*$/u.test(query)) {
      return new Set();
    }
    return new Set(
      areaSearchGroups
        .filter((group) => group.names.some((name) => name.startsWith(query)))
        .flatMap((group) => [...group.areaIds]),
    );
  };

  const isNationwide = (station: StationSummary): boolean =>
    prefectures.every((prefecture) => station.areaIds.includes(prefecture.id));

  // 覆盖都道府县最多的大区；数量相同时取靠前的大区。
  const primaryRegionId = (station: StationSummary): string | null => {
    const counts = new Map<string, number>();
    for (const areaId of station.areaIds) {
      const regionId = regionByArea.get(areaId);
      if (regionId != null) {
        counts.set(regionId, (counts.get(regionId) ?? 0) + 1);
      }
    }
    let best: string | null = null;
    for (const [regionId, count] of counts) {
      const bestCount = best == null ? -1 : counts.get(best) ?? -1;
      if (
        count > bestCount
        || (count === bestCount && (regionIndex.get(regionId) ?? Infinity) < (regionIndex.get(best!) ?? Infinity))
      ) {
        best = regionId;
      }
    }
    return best;
  };

  const groupIndex = (station: StationSummary): number => {
    if (isNationwide(station)) {
      return 0;
    }
    const regionId = primaryRegionId(station);
    return 1 + (regionId == null ? regions.length : regionIndex.get(regionId) ?? regions.length);
  };

  const firstPrefectureIndex = (station: StationSummary): number => {
    const regionId = isNationwide(station) ? null : primaryRegionId(station);
    const indexes = station.areaIds
      .filter((areaId) => regionId == null || regionByArea.get(areaId) === regionId)
      .map((areaId) => prefectureIndex.get(areaId) ?? Infinity);
    return indexes.length === 0 ? Infinity : Math.min(...indexes);
  };

  // 全国台、各大区依次排列，大区内按覆盖的第一个都道府县、再按台号排。
  const catalogOrder = (left: StationSummary, right: StationSummary): number =>
    groupIndex(left) - groupIndex(right)
    || firstPrefectureIndex(left) - firstPrefectureIndex(right)
    || compareText(left.id, right.id);

  // 0：台号或台名以搜索词开头；1：台号或台名包含搜索词；2：能在搜索词点名的地区收听。
  const matchScore = (station: StationSummary, query: string, matchedAreaIds: Set<string>): number | null => {
    const id = normalizeForSearch(station.id);
    const name = normalizeForSearch(station.name);
    if (id.startsWith(query) || name.startsWith(query)) return 0;
    if (id.includes(query) || name.includes(query)) return 1;
    // 全国台在每个地区都收得到，按地区名搜时不列出，否则每次都混进同样几个台。
    if (isNationwide(station)) return null;
    return station.areaIds.some((areaId) => matchedAreaIds.has(areaId)) ? 2 : null;
  };

  const groupByRegion = (items: StationSummary[], scores: Map<string, number>): StationSection[] => {
    const order = (left: StationSummary, right: StationSummary) =>
      (scores.get(left.id) ?? 0) - (scores.get(right.id) ?? 0) || catalogOrder(left, right);
    const sections: StationSection[] = [
      {
        key: "nationwide",
        kind: "nationwide",
        groupId: null,
        stations: items.filter(isNationwide).sort(order),
      },
      ...regions.map((region) => ({
        key: `region:${region.id}`,
        kind: "region" as const,
        groupId: region.id,
        stations: items
          .filter((station) => !isNationwide(station) && primaryRegionId(station) === region.id)
          .sort(order),
      })),
    ];
    return sections.filter((section) => section.stations.length > 0);
  };

  return {
    isNationwide,
    coverage(station) {
      if (isNationwide(station)) {
        return { kind: "nationwide" };
      }
      if (station.areaIds.length === 1) {
        return { kind: "prefecture", areaId: station.areaIds[0] };
      }
      const regionId = primaryRegionId(station);
      return regionId == null
        ? { kind: "prefecture", areaId: station.areaIds[0] ?? "" }
        : { kind: "region", regionId };
    },
    coveringAreas(items, preferredAreaIds = []) {
      const rank = (areaId: string): number => {
        const preferredIndex = preferredAreaIds.indexOf(areaId);
        return preferredIndex >= 0 ? preferredIndex : preferredAreaIds.length + (prefectureIndex.get(areaId) ?? Infinity);
      };
      let uncovered = [...new Map(items.map((station) => [station.id, station])).values()];
      const chosen: string[] = [];
      while (uncovered.length > 0) {
        const candidates = [...new Set(uncovered.flatMap((station) => station.areaIds))];
        if (candidates.length === 0) {
          break;
        }
        let best = candidates[0];
        let bestCount = -1;
        for (const areaId of candidates) {
          const count = uncovered.filter((station) => station.areaIds.includes(areaId)).length;
          if (count > bestCount || (count === bestCount && rank(areaId) < rank(best))) {
            best = areaId;
            bestCount = count;
          }
        }
        chosen.push(best);
        uncovered = uncovered.filter((station) => !station.areaIds.includes(best));
      }
      return chosen;
    },
    sortedAreaIds(station) {
      return [...station.areaIds].sort(
        (left, right) => (prefectureIndex.get(left) ?? Infinity) - (prefectureIndex.get(right) ?? Infinity),
      );
    },
    sections(areaId, query) {
      const normalizedQuery = normalizeForSearch(query);
      if (normalizedQuery === "") {
        if (areaId === ALL_AREAS_ID) {
          return groupByRegion(stations, new Map());
        }
        const inArea = stations.filter((station) => station.areaIds.includes(areaId));
        return inArea.length === 0 ? [] : [{ key: `area:${areaId}`, kind: "area", groupId: areaId, stations: inArea }];
      }

      const matchedAreaIds = areasNamed(normalizedQuery);
      const scores = new Map<string, number>();
      for (const station of stations) {
        const score = matchScore(station, normalizedQuery, matchedAreaIds);
        if (score != null) {
          scores.set(station.id, score);
        }
      }
      const matches = stations.filter((station) => scores.has(station.id));
      if (areaId === ALL_AREAS_ID) {
        return groupByRegion(matches, scores);
      }

      const order = (left: StationSummary, right: StationSummary) =>
        (scores.get(left.id) ?? 0) - (scores.get(right.id) ?? 0) || catalogOrder(left, right);
      const sections: StationSection[] = [
        {
          key: `current-area:${areaId}`,
          kind: "current-area",
          groupId: areaId,
          stations: matches.filter((station) => station.areaIds.includes(areaId)).sort(order),
        },
        {
          key: "other-areas",
          kind: "other-areas",
          groupId: null,
          stations: matches.filter((station) => !station.areaIds.includes(areaId)).sort(order),
        },
      ];
      return sections.filter((section) => section.stations.length > 0);
    },
  };
}

/** 地区选择器里显示的名字；ALL_AREAS_ID 显示为「全国」。 */
export function areaLabel(bootstrap: BootstrapResponse | null, areaId: string, language: LanguageCode): string {
  if (areaId === ALL_AREAS_ID) {
    return catalogCopy(language).allAreas;
  }
  const prefecture = bootstrap?.prefectures.find((item) => item.id === areaId);
  return prefecture ? localizeText(prefecture.names, language) : areaId;
}

export function coverageLabel(
  bootstrap: BootstrapResponse | null,
  coverage: StationCoverage,
  language: LanguageCode,
): string {
  switch (coverage.kind) {
    case "nationwide":
      return catalogCopy(language).allAreas;
    case "prefecture":
      return areaLabel(bootstrap, coverage.areaId, language);
    case "region": {
      const region = bootstrap?.regions.find((item) => item.id === coverage.regionId);
      return region ? localizeText(region.names, language) : coverage.regionId;
    }
  }
}

/** 首页分组标题；普通单地区列表没有标题，返回 null。 */
export function sectionTitle(
  bootstrap: BootstrapResponse | null,
  section: StationSection,
  language: LanguageCode,
): string | null {
  const copy = catalogCopy(language);
  switch (section.kind) {
    case "area":
      return null;
    case "current-area":
      return areaLabel(bootstrap, section.groupId ?? "", language);
    case "other-areas":
      return copy.otherAreasSection;
    case "nationwide":
      return copy.nationwideSection;
    case "region":
      return coverageLabel(bootstrap, { kind: "region", regionId: section.groupId ?? "" }, language);
  }
}

const CATALOG_COPY: Record<
  LanguageCode,
  { allAreas: string; nationwideSection: string; otherAreasSection: string; serviceAreasLabel: string }
> = {
  "zh-CN": { allAreas: "全国", nationwideSection: "全国播出", otherAreasSection: "其他地区", serviceAreasLabel: "可收听地区" },
  "zh-TW": { allAreas: "全國", nationwideSection: "全國播出", otherAreasSection: "其他地區", serviceAreasLabel: "可收聽地區" },
  en: { allAreas: "All Japan", nationwideSection: "Nationwide", otherAreasSection: "Other areas", serviceAreasLabel: "Available in" },
  ja: { allAreas: "全国", nationwideSection: "全国放送", otherAreasSection: "他の地域", serviceAreasLabel: "聴取可能エリア" },
  ko: { allAreas: "전국", nationwideSection: "전국 방송", otherAreasSection: "다른 지역", serviceAreasLabel: "청취 가능 지역" },
};

export function catalogCopy(language: LanguageCode) {
  return CATALOG_COPY[language];
}

/** 卡片上的「播到几点」：endAt 是东京时间 yyyyMMddHHmmss。 */
export function formatUntil(language: LanguageCode, endAt: string): string {
  const time = `${endAt.slice(8, 10)}:${endAt.slice(10, 12)}`;
  switch (language) {
    case "en":
      return `until ${time}`;
    case "ko":
      return `~${time}`;
    case "ja":
      return `〜${time}`;
    default:
      return `～${time}`;
  }
}

/** 全角英数转半角、去首尾空白、转小写，让「ＦＭ」和「fm」、「ＨＢＣ」和「hbc」能互相匹配。 */
export function normalizeForSearch(text: string): string {
  let folded = "";
  for (const char of text) {
    const code = char.charCodeAt(0);
    if (code >= 0xff01 && code <= 0xff5e) {
      folded += String.fromCharCode(code - 0xfee0);
    } else if (code === 0x3000) {
      folded += " ";
    } else {
      folded += char;
    }
  }
  return folded.trim().toLowerCase();
}

/**
 * 由两部分组成的大区名（「北海道・東北」「Hokkaido / Tohoku」）里，每一部分各自对应的都道府县。
 * 各语言的名字都按同样顺序写，所以搜「東北」「Tohoku」只命中东北六县，搜「北海道」不会带出东北。
 */
const COMPOSITE_REGION_PARTS: Record<string, string[][]> = {
  "hokkaido-tohoku": [["JP1"], ["JP2", "JP3", "JP4", "JP5", "JP6", "JP7"]],
  "hokuriku-koushinetsu": [["JP16", "JP17", "JP18"], ["JP15", "JP19", "JP20"]],
  "chugoku-shikoku": [["JP31", "JP32", "JP33", "JP34", "JP35"], ["JP36", "JP37", "JP38", "JP39"]],
  kyushu: [["JP40", "JP41", "JP42", "JP43", "JP44", "JP45", "JP46"], ["JP47"]],
};

const REGION_NAME_SEPARATOR = /[・/／]/u;

function searchNames(names: string[]): string[] {
  return [...new Set(names.map(normalizeForSearch).filter((name) => name !== ""))];
}

// Kotlin 的 compareBy 按码点比较字符串，这里也用码点比较，保证两端排序一致。
function compareText(left: string, right: string): number {
  return left < right ? -1 : left > right ? 1 : 0;
}
