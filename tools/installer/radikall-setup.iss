; ============================================================
;  Radikall — Inno Setup 安装脚本
;  支持: English / 简体中文 / 繁體中文 / 日本語 / 한국어
; ============================================================

#define AppName "Radikall"
#define AppVersion "0.1.0"
#define AppPublisher "baudstudio.com"
#define AppURL "https://baudstudio.com"
#define AppExeName "Radikall.exe"
#define SourceDir "..\..\desktopApp\build\compose\binaries\main-release\app\Radikall"
#define ResourceDir "..\..\desktopApp\src\main\resources"

[Setup]
AppId={{A1B2C3D4-E5F6-7890-ABCD-EF1234567890}
AppName={#AppName}
AppVersion={#AppVersion}
AppPublisher={#AppPublisher}
AppPublisherURL={#AppURL}
DefaultDirName={autopf}\{#AppName}
DefaultGroupName={#AppName}
OutputDir=..\..\desktopApp\build\compose\binaries\main-release
OutputBaseFilename=Radikall-Setup-{#AppVersion}
SetupIconFile={#ResourceDir}\logo2.ico
WizardImageFile={#ResourceDir}\wix-dialog.bmp
WizardSmallImageFile={#ResourceDir}\wix-banner.bmp
WizardImageStretch=yes
Compression=lzma2/ultra64
SolidCompression=yes
PrivilegesRequired=admin
ShowLanguageDialog=yes
ArchitecturesInstallIn64BitMode=x64compatible

[Languages]
Name: "en";   MessagesFile: "compiler:Default.isl"
Name: "zhcn"; MessagesFile: "compiler:Languages\ChineseSimplified.isl"
Name: "zhtw"; MessagesFile: "compiler:Languages\ChineseTraditional.isl"
Name: "ja";   MessagesFile: "compiler:Languages\Japanese.isl"
Name: "ko";   MessagesFile: "compiler:Languages\Korean.isl"

[CustomMessages]
en.VlcWarning=VLC media player is required for audio playback.%nPlease install VLC before launching Radikall.
zhcn.VlcWarning=音频播放需要 VLC 媒体播放器。%n请在启动 Radikall 前先安装 VLC。
zhtw.VlcWarning=音訊播放需要 VLC 媒體播放器。%n請在啟動 Radikall 前先安裝 VLC。
ja.VlcWarning=音声再生には VLC メディアプレーヤーが必要です。%nRadikall を起動する前に VLC をインストールしてください。
ko.VlcWarning=오디오 재생을 위해 VLC 미디어 플레이어가 필요합니다.%nRadikall 실행 전에 VLC를 먼저 설치해 주세요。

[Files]
Source: "{#SourceDir}\{#AppExeName}"; DestDir: "{app}"; Flags: ignoreversion
Source: "{#SourceDir}\app\*";         DestDir: "{app}\app";     Flags: ignoreversion recursesubdirs createallsubdirs
Source: "{#SourceDir}\runtime\*";     DestDir: "{app}\runtime"; Flags: ignoreversion recursesubdirs createallsubdirs

[Icons]
; 开始菜单
Name: "{group}\{#AppName}"; Filename: "{app}\{#AppExeName}"
; 桌面快捷方式
Name: "{autodesktop}\{#AppName}"; Filename: "{app}\{#AppExeName}"

[Run]
Filename: "{app}\{#AppExeName}"; \
    Description: "{cm:LaunchProgram,{#AppName}}"; \
    Flags: nowait postinstall skipifsilent

[Code]
procedure CurStepChanged(CurStep: TSetupStep);
begin
  if CurStep = ssPostInstall then
    MsgBox(CustomMessage('VlcWarning'), mbInformation, MB_OK);
end;