# MTK Client for Android

Native Android host for [mtkclient](https://github.com/bkerler/mtkclient) 2.1.4.1. Python protocol code runs inside the app via Chaquopy; USB I/O uses Android USB Host (OTG).

This application provides an Android interface to mtkclient, supporting partition read and write, bootloader unlock, and Download Agent bypass operations on supported MediaTek devices over USB, without requiring a desktop computer.

## 1.1

- Jetpack Compose UI with Material 3 Expressive
- Bottom navigation: Console, Commands, Settings
- Persistent protocol defaults, command pinning, command history
- Cooperative command cancellation and transfer progress
- OLED theme and selectable color palettes, including Stock Android and Material You
- Settings export and import

## Build

Open this folder in Android Studio, or:

```
./gradlew assembleDebug
```

CI runs the same task on every push to `main` (see `.github/workflows/android-build.yml`).

Requires JDK 17 and Python 3.11 on the build machine (matching Chaquopy).

## Using it

1. Connect the host phone to the target with a USB OTG cable.
2. Open Console and tap **Detect device**, then grant USB permission.
3. Power the target fully off.
4. Start a command, wait until the engine is listening, then plug the target in (no buttons = Preloader; Volume up + Volume down = BootROM).

Destructive write and erase commands always ask for confirmation.

## License

The bundled mtkclient engine is GPLv3 (B. Kerler). Keep that license when you redistribute.
bypass_utility is MIT (Dinolek, 2021).
