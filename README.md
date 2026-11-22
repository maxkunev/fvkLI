<p align="center">
  <img src="app/src/main/res/drawable/logo_li_no_bg.png" width="240" alt="LI (fvkLI) Logo" />
</p>

<h1 align="center">LI (fvkLI) — Offline Android Music Player</h1>

<p align="center">
  <strong>Standalone, open-source Android music player and local YouTube / TikTok audio downloader.</strong><br/>
  No external servers. No subscriptions. Original audio quality stored directly on your device.
</p>

<p align="center">
  <a href="https://github.com/maxkunev/fvkLI/releases/latest">
    <img src="https://img.shields.io/github/v/release/maxkunev/fvkLI?color=1DB954&style=for-the-badge" alt="Latest Release" />
  </a>
  <img src="https://img.shields.io/badge/Platform-Android-3DDC84?style=for-the-badge&logo=android&logoColor=white" alt="Android" />
  <img src="https://img.shields.io/badge/Kotlin-7F52FF?style=for-the-badge&logo=kotlin&logoColor=white" alt="Kotlin" />
  <img src="https://img.shields.io/badge/Jetpack%20Compose-4285F4?style=for-the-badge&logo=jetpackcompose&logoColor=white" alt="Jetpack Compose" />
  <img src="https://img.shields.io/badge/Media3-ExoPlayer-000000?style=for-the-badge" alt="Jetpack Media3" />
</p>

---

## Why LI?

Most music apps require a constant internet connection, paid subscriptions, or third-party cloud backends to download tracks. **LI** runs completely on-device using `yt-dlp`, keeping your audio library offline, private, and free of lossy re-encoding.

## Features

- **Direct YouTube & TikTok Audio Extraction:** Paste a link via the floating `+` button to download tracks directly on your phone using `youtubedl-android` (`yt-dlp`).
- **Original Audio Quality (No Re-encoding):** Saves native audio streams (`.opus`, `.m4a`, `.mp3`, `.webm`) without unnecessary FFmpeg transcoding.
- **Background Playback (Jetpack Media3):** Persistent foreground playback service powered by ExoPlayer with lock-screen controls, notification widget, headset button support, and continuous loop playback.
- **Full-Screen & Mini Player:** Expandable `PlayerScreen` with cover art, interactive seek slider, and timers, paired with a compact bottom `MiniPlayer`.
- **Smart Local Library:** Automatically fetches track thumbnails, splits raw video titles into Artist & Title, highlights the active track with play/pause indicators, and supports manual renaming and deletion.

## Installation

1. Go to the [**Latest Release**](https://github.com/maxkunev/fvkLI/releases/latest) page.
2. Download `LI-v0.1.0.apk` from the **Assets** section.
3. Open the APK on your Android device and allow installation from unknown sources if prompted.

## Tech Stack

- **Language:** Kotlin
- **UI:** Jetpack Compose (Material 3)
- **Audio Engine:** Jetpack Media3 (ExoPlayer & MediaSessionService)
- **Downloader:** `youtubedl-android` (`yt-dlp`)
- **Image Loading:** Coil
- **Metadata Storage:** Lightweight local `.json` files alongside audio tracks

## Roadmap

- [x] Offline YouTube & TikTok audio downloading without re-encoding
- [x] Jetpack Media3 background service & loop queue
- [x] Automatic covers, MiniPlayer & full-screen PlayerScreen with seek bar
- [x] Manual track editing & deletion
- [ ] Direct downloading via Android **Share** menu from YouTube / TikTok (`v0.2.0`)
- [ ] Custom playlists & library search
- [ ] Local on-device AI for automatic metadata cleanup & lyrics/subtitles

## Building from Source

1. Clone the repository:
   ```bash
   git clone [https://github.com/maxkunev/fvkLI.git](https://github.com/maxkunev/fvkLI.git)
   ```
2. Open the project in **Android Studio**.
3. Let Gradle sync dependencies and run the `app` configuration on an emulator or physical Android device.

---

<sub>
<strong>Tags & Keywords:</strong> android music player, offline music player android, open source spotify alternative, youtube audio downloader android, tiktok audio downloader, yt-dlp android app, youtubedl-android, jetpack media3 exoplayer, jetpack compose music player, kotlin audio player, local mp3 opus player, free ad-free music app android.
</sub>
