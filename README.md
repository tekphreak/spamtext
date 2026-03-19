# SpamText

An Android app that automatically sends a custom SMS reply to suspected spam/telemarketer callers.

## How It Works

SpamText uses Android's `CallScreeningService` to intercept incoming calls before they ring. If the caller is **not in your contacts** (and/or fails STIR/SHAKEN carrier verification), it silently fires a configurable SMS message back to the caller — discouraging robocallers and telemarketers without disrupting your experience.

## Features

- **Auto SMS reply** to callers not in your contacts
- **STIR/SHAKEN support** — also triggers on calls that fail carrier spam verification (Android 11+)
- **Customizable message** with dynamic template tags:
  - `[timestamp]` — full date and time of the call
  - `[date]` — date of the call
  - `[time]` — time of the call
  - `[numbercalled]` — the caller's phone number
- **Call log** — view a masked log of all SMS replies sent
- **Black + green UI** — clean, minimal dark theme

## Requirements

- Android 7.0+ (API 24)
- Permissions: Read Contacts, Send SMS, Read Phone State, Read Call Log
- Call Screening role (granted via system dialog on first enable)

## Installation

Download the latest APK from [Releases](../../releases) and install it on your device.

> You may need to allow "Install from unknown sources" in your device settings.

## Building from Source

```bash
./gradlew assembleDebug
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

## Support

If SpamText saves you from one more robocall, consider buying me a coffee:

[![Ko-fi](https://ko-fi.com/img/githubbutton_sm.svg)](https://ko-fi.com/tekphreak)

## Author

[tekphreak.com](https://tekphreak.com)
