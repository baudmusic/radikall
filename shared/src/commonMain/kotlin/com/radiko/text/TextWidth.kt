package com.radiko.text

/**
 * radiko 的台名和节目名里常有全角英数（「ＨＢＣラジオ」「深夜便ＮＥＸＴ」），显示出来字距很松。这里转成半角：
 *
 * - 全角字母、数字一律转；
 * - 全角符号和全角空格只在两边都是半角英数时才转（「Ｖｏｌ．２」→「Vol.2」「ＴＯＫＹＯ　ＢＥＡＴＳ」→「TOKYO BEATS」），
 *   日文语境里的「らじお！」「ニチレイプレゼンツ　オードリー」保持原样；
 * - 括号和「～」一律不动，免得「（６）」变成「（6)」这种不成对的样子。
 *
 * Web 端 webApp/worker/src/text.ts 是同一套规则。
 */
fun String.narrowAlphanumerics(): String {
    if (none { it in FULLWIDTH_ASCII || it == IDEOGRAPHIC_SPACE }) return this
    val chars = CharArray(length) { index ->
        val char = this[index]
        if (char.isFullwidthAlphanumeric()) char - FULLWIDTH_OFFSET else char
    }
    for (index in chars.indices) {
        val char = chars[index]
        val narrow = when {
            char == IDEOGRAPHIC_SPACE -> ' '
            char in FULLWIDTH_ASCII && char !in KEEP_WIDE -> char - FULLWIDTH_OFFSET
            else -> continue
        }
        if (chars.getOrNull(index - 1).isAsciiAlphanumeric() && chars.getOrNull(index + 1).isAsciiAlphanumeric()) {
            chars[index] = narrow
        }
    }
    return chars.concatToString()
}

private val FULLWIDTH_ASCII = '！'..'～'
private const val IDEOGRAPHIC_SPACE = '　'
private const val FULLWIDTH_OFFSET = 0xFEE0
private val KEEP_WIDE = setOf('（', '）', '［', '］', '｛', '｝', '＜', '＞', '～')

private fun Char.isFullwidthAlphanumeric(): Boolean =
    this in '０'..'９' || this in 'Ａ'..'Ｚ' || this in 'ａ'..'ｚ'

private fun Char?.isAsciiAlphanumeric(): Boolean =
    this != null && (this in '0'..'9' || this in 'A'..'Z' || this in 'a'..'z')
