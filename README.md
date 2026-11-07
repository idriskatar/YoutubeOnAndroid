# Youtube On Auto

Youtube On Auto is a WebView browser with YouTube, Google, Kantan News, and Browser launch choices. Its home screen uses a responsive dark card layout with one column in portrait and two columns in landscape.

## Android Auto

Android Auto closes parked-app screens when vehicle motion is detected. Audio-only playback can continue through a supported media app and playback service, but this browser does not currently provide a background audio playback service. Android Auto also does not provide this phone-projection app with a park-state callback to automatically reopen its screen when the vehicle is put in Park.

## GitHub Actions

The **Build Android APK** workflow builds the debug APK after pushes to `main` or `master`, on pull requests, and when manually started from the Actions tab. Download `YoutubeOnAuto-debug-apk` from the completed run's **Artifacts** section.

## Build locally

Open the repository root in Android Studio, or run:

```bash
./gradlew :app:assembleDebug
```

The project uses the Android 37 SDK and Java 21.

## License

Youtube On Auto is derived from [kododake/AABrowser](https://github.com/kododake/AABrowser) and remains under GPL-3.0-or-later. See [LICENSE](LICENSE).
