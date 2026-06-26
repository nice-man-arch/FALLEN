<div align="center">

<br />

<img src="banner.png" alt="Fallan — between heaven, the void, and ruin" width="100%" />

<br />

```
░▒▓ F A L L A N ▓▒░

███████╗ █████╗ ██╗     ██╗      █████╗ ███╗  ██╗
██╔════╝██╔══██╗██║     ██║     ██╔══██╗████╗ ██║
█████╗  ███████║██║     ██║     ███████║██╔██╗██║
██╔══╝  ██╔══██║██║     ██║     ██╔══██║██║╚████║
██║     ██║  ██║███████╗███████╗██║  ██║██║ ╚███║
╚═╝     ╚═╝  ╚═╝╚══════╝╚══════╝╚═╝  ╚═╝╚═╝  ╚══╝

· · ───────────── ✦ ───────────── · ·
✧  BETWEEN HEAVEN  ·  THE VOID  ·  AND RUIN  ✧
⛧  ༢  ⛧  ⺸  ⛧  ༢  ⛧
```

<sub>vibe coded with Claude Sonnet 4.6 · Anthropic · 2026</sub>

<br />

**A music streaming app for Android — powered by JioSaavn**

![Android](https://img.shields.io/badge/Android-3DDC84?style=flat&logo=android&logoColor=white)
![Kotlin](https://img.shields.io/badge/Kotlin-7F52FF?style=flat&logo=kotlin&logoColor=white)
![Jetpack Compose](https://img.shields.io/badge/Jetpack%20Compose-4285F4?style=flat&logo=jetpackcompose&logoColor=white)
![Status](https://img.shields.io/badge/status-active%20development-orange?style=flat)

</div>

---

## What is Fallan?

Fallan is a native Android music streaming app built with Kotlin and Jetpack Compose. It uses JioSaavn as its backend to search and stream music, with a feature set built to match — and in some ways exceed — what mainstream players offer.

---

## Features

**Playback**
- Stream music via JioSaavn search and URL resolution
- Gapless playback — zero silence between songs
- Crossfade with adjustable duration (0–10s), mutually exclusive with gapless
- 5-band equalizer with presets (Flat, Bass Boost, Pop, Rock, Jazz, Classical)
- Sleep timer with volume fade, custom drum-roll time picker, and "after current song" mode
- Background playback with lock screen and notification controls (MediaSession + foreground service)
- Stops on swipe-from-recents, continues on screen off

**Player UI**
- Spinning disc album art with sweep gradient border
- Dynamic palette background — extracts dominant color from album art, animates on song change
- Custom Canvas background — set a local image or looping video as your player background, with adjustable blur/haze
- Album art as background option (Spotify Canvas-style)
- Waveform visualizer — 28 animated bars below the disc

**Lyrics**
- Fetches lyrics via LRCLib (synced or plain)
- Full-screen lyrics view, swipe up from player to open
- 3 switchable display styles:
  - **Centered** — Apple Music style, current line bold, others dimmed, auto-scrolls
  - **Typewriter** — monospace, lines type out character by character
  - **Poster** — large all-caps stacked display, lines punch in with scale animation
- All styles estimate current line from playback position

**Queue**
- Full-screen queue, swipe up from player to open
- Drag to reorder (long press handle)
- Swipe left to remove
- Tap any song to jump to it
- Inline search to add songs directly to the queue

**Library**
- Room database with recently played, liked songs, downloads, and playlists
- Background downloads with notification progress
- Download tracking with local file path storage

---

## Stack

| Layer | Technology |
|---|---|
| Language | Kotlin |
| UI | Jetpack Compose |
| Audio | MediaPlayer, MediaSession, ExoPlayer (video backgrounds) |
| Networking | OkHttp |
| Database | Room |
| Image loading | Coil |
| Music source | JioSaavn |
| Lyrics | LRCLib |

---

## Getting Started

**Prerequisites:** [Android Studio](https://developer.android.com/studio) (latest stable)

1. Clone the repository and open the project folder in Android Studio
2. Let Android Studio sync and resolve Gradle dependencies
3. Create a `.env` file in the project root and add your Gemini API key:
   ```
   GEMINI_API_KEY=your_key_here
   ```
   See `.env.example` for the expected format
4. In `app/build.gradle.kts`, remove this line:
   ```kotlin
   signingConfig = signingConfigs.getByName("debugConfig")
   ```
5. Connect a physical device or start an emulator (API 31+ recommended for blur effects)
6. Hit **Run**

---

## Project Structure

```
app/src/main/java/com/aistudio/fallan/streams/
├── MainActivity.kt          — Entry point, starts MusicService
├── MusicService.kt          — Foreground service, MediaSession, notification controls
├── FallenApp.kt             — All UI composables (~4000 lines)
├── MainViewModel.kt         — App state, SharedPreferences persistence
├── data/
│   └── FallenDatabase.kt    — Room DB: SongModel, SongEntity, MusicSource, playlists
├── player/
│   ├── PlaybackManager.kt   — Singleton: all playback logic, queue, crossfade, gapless
│   ├── FallenApi.kt         — JioSaavn search + stream resolution, LRCLib lyrics
│   └── FallenDownloadManager.kt — Background downloads with notification progress
```

---

## Key Decisions

| Decision | Outcome |
|---|---|
| YouTube / YT Music | Removed — InnerTube implementation built but deferred until device-testable |
| Piped | Removed — too unreliable |
| JioSaavn code | Never modify — search, resolution, and playback are stable |
| Crossfade + Gapless | Mutually exclusive — crossfade takes priority when active |
| Canvas URL inputs | Removed — local gallery + album art used instead |
| `MusicSource` enum values | Keep `YOUTUBE` and `YOUTUBE_MUSIC` in DB schema — Room migration safety |

---

## Known Issues

- Gapless + "sleep after current song": if gapless promotion fires at song end, `handleCompletion` is bypassed and the sleep timer won't trigger — needs a polish pass
- `FallenApp.kt` is ~4000 lines — some AI tools can't output it fully in one response; use the file tree panel or export ZIP to inspect

---

## Roadmap

- [ ] YouTube / YT Music re-integration (InnerTube implementation ready to drop in)
- [ ] Synced lyrics (LRC timestamp-based word-by-word highlight)
- [ ] Cross-source fallback for songs not in JioSaavn's index
- [ ] Split `FallenApp.kt` into separate composable files

---

<div align="center">
  <sub>Built with Kotlin + Jetpack Compose · Streamed via JioSaavn</sub>
</div>
