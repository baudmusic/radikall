import fs from "node:fs";
import path from "node:path";
import { fileURLToPath } from "node:url";

const __filename = fileURLToPath(import.meta.url);
const __dirname = path.dirname(__filename);
const projectRoot = path.resolve(__dirname, "..");
const repoRoot = path.resolve(projectRoot, "..");

const staticJs = fs.readFileSync(path.join(repoRoot, "rajiko", "modules", "static.js"), "utf8");
const constantsJs = fs.readFileSync(path.join(repoRoot, "rajiko", "modules", "constants.js"), "utf8");

function extractInitializer(source, declaration) {
    const index = source.indexOf(declaration);
    if (index === -1) {
        throw new Error(`Declaration not found: ${declaration}`);
    }

    let cursor = index + declaration.length;
    while (cursor < source.length && /\s/.test(source[cursor])) {
        cursor += 1;
    }

    const initialChar = source[cursor];
    let depth = 0;
    let quote = null;
    let escaped = false;
    let inLineComment = false;
    let inBlockComment = false;
    let started = false;

    for (let i = cursor; i < source.length; i += 1) {
        const char = source[i];
        const next = source[i + 1];

        if (inLineComment) {
            if (char === "\n") {
                inLineComment = false;
            }
            continue;
        }

        if (inBlockComment) {
            if (char === "*" && next === "/") {
                inBlockComment = false;
                i += 1;
            }
            continue;
        }

        if (quote) {
            if (escaped) {
                escaped = false;
                continue;
            }

            if (char === "\\") {
                escaped = true;
                continue;
            }

            if (char === quote) {
                quote = null;
                if (started && depth === 0 && initialChar === source[cursor]) {
                    return source.slice(cursor, i + 1).trim();
                }
            }
            continue;
        }

        if (char === "/" && next === "/") {
            inLineComment = true;
            i += 1;
            continue;
        }

        if (char === "/" && next === "*") {
            inBlockComment = true;
            i += 1;
            continue;
        }

        if (char === '"' || char === "'" || char === "`") {
            started = true;
            quote = char;
            continue;
        }

        if (char === "{" || char === "[" || char === "(") {
            started = true;
            depth += 1;
            continue;
        }

        if (char === "}" || char === "]" || char === ")") {
            depth -= 1;
            if (started && depth === 0) {
                return source.slice(cursor, i + 1).trim();
            }
            continue;
        }

        if (char === ";" && depth === 0) {
            return source.slice(cursor, i).trim();
        }
    }

    throw new Error(`Unable to find expression terminator for ${declaration}`);
}

function evaluateLiteral(literal) {
    return Function(`"use strict"; return (${literal});`)();
}

function sanitizeText(value) {
    return String(value).replace(/[\u200B-\u200D\uFEFF]/g, "").replace(/\r/g, "");
}

function escapeKotlin(value) {
    return sanitizeText(value)
        .replace(/\\/g, "\\\\")
        .replace(/"/g, '\\"')
        .replace(/\$/g, "\\$");
}

function kotlinString(value) {
    return `"${escapeKotlin(value)}"`;
}

function writeGenerated(relativePath, content) {
    const target = path.join(projectRoot, relativePath);
    fs.mkdirSync(path.dirname(target), { recursive: true });
    fs.writeFileSync(target, `${content.trim()}\n`, "utf8");
    console.log(`Generated ${path.relative(projectRoot, target)}`);
}

function chunk(items, size) {
    const chunks = [];
    for (let i = 0; i < items.length; i += size) {
        chunks.push(items.slice(i, i + size));
    }
    return chunks;
}

function kotlinStringList(values, indent = "        ", chunkSize = 6) {
    const lines = chunk(values, chunkSize).map(
        (group) => `${indent}${group.map((item) => kotlinString(item)).join(", ")}`
    );
    return `listOf(\n${lines.join(",\n")}\n${indent.slice(0, -4)})`;
}

const fullKey = evaluateLiteral(extractInitializer(staticJs, "const aSmartPhone8_fullkey_b64 ="));
const versionMap = evaluateLiteral(extractInitializer(staticJs, "export const VERSION_MAP ="));
const modelList = evaluateLiteral(extractInitializer(staticJs, "export const MODEL_LIST ="));
const appVersionMap = evaluateLiteral(extractInitializer(staticJs, "export const APP_VERSION_MAP ="));
const coordinates = evaluateLiteral(extractInitializer(staticJs, "export const coordinates ="));
const regions = evaluateLiteral(extractInitializer(constantsJs, "export const regions ="));
const areaList = evaluateLiteral(extractInitializer(constantsJs, "export const areaList ="));
const areaMap = evaluateLiteral(extractInitializer(constantsJs, "export const areaMap ="));
const areaListParRegion = evaluateLiteral(extractInitializer(constantsJs, "export const areaListParRegion ="));
const radioAreaId = evaluateLiteral(extractInitializer(constantsJs, "export const radioAreaId ="));

const deviceSpoofingKt = `
package com.radiko.device

import kotlin.random.Random

private data class AndroidVersionSeed(
    val sdk: String,
    val builds: List<String>,
)

data class DeviceInfo(
    val appVersion: String,
    val appName: String,
    val userId: String,
    val userAgent: String,
    val device: String,
)

object DeviceSpoofing {
    private val appVersionMap: Map<String, String> = mapOf(
${Object.entries(appVersionMap)
    .map(([version, app]) => `        ${kotlinString(version)} to ${kotlinString(app)}`)
    .join(",\n")}
    )

    private val versionMap: Map<String, AndroidVersionSeed> = mapOf(
${Object.entries(versionMap)
    .map(
        ([version, info]) =>
            `        ${kotlinString(version)} to AndroidVersionSeed(${kotlinString(info.sdk)}, ${kotlinStringList(info.builds, "            ")})`
    )
    .join(",\n")}
    )

    private val modelList = ${kotlinStringList(modelList, "        ", 8)}

    val supportedAreas: Set<String> = versionMap.keys

    fun appNameForVersion(version: String): String = appVersionMap[version] ?: "aSmartPhone8"

    fun randomHex(length: Int, random: Random = Random.Default): String {
        val alphabet = "0123456789abcdef"
        return buildString(length) {
            repeat(length) {
                append(alphabet[random.nextInt(alphabet.length)])
            }
        }
    }

    fun generate(random: Random = Random.Default): DeviceInfo {
        val androidVersion = versionMap.keys.toList().random(random)
        val versionSeed = versionMap.getValue(androidVersion)
        val build = versionSeed.builds.random(random)
        val model = modelList.random(random)
        val appVersion = appVersionMap.keys.toList().random(random)
        val appName = appVersionMap.getValue(appVersion)
        val userId = randomHex(length = 32, random = random)
        val device = "\${versionSeed.sdk}.\$model"
        val userAgent = "Dalvik/2.1.0 (Linux; U; Android \$androidVersion; \$model/\$build)"
        return DeviceInfo(
            appVersion = appVersion,
            appName = appName,
            userId = userId,
            userAgent = userAgent,
            device = device,
        )
    }
}
`;

const gpsSpoofingKt = `
package com.radiko.device

import kotlin.random.Random

object GpsSpoofing {
    private val areaList = ${kotlinStringList(areaList, "        ", 8)}

    private val coordinates: Map<String, Pair<Double, Double>> = mapOf(
${Object.entries(coordinates)
    .map(
        ([name, [lat, lng]]) =>
            `        ${kotlinString(name)} to Pair(${lat}, ${lng})`
    )
    .join(",\n")}
    )

    fun prefectureName(areaId: String): String {
        val index = areaId.removePrefix("JP").toInt() - 1
        return areaList[index]
    }

    fun generate(areaId: String, random: Random = Random.Default): String {
        val prefecture = prefectureName(areaId)
        val (baseLat, baseLng) = coordinates.getValue(prefecture)
        val lat = baseLat + random.nextDouble() / 40.0 * if (random.nextBoolean()) 1 else -1
        val lng = baseLng + random.nextDouble() / 40.0 * if (random.nextBoolean()) 1 else -1
        return "%.6f,%.6f,gps".format(lat, lng)
    }
}
`;

const prefectureDataKt = `
package com.radiko.station

object PrefectureData {
    val regions = listOf(
${regions
    .map((region) => `        Region(id = ${kotlinString(region.id)}, name = ${kotlinString(region.name)})`)
    .join(",\n")}
    )

    private val prefecturesByRegion: Map<String, List<Prefecture>> = mapOf(
${Object.entries(areaListParRegion)
    .map(
        ([regionId, prefectures]) =>
            `        ${kotlinString(regionId)} to listOf(\n${prefectures
                .map(
                    (prefecture) =>
                        `            Prefecture(id = ${kotlinString(prefecture.id)}, name = ${kotlinString(prefecture.name)})`
                )
                .join(",\n")}\n        )`
    )
    .join(",\n")}
    )

    val areaCodeMap: Map<String, String> = mapOf(
${Object.entries(areaMap)
    .map(([id, code]) => `        ${kotlinString(id)} to ${kotlinString(code)}`)
    .join(",\n")}
    )

    val allPrefectures: List<Prefecture> = regions.flatMap { prefecturesForRegion(it.id) }

    fun prefecturesForRegion(regionId: String): List<Prefecture> = prefecturesByRegion[regionId].orEmpty()

    fun prefecture(areaId: String): Prefecture? = allPrefectures.find { it.id == areaId }

    fun regionForArea(areaId: String): Region? = regions.find { region ->
        prefecturesForRegion(region.id).any { it.id == areaId }
    }
}
`;

const stationRegistryKt = `
package com.radiko.station

object StationRegistry {
    val allStations = listOf(
${Object.entries(radioAreaId)
    .map(
        ([id, info]) =>
            `        Station(id = ${kotlinString(id)}, name = ${kotlinString(info.name)}, areaIds = listOf(${info.area
                .map((areaId) => kotlinString(areaId))
                .join(", ")}))`
    )
    .join(",\n")}
    )

    private val stationsById = allStations.associateBy { it.id }

    fun getStation(id: String): Station? = stationsById[id]

    fun getStationsForArea(areaId: String): List<Station> = allStations
        .filter { areaId in it.areaIds }
        .sortedBy { it.id }

    fun search(query: String, areaId: String? = null): List<Station> {
        val normalized = query.trim().lowercase()
        val candidateStations = if (areaId == null) allStations else getStationsForArea(areaId)
        return candidateStations.filter { station ->
            normalized.isBlank() ||
                station.id.lowercase().contains(normalized) ||
                station.name.lowercase().contains(normalized)
        }
    }
}
`;

const radikoKeyStoreKt = `
package com.radiko.crypto

object RadikoKeyStore {
    const val APP_NAME: String = "aSmartPhone8"
    val FULL_KEY_BASE64: String = buildString {
${chunk(fullKey.match(/.{1,8192}/g) ?? [fullKey], 1)
    .map(([part]) => `        append(${kotlinString(part)})`)
    .join("\n")}
    }
}
`;

writeGenerated("shared/src/commonMain/kotlin/com/radiko/device/DeviceSpoofing.kt", deviceSpoofingKt);
writeGenerated("shared/src/commonMain/kotlin/com/radiko/device/GpsSpoofing.kt", gpsSpoofingKt);
writeGenerated("shared/src/commonMain/kotlin/com/radiko/station/PrefectureData.kt", prefectureDataKt);
writeGenerated("shared/src/commonMain/kotlin/com/radiko/station/StationRegistry.kt", stationRegistryKt);
writeGenerated("shared/src/commonMain/kotlin/com/radiko/crypto/RadikoKeyStore.kt", radikoKeyStoreKt);
