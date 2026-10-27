# Spool Studio (Klipper Edition)

![Platform](https://img.shields.io/badge/Platform-Android-green)
![Kotlin](https://img.shields.io/badge/Kotlin-100%25-blue)
![Build](https://img.shields.io/badge/Build-Passing-brightgreen)
![License](https://img.shields.io/badge/License-MIT-blue)

Spool Studio (Klipper Edition) is a fork of [GeorgHo's SpoolStudio](https://github.com/GeorgHo/SpoolStudio) adapted for **standard single-toolhead Klipper 3D printers** running Moonraker with the official [Spoolman integration](https://moonraker.readthedocs.io/en/latest/configuration/#spoolman).

While the upstream project was tailored specifically for the multi-toolhead Snapmaker U1 with Paxx12 Extended Firmware and custom SpoolLink/AFC macros, this fork streamlines and replaces those proprietary commands with standard Klipper / Moonraker G-code macros and endpoints.

---

## What's Different in this Fork?

- **Standard Moonraker G-Code Macro Support**:
  - Replaced Paxx12-specific `SET_PRINT_FILAMENT_CONFIG` and `SET_SPOOL_ID LANE=...` commands with standard Klipper/Moonraker commands:
    - Setting active spool: `SET_ACTIVE_SPOOL SPOOL_ID=<id>`
    - Clearing active spool: `CLEAR_ACTIVE_SPOOL`
- **Single Toolhead Workflow**:
  - Optimized for standard single-toolhead Klipper setups.
  - Queries active spool state directly via Moonraker's `/server/spoolman/status` and updates the active spool.
- **Universal Klipper / Moonraker Health Checks**:
  - Bypassed Snapmaker/Paxx12 firmware version 1.5.0+ and `spoollink` component prerequisites.
  - Validates connection and readiness via Moonraker's standard `GET /server/spoolman/status` API, falling back to `/printer/info` and standard Klipper `software_version`.
- **Retained Full NFC / Spoolman Capabilities**:
  - Native tag support introduced in Spoolman 0.27.0+.
  - OpenSpool tag reading and writing.
  - Full spool and filament management synced directly to your Spoolman instance.

---

## Prerequisites for Your Klipper Printer

1. **Moonraker with Spoolman component enabled**:
   In your `moonraker.conf`:
   ```ini
   [spoolman]
   server: http://<your-spoolman-ip>:7912
   sync_rate: 5
   ```
2. **Klipper Spoolman Macros**:
   Ensure you have the standard macros configured in your `printer.cfg` (or included macros file):
   ```gcode
   [gcode_macro SET_ACTIVE_SPOOL]
   gcode:
     {% if params.SPOOL_ID %}
       {action_call_remote_method(
          "spoolman_set_active_spool",
          spool_id=params.SPOOL_ID|int
       )}
     {% else %}
       {action_respond_info("Parameter 'SPOOL_ID' is required")}
     {% endif %}

   [gcode_macro CLEAR_ACTIVE_SPOOL]
   gcode:
     {action_call_remote_method(
        "spoolman_set_active_spool",
        spool_id=None
     )}
   ```

---

## Quick Start

### 1. Build or Install the APK

You can build the debug APK directly with Gradle:
```bash
./gradlew :app:assembleDebug
```
The resulting APK will be at:
`app/build/outputs/apk/debug/app-debug.apk`

Transfer and install this APK onto your NFC-capable Android device.

### 2. Connect Spoolman

- Open **Settings** in Spool Studio.
- Enter your Spoolman server URL (e.g., `http://192.168.1.100:7912`).
- Tap **Test Spoolman Connection** to ensure the database is reachable.

### 3. Connect Moonraker (Klipper)

- Enter your Moonraker URL in **Settings** (e.g., `http://192.168.1.100:7125`).
- Tap **Test Moonraker Connection**.
- The app will verify connectivity against `/server/spoolman/status` and report that Spoolman integration is active.

### 4. Manage Spools & Tags

- Tap **Read RFID** to scan OpenSpool / NFC tags on your spools.
- View and edit filament details, residual weights, colors, and temperature profiles.
- Set the active spool for your printer; Spool Studio sends `SET_ACTIVE_SPOOL SPOOL_ID=<id>` directly to Klipper via Moonraker.
- Clear the active spool anytime using `CLEAR_ACTIVE_SPOOL`.

---

## Highlights & Features

- Direct Moonraker `/printer/gcode/script` integration with standard `SET_ACTIVE_SPOOL` / `CLEAR_ACTIVE_SPOOL`.
- Queries active spool from `/server/spoolman/status`.
- NFC/RFID read and write support for OpenSpool-compatible tags.
- Physical NFC card UID storage in Spoolman (supports native Spoolman 0.27.0+ tags).
- Spoolman support for creating, editing, and managing filament spools.
- Bambu Lab RFID import when a user-provided key is configured.
- Modern dark UI designed for quick spool editing on mobile.

---

## Tech Stack

- Kotlin
- Jetpack Compose
- Material 3
- Android NFC API
- Kotlin Coroutines & Flow
- Retrofit / OkHttp
- Moonraker API & Spoolman API

---

## Credits

- Original project: [Spool Studio by GeorgHo](https://github.com/GeorgHo/SpoolStudio)
- Predecessor: [SpoolPainter by ni4223](https://github.com/ni4223/SpoolPainter)
- [Spoolman by Donkie](https://github.com/Donkie/Spoolman)
- [Klipper](https://www.klipper3d.org/) & [Moonraker](https://moonraker.readthedocs.io/)
- [OpenSpool](https://github.com/spoolman/openspool) specification

---

## License

This project is licensed under the MIT License.
