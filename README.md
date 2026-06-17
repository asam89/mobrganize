# Lattice Launcher

An Android home-screen launcher built with Kotlin and Jetpack Compose that uses AI to organize your apps.

## Features

- **Home Screen Replacement** — Registers as a HOME intent handler, replacing your default launcher.
- **App Grid** — Lists all installed apps via `PackageManager`, grouped into categories.
- **AI Organization** — Enter a natural-language command (e.g., "group my apps by type") and Lattice calls the Anthropic Messages API to reorganize your layout.
- **Settings** — Enter your Anthropic API key at runtime; no key is baked into the app.
- **Persistent Layout** — Saves your organized layout locally via DataStore.

## Requirements

- JDK 17
- Android SDK (compileSdk 35, minSdk 26)
- Gradle 8.11.1

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
│   ├── AppInfo.kt           # Data class for installed apps
│   ├── AppRepository.kt     # Queries PackageManager for launcher apps
│   ├── HomeLayout.kt        # Serializable layout model (categories + hidden)
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
