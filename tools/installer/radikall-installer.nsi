; ============================================================
;  Radikall — 多语言外层安装引导
;  支持: English / 简体中文 / 繁體中文 / 日本語 / 한국어
; ============================================================

!include "MUI2.nsh"
!include "LogicLib.nsh"

; ---------- 基本信息 ----------
Name "Radikall"
OutFile "..\..\desktopApp\build\compose\binaries\main-release\Radikall-Setup.exe"
InstallDir "$PROGRAMFILES64\Radikall"
RequestExecutionLevel admin
Unicode True

; ---------- MUI 界面设置（必须在语言包之前）----------
!define MUI_ABORTWARNING
!define MUI_ICON "..\..\desktopApp\src\main\resources\logo2.ico"
!define MUI_UNICON "..\..\desktopApp\src\main\resources\logo2.ico"
!define MUI_WELCOMEFINISHPAGE_BITMAP "..\..\desktopApp\src\main\resources\wix-dialog.bmp"
!define MUI_HEADERIMAGE
!define MUI_HEADERIMAGE_BITMAP "..\..\desktopApp\src\main\resources\wix-banner.bmp"
!define MUI_FINISHPAGE_RUN "$PROGRAMFILES64\Radikall\Radikall.exe"
!define MUI_FINISHPAGE_RUN_TEXT "$(MSG_LAUNCH)"

; ---------- 安装页面流程（必须在语言包之前）----------
!insertmacro MUI_PAGE_WELCOME
!insertmacro MUI_PAGE_DIRECTORY
!insertmacro MUI_PAGE_INSTFILES
!insertmacro MUI_PAGE_FINISH

; ---------- 语言包（必须在页面定义之后）----------
!insertmacro MUI_LANGUAGE "English"
!insertmacro MUI_LANGUAGE "SimpChinese"
!insertmacro MUI_LANGUAGE "TradChinese"
!insertmacro MUI_LANGUAGE "Japanese"
!insertmacro MUI_LANGUAGE "Korean"

; ---------- 多语言字符串定义（语言包之后）----------
LangString MSG_INSTALLING ${LANG_ENGLISH}     "Installing Radikall, please wait..."
LangString MSG_INSTALLING ${LANG_SIMPCHINESE} "正在安装 Radikall，请稍候..."
LangString MSG_INSTALLING ${LANG_TRADCHINESE} "正在安裝 Radikall，請稍候..."
LangString MSG_INSTALLING ${LANG_JAPANESE}    "Radikall をインストール中です。しばらくお待ちください..."
LangString MSG_INSTALLING ${LANG_KOREAN}      "Radikall 설치 중입니다. 잠시만 기다려 주세요..."

LangString MSG_VLC_WARN ${LANG_ENGLISH}     "VLC media player is required for audio playback.$\nPlease install VLC before launching Radikall."
LangString MSG_VLC_WARN ${LANG_SIMPCHINESE} "音频播放需要 VLC 媒体播放器。$\n请在启动 Radikall 前先安装 VLC。"
LangString MSG_VLC_WARN ${LANG_TRADCHINESE} "音訊播放需要 VLC 媒體播放器。$\n請在啟動 Radikall 前先安裝 VLC。"
LangString MSG_VLC_WARN ${LANG_JAPANESE}    "音声再生には VLC メディアプレーヤーが必要です。$\nRadikall を起動する前に VLC をインストールしてください。"
LangString MSG_VLC_WARN ${LANG_KOREAN}      "오디오 재생을 위해 VLC 미디어 플레이어가 필요합니다.$\nRadikall 실행 전에 VLC를 먼저 설치해 주세요."

LangString MSG_LAUNCH ${LANG_ENGLISH}     "Launch Radikall"
LangString MSG_LAUNCH ${LANG_SIMPCHINESE} "立即启动 Radikall"
LangString MSG_LAUNCH ${LANG_TRADCHINESE} "立即啟動 Radikall"
LangString MSG_LAUNCH ${LANG_JAPANESE}    "Radikall を起動する"
LangString MSG_LAUNCH ${LANG_KOREAN}      "Radikall 실행"

; ---------- 语言选择弹窗 ----------
Function .onInit
    !insertmacro MUI_LANGDLL_DISPLAY
FunctionEnd

Section "MainSection" SEC01
    SetOutPath "$TEMP"
    DetailPrint "$(MSG_INSTALLING)"

    ; 把内层 jpackage EXE 释放到临时目录再静默安装
    File "..\..\desktopApp\build\compose\binaries\main-release\exe\Radikall-0.1.0.exe"
    
    ; jpackage 生成的 EXE 支持 /S 静默安装（WiX 封装）
    ExecWait '"$TEMP\Radikall-0.1.0.exe" /quiet'
    Delete "$TEMP\Radikall-0.1.0.exe"

    ; jpackage EXE 默认装到 C:\Program Files\Radikall\
    ; 快捷方式指向该路径
    CreateShortcut "$DESKTOP\Radikall.lnk" "$PROGRAMFILES64\Radikall\Radikall.exe"

    MessageBox MB_ICONINFORMATION "$(MSG_VLC_WARN)"

    WriteUninstaller "$PROGRAMFILES64\Radikall\NSIS-Uninstall.exe"
    WriteRegStr HKLM "Software\Microsoft\Windows\CurrentVersion\Uninstall\Radikall" \
                     "DisplayName" "Radikall"
    WriteRegStr HKLM "Software\Microsoft\Windows\CurrentVersion\Uninstall\Radikall" \
                     "UninstallString" "$PROGRAMFILES64\Radikall\NSIS-Uninstall.exe"
SectionEnd

Section "Uninstall"
    ; 调用 jpackage 自带的卸载程序
    ExecWait '"$PROGRAMFILES64\Radikall\Uninstall.exe" /quiet'
    Delete "$DESKTOP\Radikall.lnk"
    DeleteRegKey HKLM "Software\Microsoft\Windows\CurrentVersion\Uninstall\Radikall"
SectionEnd