import { mkdirSync, readFileSync, writeFileSync } from "node:fs";
import { dirname, resolve } from "node:path";

const repoRoot = resolve(import.meta.dirname, "..", "..");
const sharedRoot = resolve(repoRoot, "shared", "src", "commonMain", "kotlin", "com", "radiko");

const stationRegistrySource = readFileSync(resolve(sharedRoot, "station", "StationRegistry.kt"), "utf8");
const prefectureDataSource = readFileSync(resolve(sharedRoot, "station", "PrefectureData.kt"), "utf8");
const localizationSource = readFileSync(resolve(sharedRoot, "i18n", "AppLocalization.kt"), "utf8");
const gpsSource = readFileSync(resolve(sharedRoot, "device", "GpsSpoofing.kt"), "utf8");
const keyStoreSource = readFileSync(resolve(sharedRoot, "crypto", "RadikoKeyStore.kt"), "utf8");

const outputPath = resolve(repoRoot, "webApp", "worker", "generated", "worker-data.json");

const DEFAULT_AREA_ID = "JP13";
const SUPPORTED_LANGUAGES = ["zh-CN", "zh-TW", "en", "ja", "ko"];
const FEATURE_FLAGS = {
  autoPlayOnLaunch: false,
  backgroundPlaybackBestEffort: true,
  alarms: false,
  pushReminder: false,
  sleepTimer: false,
  wifiOnlyPlayback: false,
  confirmMobileDataPlayback: false,
};

function main() {
  const regionTranslations = parseLocalizedMap(
    between(localizationSource, "private val regionTranslations = mapOf(", "private val prefectureTranslations = mapOf("),
  );
  const prefectureTranslations = parseLocalizedMap(
    after(localizationSource, "private val prefectureTranslations = mapOf("),
  );

  const regions = parseRegions(prefectureDataSource).map((region) => ({
    id: region.id,
    names: regionTranslations.get(region.id) ?? localizedFallback(region.name),
  }));

  const prefecturesByRegion = parsePrefecturesByRegion(prefectureDataSource);
  const areaCodeMap = parseAreaCodeMap(prefectureDataSource);
  const prefectures = prefecturesByRegion.flatMap(({ regionId, prefectures: regionPrefectures }) =>
    regionPrefectures.map((prefecture) => ({
      id: prefecture.id,
      regionId,
      areaCode: areaCodeMap.get(prefecture.id) ?? "",
      names: prefectureTranslations.get(prefecture.id) ?? localizedFallback(prefecture.name),
    })),
  );

  const stations = parseStations(stationRegistrySource).map((station) => ({
    id: station.id,
    name: station.name,
    areaIds: station.areaIds,
    logoUrl: `https://radiko.jp/v2/static/station/logo/${station.id}/224x100.png`,
  }));

  const gpsCoordinates = parseGpsCoordinates(gpsSource);
  const fullKeyBase64 = parseFullKeyBase64(keyStoreSource);

  const payload = {
    generatedAt: new Date().toISOString(),
    bootstrap: {
      defaultAreaId: DEFAULT_AREA_ID,
      supportedLanguages: SUPPORTED_LANGUAGES,
      featureFlags: FEATURE_FLAGS,
      regions,
      prefectures,
      stations,
    },
    gpsCoordinates,
    fullKeyBase64,
  };

  mkdirSync(dirname(outputPath), { recursive: true });
  writeFileSync(outputPath, JSON.stringify(payload, null, 2) + "\n", "utf8");
  process.stdout.write(`Generated ${outputPath}\n`);
}

function parseRegions(source) {
  const regionSection = between(source, "val regions = listOf(", "\n    )\n\n    private val prefecturesByRegion");
  return [...regionSection.matchAll(/Region\(id = "([^"]+)", name = "((?:[^"\\]|\\.)*)"\)/g)].map(
    ([, id, name]) => ({
      id,
      name: unescapeKotlinString(name),
    }),
  );
}

function parsePrefecturesByRegion(source) {
  const section = between(source, "private val prefecturesByRegion: Map<String, List<Prefecture>> = mapOf(", "\n    )\n\n    val areaCodeMap");
  return [...section.matchAll(/"([^"]+)"\s+to\s+listOf\(([\s\S]*?)\n\s*\)/g)].map(([_, regionId, block]) => ({
    regionId,
    prefectures: [...block.matchAll(/Prefecture\(id = "([^"]+)", name = "((?:[^"\\]|\\.)*)"\)/g)].map(
      ([, id, name]) => ({
        id,
        name: unescapeKotlinString(name),
      }),
    ),
  }));
}

function parseAreaCodeMap(source) {
  const section = between(source, "val areaCodeMap: Map<String, String> = mapOf(", "\n    )\n\n    val allPrefectures");
  return new Map(
    [...section.matchAll(/"(JP\d+)"\s+to\s+"([^"]+)"/g)].map(([_, id, areaCode]) => [id, areaCode]),
  );
}

function parseStations(source) {
  return [...source.matchAll(/Station\(id = "([^"]+)", name = "((?:[^"\\]|\\.)*)", areaIds = listOf\(([^)]*)\)\)/g)].map(
    ([, id, name, areaIdsBlock]) => ({
      id,
      name: unescapeKotlinString(name),
      areaIds: [...areaIdsBlock.matchAll(/"(JP\d+)"/g)].map((match) => match[1]),
    }),
  );
}

function parseLocalizedMap(section) {
  return new Map(
    [...section.matchAll(/"([^"]+)"\s+to\s+LocalizedText\("((?:[^"\\]|\\.)*)",\s*"((?:[^"\\]|\\.)*)",\s*"((?:[^"\\]|\\.)*)",\s*"((?:[^"\\]|\\.)*)",\s*"((?:[^"\\]|\\.)*)"\)/g)].map(
      ([, key, zhCn, zhTw, en, ja, ko]) => [
        key,
        {
          "zh-CN": unescapeKotlinString(zhCn),
          "zh-TW": unescapeKotlinString(zhTw),
          en: unescapeKotlinString(en),
          ja: unescapeKotlinString(ja),
          ko: unescapeKotlinString(ko),
        },
      ],
    ),
  );
}

function parseGpsCoordinates(source) {
  const areaListSection = between(source, "private val areaList = listOf(", "\n    )\n\n    private val coordinates");
  const prefectureNames = [...areaListSection.matchAll(/"((?:[^"\\]|\\.)*)"/g)].map((match) =>
    unescapeKotlinString(match[1]),
  );

  const coordinateSection = between(
    source,
    "private val coordinates: Map<String, Pair<Double, Double>> = mapOf(",
    "\n    )\n\n    fun prefectureName",
  );
  const coordinatesByName = new Map(
    [...coordinateSection.matchAll(/"((?:[^"\\]|\\.)*)"\s+to\s+Pair\((-?\d+(?:\.\d+)?),\s*(-?\d+(?:\.\d+)?)\)/g)].map(
      ([, name, lat, lng]) => [
        unescapeKotlinString(name),
        {
          latitude: Number.parseFloat(lat),
          longitude: Number.parseFloat(lng),
        },
      ],
    ),
  );

  return Object.fromEntries(
    prefectureNames.map((name, index) => {
      const areaId = `JP${index + 1}`;
      const coordinate = coordinatesByName.get(name);
      if (coordinate == null) {
        throw new Error(`Missing GPS coordinate for ${name} (${areaId})`);
      }
      return [areaId, coordinate];
    }),
  );
}

function parseFullKeyBase64(source) {
  const block = between(source, 'val FULL_KEY_BASE64: String = buildString {', "\n    }\n}");
  const parts = [...block.matchAll(/append\("((?:[^"\\]|\\.)*)"\)/g)].map((match) => unescapeKotlinString(match[1]));
  if (parts.length === 0) {
    throw new Error("Unable to extract FULL_KEY_BASE64");
  }
  return parts.join("");
}

function localizedFallback(value) {
  return {
    "zh-CN": value,
    "zh-TW": value,
    en: value,
    ja: value,
    ko: value,
  };
}

function between(source, startToken, endToken) {
  const startIndex = source.indexOf(startToken);
  if (startIndex < 0) {
    throw new Error(`Start token not found: ${startToken}`);
  }
  const contentStart = startIndex + startToken.length;
  const endIndex = source.indexOf(endToken, contentStart);
  if (endIndex < 0) {
    throw new Error(`End token not found for: ${startToken}`);
  }
  return source.slice(contentStart, endIndex);
}

function after(source, startToken) {
  const startIndex = source.indexOf(startToken);
  if (startIndex < 0) {
    throw new Error(`Start token not found: ${startToken}`);
  }
  return source.slice(startIndex + startToken.length);
}

function unescapeKotlinString(value) {
  return JSON.parse(`"${value.replace(/\\/g, "\\\\").replace(/"/g, '\\"')}"`);
}

main();
