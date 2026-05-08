# NALoader

A dynamic loader for Android NativeActivity libraries. Load and run arbitrary `.so` files that implement the `ANativeActivity_onCreate` entry point, without repackaging or rebuilding the host app.

## Motivation

Android's `NativeActivity` system ties the native library to the APK at build time. NALoader breaks that constraint — useful for iterating on native code without full rebuilds, or for loading libraries produced externally.

Built to support a mobile-only development workflow using Termux and the Android NDK directly on device, where repackaging the APK on every native change is impractical.

## How it works

NALoader is a host APK with two components:

- **`MainActivity`** — file picker that receives a `.so` path and launches the loader
- **`LoaderActivity`** — runs in a separate process (`android:process=":loaded"`), copies the selected `.so` to internal storage, and loads it via `dlopen`

The loaded library receives a fully initialized `ANativeActivity*` — the same pointer the system would have passed if the library had been loaded normally. Lifecycle callbacks (`onPause`, `onDestroy`, surface events, input events) are relayed from the Java side to the loaded library as the system calls them.

The separate process ensures clean isolation: when `LoaderActivity` finishes, the process is killed, releasing all resources held by the loaded library regardless of its internal state.

Logcat output from the loaded library is captured to `logcat.txt` in internal storage on pause and on destroy, readable via any file manager without a PC.

## Requirements

- Android API 33+
- NDK 29
- A `.so` that exports `ANativeActivity_onCreate`

## Building

```bash
# With Gradle
./gradlew assembleDebug
```

The APK will be at `app/build/outputs/apk/debug/app-debug.apk`.

## Usage

1. Install the APK
2. Open NALoader
3. Tap **Choose File** and select a `.so` file
4. Tap **Load**

The library runs immediately. To read its logcat output, open `Android/data/com.lualvsil.naloader/files/logcat.txt` (or the internal files directory) in a file manager after closing the activity.

## Limitations

- The loaded library must export `ANativeActivity_onCreate`
- Libraries that do not handle `APP_CMD_DESTROY` correctly may delay process shutdown
- No support for native bridge (non-native ABIs)
- One library loaded at a time

## License

MIT
