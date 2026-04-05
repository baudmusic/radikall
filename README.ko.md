<h1 align="center">Radikall</h1>

<p align="center">
  <img src=".github/assets/logo3.png" alt="Radikall logo" width="260" />
</p>

<p align="center">
  Windows와 Android를 위한 일본 라디오 클라이언트 — 전 세계 어디서나 모든 Radiko 방송국을 들을 수 있습니다.
</p>

<p align="center">
  <a href="https://baudstudio.com">baudstudio.com</a>
</p>

<p align="center">
  <a href="README.md">English</a> | <a href="README.zh-CN.md">简体中文</a> | <a href="README.zh-TW.md">繁體中文</a> | <a href="README.ja.md">日本語</a> | <a href="README.ko.md">한국어</a>
</p>

## 시작하기 전에

야마시타 타츠로의 라쿠텐카드 선데이 송북(山下達郎の楽天カード サンデー・ソングブック)을 더 편하게 듣기 위해 이 앱을 만들었습니다. 곧 여름이네요 — Big Wave 앨범을 들을 시간입니다.

덧붙여 한마디 하자면, Claude는 정말 인색합니다. 사용량 제한이 순식간에 사라지더군요.

## 개요

Radikall은 Radiko가 공식 제공하는 47개 도도부현의 모든 일본 방송국을 아우르는 크로스 플랫폼 라디오 클라이언트입니다. 위치 잠금도 IP 잠금도 없어 인터넷이 연결된 세계 어디서나 들을 수 있습니다.

부드러운 방송국 탐색, 현재 재생 중 상세 정보, 주간 편성표, Windows 트레이 재생 제어, 그리고 Windows와 Android를 아우르는 다국어 설정 시스템을 제공합니다.

이 프로젝트는 [jackyzy823/rajiko](https://github.com/jackyzy823/rajiko)의 아이디어, 리버스 엔지니어링 경로, 그리고 영감 덕분에 가능했습니다. 그 저장소 없이는 이 네이티브 앱 방식에 도달하기 훨씬 어려웠을 것입니다.

## 사용 전에

소스에서 빌드하는 대신 앱을 사용하려면 먼저 이 섹션을 읽어주세요.

### Windows

1. GitHub Releases 페이지에서 최신 Windows 설치 프로그램을 다운로드하세요.
2. 먼저 **VLC 미디어 플레이어**를 설치하세요.
3. VLC 설치 후 Radikall을 실행하세요. 데스크톱 재생은 `vlcj`를 통한 `libVLC`에 의존합니다.

권장 환경:

- Windows 10 또는 Windows 11
- 64비트 시스템
- 안정적인 인터넷 연결

### Android

1. GitHub Releases 페이지에서 최신 **서명된** Android APK를 다운로드하세요.
2. Android가 물어보면 알 수 없는 출처의 설치를 허용하세요.
3. 앱을 열고 재생 전에 지역을 선택하세요.

권장 환경:

- Android 7.0 이상
- 안정적인 Wi-Fi 또는 모바일 네트워크

### 소스에서 빌드

프로젝트를 직접 빌드하려면 다음을 먼저 설치하세요:

- Git
- 완전한 JDK 24 설치
- Android SDK 35가 포함된 Android Studio
- Platform Tools / `adb`
- 데스크톱 재생 테스트를 위한 Windows의 VLC 미디어 플레이어

그런 다음 실행하세요:

```powershell
.\gradlew.bat :desktopApp:run
.\gradlew.bat :androidApp:installDebug
```

## 릴리스 빌드

### Windows

```powershell
.\gradlew.bat :desktopApp:packageReleaseExe :desktopApp:packageReleaseMsi
```

출력:

- `desktopApp/build/compose/binaries/main-release/exe/`
- `desktopApp/build/compose/binaries/main-release/msi/`

### Android

```powershell
.\gradlew.bat :androidApp:assembleRelease
```

출력:

- `androidApp/build/outputs/apk/release/`

참고:

- 이 프로젝트는 현재 **하나의 범용 Android APK**를 빌드합니다. ABI 분할이나 밀도 분할은 활성화되어 있지 않습니다.
- Windows에서는 **두 가지 설치 형식**인 `EXE`와 `MSI`가 생성됩니다.
- Windows 패키징에는 **`jpackage`가 포함된 완전한 JDK**가 필요합니다. 일부 IDE에 번들된 간소화된 JetBrains Runtime으로는 작동하지 않습니다.
- `assembleRelease`는 현재 `androidApp-release-unsigned.apk`를 생성합니다. 사용자에게 배포하기 전에 자신의 키스토어로 서명해야 합니다.

## 기술 경로

Radikall은 Kotlin Multiplatform 아키텍처로 구축되었습니다:

- `shared/`: 공유 비즈니스 로직, Compose UI, 테마 시스템, 로컬라이제이션, 설정, 방송국 데이터, 재생 상태, 편성표 로직
- `androidApp/`: Android 호스트 앱, `Media3 ExoPlayer`, 라이프사이클 훅, 알람, 뒤로 가기 네비게이션, APK 패키징
- `desktopApp/`: Compose Desktop 셸, 커스텀 타이틀 바, 트레이 통합, Windows 패키징, 데스크톱 창 동작

저장소에서 사용된 핵심 기술:

- Kotlin 2.1.0
- Compose Multiplatform 1.7.3
- Material 3
- Ktor 3
- kotlinx.serialization
- kotlinx.datetime
- Coil 3
- Android `Media3 ExoPlayer`
- 데스크톱 `vlcj` + 재생을 위한 로컬 HLS 프록시
- JVM Preferences / Android 영속 설정
- 간체 중국어, 번체 중국어, 영어, 일본어, 한국어를 위한 앱 측 다국어 시스템

## 디자인 언어

이 팔레트는 우연이 아니라 의도적으로 선택되었습니다.

- `#D0104C`는 **韓紅花 / karakurenai**
- `#005CAF`는 **瑠璃 / RURI**
- `#113285`는 **紺青 / konjyo**

**韓紅花 / karakurenai**는 염료, 직물, 의식, 동아시아 전통 색 이름과 결합된 선명한 역사적 붉은색의 감각을 담고 있습니다. 헤이안 시대에 이 극도로 채도 높은 짙은 붉은색을 염색하려면 엄청난 양의 잇꽃(紅花, 사플라워) 꽃잎이 필요했고, 그 비용은 매우 막대했습니다. 그리하여 최고위 귀족의 부와 권력의 상징이 되었으며, 한때 일본 조정에 의해 서민의 사용이 절대적으로 금지된 '금색(禁色)'으로 지정되었습니다. 백인일수(百人一首)에 수록된 아리와라노 나리히라의 명구 "치하야부루 / 카미요모 키카즈 / 타츠타가와 / 카라쿠레나이니 / 미즈 쿠쿠루토와"는 바로 이 카라쿠레나이의 화려함과 짙음을 빌려, 타츠타 강 수면을 가득 덮은 단풍잎의 압도적인 광경을 읊은 것입니다.

**瑠璃 / RURI**는 불교의 '칠보(七寶)' 중 하나로 꼽힙니다. 질병을 치유하고 수명을 연장하는 부처인 약사여래가 거하는 동방 정토는 '동방 정유리 세계(東方淨瑠璃世界)'라 불립니다. 그러므로 이 색은 일본 역사 내내 신성함, 청정함, 순결함, 그리고 신비로운 종교적 힘과 결부되어 왔습니다. 고대 일본에서는 이 색의 원료를 생산할 수 없었고, 모두 험난한 실크로드를 거쳐 운반되어야 했습니다. 나라의 쇼소인(正倉院)에는 쇼무 천황 시대의 '유리배(瑠璃杯)'가 지금도 온전히 보존되어 있으며, 당시 그것은 단순한 색이 아니라 먼 서역과 극락 정토에 대한 상상을 체현하는 것이었습니다.

**紺青 / konjyo**는 직물, 안료, 밤, 바다, 거리의 깊은 파란색을 가져옵니다. 초기 일본화에서 곤조는 남동석(藍銅鑛, 아주라이트)을 갈아 만든 전통 천연 광물 안료였습니다. 장인들은 광석을 분쇄한 후 수비법(水飛法)으로 분리하여, 입자가 가장 굵고 색이 가장 짙은 층을 곤조로 취했습니다. 아스카·나라 시대의 고분 벽화(다카마쓰즈카 고분 등)와 고급 병풍화에 널리 사용되었습니다. 에도 시대 후기에는 유럽에서 발명된 합성 안료 '프러시안 블루(Prussian Blue)'가 일본에 전해져 '곤조' 또는 '베로아이(ベロ藍)'라고도 불렸습니다. 이 새로운 곤조는 가격이 저렴하고 발색이 선명하며 퇴색이 적어, 전통 청색 안료의 한계를 단번에 돌파했습니다. 가쓰시카 호쿠사이는 바로 이 새로운 '곤조'를 대량으로 사용하여 세계를 놀라게 한 《가나가와 해변의 높은 파도 아래(神奈川沖浪裏)》를 탄생시켰으며, 이는 우키요에 풍경화(명소에)의 전면적인 번영을 직접적으로 이끌었습니다.

이 세 가지 색은 모두 '일본과 세계의 교류'를 상징합니다. 저는 이 앱에서 이 색들을 사용하여 단순한 한 가지를 전달하고 싶었습니다: 라디오는 사람보다 쉽게 바다를 건넙니다.

## 기능

- Windows + Android 듀얼 플랫폼 앱
- 네이티브 방송국 탐색 UI
- 반응형 현재 재생 중 페이지
- 현재 프로그램 자동 포커스가 있는 주간 편성표
- 데스크톱 트레이 재생 제어
- 설정 페이지 (시작 지역, 자동 재생, 수면 타이머, 알람, Wi-Fi 규칙, 테마 모드, 데스크톱 종료 동작)
- 드라이빙 모드
- 로컬 다국어 UI

## 프로젝트 구조

```text
radiko-app/
|-- androidApp/
|-- desktopApp/
|-- shared/
|-- tools/
|-- .github/assets/
`-- README.md
```

## 감사의 말

특별한 감사를:

- [jackyzy823/rajiko](https://github.com/jackyzy823/rajiko)
- Codex
- Claude Code

## 팔로우

이 프로젝트가 마음에 든다면 제 작업과 소셜 플랫폼을 팔로우해 주세요:

- [baudstudio.com](https://baudstudio.com)

## 면책 조항

Radikall은 비공식 서드파티 클라이언트 애플리케이션입니다. Radiko Co., Ltd. 또는 그 방송 파트너와는 어떠한 제휴, 보증, 연계 관계도 없습니다.

Radiko®는 Radiko Co., Ltd.의 등록 상표입니다. 이 애플리케이션을 통해 접근하는 모든 라디오 콘텐츠, 방송국 로고, 프로그램 데이터 및 오디오 스트림은 Radiko Co., Ltd. 및 각 방송권 보유자의 소유입니다. 이 애플리케이션은 해당 콘텐츠를 호스팅, 캐시, 저장 또는 재배포하지 않습니다.

이 프로젝트는 개인 사용 및 교육·기술 연구 목적으로만 제작되었습니다. 이 소프트웨어의 상업적 사용은 허용되지 않습니다.

이 소프트웨어는 공개적으로 관찰 가능한 네트워크 인터페이스를 통해 데이터에 접근합니다. 사용자는 자신의 사용이 해당 지역의 법률, 규정 및 서비스 약관을 준수하는지 스스로 확인할 책임이 있습니다.

이 소프트웨어를 사용함으로써 귀하는 본 면책 조항을 읽고 이해했으며 자신의 책임 하에 소프트웨어를 사용하는 것에 동의하는 것으로 간주됩니다. 본 프로젝트의 개발자는 이 소프트웨어의 사용 또는 오용으로 인해 발생하는 어떠한 법적, 기술적 또는 기타 결과에 대해서도 책임을 지지 않습니다.
