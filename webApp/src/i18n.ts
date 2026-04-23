import type { LanguageCode, LocalizedText } from "./types";

export type MessageKey =
  | "appTitle"
  | "tagline"
  | "searchPlaceholder"
  | "loading"
  | "loadingDetails"
  | "openNowPlaying"
  | "openSettings"
  | "close"
  | "home"
  | "nowPlaying"
  | "settings"
  | "region"
  | "stations"
  | "resumeLastStation"
  | "resumeHint"
  | "play"
  | "stop"
  | "retry"
  | "reconnecting"
  | "bestEffortNote"
  | "schedule"
  | "currentProgram"
  | "recentSongs"
  | "currentProgramSongs"
  | "noProgram"
  | "noSongs"
  | "programWebsite"
  | "openWebsite"
  | "language"
  | "theme"
  | "system"
  | "light"
  | "dark"
  | "startupArea"
  | "rememberLastArea"
  | "fixedArea"
  | "drivingMode"
  | "lastResolvedArea"
  | "backgroundPlayback"
  | "noStations"
  | "playbackFailed"
  | "searching"
  | "stationCount"
  | "today"
  | "upcoming"
  | "removeError"
  | "details"
  | "openStation"
  | "chooseStation"
  | "playerIdleTitle"
  | "playerIdleSubtitle"
  | "currentSong"
  | "settingsTitle"
  | "settingsSubtitle"
  | "tapBarForDetails"
  | "stationTapHint"
  | "rememberLastStation"
  | "startupAreaHelp"
  | "launchNote"
  | "stationSearchHint";

type Messages = Record<MessageKey, string>;

const zhCn: Messages = {
  appTitle: "Radikall Web",
  tagline: "面向 iPhone 主屏的 Radikall Web 版本。",
  searchPlaceholder: "搜索电台名称或台号",
  loading: "加载中...",
  loadingDetails: "正在刷新节目详情...",
  openNowPlaying: "打开播放页",
  openSettings: "打开设置",
  close: "关闭",
  home: "首页",
  nowPlaying: "播放页",
  settings: "设置",
  region: "地区",
  stations: "电台",
  resumeLastStation: "继续上次收听",
  resumeHint: "",
  play: "播放",
  stop: "停止",
  retry: "重试",
  reconnecting: "正在重新连接直播流...",
  bestEffortNote: "后台播放仅为尽力而为。",
  schedule: "节目表",
  currentProgram: "当前节目",
  recentSongs: "最近歌曲",
  currentProgramSongs: "本节目歌曲",
  noProgram: "当前没有可用的节目详情。",
  noSongs: "暂时没有可显示的歌曲记录。",
  programWebsite: "节目官网",
  openWebsite: "打开网站",
  language: "语言",
  theme: "主题",
  system: "跟随系统",
  light: "浅色",
  dark: "深色",
  startupArea: "启动时默认加载区",
  rememberLastArea: "记住上次地区",
  fixedArea: "固定地区",
  drivingMode: "驾驶模式",
  lastResolvedArea: "实际播放地区",
  backgroundPlayback: "后台播放",
  noStations: "没有找到匹配的电台。",
  playbackFailed: "无法开始播放。",
  searching: "正在搜索电台...",
  stationCount: "可用电台",
  today: "今天",
  upcoming: "未来几天",
  removeError: "关闭提示",
  details: "详情",
  openStation: "查看节目",
  chooseStation: "先从首页选一个电台，我们再加载节目详情和播放页。",
  playerIdleTitle: "选择一个电台开始播放",
  playerIdleSubtitle: "播放开始后，这里会显示当前节目标题。",
  currentSong: "当前歌曲",
  settingsTitle: "设置",
  settingsSubtitle: "",
  tapBarForDetails: "",
  stationTapHint: "",
  rememberLastStation: "记住上次电台",
  startupAreaHelp: "",
  launchNote: "",
  stationSearchHint: "",
};

const zhTw: Messages = {
  ...zhCn,
  tagline: "面向 iPhone 主畫面的 Radikall Web 版本。",
  searchPlaceholder: "搜尋電台名稱或台號",
  loading: "載入中...",
  loadingDetails: "正在更新節目詳情...",
  close: "關閉",
  home: "首頁",
  settings: "設定",
  region: "地區",
  stations: "電台",
  resumeLastStation: "繼續上次收聽",
  play: "播放",
  stop: "停止",
  retry: "重試",
  reconnecting: "正在重新連接直播流...",
  schedule: "節目表",
  currentProgram: "目前節目",
  recentSongs: "最近歌曲",
  currentProgramSongs: "本節目歌曲",
  noProgram: "目前沒有可用的節目詳情。",
  noSongs: "暫時沒有可顯示的歌曲記錄。",
  programWebsite: "節目官網",
  openWebsite: "開啟網站",
  language: "語言",
  theme: "主題",
  system: "跟隨系統",
  light: "淺色",
  dark: "深色",
  startupArea: "啟動時預設載入區",
  rememberLastArea: "記住上次地區",
  fixedArea: "固定地區",
  drivingMode: "駕駛模式",
  lastResolvedArea: "實際播放地區",
  backgroundPlayback: "背景播放",
  noStations: "沒有找到符合的電台。",
  playbackFailed: "無法開始播放。",
  searching: "正在搜尋電台...",
  stationCount: "可用電台",
  today: "今天",
  upcoming: "未來幾天",
  removeError: "關閉提示",
  details: "詳情",
  openStation: "查看節目",
  chooseStation: "先從首頁選一個電台，我們再載入節目詳情與播放頁。",
  playerIdleTitle: "選擇一個電台開始播放",
  playerIdleSubtitle: "播放開始後，這裡會顯示目前節目標題。",
  currentSong: "目前歌曲",
  settingsTitle: "設定",
  rememberLastStation: "記住上次電台",
};

const en: Messages = {
  appTitle: "Radikall Web",
  tagline: "A home-screen-friendly Radikall web app.",
  searchPlaceholder: "Search by station name or id",
  loading: "Loading...",
  loadingDetails: "Refreshing program details...",
  openNowPlaying: "Open now playing",
  openSettings: "Open settings",
  close: "Close",
  home: "Home",
  nowPlaying: "Now Playing",
  settings: "Settings",
  region: "Region",
  stations: "Stations",
  resumeLastStation: "Resume last station",
  resumeHint: "",
  play: "Play",
  stop: "Stop",
  retry: "Retry",
  reconnecting: "Reconnecting the live stream...",
  bestEffortNote: "Background playback is best effort only.",
  schedule: "Schedule",
  currentProgram: "Current Program",
  recentSongs: "Recent Songs",
  currentProgramSongs: "Songs In This Program",
  noProgram: "No program details are available right now.",
  noSongs: "No song history is available yet.",
  programWebsite: "Program Website",
  openWebsite: "Open Website",
  language: "Language",
  theme: "Theme",
  system: "System",
  light: "Light",
  dark: "Dark",
  startupArea: "Startup Area",
  rememberLastArea: "Remember last area",
  fixedArea: "Fixed area",
  drivingMode: "Driving mode",
  lastResolvedArea: "Resolved playback area",
  backgroundPlayback: "Background playback",
  noStations: "No matching stations were found.",
  playbackFailed: "Unable to start playback.",
  searching: "Searching stations...",
  stationCount: "Available stations",
  today: "Today",
  upcoming: "Upcoming days",
  removeError: "Dismiss",
  details: "Details",
  openStation: "Open station",
  chooseStation: "Pick a station from Home first.",
  playerIdleTitle: "Select a station to start playback",
  playerIdleSubtitle: "The current program title will appear here after playback starts.",
  currentSong: "Current Song",
  settingsTitle: "Settings",
  settingsSubtitle: "",
  tapBarForDetails: "",
  stationTapHint: "",
  rememberLastStation: "Remember last station",
  startupAreaHelp: "",
  launchNote: "",
  stationSearchHint: "",
};

const ja: Messages = {
  ...en,
  appTitle: "Radikall Web",
  tagline: "iPhone のホーム画面向け Radikall Web 版です。",
  searchPlaceholder: "局名または station id で検索",
  loading: "読み込み中...",
  loadingDetails: "番組情報を更新しています...",
  openNowPlaying: "再生画面を開く",
  openSettings: "設定を開く",
  close: "閉じる",
  home: "ホーム",
  nowPlaying: "再生中",
  settings: "設定",
  region: "地域",
  stations: "放送局",
  resumeLastStation: "前回の局を再開",
  play: "再生",
  stop: "停止",
  retry: "再試行",
  reconnecting: "ライブ配信に再接続しています...",
  schedule: "番組表",
  currentProgram: "現在の番組",
  recentSongs: "最近の曲",
  currentProgramSongs: "この番組の曲",
  noProgram: "現在利用できる番組詳細はありません。",
  noSongs: "表示できる楽曲履歴はまだありません。",
  programWebsite: "番組サイト",
  openWebsite: "サイトを開く",
  language: "言語",
  theme: "テーマ",
  system: "システム",
  light: "ライト",
  dark: "ダーク",
  startupArea: "起動時の地域",
  rememberLastArea: "前回の地域を使う",
  fixedArea: "固定地域",
  drivingMode: "ドライブモード",
  lastResolvedArea: "実際の再生地域",
  backgroundPlayback: "バックグラウンド再生",
  noStations: "一致する放送局が見つかりません。",
  playbackFailed: "再生を開始できませんでした。",
  searching: "放送局を検索しています...",
  stationCount: "利用可能な放送局",
  today: "今日",
  upcoming: "今後数日",
  removeError: "閉じる",
  details: "詳細",
  openStation: "番組を見る",
  chooseStation: "先にホームで放送局を選択してください。",
  playerIdleTitle: "放送局を選んで再生を開始",
  playerIdleSubtitle: "再生が始まると、ここに現在の番組名が表示されます。",
  currentSong: "現在の曲",
  settingsTitle: "設定",
  rememberLastStation: "前回の局を記憶",
};

const ko: Messages = {
  ...en,
  appTitle: "Radikall Web",
  tagline: "iPhone 홈 화면용 Radikall 웹 버전입니다.",
  searchPlaceholder: "방송국 이름 또는 station id 검색",
  loading: "불러오는 중...",
  loadingDetails: "프로그램 정보를 새로고침하는 중...",
  openNowPlaying: "재생 화면 열기",
  openSettings: "설정 열기",
  close: "닫기",
  home: "홈",
  nowPlaying: "재생 중",
  settings: "설정",
  region: "지역",
  stations: "방송국",
  resumeLastStation: "지난 방송국 이어 듣기",
  play: "재생",
  stop: "정지",
  retry: "다시 시도",
  reconnecting: "라이브 스트림을 다시 연결하는 중...",
  schedule: "편성표",
  currentProgram: "현재 프로그램",
  recentSongs: "최근 곡",
  currentProgramSongs: "현재 프로그램 곡",
  noProgram: "지금 표시할 프로그램 정보가 없습니다.",
  noSongs: "표시할 곡 기록이 아직 없습니다.",
  programWebsite: "프로그램 웹사이트",
  openWebsite: "웹사이트 열기",
  language: "언어",
  theme: "테마",
  system: "시스템",
  light: "라이트",
  dark: "다크",
  startupArea: "시작 지역",
  rememberLastArea: "마지막 지역 기억",
  fixedArea: "고정 지역",
  drivingMode: "드라이빙 모드",
  lastResolvedArea: "실제 재생 지역",
  backgroundPlayback: "백그라운드 재생",
  noStations: "일치하는 방송국이 없습니다.",
  playbackFailed: "재생을 시작할 수 없습니다.",
  searching: "방송국을 검색하는 중...",
  stationCount: "사용 가능한 방송국",
  today: "오늘",
  upcoming: "앞으로 며칠",
  removeError: "닫기",
  details: "상세",
  openStation: "프로그램 보기",
  chooseStation: "먼저 홈에서 방송국을 선택하세요.",
  playerIdleTitle: "방송국을 선택해 재생 시작",
  playerIdleSubtitle: "재생이 시작되면 여기 현재 프로그램 제목이 표시됩니다.",
  currentSong: "현재 곡",
  settingsTitle: "설정",
  rememberLastStation: "마지막 방송국 기억",
};

const messageTable: Record<LanguageCode, Messages> = {
  "zh-CN": zhCn,
  "zh-TW": zhTw,
  en,
  ja,
  ko,
};

export const languageLabels: Record<LanguageCode, string> = {
  "zh-CN": "简体中文",
  "zh-TW": "繁體中文",
  en: "English",
  ja: "日本語",
  ko: "한국어",
};

export function resolveLanguageTag(value: string | null | undefined): LanguageCode {
  const normalized = (value ?? "").toLowerCase();
  if (normalized.startsWith("zh-cn") || normalized.startsWith("zh-hans")) {
    return "zh-CN";
  }
  if (normalized.startsWith("zh-tw") || normalized.startsWith("zh-hant") || normalized.startsWith("zh-hk")) {
    return "zh-TW";
  }
  if (normalized.startsWith("ja")) {
    return "ja";
  }
  if (normalized.startsWith("ko")) {
    return "ko";
  }
  return "en";
}

export function t(language: LanguageCode, key: MessageKey): string {
  return messageTable[language][key];
}

export function localizeText(text: LocalizedText, language: LanguageCode): string {
  switch (language) {
    case "zh-CN":
      return text["zh-CN"];
    case "zh-TW":
      return text["zh-TW"];
    case "ja":
      return text.ja;
    case "ko":
      return text.ko;
    default:
      return text.en;
  }
}
