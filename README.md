# Lattice Launcher

An Android home-screen launcher built with Kotlin and Jetpack Compose that organizes apps locally, with optional AI-powered customization.

## Features

- **Home Screen Replacement** — Registers as a HOME intent handler, replacing your default launcher.
- **App Grid** — Lists all installed apps via `PackageManager`, grouped into categories.
- **Offline Organization** — Groups apps locally using Android category metadata and deterministic package/label rules; no account, network request, or API key is required.
- **Editable Layout** — Long-press an app to move it between categories.
- **Optional AI Organization** — When configured, natural-language requests can reorganize the local layout through the Anthropic Messages API.
- **Settings** — Optionally enter an Anthropic API key or proxy URL at runtime; no key is baked into the app.
- **Persistent Layout** — Saves your organized layout locally via DataStore.

## Requirements

- JDK 17
- Android SDK (compileSdk 35, minSdk 26)
- Gradle 8.11.1

## Download

Installable Android APKs are published under [GitHub Releases](https://github.com/asam89/mobrganize/releases). The current packages are debug-signed testing builds; Android may ask you to allow installs from your browser or file manager.

## Build

```bash
# Generate Gradle wrapper (if not present)
gradle wrapper --gradle-version 8.11.1

# Build debug APK
./gradlew assembleDebug

# Run unit tests
./gradlew test

# Lint
./gradlew lint
```

The debug APK will be at `app/build/outputs/apk/debug/app-debug.apk`.

## Project Structure

```
app/src/main/java/com/lattice/launcher/
├── data/
│   ├── AppCategory.kt       # Platform-independent app category model
│   ├── AppInfo.kt           # Data class for installed apps
│   ├── AppRepository.kt     # Queries PackageManager for launcher apps
│   ├── HomeLayout.kt        # Serializable layout model (categories + hidden)
│   ├── OfflineOrganizer.kt  # Local deterministic organization rules
│   ├── LayoutPlanner.kt     # JSON extraction, parsing, sanitization from LLM
│   └── SettingsStore.kt     # DataStore-backed preferences (API key, layout)
├── network/
│   └── AnthropicClient.kt   # OkHttp client for Anthropic Messages API
└── ui/
    ├── components/
    │   └── AppGrid.kt       # LazyVerticalGrid of categorized app icons
    ├── home/
    │   ├── HomeActivity.kt   # Main launcher activity (HOME intent)
    │   ├── HomeScreen.kt     # Compose home screen with organize prompt
    │   └── HomeViewModel.kt  # State management and AI orchestration
    ├── settings/
    │   └── SettingsScreen.kt # API key entry form
    └── theme/
        └── Theme.kt          # Material 3 dynamic color theme
```

## Proxy (Stretch Goal)

The `proxy/` folder contains a Cloudflare Worker that forwards `/v1/messages` to Anthropic with the API key as a server-side secret. Not deployed — scaffold only.

```bash
cd proxy
npm install
# Set the secret: wrangler secret put ANTHROPIC_API_KEY
npm run dev   # local dev
npm run deploy # deploy to Cloudflare
```

## License

MIT
