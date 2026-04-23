; ============================================================
;  Radikall 鈥?Inno Setup 瀹夎鑴氭湰
;  鏀寔: English / 绠€浣撲腑鏂?/ 绻侀珨涓枃 / 鏃ユ湰瑾?/ 頃滉淡鞏?
; ============================================================

#define AppName "Radikall"
#define AppVersion "1.0.1"
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

[Files]
Source: "{#SourceDir}\{#AppExeName}"; DestDir: "{app}"; Flags: ignoreversion
Source: "{#SourceDir}\app\*";         DestDir: "{app}\app";     Flags: ignoreversion recursesubdirs createallsubdirs
Source: "{#SourceDir}\runtime\*";     DestDir: "{app}\runtime"; Flags: ignoreversion recursesubdirs createallsubdirs

[Icons]
; 寮€濮嬭彍鍗?
Name: "{group}\{#AppName}"; Filename: "{app}\{#AppExeName}"
; 妗岄潰蹇嵎鏂瑰紡
Name: "{autodesktop}\{#AppName}"; Filename: "{app}\{#AppExeName}"

[Run]
Filename: "{app}\{#AppExeName}"; \
    Description: "{cm:LaunchProgram,{#AppName}}"; \
    Flags: nowait postinstall skipifsilent

[Code]
function IsVLCInstalled(): Boolean;
begin
  Result := RegKeyExists(HKLM, 'SOFTWARE\VideoLAN\VLC') or
            RegKeyExists(HKLM, 'SOFTWARE\WOW6432Node\VideoLAN\VLC');
end;

procedure CurStepChanged(CurStep: TSetupStep);
var
  ErrorCode: Integer;
begin
  if CurStep = ssPostInstall then
  begin
    if not IsVLCInstalled() then
    begin
      MsgBox(CustomMessage('VlcWarning'), mbInformation, MB_OK);
      ShellExec('open', 'https://get.videolan.org/vlc/last/win64/', '', '', SW_SHOWNORMAL, ewNoWait, ErrorCode);
    end;
  end;
end;