import type { LanguageCode } from "./types";

export const INSTALL_GUIDE_DISMISSED_KEY = "radikall.install-guide.dismissed";

export type InstallGuideKind =
  | "ios-safari"
  | "android-installable"
  | "android-manual"
  | "unsupported";

export interface DeferredInstallPromptEvent extends Event {
  prompt(): Promise<void>;
  userChoice: Promise<{
    outcome: "accepted" | "dismissed";
    platform: string;
  }>;
}

interface InstallGuideCopy {
  title: string;
  body: string;
  steps: string[];
  actionLabel: string | null;
  dismissLabel: string;
  unsupportedPlayback: string;
}

const copyTable: Record<LanguageCode, Record<InstallGuideKind, InstallGuideCopy>> = {
  "zh-CN": {
    "ios-safari": {
      title: "安装到 iPhone 主屏",
      body: "请用 Safari 打开，并按下面三步安装。安装后会以 Web App 独立窗口打开。",
      steps: [
        "点 Safari 底部的“分享”按钮",
        "选择“添加到主屏幕”",
        "保持“作为 Web App 打开”并确认添加",
      ],
      actionLabel: null,
      dismissLabel: "稍后再说",
      unsupportedPlayback: "当前浏览器不支持 HLS 直播播放。请在 iPhone 上使用 Safari 打开并安装到主屏。",
    },
    "android-installable": {
      title: "安装到 Android 主屏",
      body: "这个浏览器已经支持直接安装，你可以一键把 Radikall 加到主屏。",
      steps: [
        "点击下面的“立即安装”",
        "按浏览器提示确认安装",
        "以后直接从主屏图标打开",
      ],
      actionLabel: "立即安装",
      dismissLabel: "稍后再说",
      unsupportedPlayback: "当前浏览器不支持 HLS 直播播放。请换到支持 HLS 的现代浏览器，或在 iPhone 上使用 Safari。",
    },
    "android-manual": {
      title: "添加到 Android 主屏",
      body: "如果没有直接弹出安装按钮，也可以从浏览器菜单手动添加到主屏。",
      steps: [
        "点浏览器右上角菜单",
        "选择“安装应用”或“添加到主屏幕”",
        "确认后从主屏图标打开",
      ],
      actionLabel: null,
      dismissLabel: "稍后再说",
      unsupportedPlayback: "当前浏览器不支持 HLS 直播播放。请换到支持 HLS 的现代浏览器，或在 iPhone 上使用 Safari。",
    },
    unsupported: {
      title: "建议换浏览器安装",
      body: "为了获得最稳定的体验，iPhone 请使用 Safari，Android 请使用 Chrome。",
      steps: [
        "iPhone / iPad：用 Safari 打开当前网址",
        "Android：用 Chrome 打开当前网址",
        "再按页面提示添加到主屏",
      ],
      actionLabel: null,
      dismissLabel: "知道了",
      unsupportedPlayback: "当前浏览器不支持 HLS 直播播放。请在 iPhone 上使用 Safari，或在 Android 上使用 Chrome。",
    },
  },
  "zh-TW": {
    "ios-safari": {
      title: "安裝到 iPhone 主畫面",
      body: "請用 Safari 開啟，並依照下面三步安裝。安裝後會以 Web App 獨立視窗開啟。",
      steps: [
        "點 Safari 底部的「分享」按鈕",
        "選擇「加入主畫面」",
        "保持「作為 Web App 打開」並確認加入",
      ],
      actionLabel: null,
      dismissLabel: "稍後再說",
      unsupportedPlayback: "目前瀏覽器不支援 HLS 直播播放。請在 iPhone 上使用 Safari 開啟並加入主畫面。",
    },
    "android-installable": {
      title: "安裝到 Android 主畫面",
      body: "這個瀏覽器已支援直接安裝，你可以一鍵把 Radikall 加到主畫面。",
      steps: [
        "點下面的「立即安裝」",
        "依照瀏覽器提示確認安裝",
        "之後直接從主畫面圖示開啟",
      ],
      actionLabel: "立即安裝",
      dismissLabel: "稍後再說",
      unsupportedPlayback: "目前瀏覽器不支援 HLS 直播播放。請改用支援 HLS 的現代瀏覽器，或在 iPhone 上使用 Safari。",
    },
    "android-manual": {
      title: "加入 Android 主畫面",
      body: "如果沒有直接出現安裝按鈕，也可以從瀏覽器選單手動加入主畫面。",
      steps: [
        "點瀏覽器右上角選單",
        "選擇「安裝應用程式」或「加入主畫面」",
        "確認後從主畫面圖示開啟",
      ],
      actionLabel: null,
      dismissLabel: "稍後再說",
      unsupportedPlayback: "目前瀏覽器不支援 HLS 直播播放。請改用支援 HLS 的現代瀏覽器，或在 iPhone 上使用 Safari。",
    },
    unsupported: {
      title: "建議換瀏覽器安裝",
      body: "為了獲得更穩定的體驗，iPhone 請使用 Safari，Android 請使用 Chrome。",
      steps: [
        "iPhone / iPad：用 Safari 開啟目前網址",
        "Android：用 Chrome 開啟目前網址",
        "再依照頁面提示加入主畫面",
      ],
      actionLabel: null,
      dismissLabel: "知道了",
      unsupportedPlayback: "目前瀏覽器不支援 HLS 直播播放。請在 iPhone 上使用 Safari，或在 Android 上使用 Chrome。",
    },
  },
  en: {
    "ios-safari": {
      title: "Install on iPhone Home Screen",
      body: "Open this page in Safari and follow these three steps. After installation it will launch as a standalone Web App.",
      steps: [
        "Tap Safari's Share button",
        "Choose Add to Home Screen",
        "Keep Open as Web App enabled and confirm",
      ],
      actionLabel: null,
      dismissLabel: "Not now",
      unsupportedPlayback: "This browser does not support HLS live playback. Please open Radikall in Safari on iPhone and install it to the Home Screen.",
    },
    "android-installable": {
      title: "Install on Android Home Screen",
      body: "This browser can install Radikall directly. One tap is enough to add it to your Home Screen.",
      steps: [
        "Tap Install below",
        "Confirm the browser install prompt",
        "Open Radikall from the Home Screen icon next time",
      ],
      actionLabel: "Install now",
      dismissLabel: "Not now",
      unsupportedPlayback: "This browser does not support HLS live playback. Please use a modern HLS-capable browser, or Safari on iPhone.",
    },
    "android-manual": {
      title: "Add to Android Home Screen",
      body: "If the browser does not show a direct install button, you can still add Radikall manually from the browser menu.",
      steps: [
        "Open the browser menu",
        "Choose Install app or Add to Home Screen",
        "Confirm and open it from the Home Screen icon",
      ],
      actionLabel: null,
      dismissLabel: "Not now",
      unsupportedPlayback: "This browser does not support HLS live playback. Please use a modern HLS-capable browser, or Safari on iPhone.",
    },
    unsupported: {
      title: "Use Safari or Chrome for install",
      body: "For the most reliable experience, use Safari on iPhone/iPad and Chrome on Android.",
      steps: [
        "iPhone / iPad: open this URL in Safari",
        "Android: open this URL in Chrome",
        "Then follow the install steps shown here",
      ],
      actionLabel: null,
      dismissLabel: "Dismiss",
      unsupportedPlayback: "This browser does not support HLS live playback. Please use Safari on iPhone or Chrome on Android.",
    },
  },
  ja: {
    "ios-safari": {
      title: "iPhoneのホーム画面に追加",
      body: "Safari で開いて、次の 3 ステップで追加してください。追加後は Web App として独立表示されます。",
      steps: [
        "Safari の共有ボタンをタップ",
        "「ホーム画面に追加」を選択",
        "「Web Appとして開く」を有効のまま追加",
      ],
      actionLabel: null,
      dismissLabel: "あとで",
      unsupportedPlayback: "このブラウザは HLS ライブ再生に対応していません。iPhone の Safari で開いてホーム画面に追加してください。",
    },
    "android-installable": {
      title: "Androidのホーム画面にインストール",
      body: "このブラウザでは直接インストールできます。ワンタップでホーム画面に追加できます。",
      steps: [
        "下の「今すぐインストール」をタップ",
        "ブラウザの確認ダイアログでインストールを承認",
        "次回からホーム画面のアイコンで起動",
      ],
      actionLabel: "今すぐインストール",
      dismissLabel: "あとで",
      unsupportedPlayback: "このブラウザは HLS ライブ再生に対応していません。iPhone の Safari、または HLS 対応ブラウザを使ってください。",
    },
    "android-manual": {
      title: "Androidのホーム画面に追加",
      body: "インストールボタンが出ない場合でも、ブラウザのメニューから手動で追加できます。",
      steps: [
        "ブラウザのメニューを開く",
        "「アプリをインストール」または「ホーム画面に追加」を選ぶ",
        "追加後はホーム画面のアイコンから起動",
      ],
      actionLabel: null,
      dismissLabel: "あとで",
      unsupportedPlayback: "このブラウザは HLS ライブ再生に対応していません。iPhone の Safari、または HLS 対応ブラウザを使ってください。",
    },
    unsupported: {
      title: "Safari または Chrome を使ってください",
      body: "より安定した体験のため、iPhone / iPad は Safari、Android は Chrome の利用をおすすめします。",
      steps: [
        "iPhone / iPad: このURLを Safari で開く",
        "Android: このURLを Chrome で開く",
        "その後、この画面の手順でホーム画面に追加する",
      ],
      actionLabel: null,
      dismissLabel: "閉じる",
      unsupportedPlayback: "このブラウザは HLS ライブ再生に対応していません。iPhone では Safari、Android では Chrome を使ってください。",
    },
  },
  ko: {
    "ios-safari": {
      title: "iPhone 홈 화면에 설치",
      body: "Safari로 열고 아래 3단계를 따라 설치해 주세요. 설치 후에는 독립형 Web App으로 실행됩니다.",
      steps: [
        "Safari의 공유 버튼을 누르기",
        "홈 화면에 추가 선택",
        "Web App으로 열기 옵션을 유지한 채 추가",
      ],
      actionLabel: null,
      dismissLabel: "나중에",
      unsupportedPlayback: "현재 브라우저는 HLS 라이브 재생을 지원하지 않습니다. iPhone의 Safari에서 열고 홈 화면에 추가해 주세요.",
    },
    "android-installable": {
      title: "Android 홈 화면에 설치",
      body: "이 브라우저는 직접 설치를 지원합니다. 한 번만 누르면 홈 화면에 추가할 수 있습니다.",
      steps: [
        "아래의 지금 설치를 누르기",
        "브라우저 설치 확인 창에서 승인",
        "다음부터 홈 화면 아이콘으로 실행",
      ],
      actionLabel: "지금 설치",
      dismissLabel: "나중에",
      unsupportedPlayback: "현재 브라우저는 HLS 라이브 재생을 지원하지 않습니다. iPhone의 Safari 또는 HLS를 지원하는 최신 브라우저를 사용해 주세요.",
    },
    "android-manual": {
      title: "Android 홈 화면에 추가",
      body: "설치 버튼이 바로 보이지 않아도 브라우저 메뉴에서 수동으로 홈 화면에 추가할 수 있습니다.",
      steps: [
        "브라우저 메뉴 열기",
        "앱 설치 또는 홈 화면에 추가 선택",
        "확인 후 홈 화면 아이콘으로 실행",
      ],
      actionLabel: null,
      dismissLabel: "나중에",
      unsupportedPlayback: "현재 브라우저는 HLS 라이브 재생을 지원하지 않습니다. iPhone의 Safari 또는 HLS를 지원하는 최신 브라우저를 사용해 주세요.",
    },
    unsupported: {
      title: "Safari 또는 Chrome 사용 권장",
      body: "가장 안정적인 사용을 위해 iPhone / iPad에서는 Safari, Android에서는 Chrome 사용을 권장합니다.",
      steps: [
        "iPhone / iPad: 현재 주소를 Safari에서 열기",
        "Android: 현재 주소를 Chrome에서 열기",
        "그 다음 이 안내에 따라 홈 화면에 추가",
      ],
      actionLabel: null,
      dismissLabel: "닫기",
      unsupportedPlayback: "현재 브라우저는 HLS 라이브 재생을 지원하지 않습니다. iPhone에서는 Safari, Android에서는 Chrome을 사용해 주세요.",
    },
  },
};

export function isStandaloneDisplayMode(): boolean {
  return window.matchMedia("(display-mode: standalone)").matches || (window.navigator as Navigator & { standalone?: boolean }).standalone === true;
}

export function detectInstallGuideKind(hasInstallPrompt: boolean): InstallGuideKind | null {
  if (isStandaloneDisplayMode()) {
    return null;
  }

  const userAgent = window.navigator.userAgent;
  const isAndroid = /Android/iu.test(userAgent);
  const isIphone = /iPhone|iPad|iPod/iu.test(userAgent);
  const isSafari = /Safari/iu.test(userAgent) && !/CriOS|Chrome|EdgiOS|FxiOS|OPiOS|YaBrowser/iu.test(userAgent);
  const isChrome = /Chrome|CriOS/iu.test(userAgent) && !/Edg/iu.test(userAgent);

  if (isIphone && isSafari) {
    return "ios-safari";
  }
  if (isAndroid && hasInstallPrompt) {
    return "android-installable";
  }
  if (isAndroid && isChrome) {
    return "android-manual";
  }
  if (isIphone || isAndroid) {
    return "unsupported";
  }
  return null;
}

export function installGuideCopy(language: LanguageCode, kind: InstallGuideKind): InstallGuideCopy {
  return copyTable[language][kind];
}

export function unsupportedPlaybackCopy(language: LanguageCode): string {
  const kind = detectInstallGuideKind(false) ?? "unsupported";
  return copyTable[language][kind].unsupportedPlayback;
}
