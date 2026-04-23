package com.radiko.i18n

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import com.radiko.settings.AppLanguage
import kotlinx.datetime.DayOfWeek

object AppLocalizer {
    private val cache = mutableMapOf<AppLanguage, AppStrings>()

    fun strings(language: AppLanguage): AppStrings = cache.getOrPut(language) {
        AppStrings(language)
    }

    fun resolveLanguageTag(languageTag: String?): AppLanguage {
        val normalized = languageTag.orEmpty().lowercase()
        return when {
            normalized.startsWith("zh-hant") ||
                normalized.contains("-tw") ||
                normalized.contains("-hk") ||
                normalized.contains("-mo") -> AppLanguage.TRADITIONAL_CHINESE
            normalized.startsWith("zh") -> AppLanguage.SIMPLIFIED_CHINESE
            normalized.startsWith("ja") -> AppLanguage.JAPANESE
            normalized.startsWith("ko") -> AppLanguage.KOREAN
            normalized.startsWith("en") -> AppLanguage.ENGLISH
            else -> AppLanguage.ENGLISH
        }
    }
}

private val LocalAppStrings = staticCompositionLocalOf {
    AppLocalizer.strings(AppLanguage.ENGLISH)
}
private val LocalAppLanguage = staticCompositionLocalOf { AppLanguage.ENGLISH }

@Composable
fun ProvideAppLocalization(
    language: AppLanguage,
    content: @Composable () -> Unit,
) {
    val strings = remember(language) { AppLocalizer.strings(language) }
    CompositionLocalProvider(
        LocalAppLanguage provides language,
        LocalAppStrings provides strings,
        content = content,
    )
}

@Composable
fun appStrings(): AppStrings = LocalAppStrings.current

@Composable
fun currentAppLanguage(): AppLanguage = LocalAppLanguage.current

private data class LocalizedText(
    val simplifiedChinese: String,
    val traditionalChinese: String,
    val english: String,
    val japanese: String,
    val korean: String,
) {
    fun value(language: AppLanguage): String = when (language) {
        AppLanguage.SIMPLIFIED_CHINESE -> simplifiedChinese
        AppLanguage.TRADITIONAL_CHINESE -> traditionalChinese
        AppLanguage.ENGLISH -> english
        AppLanguage.JAPANESE -> japanese
        AppLanguage.KOREAN -> korean
    }
}

class AppStrings internal constructor(
    private val language: AppLanguage,
) {
    val settingsTitle: String get() = text("设置", "設定", "Settings", "設定", "설정")
    val settingsSubtitle: String get() = text(
        "这里可以控制地区、播放、闹钟、网络、主题和系统行为。",
        "這裡可以控制地區、播放、鬧鐘、網路、主題和系統行為。",
        "Control region, playback, alarms, network, theme, and system behavior here.",
        "地域、再生、アラーム、ネットワーク、テーマ、システム動作をここで調整できます。",
        "여기에서 지역, 재생, 알람, 네트워크, 테마, 시스템 동작을 조정할 수 있습니다.",
    )
    val languageSectionTitle: String get() = text("0. 语言", "0. 語言", "0. Language", "0. 言語", "0. 언어")
    val languageSectionDescription: String get() = text(
        "立即切换应用界面语言。",
        "立即切換應用介面語言。",
        "Switch the app language immediately.",
        "アプリの表示言語をすぐに切り替えます。",
        "앱 표시 언어를 바로 전환합니다.",
    )
    val regionSectionTitle: String get() = text("1. 地域与解锁", "1. 地域與解鎖", "1. Region & Unlock", "1. 地域と解放", "1. 지역 및 해제")
    val regionSectionDescription: String get() = text(
        "控制启动时默认加载的地区。",
        "控制啟動時預設載入的地區。",
        "Control which area loads on startup.",
        "起動時に読み込む地域を設定します。",
        "시작 시 기본으로 불러올 지역을 설정합니다.",
    )
    val playbackSectionTitle: String get() = text("2. 播放与音频设置", "2. 播放與音訊設定", "2. Playback & Audio", "2. 再生と音声設定", "2. 재생 및 오디오 설정")
    val playbackSectionDescription: String get() = text(
        "控制启动自动播放、后台播放和音频焦点行为。",
        "控制啟動自動播放、背景播放和音訊焦點行為。",
        "Control auto-play, background playback, and audio focus behavior.",
        "起動時の自動再生、バックグラウンド再生、音声フォーカス動作を設定します。",
        "자동 재생, 백그라운드 재생, 오디오 포커스 동작을 설정합니다.",
    )
    val timerSectionTitle: String get() = text("3. 定时与闹铃", "3. 定時與鬧鈴", "3. Timers & Alarm", "3. タイマーとアラーム", "3. 타이머 및 알람")
    val timerSectionDescription: String get() = text(
        "睡眠定时器和每日电台闹钟。",
        "睡眠定時器與每日電台鬧鐘。",
        "Sleep timer and daily station alarm.",
        "スリープタイマーと毎日のラジオアラームです。",
        "취침 타이머와 매일 라디오 알람입니다.",
    )
    val networkSectionTitle: String get() = text("4. 网络与流量", "4. 網路與流量", "4. Network & Data", "4. ネットワークと通信量", "4. 네트워크 및 데이터")
    val networkSectionDescription: String get() = text(
        "限制移动网络播放，并在需要时提醒确认。",
        "限制行動網路播放，並在需要時提醒確認。",
        "Restrict cellular playback and prompt when needed.",
        "モバイル通信での再生制限や確認を設定します。",
        "모바일 데이터 재생 제한과 확인 알림을 설정합니다.",
    )
    val appearanceSectionTitle: String get() = text("5. 外观与交互", "5. 外觀與互動", "5. Appearance & Interaction", "5. 外観と操作", "5. 외관 및 상호작용")
    val appearanceSectionDescription: String get() = text(
        "主题模式和驾驶模式。",
        "主題模式與駕駛模式。",
        "Theme mode and driving mode.",
        "テーマモードとドライブモードです。",
        "테마 모드와 드라이빙 모드입니다.",
    )
    val desktopSectionTitle: String get() = text("6. 桌面行为", "6. 桌面行為", "6. Desktop Behavior", "6. デスクトップ動作", "6. 데스크톱 동작")
    val desktopSectionDescription: String get() = text(
        "决定点击关闭按钮时如何处理桌面窗口。",
        "決定點擊關閉按鈕時如何處理桌面視窗。",
        "Choose what happens when the desktop close button is clicked.",
        "デスクトップで閉じるボタンを押した時の動作を設定します。",
        "데스크톱에서 닫기 버튼을 눌렀을 때의 동작을 설정합니다.",
    )
    val generalSectionTitle: String get() = text("7. 系统常规", "7. 系統常規", "7. General", "7. 一般", "7. 일반")
    val generalSectionDescription: String get() = text(
        "缓存清理、版本号与开源许可。",
        "快取清理、版本號與開源授權。",
        "Cache cleanup, version, and open-source notices.",
        "キャッシュ削除、バージョン、オープンソース表記です。",
        "캐시 정리, 버전, 오픈소스 고지입니다.",
    )

    val searchStationsPlaceholder: String get() = text("搜索电台...", "搜尋電台...", "Search stations...", "放送局を検索...", "방송국 검색...")
    val noStationsFound: String get() = text("没有找到电台", "沒有找到電台", "No stations found", "放送局が見つかりません", "방송국을 찾을 수 없습니다")
    val mobileDataPromptTitle: String get() = text("使用移动网络播放？", "使用行動網路播放？", "Play on mobile data?", "モバイル回線で再生しますか？", "모바일 데이터로 재생할까요?")
    val continuePlayback: String get() = text("继续播放", "繼續播放", "Continue", "再生を続ける", "계속 재생")
    val cancel: String get() = text("取消", "取消", "Cancel", "キャンセル", "취소")
    val wifiOnlyPlaybackError: String get() = text(
        "当前设置为仅限 Wi-Fi 播放，请连接 Wi-Fi 后再试。",
        "目前設定為僅限 Wi-Fi 播放，請連接 Wi-Fi 後再試。",
        "Playback is limited to Wi-Fi right now. Please connect to Wi-Fi and try again.",
        "現在は Wi-Fi のみ再生に設定されています。Wi-Fi に接続してからお試しください。",
        "현재 Wi-Fi에서만 재생하도록 설정되어 있습니다. Wi-Fi에 연결한 뒤 다시 시도해 주세요.",
    )
    val genericPlaybackError: String get() = text(
        "无法开始播放",
        "無法開始播放",
        "Unable to start playback",
        "再生を開始できません",
        "재생을 시작할 수 없습니다",
    )
    val unknownStationFallback: String get() = text("未知电台", "未知電台", "Unknown station", "不明な放送局", "알 수 없는 방송국")

    val playerBarIdleTitle: String get() = text(
        "选择一个电台开始播放",
        "選擇一個電台開始播放",
        "Select a station to start playback",
        "放送局を選んで再生を始めましょう",
        "방송국을 선택해 재생을 시작하세요",
    )
    val playerBarIdleSubtitle: String get() = text(
        "开始播放后，这里会显示当前节目标题",
        "開始播放後，這裡會顯示目前節目標題",
        "The current program title will appear here once playback starts",
        "再生が始まると、ここに現在の番組タイトルが表示されます",
        "재생이 시작되면 현재 프로그램 제목이 여기에 표시됩니다",
    )
    val loading: String get() = text("加载中...", "載入中...", "Loading...", "読み込み中...", "불러오는 중...")
    val play: String get() = text("播放", "播放", "Play", "再生", "재생")
    val stop: String get() = text("停止", "停止", "Stop", "停止", "정지")
    val pause: String get() = text("暂停", "暫停", "Pause", "一時停止", "일시정지")

    val nowPlayingTitle: String get() = text("正在播放", "正在播放", "Now Playing", "再生中", "지금 재생 중")
    val performerLabel: String get() = text("主持 / 表演者", "主持 / 表演者", "Performer", "出演者", "진행 / 출연")
    val onAirLabel: String get() = text("播出时间", "播出時間", "On Air", "放送時間", "방송 시간")
    val programTitleUnavailable: String get() = text("暂无节目信息", "暫無節目資訊", "Program title unavailable", "番組情報がありません", "프로그램 정보가 없습니다")
    val currentOnAirSongTitle: String get() = text("当前播出歌曲", "目前播出歌曲", "Current On-Air Song", "現在オンエア中の曲", "현재 방송 중인 곡")
    val currentOnAirSongUnavailable: String get() = text(
        "当前没有可用的歌曲信息。",
        "目前沒有可用的歌曲資訊。",
        "Current on-air song is unavailable.",
        "現在のオンエア曲情報はありません。",
        "현재 방송 중인 곡 정보가 없습니다.",
    )
    val onAirHistoryTitle: String get() = text("播出历史", "播出歷史", "On-Air History", "オンエア履歴", "방송 이력")
    val currentProgramSongsDescription: String get() = text(
        "当前节目中播出的歌曲",
        "目前節目中播出的歌曲",
        "Songs played in the current program",
        "現在の番組内で流れた曲",
        "현재 프로그램에서 재생된 곡",
    )
    val recentStationSongsDescription: String get() = text(
        "这个电台最近播过的歌曲",
        "這個電台最近播過的歌曲",
        "Recent songs from this station",
        "この放送局の最近の楽曲",
        "이 방송국의 최근 재생 곡",
    )
    val currentProgramLabel: String get() = text("当前节目", "目前節目", "Current Program", "現在の番組", "현재 프로그램")
    val fullStationLabel: String get() = text("整个电台", "整個電台", "Full Station", "放送局全体", "전체 방송국")
    val noSongsForCurrentProgram: String get() = text(
        "当前节目里还没有找到歌曲记录。",
        "目前節目裡還沒有找到歌曲紀錄。",
        "No songs found for the current program.",
        "現在の番組では曲履歴が見つかりません。",
        "현재 프로그램의 곡 기록이 없습니다.",
    )
    val noRecentStationSongs: String get() = text(
        "这个电台最近还没有歌曲记录。",
        "這個電台最近還沒有歌曲紀錄。",
        "No recent station songs found.",
        "この放送局の最近の曲履歴が見つかりません。",
        "이 방송국의 최근 곡 기록이 없습니다.",
    )
    val showMore: String get() = text("展开更多", "展開更多", "Show More", "もっと見る", "더 보기")
    val collapse: String get() = text("收起", "收起", "Collapse", "折りたたむ", "접기")

    val programDetailsTitle: String get() = text("节目介绍", "節目介紹", "Program Details", "番組詳細", "프로그램 소개")
    val noProgramDetails: String get() = text(
        "暂无节目介绍。",
        "暫無節目介紹。",
        "No program details available.",
        "番組詳細はありません。",
        "프로그램 소개가 없습니다.",
    )
    val programWebsiteTitle: String get() = text("节目官网", "節目官網", "Program Website", "番組サイト", "프로그램 웹사이트")
    val openWebsite: String get() = text("打开网站", "開啟網站", "Open Website", "サイトを開く", "웹사이트 열기")
    val weeklyScheduleTitle: String get() = text("周节目表", "週節目表", "Weekly Schedule", "週間番組表", "주간 편성표")
    val noScheduleAvailable: String get() = text(
        "当天没有节目表。",
        "當天沒有節目表。",
        "No schedule available for this day.",
        "この日の番組表はありません。",
        "이 날짜의 편성표가 없습니다.",
    )

    val rememberLastArea: String get() = text("记忆上次地区", "記住上次地區", "Remember last area", "前回の地域を使う", "지난 지역 기억")
    val fixedArea: String get() = text("固定地区", "固定地區", "Fixed area", "固定の地域", "고정 지역")
    val startupAreaTitle: String get() = text("启动时默认加载区", "啟動時預設載入區", "Startup area", "起動時の地域", "시작 시 지역")
    val startupAreaSubtitle: String get() = text(
        "记住上次选择，或者每次启动固定为某个地区。",
        "記住上次選擇，或每次啟動固定為某個地區。",
        "Remember the last area, or always start in a fixed area.",
        "前回の地域を使うか、毎回固定の地域で起動します。",
        "지난 지역을 기억하거나 항상 지정한 지역으로 시작합니다.",
    )
    val fixedStartupAreaTitle: String get() = text("固定启动地区", "固定啟動地區", "Fixed startup area", "固定起動地域", "고정 시작 지역")

    val autoPlayTitle: String get() = text("启动时自动播放", "啟動時自動播放", "Auto-play on launch", "起動時に自動再生", "시작 시 자동 재생")
    val autoPlaySubtitle: String get() = text(
        "打开 App 后自动播放上次收听的电台。",
        "開啟 App 後自動播放上次收聽的電台。",
        "Automatically play the last station after launch.",
        "アプリ起動後に前回聴いた放送局を自動再生します。",
        "앱 실행 후 마지막으로 들은 방송국을 자동 재생합니다.",
    )
    val backgroundPlaybackTitle: String get() = text("后台继续播放", "背景繼續播放", "Background playback", "バックグラウンド再生", "백그라운드 재생")
    val backgroundPlaybackSubtitle: String get() = text(
        "关闭到后台后继续保持播放。",
        "切到背景後繼續保持播放。",
        "Keep playing when the app goes to the background.",
        "バックグラウンドに移っても再生を続けます。",
        "앱이 백그라운드로 가도 계속 재생합니다.",
    )
    val audioFocusTitle: String get() = text("音频焦点策略", "音訊焦點策略", "Audio focus behavior", "音声フォーカス動作", "오디오 포커스 동작")
    val audioFocusSubtitle: String get() = text(
        "收到微信、LINE 等通知打断时，是降低音量还是暂停播放。",
        "收到微信、LINE 等通知打斷時，是降低音量還是暫停播放。",
        "Choose whether to duck audio or pause when notifications interrupt playback.",
        "通知などで音声フォーカスが奪われた時に、音量を下げるか一時停止するかを選びます。",
        "알림 등으로 오디오 포커스가 끊길 때 볼륨을 낮출지 일시정지할지 선택합니다.",
    )
    val duckAudio: String get() = text("降低音量", "降低音量", "Duck audio", "音量を下げる", "볼륨 낮추기")
    val pausePlayback: String get() = text("暂停播放", "暫停播放", "Pause playback", "一時停止", "일시정지")

    val sleepTimerTitle: String get() = text("睡眠定时器", "睡眠定時器", "Sleep timer", "スリープタイマー", "수면 타이머")
    val sleepTimerOff: String get() = text("关闭", "關閉", "Off", "オフ", "끄기")
    val alarmClockTitle: String get() = text("电台闹钟", "電台鬧鐘", "Station alarm", "ラジオアラーム", "라디오 알람")
    val alarmEnabledTitle: String get() = text("启用闹钟", "啟用鬧鐘", "Enable alarm", "アラームを有効にする", "알람 사용")
    val alarmEnabledSubtitle: String get() = text(
        "每天在设定时间自动开始播放指定电台。",
        "每天在設定時間自動開始播放指定電台。",
        "Start playing the selected station automatically at the chosen time every day.",
        "設定した時刻に毎日指定した放送局の再生を始めます。",
        "설정한 시간에 매일 지정한 방송국 재생을 시작합니다.",
    )
    val alarmTimeTitle: String get() = text("闹钟时间", "鬧鐘時間", "Alarm time", "アラーム時刻", "알람 시간")
    val alarmStationTitle: String get() = text("闹钟电台", "鬧鐘電台", "Alarm station", "アラーム放送局", "알람 방송국")
    val alarmDesktopHint: String get() = text(
        "桌面端闹钟在应用保持运行或缩小到托盘时生效。",
        "桌面端鬧鐘在應用保持執行或縮小到系統匣時生效。",
        "On desktop, the alarm works while the app remains running or minimized to the tray.",
        "デスクトップ版のアラームは、アプリが起動中またはトレイ常駐中に動作します。",
        "데스크톱 알람은 앱이 실행 중이거나 트레이에 최소화되어 있을 때 동작합니다.",
    )

    val wifiOnlyTitle: String get() = text("仅限 Wi-Fi 播放", "僅限 Wi-Fi 播放", "Wi-Fi only playback", "Wi-Fi のみ再生", "Wi-Fi 전용 재생")
    val wifiOnlySubtitle: String get() = text(
        "防止在移动网络环境下直接开始播放。",
        "防止在行動網路環境下直接開始播放。",
        "Prevent playback from starting over mobile data.",
        "モバイル回線での再生開始を防ぎます。",
        "모바일 데이터에서 바로 재생이 시작되는 것을 막습니다.",
    )
    val mobileDataConfirmTitle: String get() = text("移动网络播放提醒", "行動網路播放提醒", "Mobile data playback confirmation", "モバイル回線での確認", "모바일 데이터 재생 확인")
    val mobileDataConfirmSubtitle: String get() = text(
        "在非 Wi-Fi 环境下点击播放时先弹出确认。",
        "在非 Wi-Fi 環境下點擊播放時先跳出確認。",
        "Ask for confirmation before playing on mobile data.",
        "Wi-Fi 以外で再生する前に確認します。",
        "Wi-Fi가 아닐 때 재생 전에 확인합니다.",
    )

    val themeModeTitle: String get() = text("主题模式", "主題模式", "Theme mode", "テーマモード", "테마 모드")
    val themeModeSubtitle: String get() = text(
        "浅色、深色或跟随系统。",
        "淺色、深色或跟隨系統。",
        "Light, dark, or follow the system.",
        "ライト、ダーク、またはシステムに追従します。",
        "라이트, 다크, 또는 시스템 설정을 따릅니다.",
    )
    val followSystem: String get() = text("跟随系统", "跟隨系統", "Follow system", "システムに合わせる", "시스템 설정 따르기")
    val lightTheme: String get() = text("浅色", "淺色", "Light", "ライト", "라이트")
    val darkTheme: String get() = text("深色", "深色", "Dark", "ダーク", "다크")
    val drivingModeTitle: String get() = text("驾驶模式", "駕駛模式", "Driving mode", "ドライブモード", "드라이빙 모드")
    val drivingModeSubtitle: String get() = text(
        "隐藏搜索、放大按钮并简化首页布局。",
        "隱藏搜尋、放大按鈕並簡化首頁版面。",
        "Hide search, enlarge controls, and simplify the home layout.",
        "検索を隠し、ボタンを大きくしてホーム画面を簡素化します。",
        "검색을 숨기고 버튼을 키워 홈 화면을 단순화합니다.",
    )

    val closeButtonBehaviorTitle: String get() = text("点击关闭按钮时", "點擊關閉按鈕時", "When clicking the close button", "閉じるボタンを押した時", "닫기 버튼을 눌렀을 때")
    val closeButtonBehaviorSubtitle: String get() = text(
        "选择最小化到托盘，或直接退出应用。",
        "選擇縮小到系統匣，或直接退出應用。",
        "Choose whether to minimize to the tray or exit the app.",
        "トレイへ最小化するか、アプリを終了するかを選びます。",
        "트레이로 최소화할지 바로 종료할지 선택합니다.",
    )
    val minimizeToTray: String get() = text("最小化到托盘", "最小化到系統匣", "Minimize to tray", "トレイに最小化", "트레이로 최소화")
    val exitApp: String get() = text("直接关闭应用", "直接關閉應用", "Exit app", "アプリを終了", "앱 종료")

    val clearCacheTitle: String get() = text("清除缓存", "清除快取", "Clear cache", "キャッシュを削除", "캐시 삭제")
    val clearCacheSubtitle: String get() = text(
        "清理电台 Logo、节目封面等缓存文件。",
        "清理電台 Logo、節目封面等快取檔案。",
        "Clear cached station logos, artwork, and related files.",
        "放送局ロゴや番組画像などのキャッシュを削除します。",
        "방송국 로고와 프로그램 이미지 등의 캐시 파일을 정리합니다.",
    )
    val aboutTitle: String get() = text("关于 / 开源许可", "關於 / 開源授權", "About / Open-source notices", "このアプリについて / オープンソース表記", "정보 / 오픈소스 고지")

    val trayPlaybackControl: String get() = text("系统托盘播放控制", "系統匣播放控制", "Tray Playback Controls", "システムトレイ再生コントロール", "트레이 재생 제어")
    val currentlyNotPlaying: String get() = text("当前未播放", "目前未播放", "Not currently playing", "現在は未再生", "현재 재생 중이 아님")
    val nowPlayingStatus: String get() = text("正在播放", "正在播放", "Now playing", "再生中", "재생 중")
    val noProgramPlaying: String get() = text(
        "还没有正在播放的节目",
        "還沒有正在播放的節目",
        "No program is playing yet",
        "まだ再生中の番組はありません",
        "아직 재생 중인 프로그램이 없습니다",
    )
    val chooseStation: String get() = text("选择电台", "選擇電台", "Choose station", "放送局を選ぶ", "방송국 선택")
    val previousStation: String get() = text("上一个电台", "上一個電台", "Previous station", "前の放送局", "이전 방송국")
    val nextStation: String get() = text("下一个电台", "下一個電台", "Next station", "次の放送局", "다음 방송국")
    val trayIconLabel: String get() = text("托盘", "系統匣", "Tray", "トレイ", "트레이")
    val minimizeToTrayPromptTitle: String get() = text(
        "是否缩小到系统托盘？",
        "是否縮小到系統匣？",
        "Minimize to the system tray?",
        "システムトレイに最小化しますか？",
        "시스템 트레이로 최소화할까요?",
    )
    val minimizeToTrayPromptBody: String get() = text(
        "缩小后会继续播放，你可以从托盘里恢复窗口或控制播放。",
        "縮小後會繼續播放，你可以從系統匣恢復視窗或控制播放。",
        "Playback will continue after minimizing, and you can restore the window or control playback from the tray.",
        "最小化後も再生は続き、トレイからウィンドウの復元や再生操作ができます。",
        "최소화 후에도 재생은 계속되며, 트레이에서 창 복원과 재생 제어를 할 수 있습니다.",
    )
    val doNotShowAgain: String get() = text("下次不再显示", "下次不再顯示", "Don't show again", "次回から表示しない", "다음부터 표시하지 않기")
    val yes: String get() = text("是", "是", "Yes", "はい", "예")
    val no: String get() = text("否", "否", "No", "いいえ", "아니오")

    fun mobileDataPromptBody(stationName: String): String = when (language) {
        AppLanguage.SIMPLIFIED_CHINESE -> "当前不是 Wi-Fi 环境。继续播放 $stationName 可能会产生流量费用。"
        AppLanguage.TRADITIONAL_CHINESE -> "目前不是 Wi-Fi 環境。繼續播放 $stationName 可能會產生流量費用。"
        AppLanguage.ENGLISH -> "You are not on Wi-Fi. Continuing with $stationName may use mobile data."
        AppLanguage.JAPANESE -> "現在は Wi-Fi 環境ではありません。$stationName を再生すると通信量が発生する場合があります。"
        AppLanguage.KOREAN -> "현재 Wi-Fi 환경이 아닙니다. $stationName 재생을 계속하면 데이터 요금이 발생할 수 있습니다."
    }

    fun sleepTimerSubtitle(minutesLeft: Long?): String = when {
        minutesLeft == null -> text(
            "15 / 30 / 60 / 90 分钟后自动停止播放。",
            "15 / 30 / 60 / 90 分鐘後自動停止播放。",
            "Stop playback automatically after 15, 30, 60, or 90 minutes.",
            "15 / 30 / 60 / 90 分後に自動で再生を停止します。",
            "15 / 30 / 60 / 90분 후 자동으로 재생을 멈춥니다.",
        )
        else -> when (language) {
            AppLanguage.SIMPLIFIED_CHINESE -> "已开启，约 $minutesLeft 分钟后自动停止播放。"
            AppLanguage.TRADITIONAL_CHINESE -> "已開啟，約 $minutesLeft 分鐘後自動停止播放。"
            AppLanguage.ENGLISH -> "Enabled. Playback will stop in about $minutesLeft minutes."
            AppLanguage.JAPANESE -> "有効です。約 $minutesLeft 分後に再生を停止します。"
            AppLanguage.KOREAN -> "활성화됨. 약 ${minutesLeft}분 후 재생이 중지됩니다."
        }
    }

    fun sleepTimerOption(minutes: Int): String = when (language) {
        AppLanguage.SIMPLIFIED_CHINESE -> "$minutes 分钟"
        AppLanguage.TRADITIONAL_CHINESE -> "$minutes 分鐘"
        AppLanguage.ENGLISH -> "$minutes min"
        AppLanguage.JAPANESE -> "$minutes 分"
        AppLanguage.KOREAN -> "${minutes}분"
    }

    fun alarmTimeValue(hour: Int, minute: Int): String = when (language) {
        AppLanguage.SIMPLIFIED_CHINESE,
        AppLanguage.TRADITIONAL_CHINESE,
        AppLanguage.JAPANESE,
        AppLanguage.KOREAN -> "${hour.toString().padStart(2, '0')}:${minute.toString().padStart(2, '0')}"
        AppLanguage.ENGLISH -> "${hour.toString().padStart(2, '0')}:${minute.toString().padStart(2, '0')}"
    }


    fun aboutBody(version: String): String = when (language) {
        AppLanguage.SIMPLIFIED_CHINESE -> "版本 $version"
        AppLanguage.TRADITIONAL_CHINESE -> "版本 $version"
        AppLanguage.ENGLISH -> "Version $version"
        AppLanguage.JAPANESE -> "バージョン $version"
        AppLanguage.KOREAN -> "버전 $version"
    }


     val aboutOpenSourceLabel: String get() = text(
        "完全开源免费",
        "完全開源免費",
        "Free and Open Source",
        "完全無料・オープンソース",
        "완전 무료 오픈소스",
    )
val aboutOpenSourceBody: String get() = text(
    "Radikall 完全开源且永久免费。任何以此应用收费的行为均为欺诈，请认准原仓库。",
    "Radikall 完全開源且永久免費。任何以此應用收費的行為均為欺詐，請認準原倉庫。",
    "Radikall is completely free and open source. Any paid version of this app is fraudulent. Always download from the official repository.",
    "Radikall は完全無料かつオープンソースです。有料で販売されているものはすべて偽物です。必ず公式リポジトリからダウンロードしてください。",
    "Radikall은 완전히 무료이며 오픈소스입니다. 유료로 판매되는 버전은 모두 사기입니다. 반드시 공식 저장소에서 다운로드하세요.",
)
val aboutRepoLabel: String get() = text("原始仓库", "原始倉庫", "Source Repository", "ソースリポジトリ", "소스 저장소")
val aboutRepoUrl: String get() = "https://github.com/baudmusic/radikall"
val aboutSiteLabel: String get() = text(
    "关注开发者",
    "關注開發者",
    "Follow the Developer",
    "開発者をフォロー",
    "개발자 팔로우",
)
val aboutSiteBody: String get() = text(
    "如果你喜欢这个应用，欢迎访问 baudstudio.com 并关注我的社交账号。我的本职是一名 Acapella 博主，谢谢你的支持。",
    "如果你喜歡這個應用，歡迎訪問 baudstudio.com 並關注我的社交帳號。我的本職是一名 Acapella 博主，謝謝你的支持。",
    "If you enjoy this app, please visit baudstudio.com and follow my social accounts. I'm primarily an Acapella content creator. Thank you for your support.",
    "このアプリが気に入ったら、baudstudio.com にアクセスして SNS をフォローしてください。本業はアカペラ系クリエイターです。応援ありがとうございます。",
    "이 앱이 마음에 드셨다면 baudstudio.com을 방문하고 SNS를 팔로우해 주세요. 본업은 아카펠라 크리에이터입니다. 응원해 주셔서 감사합니다.",
)
val aboutSiteUrl: String get() = "https://baudstudio.com"
val aboutCheckUpdate: String get() = text("检查更新", "檢查更新", "Check for Updates", "アップデートを確認", "업데이트 확인")
val aboutCheckUpdateUrl: String get() = "https://github.com/baudmusic/radikall/releases/latest"
val aboutFontNotice: String get() = text(
    "界面使用 Noto Sans JP 字体，遵循 SIL Open Font License 1.1（OFL）授权。",
    "介面使用 Noto Sans JP 字體，遵循 SIL Open Font License 1.1（OFL）授權。",
    "The interface uses the Noto Sans JP font under the SIL Open Font License 1.1 (OFL).",
    "UI には Noto Sans JP フォントを使用しており、SIL Open Font License 1.1（OFL）に従います。",
    "인터페이스에는 SIL Open Font License 1.1(OFL)을 따르는 Noto Sans JP 글꼴을 사용합니다.",
)
val aboutDisclaimerLabel: String get() = text("免责声明", "免責聲明", "Disclaimer", "免責事項", "면책 조항")
val aboutDisclaimerBody: String get() = text(
    "Radikall 是非官方第三方客户端，与 Radiko Co., Ltd. 及其广播合作伙伴无任何关联。本应用仅供个人学习与技术研究使用，不得用于商业目的。使用者须自行承担相关法律责任。",
    "Radikall 是非官方第三方客戶端，與 Radiko Co., Ltd. 及其廣播合作夥伴無任何關聯。本應用僅供個人學習與技術研究使用，不得用於商業目的。使用者須自行承擔相關法律責任。",
    "Radikall is an unofficial third-party client, unaffiliated with Radiko Co., Ltd. or its broadcasting partners. For personal and educational use only. Commercial use is not permitted. Users assume all legal responsibility.",
    "Radikall は非公式のサードパーティクライアントであり、Radiko Co., Ltd. およびその放送パートナーとは一切関係ありません。個人利用および技術研究目的のみ。商用利用は禁止です。利用者は一切の法的責任を負います。",
    "Radikall은 비공식 서드파티 클라이언트이며 Radiko Co., Ltd. 및 방송 파트너와 무관합니다. 개인 학습 및 기술 연구 목적으로만 사용 가능합니다. 상업적 사용은 금지됩니다. 이용자는 모든 법적 책임을 집니다.",
)
val aboutCreditsLabel: String get() = text("致谢", "致謝", "Credits", "クレジット", "크레딧")
val aboutCreditsBody: String get() = text(
    "本项目得益于 jackyzy823/rajiko 项目的思路与逆向工程工作。",
    "本專案得益於 jackyzy823/rajiko 專案的思路與逆向工程工作。",
    "This project builds on the ideas and reverse-engineering work of jackyzy823/rajiko.",
    "このプロジェクトは jackyzy823/rajiko の取り組みとリバースエンジニアリングの成果に基づいています。",
    "이 프로젝트는 jackyzy823/rajiko의 아이디어와 리버스 엔지니어링 작업을 기반으로 합니다.",
)
val aboutCreditsUrl: String get() = "https://github.com/jackyzy823/rajiko"

    fun cacheClearMessage(cleared: Boolean): String = when {
        cleared -> text(
            "已清理当前平台的缓存目录。",
            "已清理目前平台的快取目錄。",
            "Cleared the cache directory for this platform.",
            "このプラットフォームのキャッシュディレクトリを削除しました。",
            "현재 플랫폼의 캐시 디렉터리를 정리했습니다.",
        )
        else -> text(
            "当前没有可清理的独立缓存目录，或缓存已经为空。",
            "目前沒有可清理的獨立快取目錄，或快取已經是空的。",
            "No separate cache directory is available to clear, or it is already empty.",
            "削除できる独立キャッシュディレクトリがないか、すでに空です。",
            "정리할 별도 캐시 디렉터리가 없거나 이미 비어 있습니다.",
        )
    }

    fun languageName(value: AppLanguage): String = when (value) {
        AppLanguage.SIMPLIFIED_CHINESE -> text("简体中文", "簡體中文", "Simplified Chinese", "簡体字中国語", "중국어 간체")
        AppLanguage.TRADITIONAL_CHINESE -> text("繁體中文", "繁體中文", "Traditional Chinese", "繁体字中国語", "중국어 번체")
        AppLanguage.ENGLISH -> text("英语", "英語", "English", "英語", "영어")
        AppLanguage.JAPANESE -> text("日语", "日語", "Japanese", "日本語", "일본어")
        AppLanguage.KOREAN -> text("韩语", "韓語", "Korean", "韓国語", "한국어")
    }

    fun regionName(regionId: String, fallback: String): String =
        regionTranslations[regionId]?.value(language) ?: fallback

    fun prefectureName(areaId: String, fallback: String): String =
        prefectureTranslations[areaId]?.value(language) ?: fallback

    fun weekdayShort(dayOfWeek: DayOfWeek): String = when (dayOfWeek) {
        DayOfWeek.MONDAY -> text("周一", "週一", "Mon", "月", "월")
        DayOfWeek.TUESDAY -> text("周二", "週二", "Tue", "火", "화")
        DayOfWeek.WEDNESDAY -> text("周三", "週三", "Wed", "水", "수")
        DayOfWeek.THURSDAY -> text("周四", "週四", "Thu", "木", "목")
        DayOfWeek.FRIDAY -> text("周五", "週五", "Fri", "金", "금")
        DayOfWeek.SATURDAY -> text("周六", "週六", "Sat", "土", "토")
        DayOfWeek.SUNDAY -> text("周日", "週日", "Sun", "日", "일")
        else -> dayOfWeek.name.take(3)
    }

    private fun text(
        simplifiedChinese: String,
        traditionalChinese: String,
        english: String,
        japanese: String,
        korean: String,
    ): String = LocalizedText(
        simplifiedChinese = simplifiedChinese,
        traditionalChinese = traditionalChinese,
        english = english,
        japanese = japanese,
        korean = korean,
    ).value(language)
}

private val regionTranslations = mapOf(
    "hokkaido-tohoku" to LocalizedText("北海道・东北", "北海道・東北", "Hokkaido / Tohoku", "北海道・東北", "홋카이도 / 도호쿠"),
    "kanto" to LocalizedText("关东", "關東", "Kanto", "関東", "간토"),
    "hokuriku-koushinetsu" to LocalizedText("北陆・甲信越", "北陸・甲信越", "Hokuriku / Koshinetsu", "北陸・甲信越", "호쿠리쿠 / 고신에쓰"),
    "chubu" to LocalizedText("中部", "中部", "Chubu", "中部", "주부"),
    "kinki" to LocalizedText("近畿", "近畿", "Kinki", "近畿", "긴키"),
    "chugoku-shikoku" to LocalizedText("中国・四国", "中國・四國", "Chugoku / Shikoku", "中国・四国", "주고쿠 / 시코쿠"),
    "kyushu" to LocalizedText("九州・冲绳", "九州・沖繩", "Kyushu / Okinawa", "九州・沖縄", "규슈 / 오키나와"),
)

private val prefectureTranslations = mapOf(
    "JP1" to LocalizedText("北海道", "北海道", "Hokkaido", "北海道", "홋카이도"),
    "JP2" to LocalizedText("青森", "青森", "Aomori", "青森", "아오모리"),
    "JP3" to LocalizedText("岩手", "岩手", "Iwate", "岩手", "이와테"),
    "JP4" to LocalizedText("宫城", "宮城", "Miyagi", "宮城", "미야기"),
    "JP5" to LocalizedText("秋田", "秋田", "Akita", "秋田", "아키타"),
    "JP6" to LocalizedText("山形", "山形", "Yamagata", "山形", "야마가타"),
    "JP7" to LocalizedText("福岛", "福島", "Fukushima", "福島", "후쿠시마"),
    "JP8" to LocalizedText("茨城", "茨城", "Ibaraki", "茨城", "이바라키"),
    "JP9" to LocalizedText("栃木", "栃木", "Tochigi", "栃木", "도치기"),
    "JP10" to LocalizedText("群马", "群馬", "Gunma", "群馬", "군마"),
    "JP11" to LocalizedText("埼玉", "埼玉", "Saitama", "埼玉", "사이타마"),
    "JP12" to LocalizedText("千叶", "千葉", "Chiba", "千葉", "지바"),
    "JP13" to LocalizedText("东京", "東京", "Tokyo", "東京", "도쿄"),
    "JP14" to LocalizedText("神奈川", "神奈川", "Kanagawa", "神奈川", "가나가와"),
    "JP15" to LocalizedText("新潟", "新潟", "Niigata", "新潟", "니가타"),
    "JP16" to LocalizedText("富山", "富山", "Toyama", "富山", "도야마"),
    "JP17" to LocalizedText("石川", "石川", "Ishikawa", "石川", "이시카와"),
    "JP18" to LocalizedText("福井", "福井", "Fukui", "福井", "후쿠이"),
    "JP19" to LocalizedText("山梨", "山梨", "Yamanashi", "山梨", "야마나시"),
    "JP20" to LocalizedText("长野", "長野", "Nagano", "長野", "나가노"),
    "JP21" to LocalizedText("岐阜", "岐阜", "Gifu", "岐阜", "기후"),
    "JP22" to LocalizedText("静冈", "靜岡", "Shizuoka", "静岡", "시즈오카"),
    "JP23" to LocalizedText("爱知", "愛知", "Aichi", "愛知", "아이치"),
    "JP24" to LocalizedText("三重", "三重", "Mie", "三重", "미에"),
    "JP25" to LocalizedText("滋贺", "滋賀", "Shiga", "滋賀", "시가"),
    "JP26" to LocalizedText("京都", "京都", "Kyoto", "京都", "교토"),
    "JP27" to LocalizedText("大阪", "大阪", "Osaka", "大阪", "오사카"),
    "JP28" to LocalizedText("兵库", "兵庫", "Hyogo", "兵庫", "효고"),
    "JP29" to LocalizedText("奈良", "奈良", "Nara", "奈良", "나라"),
    "JP30" to LocalizedText("和歌山", "和歌山", "Wakayama", "和歌山", "와카야마"),
    "JP31" to LocalizedText("鸟取", "鳥取", "Tottori", "鳥取", "돗토리"),
    "JP32" to LocalizedText("岛根", "島根", "Shimane", "島根", "시마네"),
    "JP33" to LocalizedText("冈山", "岡山", "Okayama", "岡山", "오카야마"),
    "JP34" to LocalizedText("广岛", "廣島", "Hiroshima", "広島", "히로시마"),
    "JP35" to LocalizedText("山口", "山口", "Yamaguchi", "山口", "야마구치"),
    "JP36" to LocalizedText("德岛", "德島", "Tokushima", "徳島", "도쿠시마"),
    "JP37" to LocalizedText("香川", "香川", "Kagawa", "香川", "가가와"),
    "JP38" to LocalizedText("爱媛", "愛媛", "Ehime", "愛媛", "에히메"),
    "JP39" to LocalizedText("高知", "高知", "Kochi", "高知", "고치"),
    "JP40" to LocalizedText("福冈", "福岡", "Fukuoka", "福岡", "후쿠오카"),
    "JP41" to LocalizedText("佐贺", "佐賀", "Saga", "佐賀", "사가"),
    "JP42" to LocalizedText("长崎", "長崎", "Nagasaki", "長崎", "나가사키"),
    "JP43" to LocalizedText("熊本", "熊本", "Kumamoto", "熊本", "구마모토"),
    "JP44" to LocalizedText("大分", "大分", "Oita", "大分", "오이타"),
    "JP45" to LocalizedText("宫崎", "宮崎", "Miyazaki", "宮崎", "미야자키"),
    "JP46" to LocalizedText("鹿儿岛", "鹿兒島", "Kagoshima", "鹿児島", "가고시마"),
    "JP47" to LocalizedText("冲绳", "沖繩", "Okinawa", "沖縄", "오키나와"),
)
