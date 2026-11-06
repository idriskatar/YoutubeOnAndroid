# Youtube On Auto

Android WebView application based on [kododake/AABrowser](https://github.com/kododake/AABrowser).

## What it does

When opened, the app shows three choices:

- **YouTube** opens `m.youtube.com` in the built-in WebView.
- **Google** opens Google Search in the built-in WebView.
- **Browser** opens the address and search start page from AABrowser.

The original browser engine and features such as tabs, bookmarks, fullscreen, and desktop mode are retained. The launcher name and icon use the supplied Youtube On Auto branding.

## Build

Open this folder in Android Studio and build the `app` debug variant. The project inherits the upstream Android 15+ minimum SDK and current Android Gradle tooling requirements.

Push the project to GitHub to run **Build Android APK** on pushes to `main` or `master`, pull requests, or manually from the Actions tab. When the run succeeds, download `YoutubeOnAuto-debug-apk` from that run's **Artifacts** section. The workflow builds a debug-signed APK; it is suitable for installing directly for testing.

## License

This is a derivative of AABrowser and remains under GPL-3.0-or-later. See [LICENSE](LICENSE) and the upstream repository for source history and contributor attribution.
