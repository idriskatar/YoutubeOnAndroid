# Youtube On Auto + MyOBD

This repository builds two Android APKs:

- **Youtube On Auto**: WebView browser with YouTube, Google, MyOBD, and Browser launch choices.
- **MyOBD**: connects to a paired Bluetooth Classic ELM327, reads standard OBD-II PIDs, and serves a local dashboard on `http://127.0.0.1:8765/`.

The MyOBD service binds only to the phone's loopback address. Its read-only JSON endpoint is `http://127.0.0.1:8765/api/state`. The dashboard lets you show or hide gauges, choose one or two columns, and select an accent color; these preferences stay in the browser on that phone.

## Using MyOBD

1. Pair the ELM327 in the phone's Android Bluetooth settings, using the PIN shown in the adapter manual if Android asks for one.
2. Open **MyOBD**, grant Nearby devices/Bluetooth access, choose the paired adapter, then tap **Bağlan ve servisi başlat**.
3. Open **Youtube On Auto** and choose **MyOBD**. The app opens its local web dashboard.
4. To stop polling and the local server, return to MyOBD and tap **Bağlantıyı ve servisi durdur**.

The dashboard shows six analog gauges in a three-column layout: engine RPM, vehicle speed, coolant temperature, engine load, fuel level, and adapter voltage. Fuel level uses the standard `01 2F` PID when the vehicle supports it and is reported as a percentage; it is not a direct litre reading. Unsupported PIDs remain blank. The app does not clear diagnostic trouble codes or send vehicle-control commands.

## GitHub Actions

The **Build Android APK** workflow builds both debug APKs after pushes to `main` or `master`, on pull requests, and when manually started from the Actions tab. Download `YoutubeOnAuto-and-MyOBD-debug-apks` from the completed run's **Artifacts** section.

## Build locally

Open the repository root in Android Studio, or run:

```bash
./gradlew :app:assembleDebug :myobd:assembleDebug
```

The projects use the Android 37 SDK and Java 21.

## License

Youtube On Auto is derived from [kododake/AABrowser](https://github.com/kododake/AABrowser) and remains under GPL-3.0-or-later. See [LICENSE](LICENSE). MyOBD is provided under the same repository license.
