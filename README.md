# Phrolova Core X

Phrolova Core X is an Android tuning utility for rooted devices and compatible Android devices.

Created by **Sparxiee22**.

## Requirements

- Rooted device (Magisk recommended)
- Android 10+ (API 29)

## Features

- Renderer Change Vulkan / SkiaGL
- CPU & GPU Tuning
- I/O Scheduler
- Thermal Tuning
- Memory & VM
- Zram Tuning
- Live monitoring dashboard
- Material You theme (light / dark / system)

## Build

Open the project in Android Studio and build the debug APK, or run:

```sh
./gradlew :app:assembleDebug
```

The signed release APK can be built with:

```sh
./gradlew :app:assembleRelease
```

## Warning

Many controls write directly to kernel nodes or Android system settings. Disabling thermal protection, forcing clocks, or changing charging behavior can cause overheating, instability, data loss, or hardware damage. Use carefully.

## License

This project is licensed under the MIT License. Everyone is free to use, modify, fork, and redistribute it as long as the license notice is kept.
