# FileSaver

Android app that saves shared files via SAF (Storage Access Framework).

## Usage

1. Share file(s) from any app → select **FileSaver**
2. Confirm or edit file name(s)
3. Pick a destination folder (SAF directory picker)
4. Files are saved to the chosen directory

No launcher icon — the app only appears in the system share sheet.

## Requirements

- JDK 21: `C:\Program Files\Java\jdk-21`
- Android SDK: install `platforms;android-35`, `build-tools;35.0.0`, `platform-tools`
- SDK cmdline-tools: `C:\Program Files\AndroidStudioCommandLineTools\cmdline-tools\bin`

## Build

```bash
# Set JAVA_HOME (or add to system env)
set JAVA_HOME=C:\Program Files\Java\jdk-21

# Debug build (uses auto-generated debug keystore)
gradlew assembleDebug

# Release build — option A: passwords via command-line args
gradlew assembleRelease -PstorePassword=xxx -PkeyPassword=xxx

# Release build — option B: interactive prompt (run in a real terminal)
gradlew assembleRelease
# → will prompt: "Keystore password:" and "Key password:"
```

## Install

```bash
adb install app\build\outputs\apk\debug\filesaver-v2.0-debug.apk
```

## Release Signing

Edit `local.properties` (not committed):

```properties
sdk.dir=C\:\\Users\\xxxx\\AppData\\Local\\Android\\Sdk
keystore.path=C\:\\Projects\\xxxx.jks
keystore.alias=your_alias_here
```

Passwords are **never** stored in files. They are provided either:
- As Gradle properties: `-PstorePassword=xxx -PkeyPassword=xxx`
- Via interactive console prompt (when running in a terminal with `System.console()`)

## License

GPL-3.0
