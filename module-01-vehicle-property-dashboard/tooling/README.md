# Tooling

Windows automation for the whole loop: install the toolchain, create the automotive emulator, build,
install, launch, capture.

These scripts exist because the development machine had no free space on `C:` and the entire Android
toolchain had to be installed to `D:` instead — reproducibly, and without a half-manual setup that would
be impossible to repeat. See [docs/01 — Environment setup](../docs/01-environment-setup.md) for the
reasoning.

> **Paths are hard-coded to `D:\Android`.** These are working scripts from a real setup, kept as-is rather
> than genericised. To use another location, change the `$Root` variable at the top of each `.ps1`.

## The scripts

| Script | What it does |
|---|---|
| `setup-aaos-on-D.ps1` | One-shot install: sets the environment variables, downloads JDK 17 and the SDK command-line tools, accepts licences, installs `platform-tools`, `emulator`, `platforms;android-34`, `build-tools;34.0.0` and the automotive system image, creates the tuned AVD, and builds the APK. Logs everything. |
| `run-app.ps1` | Starts the emulator if it is not running, waits for boot, installs the APK, launches the activity. |
| `rebuild-app.ps1` | Rebuilds after a source change and reinstalls onto the running emulator. |
| `capture-shots.ps1` | Captures the emulator screen to `screenshots/shotN.png`, auto-numbering. |

Each has a double-clickable `.bat` wrapper — `INSTALL.bat`, `RUN-APP.bat`, `REBUILD-APP.bat`,
`CAPTURE.bat` — that just invokes the PowerShell script with `-ExecutionPolicy Bypass`.

`CHECK-APP.bat` dumps recent logcat to a file, for diagnosing a misbehaving app.

## Order

```
INSTALL.bat        →  once, ~20–40 min (mostly downloading)
RUN-APP.bat        →  start the emulator and the app
REBUILD-APP.bat    →  after each source change
CAPTURE.bat        →  grab a screenshot
```

## The one step that cannot be scripted

CPU virtualisation lives in firmware. Enable *Intel VT-x* / *AMD-V* in the BIOS, or turn on the *Windows
Hypervisor Platform* feature, then verify:

```
D:\Android\Sdk\emulator\emulator.exe -accel-check
```

Without it the automotive emulator is slow enough to be unusable, and the symptom looks like a hang
rather than an error.

## Notes on two implementation details

**Screenshot capture uses `screencap` + `pull`, not `exec-out`.** Piping `adb exec-out screencap -p`
through a PowerShell redirect corrupts the PNG, because the pipeline re-encodes the binary stream.
Writing to `/sdcard` and pulling the file avoids the round-trip entirely.

**The system image is discovered, not hard-coded.** `setup-aaos-on-D.ps1` queries `sdkmanager --list` for
the newest matching `system-images;android-NN;android-automotive;x86_64` package and falls back to a
known-good one only if that fails — so the script does not rot when a new image is published.
