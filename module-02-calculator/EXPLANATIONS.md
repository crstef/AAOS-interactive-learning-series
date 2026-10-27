# Calculator AAOS: detailed explanations

A calculator app for **Android Automotive OS (AAOS)**, written in Kotlin with Jetpack Compose.
It runs on the `AAOS_Capstone` emulator, alongside the `AutomotiveCarPropApp` (dashboard) app.

The commands to build and run it are in `COMMANDS.md`.

---

## Contents

1. [What the app does](#1-what-the-app-does)
2. [Project structure](#2-project-structure)
3. [Configuration files](#3-configuration-files)
4. [Architecture: how the Kotlin files work together](#4-architecture-how-the-kotlin-files-work-together)
5. [CalculatorEngine.kt: the logic](#5-calculatorenginekt-the-logic)
6. [CalculatorViewModel.kt: the state](#6-calculatorviewmodelkt-the-state)
7. [CalculatorScreen.kt: the UI](#7-calculatorscreenkt-the-ui)
8. [MainActivity.kt: the entry point](#8-mainactivitykt-the-entry-point)
9. [Unit tests](#9-unit-tests)
10. [What happens when you press a button (step by step)](#10-what-happens-when-you-press-a-button-step-by-step)
11. [Build and tool chain](#11-build-and-tool-chain)
12. [Problems we hit and lessons learned](#12-problems-we-hit-and-lessons-learned)
13. [What's next (extension ideas)](#13-whats-next-extension-ideas)
14. [Glossary](#14-glossary)

---

## 1. What the app does

| Button | Action |
|---|---|
| `0`–`9` | Type digits (up to 12 digits per number) |
| `.` | Decimal point (only one per number; `.5` becomes `0.5`) |
| `+` `−` `×` `÷` | Choose the operation |
| `=` | Compute the result |
| `C` | Clear everything |
| `⌫` | Delete the last typed digit |
| `±` | Change the sign of the number |
| `%` | Divide the number by 100 |

Special behaviours:
- **Chained operations:** `2 + 3 ×` immediately shows `5`, then continues with `5 × ...`
- **Changing the operator:** `2 + ×` replaces `+` with `×`
- **Division by zero** shows `Error` (in red). After an error, only `C`, a digit or `.` do anything
- **The result can be reused:** `2 + 3 = × 2 =` gives `10`
- **No floating-point errors:** `0.1 + 0.2 = 0.3` (not `0.30000000000000004`)
- **The line above the number** shows the expression: `12 +`, then `12 + 3 =`

---

## 2. Project structure

```
Calculator/
├── settings.gradle.kts           ← project name + modules (:app) + where to download from
├── build.gradle.kts              ← DECLARES the plugins (apply false)
├── gradle.properties             ← Gradle settings (memory, AndroidX)
├── gradlew / gradlew.bat         ← Gradle Wrapper (Linux / Windows)
├── gradle/
│   ├── libs.versions.toml        ← version catalog (all libraries)
│   └── wrapper/                  ← jar + properties (which Gradle version)
├── EXPLANATIONS.md               ← this file
├── COMMANDS.md                   ← PowerShell commands
└── app/                          ← the app module
    ├── build.gradle.kts          ← APPLIES the plugins; namespace, SDKs, dependencies
    └── src/
        ├── main/                 ← app code
        │   ├── AndroidManifest.xml
        │   ├── res/
        │   │   ├── values/strings.xml          ← app name
        │   │   ├── values/colors.xml           ← icon background color
        │   │   ├── drawable/ic_launcher_foreground.xml   ← icon drawing
        │   │   └── mipmap-anydpi-v26/ic_launcher*.xml    ← adaptive icon
        │   └── java/com/cristi/calculator/
        │       ├── MainActivity.kt          ← entry point: theme + screen
        │       ├── CalculatorScreen.kt      ← the UI (buttons, display)
        │       ├── CalculatorViewModel.kt   ← holds the state
        │       └── CalculatorEngine.kt      ← the logic (pure Kotlin)
        └── test/                 ← unit tests (run on the PC)
            └── java/com/cristi/calculator/
                └── CalculatorEngineTest.kt
```

**Golden rule:** Gradle and Android look for files at **exact** paths and names
(*convention over configuration*). A misnamed file, such as `gradle.proprieties`
or `libs.version.toml`, **does not cause an error: it is silently ignored**.

### 2.1 Why here: same workspace as the dashboard workshop, but a separate folder

```
D:\Android\                       ← shared workspace (everything on D:)
├── jdk\                          ← Java 17, shared
├── Sdk\                          ← Android SDK, emulator, adb, shared
├── .android\avd\AAOS_Capstone    ← the AAOS emulator, shared
└── Projects\
    ├── AutomotiveCarPropApp\     ← workshop 1: the dashboard (reference, untouched)
    └── Calculator\               ← workshop 2: this project (independent)
```

**Why the same parent folder (`D:\Android\Projects`)?**
- **One toolchain for everything.** The JDK, the Android SDK, the emulator and adb were already installed
  on `D:` for the first workshop. The calculator uses exactly the same ones: nothing extra to install,
  no second copy of a multi-GB SDK, and `JAVA_HOME` / `ANDROID_HOME` already point to the right places.
- **Same emulator.** Both apps run on `AAOS_Capstone`, side by side in the car's launcher.
  That's what makes the safety demo possible (the calculator blocked while driving, the dashboard still working).
- **The reference model is right next to it.** We built the calculator by reading and adapting
  `AutomotiveCarPropApp` file by file (`settings.gradle.kts`, `build.gradle.kts`, the Manifest, the wrapper).
  Having it one folder away made the comparison easy.
- **Shared Gradle home.** Both projects use the same Gradle home, `D:\Android\.gradle`
  (the `GRADLE_USER_HOME` environment variable). Gradle versions and libraries (Compose, AndroidX)
  are downloaded there once and reused by every project in the workspace, and nothing ends up on `C:`.

**Why a separate folder (and not inside the dashboard project)?**
- **Independent projects.** Each folder is a complete Gradle project with its own `settings.gradle.kts`,
  wrapper and `app` module. You can build, test, zip, version or delete one without touching the other.
- **Zero risk to the first workshop.** The dashboard is finished work. Every change to the calculator
  (dependencies, Manifest, permissions) stays in its own folder. The dashboard was never modified.
- **Different identities.** Separate `applicationId`s (`com.cristi.calculator` vs `com.infosys.cardashboard`)
  mean two separate apps on the emulator. Neither replaces the other.
- **Different needs.** The dashboard needs `android.car`, vehicle permissions and `distractionOptimized`.
  The calculator needs none of them. Separate projects keep each configuration minimal and clear.
- **Learning from scratch.** Creating a new project folder meant going through every step
  (structure, wrapper, Gradle, Manifest, first run) instead of just adding a screen to an existing app.

**The alternatives we considered at the start:**

| Option | What it means | Why not (for now) |
|---|---|---|
| **A. Two separate apps** ✅ | A new project folder; two icons on the emulator | The chosen one: independent, safe, teaches every step |
| B. Calculator inside the dashboard | Add a "Calculator" screen to `AutomotiveCarPropApp` | Mixes two workshops, and the dashboard's `distractionOptimized` would let the calculator run while driving |
| C. Calculator using car data | E.g. fuel consumption from `CarPropertyManager` | A good next step after A (see section 13) |

---

## 3. Configuration files

### 3.1 `settings.gradle.kts`
- `pluginManagement { repositories {...} }`: **where** plugins (AGP, Kotlin) are downloaded from.
- `dependencyResolutionManagement { repositories {...} }`: **where** libraries are downloaded from.
  `FAIL_ON_PROJECT_REPOS` = repositories may only be declared here, centrally.
- `rootProject.name = "Calculator"` and `include(":app")`: the project name and its modules.

### 3.2 Root `build.gradle.kts`: `apply false`
The root file is like a **restaurant menu**: it says which plugins *exist* and their versions,
but doesn't "order" them. The root is not an app, so it doesn't need the Android plugin.
The `:app` module **applies** them (without `apply false`) and takes the version from the root.

### 3.3 `gradle/libs.versions.toml`: the version catalog
All versions live in one place. From it, Gradle generates the `libs` object:
`androidx-material3` in the toml becomes `libs.androidx.material3` in `build.gradle.kts`
(dashes become dots).

The Compose libraries (`ui`, `material3`) **have no version**: the **BOM**
(`compose-bom`) provides it. A BOM is a "bundle" of versions tested together.

### 3.4 `app/build.gradle.kts`: the 3 questions
1. **`plugins { }`**: *What kind of module is this?* An Android app (`android.application`),
   written in Kotlin (`kotlin.android`), using Compose (`kotlin.compose`).
2. **`android { }`**: *How is it built?*
   - `namespace = "com.cristi.calculator"`: used **at compile time**. The `R` class is generated in it,
     and `.MainActivity` in the Manifest expands to `com.cristi.calculator.MainActivity`.
   - `applicationId = "com.cristi.calculator"`: the app's identity **on the device**.
     If it were the same as the dashboard's, the calculator **would replace the dashboard**.
   - `compileSdk = 34` (which API you compile against), `minSdk = 29` (oldest Android version
     accepted; AAOS starts at 28/29), `targetSdk = 34` (which version the behaviour is tested for).
   - Java 17 (`compileOptions`, `kotlinOptions`), `compose = true`.
3. **`dependencies { }`**: *Which libraries does it need?*
   - `implementation`: goes into the app
   - `debugImplementation`: only in the debug variant
   - `testImplementation`: only in unit tests (JUnit)

**What we removed compared to the dashboard, and why:**
- `useLibrary("android.car")`: the API for the car's sensors (speed, fuel). The calculator doesn't use it.
- `material-icons-extended`: thousands of icons (several MB). The calculator has text buttons.

### 3.5 `AndroidManifest.xml`: the identity card
The system reads it **before** running a single line of code.
- `<uses-feature android.hardware.type.automotive required=true>`: **this is what makes it an "AAOS" app**.
- `android:icon` / `android:roundIcon`: the launcher icon (see section 13).
- `android:label="@string/app_name"`: the name under the icon (from `strings.xml`).
- `<activity android:name=".MainActivity" android:exported="true">`: the main screen.
  `exported=true` = it can be started from outside the app (by the launcher).
- `<intent-filter>` with `MAIN` + `LAUNCHER`: "this is the entry point, put the icon in the app list".

**What we removed compared to the dashboard:**
- The `CAR_*` permissions and `<uses-library android.car>`: we don't read car data.
- `distractionOptimized`: a calculator requires typing, so it distracts the driver.
  Without this flag, AAOS **blocks** the app while the car is moving. This is the correct behaviour:
  the calculator is only for use while parked.
- `android:theme`: the theme comes from Compose (`MaterialTheme`).

---

## 4. Architecture: how the Kotlin files work together

```
┌──────────────────────────────────────────────────────────────────┐
│ MainActivity           (Android: the entry point)                │
│   MaterialTheme → Surface → CalculatorScreen                     │
└───────────────┬──────────────────────────────────────────────────┘
                │ creates / reuses
                ▼
┌──────────────────────────────┐   state (reads)     ┌────────────────────────┐
│ CalculatorViewModel          │ ──────────────────▶ │ CalculatorScreen       │
│   var state: CalculatorState │                     │   (UI, Compose)        │
│   fun onKey(key)             │ ◀────────────────── │   Display + 20 buttons │
└───────────────┬──────────────┘   onKey (press)     └────────────────────────┘
                │ delegates the calculation
                ▼
┌──────────────────────────────────────────────────┐
│ CalculatorEngine   (pure Kotlin, no Android)     │
│   press(oldState, key) → newState                │
└──────────────────────────────────────────────────┘
                ▲
                │ tested by
┌──────────────────────────────────────────────────┐
│ CalculatorEngineTest  (23 JUnit tests, on the PC)│
└──────────────────────────────────────────────────┘
```

**Why three separate files (and not everything in MainActivity)?**

| File | Responsibility | Benefit |
|---|---|---|
| `CalculatorEngine` | *What* gets calculated | Tested without an emulator, in seconds |
| `CalculatorViewModel` | *Where* the state lives | The state survives screen recreation |
| `CalculatorScreen` | *How* it looks | You can change the design without touching the logic |

This principle is called **separation of concerns**: each piece has a single job.

**Data flows in one direction** (*Unidirectional Data Flow*):
**state flows down** (ViewModel → Screen), **events flow up** (Screen → ViewModel).
The screen never changes the state directly; it only "reports" what was pressed.

---

## 5. `CalculatorEngine.kt`: the logic

### 5.1 Data types

**`enum class Operation`**: the 4 operations, each with its symbol (`+ − × ÷`).

**`sealed interface CalculatorKey`**: every possible key.
- `data class Digit(val value: Int)`: a digit, with its value
- `data class Op(val operation: Operation)`: an operator
- `data object Decimal`, `Equals`, `Clear`, `Delete`, `ToggleSign`, `Percent`: keys with no data

`sealed` means **the list is closed**. In `when (key) { ... }`, the compiler checks that you handled
**every** variant. If you add a new key and forget to handle it, the code doesn't compile.
The compiler helps you not forget anything.

**`data class CalculatorState`**: a "snapshot" of the calculator:

| Field | Example | Role |
|---|---|---|
| `display` | `"12.5"` | The big number on screen. Kept as text, to preserve exactly what was typed (e.g. `"0."`) |
| `expression` | `"12 +"` | The small line above |
| `firstOperand` | `12` | The first number, stored when an operator is pressed |
| `pendingOperation` | `Add` | The operation waiting for the second number |
| `startNewNumber` | `true` | The next digit **replaces** the display (instead of being appended) |
| `isError` | `false` | `true` after division by zero |

All fields are `val` (**immutable**). The state is never modified: a new one is created with
`state.copy(display = "5")`. The benefit is that there are no "hidden side effects", and every state is easy to test.

### 5.2 The main function: `press(state, key)`

```kotlin
fun press(state: CalculatorState, key: CalculatorKey): CalculatorState
```

It takes the old state and the pressed key, and **returns the new state**.
It is a **pure function**: the same input always gives the same output, with no side effects.
That's why it is easy to test.

### 5.3 The keys, one by one

**Digit (`inputDigit`)**
- If `startNewNumber` is `true` or the screen shows `"0"`, the digit **replaces** it (so `007` → `7`).
- Otherwise the digit is **appended** at the end, up to 12 digits.

**Point (`inputDecimal`)**
- At the start of a number → `"0."`
- If there is already a point → ignored

**Operator (`chooseOperation`)**
- Normal case: stores the number on screen in `firstOperand`, stores the operation
  and sets `startNewNumber = true`.
- **Chaining:** if an operation is already pending **and** a second number was typed,
  it calculates first (`2 + 3` → `5`), then stores the new operation.
- If you press two operators in a row, the second one replaces the first.

**Equals (`equals`)**
- With no pending operation → does nothing.
- Otherwise computes `firstOperand (op) display`, shows the result, writes the full expression
  (`12 + 3 =`) and clears the pending operation.

**Delete (`delete`)**: removes the last character, but only from the number being typed (not from results).

**± (`toggleSign`)**: adds or removes the leading `-`. Does nothing on zero (no `-0`).

**% (`percent`)**: divides by 100.

### 5.4 Why `BigDecimal` and not `Double`?

`Double` stores numbers in **binary**, and `0.1` has no exact binary representation
(just like 1/3 has no exact decimal one). Result: `0.1 + 0.2 = 0.30000000000000004`.

`BigDecimal` works in **decimal**, like a person on paper, so `0.1 + 0.2 = 0.3`.
`MathContext(12, HALF_UP)` = the result is rounded to 12 significant digits, "the school way"
(5 rounds up). This is needed for division: `1 ÷ 3` would otherwise have infinitely many digits.

### 5.5 `format(value)`: number → text
- `round(PRECISION)`: at most 12 significant digits
- `stripTrailingZeros()`: `3.500` → `3.5`
- `toPlainString()`: avoids scientific notation for normal numbers (`1E+2` → `100`)
- Huge numbers (more than 15 digits) stay in scientific notation (`1.5E+20`), so they fit on screen.

---

## 6. `CalculatorViewModel.kt`: the state

```kotlin
class CalculatorViewModel : ViewModel() {
    var state by mutableStateOf(CalculatorState())
        private set

    fun onKey(key: CalculatorKey) {
        state = CalculatorEngine.press(state, key)
    }
}
```

- **`ViewModel`**: an object that **survives** when the Activity is destroyed and recreated.
  This happens on screen rotation, language change, or day/night switching in the car.
  Without a ViewModel, the calculation in progress would be lost.
- **`mutableStateOf(...)`**: a value **observed** by Compose. When it changes,
  every `@Composable` function that reads it **redraws automatically** (*recomposition*).
- **`by`**: Kotlin delegation. You write `state` instead of `state.value`.
- **`private set`**: only the ViewModel can write the state; the screen can only read it.
- **`onKey`**: the only "door" through which the state changes. It passes everything to the engine.

---

## 7. `CalculatorScreen.kt`: the UI

### 7.1 The layout

```
Column (full screen, 24dp padding, 12dp spacing between items)
├── Display            weight 1.4  ← expression + number, aligned bottom-right
├── Row (row 1)        weight 1    →  7  8  9  ÷  C
├── Row (row 2)        weight 1    →  4  5  6  ×  ⌫
├── Row (row 3)        weight 1    →  1  2  3  −  %
└── Row (row 4)        weight 1    →  ±  0  .  +  =
         each button: weight 1 (equal widths) + fillMaxHeight
```

- **`Column`** = items **below** each other. **`Row`** = items **next to** each other.
- **`Modifier.weight(x)`** = "take x parts of the remaining space". The display gets 1.4 parts and each row gets 1,
  so the display is slightly taller than a row of buttons. Everything is **proportional**,
  so it adapts to any screen size.
- **`Arrangement.spacedBy(12.dp)`**: equal spacing between items.
- **`dp`** (*density-independent pixels*): a unit that looks the same on screens with different densities.
  **`sp`**: like `dp`, but for text (it also respects the user's font size setting).

### 7.2 Design for the car
- **5 × 4 grid (landscape)**, because car screens are wide.
- **Large buttons** (~90dp), above the **76dp** minimum Google recommends for AAOS,
  so they are easy to hit on a dashboard screen.
- **Dark theme**, so it doesn't dazzle the driver at night.
- **Colors by button type**, so you recognize the function without reading:

| Type | Buttons | Theme color |
|---|---|---|
| Number | 0–9, `.` | `surfaceVariant` (grey) |
| Operation | `+ − × ÷` | `secondaryContainer` |
| Function | `C ⌫ % ±` | `tertiaryContainer` |
| Equals | `=` | `primary` (the most visible) |

The colors come from **`MaterialTheme.colorScheme`**, not hard-coded. If you change the theme,
for example to `lightColorScheme()`, all buttons adapt automatically.

### 7.3 The keypad as **data**, not repeated code
```kotlin
private val keypad: List<List<KeySpec>> = listOf(
    listOf(digit(7), digit(8), digit(9), operation(Operation.Divide), KeySpec("C", ...)),
    ...
)
```
Each button is described by a `KeySpec(label, key, style)`. The screen loops over the list with `forEach`
and draws one button per entry. To move a button, you change **only the list**. There's no need to write 20 `Button(...)` calls.

### 7.4 A "stateless" component
`CalculatorScreen(state, onKey)` **holds no state**. It receives everything from outside.
The benefit: it is predictable (same state gives the same screen) and easy to reuse and test.
This pattern is called **state hoisting** (the state is "lifted" to the parent).

---

## 8. `MainActivity.kt`: the entry point

```kotlin
setContent {
    MaterialTheme(colorScheme = darkColorScheme()) {
        Surface(modifier = Modifier.fillMaxSize()) {
            val viewModel: CalculatorViewModel = viewModel()
            CalculatorScreen(state = viewModel.state, onKey = viewModel::onKey)
        }
    }
}
```

- **`super.onCreate(savedInstanceState)` first**: the parent class (`ComponentActivity`) must do its
  own setup (lifecycle, restoring saved state, wiring up ViewModels) **before** we use anything from it.
  If you forget it, Android throws an exception.
- **`setContent { }`**: from here on, the UI is drawn with Compose (not XML).
- **`MaterialTheme`**: the color palette for everything inside.
- **`Surface`**: paints the background (`surface`) and gives its children the text color (`onSurface`).
  **Without it we had "black text on black"** (see section 12).
- **`viewModel()`**: creates the ViewModel the first time and **reuses** it when the Activity is recreated.
- **`viewModel::onKey`**: a function reference. We give the screen "what to call" on a press.

---

## 9. Unit tests

File: `app/src/test/java/com/cristi/calculator/CalculatorEngineTest.kt`

- The `src/test/` folder = tests that run **on the PC** (JVM), **without an emulator**.
  (There is also `src/androidTest/`, for tests that run on the device. We don't use it yet.)
- The helper function `type("2+3=")` turns text into key presses, so the tests are easy to read.
- **23 tests**, all passing. They cover: the operations, decimals, division by zero, chaining,
  leading zeros, delete, percent, ±, reusing the result, changing the operator.

Example:
```kotlin
@Test fun `no floating point errors`() = assertEquals("0.3", type("0.1+0.2=").display)
```

**Run:**
```
.\gradlew.bat testDebugUnitTest
```
HTML report: `app/build/reports/tests/testDebugUnitTest/index.html`

The tests exist thanks to separating the logic (section 4). If the logic were in `MainActivity`,
every check would need the emulator and manual button presses.

---

## 10. What happens when you press a button (step by step)

Example: the screen shows `12 +` / `3` and you press **`=`**.

1. **`Button(onClick = ...)`** in `CalculatorScreen` detects the touch.
2. It calls `onKey(CalculatorKey.Equals)`. This is `viewModel::onKey`, passed down from `MainActivity`.
3. **`CalculatorViewModel.onKey`** calls `CalculatorEngine.press(state, Equals)`.
4. **`CalculatorEngine.equals`** computes `12 + 3 = 15` with `BigDecimal` and returns a new state:
   `display = "15"`, `expression = "12 + 3 ="`.
5. The ViewModel does `state = newState`. Because `state` is a `mutableStateOf`, Compose notices the change.
6. Compose **re-runs** (*recomposes*) only the functions that read `state`, i.e. `Display`.
7. The screen shows `12 + 3 =` / `15`.

All of these steps take a few milliseconds.

---

## 11. Build and tool chain

All the commands are in `COMMANDS.md`.

**The development loop:** edit → save → `.\gradlew.bat installDebug` → start the app → check on the emulator.

**The tool chain:**
```
gradlew.bat  → downloads (first time only) and starts Gradle 8.9
Gradle       → reads build.gradle.kts, decides which tasks to run
kotlinc      → compileDebugKotlin: .kt → bytecode
D8 / AGP     → dexBuilder, packageDebug: bytecode → DEX → APK
adb          → installDebug: APK → emulator
```

**Incremental build:** the first build took almost 6 minutes (downloading dependencies), then
55 seconds, then 6 seconds. Tasks marked `UP-TO-DATE` are skipped, because their inputs didn't change.

**Why is the debug APK ~24 MB?** It contains all of Compose and Material 3, unoptimized.
In a **release** build with `isMinifyEnabled = true`, R8 removes unused code and the APK drops to ~1–2 MB.

---

## 12. Problems we hit and lessons learned

| Problem | Cause | Lesson |
|---|---|---|
| `gradle.proprieties`, `libs.version.toml` | Typos in file names | Gradle silently ignores misnamed files |
| `java` created as a file, `gradlew` as a folder | File vs folder mix-up | In `Get-ChildItem`: `d----` = folder, `-a---` = file |
| `gradlew.bat` starting with `#!/bin/sh` | The Linux script, renamed | They are two different files (Linux and Windows) |
| 0-byte files after "copy-paste" | Not saved in the editor | Enable auto-save in Zed: `"autosave": "on_focus_change"` |
| `cd: D:AndroidProjectsCalculator` | `\` is a special character in Bash | In Bash: `/d/Android/...`; in PowerShell: `D:\Android\...` |
| `Unresolved reference` (in theory) | Imports removed, but the code using them remained | When adapting code: delete the code first, then the unused imports |
| `color.White` | Mixing up the `color` parameter and the `Color` class | Kotlin is case-sensitive |
| **Black screen** | The AAOS theme has a black background, and `Text` with no theme is black | Use `MaterialTheme` + `Surface` |
| `No connected devices!` / `no devices/emulators found` | The emulator was closed | The emulator must keep running. Minimize it, don't close it |
| The emulator opens straight into the app | The snapshot saves the state on close | Normal; the 🏠 button takes you Home |
| "Can't use this app while driving" | The simulated car had speed ≠ 0 (left over from the dashboard tests), so AAOS considered it "moving". Without `distractionOptimized`, the app is blocked | Set speed 0 and gear P: from the emulator, `⋯` → *Car data*, or with the commands in `COMMANDS.md` section 2 |

**The debugging method we used for the black screen:**
1. **Hypothesis:** black text on a black background.
2. **Minimal experiment:** change **one single thing** (`color = Color.White`).
3. **Confirmation:** the text appeared, so the hypothesis is right.
4. **The real fix:** the theme (`MaterialTheme` + `Surface`), not a hard-coded color everywhere.

---

## 13. What's next (extension ideas)

1. **Day/night theme** that follows the car's setting: `isSystemInDarkTheme()` + `lightColorScheme()`.
2. ~~Custom icon~~ ✅ **Done:** an adaptive icon. A blue background (`res/values/colors.xml`) +
   a vector drawing of a white calculator with a green display and an orange "=" key
   (`res/drawable/ic_launcher_foreground.xml`), combined in `res/mipmap-anydpi-v26/ic_launcher.xml`
   and declared in the Manifest with `android:icon` / `android:roundIcon`.
   The drawing stays inside the "safe zone" (the central 66 dp circle out of 108 dp), because the system crops
   the icon into the shape chosen by the manufacturer (circle, rounded square, etc.).
3. **Calculation history** (a list of the latest results).
4. **Repeated `=`** (`2 + 3 = = =` → 5, 8, 11). Right now, a repeated `=` does nothing.
5. **UI tests** with `compose-ui-test`, which press buttons on the emulator.
6. **Option C** from the start: a calculator that uses **car data**,
   e.g. average consumption = litres / km from `CarPropertyManager`
   (that would bring back `useLibrary("android.car")` and the permissions).

---

## 14. Glossary

| Term | Meaning |
|---|---|
| **AAOS** | Android Automotive OS: Android running directly on the car's system |
| **AGP** | Android Gradle Plugin: teaches Gradle how to build Android apps |
| **APK** | The app's installable file |
| **adb** | Android Debug Bridge: the tool the PC uses to talk to the device |
| **AVD** | Android Virtual Device: an emulator configuration (`AAOS_Capstone`) |
| **BOM** | Bill of Materials: a set of compatible versions for related libraries |
| **Composable** | A function marked `@Composable` that describes a piece of UI |
| **Recomposition** | Compose automatically redraws what changed |
| **DEX** | The bytecode format Android runs |
| **Gradle Wrapper** | The `gradlew` script that fetches the correct Gradle version |
| **Intent** | A "message" to the system; `am start -n ...` sends one that starts the Activity |
| **Manifest** | The app's identity file, read by the system before any code runs |
| **Module** | A part of the Gradle project with its own `build.gradle.kts` (here only `:app`) |
| **namespace / applicationId** | The package for **compilation** / the identity on the **device** |
| **sealed** | A closed hierarchy: the compiler knows every variant |
| **Snapshot** | The saved state of the emulator, for a fast start |
| **State hoisting** | The UI component doesn't hold state; it receives it from its parent |
| **UDF** | Unidirectional Data Flow: state flows down, events flow up |
| **ViewModel** | An object that keeps state across screen recreation |
