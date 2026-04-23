# iOS on Windows + VMware macOS

This repo now contains an `iosApp` Xcode host plus the `shared` iOS targets needed for Kotlin Multiplatform direct integration.

## What to move to the macOS VM

Move the tracked `radiko-app` repo by Git:

```bash
git clone <your-repo-url>
cd radiko-app
```

Do not copy these Windows-local or generated folders into the VM:

- `.gradle/`
- `.gradle-dist/`
- `.kotlin/`
- `.idea/`
- any `build/` directory
- `local.properties`

Keep these VM-only:

- Apple ID / Keychain state
- provisioning profiles
- signing certificates
- App Store Connect credentials

## One-time VM setup

1. Install Xcode 16 and open it once.
2. Run:

```bash
xcode-select --install
sudo xcodebuild -license accept
```

3. Install JDK 17 and confirm:

```bash
java -version
```

4. Install the Android SDK command-line tools plus platform 35.
5. Create `local.properties` in the repo root with the Android SDK path from the VM:

```properties
sdk.dir=/Users/<you>/Library/Android/sdk
```

The Android SDK is still needed because Gradle configures the Android modules even when the final build target is iOS.

## Xcode project wiring

Open:

- `iosApp/iosApp.xcodeproj`

The target already includes the Gradle build phase:

```bash
cd "$SRCROOT/.."
./gradlew :shared:embedAndSignAppleFrameworkForXcode
```

In Xcode, verify these settings on the `iosApp` target:

- `Build Phases` -> `Embed Kotlin Framework` exists and sits before `Compile Sources`
- `Build Settings` -> `Enable User Script Sandboxing` = `No`
- `Signing & Capabilities` can stay unset for simulator-only work

## First simulator build in the VM

The first milestone is a simulator build, not device signing.

Sanity-check the framework build:

```bash
./gradlew :shared:linkDebugFrameworkIosX64
```

Then build the app:

```bash
xcodebuild \
  -project iosApp/iosApp.xcodeproj \
  -scheme iosApp \
  -configuration Debug \
  -sdk iphonesimulator \
  -destination 'platform=iOS Simulator,name=iPhone 16' \
  build
```

If the simulator name is unavailable, list installed simulators:

```bash
xcrun simctl list devices
```

## Optional Apple ID / signing branch

If Xcode shows a `Personal Team` under `Signing & Capabilities`, you can try a local Debug run to your own iPhone.

Use that branch only after the simulator build works. Do not block on signing before the app launches in Simulator.

## Alarm behavior on iOS

The iOS build now aims for this split behavior:

- if the app is in the foreground when the alarm fires, playback starts automatically
- a local notification is also scheduled daily for the selected alarm time
- if the app is not active, tapping that notification opens the app and starts playback

To test this in the VM or on a device:

1. Open the app and enable the station alarm.
2. Allow notification permission when prompted.
3. Set the alarm for a minute or two ahead.
4. Test once with the app kept in the foreground.
5. Test again with the app backgrounded, then tap the delivered notification.

## Later paid-program branch

After joining the Apple Developer Program:

1. Keep the bundle identifier stable.
2. Enable automatic signing.
3. Add `Background Modes` -> `Audio, AirPlay, and Picture in Picture`.
4. Archive from the VM:

```bash
xcodebuild \
  -project iosApp/iosApp.xcodeproj \
  -scheme iosApp \
  -configuration Release \
  -destination 'generic/platform=iOS' \
  -archivePath build/ios/Radikall.xcarchive \
  archive
```

5. Export or upload from Xcode Organizer after the archive succeeds.

## Daily workflow

1. Edit on Windows.
2. Commit and push.
3. Pull inside the macOS VM.
4. Run the simulator build from Xcode or `xcodebuild`.
5. Keep signing assets only inside the VM.
