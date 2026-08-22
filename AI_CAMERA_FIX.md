# AI Camera identification fix

This build improves Biomate's **Camera → Identify** path.

## What changed

1. CameraX frames are converted to a normal, correctly oriented JPEG before Gemini sees
   them. The image is scaled to at most 1600 px on its longest edge and JPEG-compressed to
   keep mobile requests fast and consistent across phones.
2. The Gemini HTTP read timeout is now 75 seconds (90-second total call timeout). The old
   default could time out a vision request after about 10 seconds and silently fall back.
3. Extended Gemini thinking is disabled for this simple classification request to reduce
   latency.
4. 429/5xx responses are retried once.
5. **Identify mode no longer silently returns a random offline field-guide result when a
   configured Gemini key fails.** It now shows the real Google API/network error in the UI.
6. Confidence parsing accepts either `0..1` or `0..100` model output.
7. The prompt now identifies the main visible subject rather than forcing every photo to be
   a Victorian plant/animal.

## Quick API-key test

From the project root in PowerShell:

```powershell
.\test_gemini_api.ps1
```

The script reads `.env`, does not print the key, and makes one tiny text request to
`gemini-2.5-flash`.

If it reports `Gemini API connection: OK`, then rebuild/reinstall the app and test Camera →
Identify again.

If it reports an API error, the Google error/status will be shown without exposing the key.

## Android logs

If the API test passes but the camera still fails, connect the phone and run:

```powershell
& "$env:LOCALAPPDATA\Android\Sdk\platform-tools\adb.exe" logcat -s GeminiDirectIdentify PhotoScanScreen AppContainer
```

Then reproduce Camera → Identify. The logs now show the actual failure rather than hiding it
behind the offline catalogue.
