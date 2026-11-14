/**
 * radiko 的台名和节目名里常有全角英数（「ＨＢＣラジオ」「深夜便ＮＥＸＴ」），显示出来字距很松。这里转成半角：
 *
 * - 全角字母、数字一律转；
 * - 全角符号和全角空格只在两边都是半角英数时才转（「Ｖｏｌ．２」→「Vol.2」「ＴＯＫＹＯ　ＢＥＡＴＳ」→「TOKYO BEATS」），
 *   日文语境里的「らじお！」「ニチレイプレゼンツ　オードリー」保持原样；
 * - 括号和「～」一律不动，免得「（６）」变成「（6)」这种不成对的样子。
 *
 * 原生端 shared/.../text/TextWidth.kt 是同一套规则。
 */
export function narrowAlphanumerics(text: string): string {
  if (!/[！-～　]/u.test(text)) return text;
  const chars = [...text].map((char) => (isFullwidthAlphanumeric(char) ? shiftToAscii(char) : char));
  for (let index = 0; index < chars.length; index += 1) {
    const char = chars[index];
    let narrow: string;
    if (char === IDEOGRAPHIC_SPACE) {
      narrow = " ";
    } else if (isFullwidthAscii(char) && !KEEP_WIDE.has(char)) {
      narrow = shiftToAscii(char);
    } else {
      continue;
    }
    if (isAsciiAlphanumeric(chars[index - 1]) && isAsciiAlphanumeric(chars[index + 1])) {
      chars[index] = narrow;
    }
  }
  return chars.join("");
}

const IDEOGRAPHIC_SPACE = "　";
const FULLWIDTH_OFFSET = 0xfee0;
const KEEP_WIDE = new Set(["（", "）", "［", "］", "｛", "｝", "＜", "＞", "～"]);

function shiftToAscii(char: string): string {
  return String.fromCharCode(char.charCodeAt(0) - FULLWIDTH_OFFSET);
}

function isFullwidthAscii(char: string): boolean {
  const code = char.charCodeAt(0);
  return code >= 0xff01 && code <= 0xff5e;
}

function isFullwidthAlphanumeric(char: string): boolean {
  return /^[０-９Ａ-Ｚａ-ｚ]$/u.test(char);
}

function isAsciiAlphanumeric(char: string | undefined): boolean {
  return char !== undefined && /^[0-9A-Za-z]$/u.test(char);
}
