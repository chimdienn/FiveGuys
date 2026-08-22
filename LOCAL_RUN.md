# Biomate local run

This copy is prepared for JDK 17 and includes live Google Maps + direct Gemini camera support.

## API configuration

The repository root must contain `.env`:

```properties
MAPS_API_KEY=...
GEMINI_API_KEY=...
```

Do not commit or share `.env`.

## Build

In PowerShell, from this folder:

```powershell
.\verify_and_build.ps1
```

The APK will be at:

`app\build\outputs\apk\debug\app-debug.apk`

## Install on phone

Connect an Android phone with USB debugging enabled, then run:

```powershell
.\install_debug.ps1
```

If Android reports `INSTALL_FAILED_UPDATE_INCOMPATIBLE`, uninstall the older Biomate build from the phone once and run the installer again.

## Test Google Maps

Open a trail -> OnTrail -> grant precise location. The map should show Google terrain tiles, the trail route, Trail Moments and your live GPS position.

The Maps key must have **Maps SDK for Android** enabled. If you restrict it by Android app, use package `com.aistudio.biomate.advntr` and the SHA-1 of the debug signing certificate.

## Test Gemini camera

Open Camera -> Identify, take a photo and tap Identify. Biomate sends the captured JPEG to Gemini and displays a structured outdoor identification.

Camera -> Challenge uses Gemini to judge whether the photo matches the selected challenge and returns pass/fail, confidence and a short explanation.

If the Gemini request fails, Biomate falls back to the existing offline service so the screen remains usable. For a public production release, move Gemini behind Firebase AI Logic or a backend because a direct mobile API key is extractable from an APK.
