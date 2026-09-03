Back to main [README](../README.md)

# Running tests with Appium-Java

For `compareAndroidWithFigma`/`compareIosWithFigma`'s actual behavior (the
`ScenarioFlow`/registry pattern, why `Scenario Name` is required for every mobile
row), see figmacompare's
[CompareWithFigma.md](https://github.com/anandbagmar/figmacompare/blob/main/docs/CompareWithFigma.md).
This page covers this repo's own example apps/tests and local run setup.

## Table of contents

- [Setup Appium server](#setup-appium-server)
- [Start the Emulator](#start-the-emulator)
- [Usage](#usage)
  - [Set your APPLITOOLS_API_KEY](#set-your-applitools_api_key)
  - [Install the example application](#install-the-example-application)
  - [Launch the tests](#launch-the-tests)
- [Example Test Source Files](#example-test-source-files)
  - [Android](#android)
  - [iOS](#ios)

## [Setup Appium server](README_MachineSetupInstructions.md)

## [Start the Emulator](README_MachineSetupInstructions.md)

## Usage

You will need to configure the Applitools Eyes Appium SDK, and launch an Android device
emulator (or iOS Simulator) on your local system before you run the example tests in
this project. Each test class starts its own local Appium server in `@BeforeSuite` and
stops it in `@AfterSuite`, so you do not need to start Appium yourself.

If you're testing your application using a mobile device grid, you should already know how
to connect an Appium test to your test device. The instructions in this document describe
how to connect the tests to an emulator/simulator running on your computer.

### Set your APPLITOOLS_API_KEY

First,
[retrieve your API key](https://applitools.com/docs/topics/overview/obtain-api-key.html)
for your user account from the Eyes Dashboard UI. Set it as the `APPLITOOLS_API_KEY`
environment variable — every test class in this project reads it from there
(`System.getenv("APPLITOOLS_API_KEY")`), so there's nothing to hard-code in the test source.

**macOS / Linux** (bash/zsh):
```bash
export APPLITOOLS_API_KEY=<your-api-key>
```

**Windows (Command Prompt):**
```
set APPLITOOLS_API_KEY=<your-api-key>
```

**Windows (PowerShell):**
```powershell
$env:APPLITOOLS_API_KEY="<your-api-key>"
```

***Note:*** Your Applitools API key is a secret value. Treat it like a password and do
not share it or commit it into source control. `set`/`$env:` only apply to the current
terminal session — set it again (or add it to your system's persistent environment
variables) in a new session.

### Install the example application

The `sampleApps/` directory contains the APK/app files the Android and iOS tests install
and launch automatically — you don't need to install or launch them yourself:

* `sampleApps/Calculator_8.4.1.apk` — used by `CalculatorTest`/`CalculatorFigmaTest` (`io.eot.calculator`)
* `sampleApps/app_npu_v8.3.17.apk` — the Bajaj Finserv Android app, used by `BajajFinservAndroidTest` (`io.eot.bajajfinserv`)
* `sampleApps/App Automation Playground-debug.apk` / `.app.zip` (unzip to `MockedE2EDemo.app`) — the "App Automation Playground" demo app, used by the `AppAutomationPlayground*Test` classes (`io.eot.mockede2e`)
* `sampleApps/HelloWorldiOS.app` — used by `HelloWorldTest` (`io.eot.helloworld`)

### Launch the tests

Run tests from a terminal using the Gradle `test` task and the `testClass` project property
(see [build.gradle](../build.gradle)):

**macOS / Linux:**
```bash
./gradlew test -PtestClass=<fully.qualified.TestClassName>
```

**Windows (Command Prompt or PowerShell):**
```
gradlew.bat test -PtestClass=<fully.qualified.TestClassName>
```

> Every other `./gradlew ...` command shown below and elsewhere in this repo's docs
> works the same way on Windows — just swap `./gradlew` for `gradlew.bat`.

Or run a specific test class directly from your IDE (IntelliJ/Eclipse) by right-clicking the
class and choosing **Run**.

## Example Test Source Files

### Android

* [CalculatorTest.java](../src/test/java/io/eot/calculator/appium/android/CalculatorTest.java) — a
  basic native Android Appium test against the Calculator sample app, with Applitools Eyes
  ```bash
  ./gradlew test -PtestClass=io.eot.calculator.appium.android.CalculatorTest
  ```

* [CalculatorFigmaTest.java](../src/test/java/io/eot/calculator/appium/android/CalculatorFigmaTest.java) —
  same Calculator app test, but the visual baseline is uploaded from a locally-stored Figma
  export via `Baseline.java` (in figmacompare) before comparing
  ```bash
  ./gradlew test -PtestClass=io.eot.calculator.appium.android.CalculatorFigmaTest
  ```

* `AndroidCompareRunner` (in figmacompare) +
  [CompareAndroidWithFigmaTest.java](../src/test/java/io/eot/pipeline/appium/android/CompareAndroidWithFigmaTest.java)
  (thin TestNG shim, in this repo) — the mobile (Android) path of `compareWithFigma` —
  see figmacompare's [CompareWithFigma.md](https://github.com/anandbagmar/figmacompare/blob/main/docs/CompareWithFigma.md)
  for how it behaves.
  ```bash
  ./gradlew compareAndroidWithFigma
  ```
  (a shortcut for
  `./gradlew test -PtestClass=io.eot.pipeline.appium.android.CompareAndroidWithFigmaTest`,
  which still works too if you want it.)

* [BajajFinservAndroidTest.java](../src/test/java/io/eot/bajajfinserv/appium/android/BajajFinservAndroidTest.java) —
  **not a TestNG test itself** — a *scenario provider* for the Bajaj Finserv app
  (`app_npu_v8.3.17.apk`), registering this app's scenarios into
  `AndroidScenarioRegistry` (see figmacompare's
  [CompareWithFigma.md](https://github.com/anandbagmar/figmacompare/blob/main/docs/CompareWithFigma.md)
  for how the registry/`ScenarioFlow` pattern works, and
  [README_AddingTests.md](README_AddingTests.md) for how to add a new one).

  Two more scenario providers, for the "App Automation Playground" demo app
  (`App Automation Playground-debug.apk`), show both a standalone and a multi-step
  scenario side by side:
  [AppAutomationPlaygroundAndroidHomeTest.java](../src/test/java/io/eot/mockede2e/appium/android/AppAutomationPlaygroundAndroidHomeTest.java)
  (single screen) and
  [AppAutomationPlaygroundAndroidPlannerScenarioTest.java](../src/test/java/io/eot/mockede2e/appium/android/AppAutomationPlaygroundAndroidPlannerScenarioTest.java)
  (4-screen Community Meeting Planner flow).

### iOS

* [HelloWorldTest.java](../src/test/java/io/eot/helloworld/appium/ios/HelloWorldTest.java) — a basic
  native iOS Appium test (`HelloWorldiOS.app`) with Applitools Eyes
  ```bash
  ./gradlew test -PtestClass=io.eot.helloworld.appium.ios.HelloWorldTest
  ```

* [WebiOSHelloWorldTest.java](../src/test/java/io/eot/helloworld/appium/ios/WebiOSHelloWorldTest.java) —
  an Appium test that drives Safari on an iOS Simulator (mobile web), with Applitools Eyes
  ```bash
  ./gradlew test -PtestClass=io.eot.helloworld.appium.ios.WebiOSHelloWorldTest
  ```

* `IosCompareRunner` (in figmacompare) +
  [CompareIosWithFigmaTest.java](../src/test/java/io/eot/pipeline/appium/ios/CompareIosWithFigmaTest.java)
  (thin TestNG shim, in this repo) — the mobile (iOS) path of `compareWithFigma`,
  mirroring `AndroidCompareRunner`/`CompareAndroidWithFigmaTest` exactly.
  ```bash
  ./gradlew compareIosWithFigma
  ```
  Scenario providers register a `.app` bundle path (not a `.zip` — unzip it once
  under `sampleApps/`) instead of an APK:
  [AppAutomationPlaygroundIosHomeTest.java](../src/test/java/io/eot/mockede2e/appium/ios/AppAutomationPlaygroundIosHomeTest.java)
  (single screen) and
  [AppAutomationPlaygroundIosPlannerScenarioTest.java](../src/test/java/io/eot/mockede2e/appium/ios/AppAutomationPlaygroundIosPlannerScenarioTest.java)
  (4-screen flow) — the iOS counterparts of the Android providers above, using the
  same accessibility identifiers (this app is built as a deliberate cross-platform
  automation demo sharing testIDs between platforms).

Back to main [README](../README.md)
