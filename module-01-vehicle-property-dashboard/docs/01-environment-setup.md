# 01 — Environment setup

> Moving the entire Android toolchain off the system drive, and why the order matters.

## The constraint

The development machine — Windows 11, Intel i5, 20 GB RAM — had almost no free space on `C:`. An AAOS
setup is not small:

| Component | Approximate size |
|---|---|
| JDK 17 | 300 MB |
| Android SDK command-line tools | 150 MB |
| Platform + build-tools | 1 GB |
| **Automotive system image** | 6–8 GB |
| Emulator + AVD data partition | 7 GB |
| Gradle cache | 2–3 GB |

Roughly 20–25 GB, essentially all of which defaults to `C:`. The whole toolchain had to live on `D:`
instead.

## The important part: variables first, install second

Every Android tool decides where to write **at first run**, by reading environment variables. Set them
after installing and you get a split installation — some things on `D:`, some already on `C:`, and a
`sdkmanager` that cannot find what it downloaded.

So the variables go in first, at User scope, before anything is installed:

| Variable | Value | What it moves |
|---|---|---|
| `ANDROID_HOME` | `D:\Android\Sdk` | The SDK itself |
| `ANDROID_SDK_ROOT` | `D:\Android\Sdk` | Same, for tools that read the older name |
| `ANDROID_USER_HOME` | `D:\Android\.android` | Emulator config, keystores, caches |
| `ANDROID_AVD_HOME` | `D:\Android\.android\avd` | **The virtual devices and their disk images** — the biggest single item |
| `GRADLE_USER_HOME` | `D:\Android\.gradle` | The Gradle dependency cache |
| `JAVA_HOME` | `D:\Android\jdk\jdk-17.0.20+8` | The JDK Gradle builds with |

`ANDROID_AVD_HOME` is the one that matters most. Without it the AVD's 7 GB data partition and its
snapshots land in `C:\Users\<you>\.android\avd` regardless of where the SDK lives.

[`tooling/setup-aaos-on-D.ps1`](../tooling/setup-aaos-on-D.ps1) sets all of these persistently and in the
current process, then does the installation in the same run:

```powershell
[Environment]::SetEnvironmentVariable($k, $vars[$k], "User")
```

## CPU virtualisation

The automotive emulator is an x86_64 virtual machine. Without hardware acceleration it does technically
boot, but slowly enough to be unusable — and the failure looks like a hang, not an error.

This is the one step that cannot be scripted, because it lives in firmware:

- **BIOS/UEFI** — enable *Intel Virtualization Technology (VT-x)* or *AMD-V*.
- **or Windows** — enable the *Windows Hypervisor Platform* feature.

Verify before going any further:

```
%ANDROID_HOME%\emulator\emulator.exe -accel-check
```

Check this first, not last. Every subsequent problem is easier to diagnose once acceleration is known
to be working.

## SDK packages installed

```
platform-tools
emulator
platforms;android-34
build-tools;34.0.0
system-images;android-33;android-automotive;x86_64
```

`platforms;android-34` is not optional and not merely the compile target — it is where `android.car.jar`
comes from. See [02 — Emulator and AVD](02-emulator-and-avd.md#where-androidcarjar-actually-comes-from).

Note the version split, which is deliberate and fine: the app **compiles** against platform 34 while the
emulator **runs** an API 33 automotive image. `minSdk` is 29, so the APK is valid on the image.

## Automating it

The setup script handles licence acceptance non-interactively, discovers the newest available automotive
system image rather than hard-coding one, and falls back to a known-good package if discovery fails:

```powershell
$list = cmd /c "$sdkmanager --list --sdk_root=$SdkDir 2>nul"
$cand = $list | Select-String -Pattern "system-images;android-\d+;android-automotive(-playstore)?;x86_64" -AllMatches
if (-not $imagePkg) { $imagePkg = "system-images;android-33;android-automotive;x86_64" }
```

Everything is logged, so a failed run — almost always a dropped download — can be diagnosed and simply
re-run.

---

**Next:** [02 — Emulator and AVD](02-emulator-and-avd.md)
