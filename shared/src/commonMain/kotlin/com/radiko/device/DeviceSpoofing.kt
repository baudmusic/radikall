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
        "8.2.4" to "aSmartPhone8",
        "8.2.2" to "aSmartPhone8",
        "8.2.1" to "aSmartPhone8",
        "8.2.0" to "aSmartPhone8",
        "8.1.11" to "aSmartPhone8",
        "8.1.8" to "aSmartPhone8",
        "8.1.7" to "aSmartPhone8",
        "8.1.6" to "aSmartPhone8",
        "8.1.5" to "aSmartPhone8",
        "8.1.4" to "aSmartPhone8",
        "8.1.2" to "aSmartPhone8",
        "8.1.1" to "aSmartPhone8",
        "8.1.0" to "aSmartPhone8",
        "8.0.16" to "aSmartPhone8",
        "8.0.15" to "aSmartPhone8",
        "8.0.14" to "aSmartPhone8",
        "8.0.12" to "aSmartPhone8",
        "8.0.11" to "aSmartPhone8",
        "8.0.10" to "aSmartPhone8",
        "8.0.9" to "aSmartPhone8",
        "8.0.7" to "aSmartPhone8",
        "8.0.6" to "aSmartPhone8",
        "8.0.5" to "aSmartPhone8",
        "8.0.4" to "aSmartPhone8",
        "8.0.3" to "aSmartPhone8",
        "8.0.2" to "aSmartPhone8"
    )

    private val versionMap: Map<String, AndroidVersionSeed> = mapOf(
        "7.0.0" to AndroidVersionSeed("24", listOf(
            "NBD92Q", "NBD92N", "NBD92G", "NBD92F", "NBD92E", "NBD92D",
            "NBD91Z", "NBD91Y", "NBD91X", "NBD91U", "N5D91L", "NBD91P",
            "NRD91K", "NRD91N", "NBD90Z", "NBD90X", "NBD90W", "NRD91D",
            "NRD90U", "NRD90T", "NRD90S", "NRD90R", "NRD90M"
        )),
        "7.1.0" to AndroidVersionSeed("25", listOf(
            "NDE63X", "NDE63V", "NDE63U", "NDE63P", "NDE63L", "NDE63H"
        )),
        "7.1.1" to AndroidVersionSeed("25", listOf(
            "N9F27M", "NGI77B", "N6F27M", "N4F27P", "N9F27L", "NGI55D",
            "N4F27O", "N8I11B", "N9F27H", "N6F27I", "N4F27K", "N9F27F",
            "N6F27H", "N4F27I", "N9F27C", "N6F27E", "N4F27E", "N6F27C",
            "N4F27B", "N6F26Y", "NOF27D", "N4F26X", "N4F26U", "N6F26U",
            "NUF26N", "NOF27C", "NOF27B", "N4F26T", "NMF27D", "NMF26X",
            "NOF26W", "NOF26V", "N6F26R", "NUF26K", "N4F26Q", "N4F26O",
            "N6F26Q", "N4F26M", "N4F26J", "N4F26I", "NMF26V", "NMF26U",
            "NMF26R", "NMF26Q", "NMF26O", "NMF26J", "NMF26H", "NMF26F"
        )),
        "7.1.2" to AndroidVersionSeed("25", listOf(
            "N2G48H", "NZH54D", "NKG47S", "NHG47Q", "NJH47F", "N2G48C",
            "NZH54B", "NKG47M", "NJH47D", "NHG47O", "N2G48B", "N2G47Z",
            "NJH47B", "NJH34C", "NKG47L", "NHG47N", "N2G47X", "N2G47W",
            "NHG47L", "N2G47T", "N2G47R", "N2G47O", "NHG47K", "N2G47J",
            "N2G47H", "N2G47F", "N2G47E", "N2G47D"
        )),
        "8.0.0" to AndroidVersionSeed("26", listOf(
            "5650811", "5796467", "5948681", "6107732", "6127070"
        )),
        "8.1.0" to AndroidVersionSeed("27", listOf(
            "5794017", "6107733", "6037697"
        )),
        "9.0.0" to AndroidVersionSeed("28", listOf(
            "5948683", "5794013", "6127072"
        )),
        "10.0.0" to AndroidVersionSeed("29", listOf(
            "5933585", "6969601", "7023426", "7070703"
        )),
        "11.0.0" to AndroidVersionSeed("30", listOf(
            "RP1A.201005.006", "RQ1A.201205.011", "RQ1A.210105.002"
        )),
        "12.0.0" to AndroidVersionSeed("31", listOf(
            "SD1A.210817.015.A4", "SD1A.210817.019.B1", "SD1A.210817.037", "SQ1D.220105.007"
        )),
        "13.0.0" to AndroidVersionSeed("33", listOf(
            "TQ3C.230805.001.B2", "TQ3A.230805.001.A2", "TQ3A.230705.001.A1", "TQ2B.230505.005.A1", "TQ2A.230505.002", "TQ2A.230405.003.E1"
        )),
        "14.0.0" to AndroidVersionSeed("34", listOf(
            "AP2A.240805.005.S4", "AD1A.240905.004", "AP2A.240905.003", "AD1A.240530.047", "AD1A.240530.030", "AP2A.240805.005"
        )),
        "15.0.0" to AndroidVersionSeed("35", listOf(
            "AP4A.250105.002.B1", "AP4A.250105.002.A1", "AP4A.241205.013", "AP3A.241005.015"
        ))
    )

    private val modelList = listOf(
        "SC-02H", "SCV33", "SM-G935F", "SM-G935X", "SM-G935W8", "SM-G935K", "SM-G935L", "SM-G935S",
        "SAMSUNG-SM-G935A", "SM-G935VC", "SM-G9350", "SM-G935P", "SM-G935T", "SM-G935U", "SM-G935R4", "SM-G935V",
        "SC-02J", "SCV36", "SM-G950F", "SM-G950N", "SM-G950W", "SM-G9500", "SM-G9508", "SM-G950U",
        "SM-G950U1", "SM-G892A", "SM-G892U", "SC-03J", "SCV35", "SM-G955F", "SM-G955N", "SM-G955W",
        "SM-G9550", "SM-G955U", "SM-G955U1", "SM-G960F", "SM-G960N", "SM-G9600", "SM-G9608", "SM-G960W",
        "SM-G960U", "SM-G960U1", "SM-G965F", "SM-G965N", "SM-G9650", "SM-G965W", "SM-G965U", "SM-G965U1",
        "SC-01J", "SCV34", "SM-N930F", "SM-N930X", "SM-N930K", "SM-N930L", "SM-N930S", "SM-N930R7",
        "SAMSUNG-SM-N930A", "SM-N930W8", "SM-N9300", "SGH-N037", "SM-N930R6", "SM-N930P", "SM-N930VL", "SM-N930T",
        "SM-N930U", "SM-N930R4", "SM-N930V", "SC-01K", "SCV37", "SM-N950F", "SM-N950N", "SM-N950XN",
        "SM-N950U", "SM-N9500", "SM-N9508", "SM-N950W", "SM-N950U1", "WX06K", "404KC", "503KC",
        "602KC", "KYV32", "E6782", "KYL22", "WX04K", "KYV36", "KYL21", "302KC",
        "KYV36", "KYV42", "KYV37", "C5155", "SKT01", "KYY24", "KYV35", "KYV41",
        "E6715", "KYY21", "KYY22", "KYY23", "KYV31", "KYV34", "KYV38", "WX10K",
        "KYL23", "KYV39", "KYV40", "C6902", "C6903", "C6906", "C6916", "C6943",
        "L39h", "L39t", "L39u", "SO-01F", "SOL23", "D5503", "M51w", "SO-02F",
        "D6502", "D6503", "D6543", "SO-03F", "SGP511", "SGP512", "SGP521", "SGP551",
        "SGP561", "SO-05F", "SOT21", "D6563", "401SO", "D6603", "D6616", "D6643",
        "D6646", "D6653", "SO-01G", "SOL26", "D6603", "D5803", "D5833", "SO-02G",
        "D5803", "D6633", "D6683", "SGP611", "SGP612", "SGP621", "SGP641", "E6553",
        "E6533", "D6708", "402SO", "SO-03G", "SOV31", "SGP712", "SGP771", "SO-05G",
        "SOT31", "E6508", "501SO", "E6603", "E6653", "SO-01H", "SOV32", "E5803",
        "E5823", "SO-02H", "E6853", "E6883", "SO-03H", "E6833", "E6633", "E6683",
        "C6502", "C6503", "C6506", "L35h", "SOL25", "C5306", "C5502", "C5503",
        "601SO", "F8331", "F8332", "SO-01J", "SOV34", "G8141", "G8142", "G8188",
        "SO-04J", "701SO", "G8341", "G8342", "G8343", "SO-01K", "SOV36", "G8441",
        "SO-02K", "602SO", "G8231", "G8232", "SO-03J", "SOV35", "605SH", "SH-03J",
        "SHV39", "701SH", "SH-M06", "101F", "201F", "202F", "301F", "IS12F",
        "F-03D", "F-03E", "M01", "M305", "M357", "M555", "M555", "F-11D",
        "F-06E", "EM01F", "F-05E", "FJT21", "F-01D", "FAR70B", "FAR7", "F-04E",
        "F-02E", "F-10D", "F-05D", "FJL22", "ISW11F", "ISW13F", "FJL21", "F-074",
        "F-07D", "G9FPL", "GWKK3", "GHL1X", "G0DZQ", "G82U8", "GP4BC", "GE2AE",
        "GVU6C", "GQML3", "GX7AS", "GB62Z", "G1AZG", "GLUOG", "G8VOU", "GB7N6",
        "G9S9B16", "G1F8F", "G4S1M", "GD1YQ", "GTT9Q"
    )

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
        val device = "${versionSeed.sdk}.$model"
        val userAgent = "Dalvik/2.1.0 (Linux; U; Android $androidVersion; $model/$build)"
        return DeviceInfo(
            appVersion = appVersion,
            appName = appName,
            userId = userId,
            userAgent = userAgent,
            device = device,
        )
    }
}
