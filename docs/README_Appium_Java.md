Back to main [README](../README.md)

# Running tests with Appium-Java

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

```bash
export APPLITOOLS_API_KEY=<your-api-key>
```

***Note:*** Your Applitools API key is a secret value. Treat it like a password and do
not share it or commit it into source control.

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

```bash
./gradlew test -PtestClass=<fully.qualified.TestClassName>
```

Or run a specific test class directly from your IDE (IntelliJ/Eclipse) by right-clicking the
class and choosing **Run**.

## Example Test Source Files

### Android

* [CalculatorTest.java](../samples/src/test/java/io/eot/calculator/appium/android/CalculatorTest.java) — a
  basic native Android Appium test against the Calculator sample app, with Applitools Eyes
  ```bash
  ./gradlew test -PtestClass=io.eot.calculator.appium.android.CalculatorTest
  ```

* [CalculatorFigmaTest.java](../samples/src/test/java/io/eot/calculator/appium/android/CalculatorFigmaTest.java) —
  same Calculator app test, but the visual baseline is uploaded from a locally-stored Figma
  export via `Baseline.java` (in figmacompare) before comparing
  ```bash
  ./gradlew test -PtestClass=io.eot.calculator.appium.android.CalculatorFigmaTest
  ```

* `AndroidCompareRunner` (plain-Java orchestration, in figmacompare) +
  [CompareAndroidWithFigmaTest.java](../samples/src/test/java/io/eot/pipeline/appium/android/CompareAndroidWithFigmaTest.java)
  (thin TestNG shim, in `samples`) — together the **one runner** for the mobile
  (Android) path of `compareWithFigma`; see
  [README_FigmaVisualValidation.md](../README_FigmaVisualValidation.md). Data-driven from
  the same shared Figma Excel file as the web path (default
  `figma-visual-testing/figma_visual_tests.xlsx`, override with `-PfigmaExcel=<path>`),
  one invocation per group of non-`Skip` `Platform=Android` rows sharing a `Scenario Name`
  (mandatory for every Android/iOS row — see below). For each group it looks up that
  `Scenario Name` in `AndroidScenarioRegistry` (in figmacompare),
  launches the registered app (APK), and runs the registered `ScenarioFlow` in one
  continuous app session (no relaunch between steps), then does an Applitools Eyes
  comparison against the group's `Baseline Env Name` baseline and writes
  `Comparison Batch URL` + `Validation Status` back into the file in place, same as the
  web path.
  ```bash
  ./gradlew compareAndroidWithFigma
  ```
  (a shortcut for
  `./gradlew test -PtestClass=io.eot.pipeline.appium.android.CompareAndroidWithFigmaTest`,
  which still works too if you want it.)

* [BajajFinservAndroidTest.java](../samples/src/test/java/io/eot/bajajfinserv/appium/android/BajajFinservAndroidTest.java) —
  **not a TestNG test itself** — a *scenario provider* for the Bajaj Finserv app
  (`app_npu_v8.3.17.apk`). Its static initializer registers this app's scenarios into
  `AndroidScenarioRegistry`:
  ```java
  AndroidScenarioRegistry.register("android-home-screen", APK_NAME, APP_NAME, (driver, eyes, rows) -> {
      // whatever login/navigation this app's real screen needs, then:
      eyes.checkWindow(resolveStepName(rows.get(0)));
  });
  ```
  Unlike web, there's no generic way to navigate a native app to an arbitrary screen —
  reaching even one screen can need login/menu navigation specific to that app. So every
  `ScenarioFlow` must be **fully self-contained**: it owns the whole sequence for its
  scenario's rows (however many `eyes.checkWindow()` calls it makes, in whatever order),
  and it's looked up purely by `Scenario Name`, regardless of which class registered it —
  `AndroidCompareRunner` doesn't know or care where a scenario came from.

  A new app means a new provider class following this same pattern, plus one line added to
  the `PROVIDER_CLASSES` list in `CompareAndroidWithFigmaTest` so its registrations
  actually run (Java only executes a class's static initializer once that class is
  loaded/referenced). `AndroidCompareRunner` itself never needs to change.

  Both classes reuse the shared
  `AppiumServerSupport` (in figmacompare),
  `AndroidDriverFactory` (in figmacompare),
  `BatchSupport` (in figmacompare),
  `ComparisonResultRecorder` (in figmacompare),
  `FigmaExcelFile` (in figmacompare), and
  `FigmaValidation` (in figmacompare) utilities.

  Two more scenario providers, for the "App Automation Playground" demo app
  (`App Automation Playground-debug.apk`), show both a standalone and a multi-step
  scenario side by side:
  [AppAutomationPlaygroundAndroidHomeTest.java](../samples/src/test/java/io/eot/mockede2e/appium/android/AppAutomationPlaygroundAndroidHomeTest.java)
  (single screen) and
  [AppAutomationPlaygroundAndroidPlannerScenarioTest.java](../samples/src/test/java/io/eot/mockede2e/appium/android/AppAutomationPlaygroundAndroidPlannerScenarioTest.java)
  (4-screen Community Meeting Planner flow).

### iOS

* [HelloWorldTest.java](../samples/src/test/java/io/eot/helloworld/appium/ios/HelloWorldTest.java) — a basic
  native iOS Appium test (`HelloWorldiOS.app`) with Applitools Eyes
  ```bash
  ./gradlew test -PtestClass=io.eot.helloworld.appium.ios.HelloWorldTest
  ```

* [WebiOSHelloWorldTest.java](../samples/src/test/java/io/eot/helloworld/appium/ios/WebiOSHelloWorldTest.java) —
  an Appium test that drives Safari on an iOS Simulator (mobile web), with Applitools Eyes
  ```bash
  ./gradlew test -PtestClass=io.eot.helloworld.appium.ios.WebiOSHelloWorldTest
  ```

* `IosCompareRunner` (plain-Java orchestration, in figmacompare) +
  [CompareIosWithFigmaTest.java](../samples/src/test/java/io/eot/pipeline/appium/ios/CompareIosWithFigmaTest.java)
  (thin TestNG shim, in `samples`) — together the **one runner** for the mobile (iOS)
  path of `compareWithFigma`, mirroring `AndroidCompareRunner`/`CompareAndroidWithFigmaTest`
  exactly (see the Android section above and
  [README_FigmaVisualValidation.md](../README_FigmaVisualValidation.md)): one
  invocation per group of `Platform=iOS` rows sharing a `Scenario Name`, dispatched
  through `IosScenarioRegistry` (in figmacompare)
  regardless of which provider class registered the scenario.
  ```bash
  ./gradlew compareIosWithFigma
  ```
  Scenario providers register a `.app` bundle path (not a `.zip` — unzip it once
  under `sampleApps/`) instead of an APK:
  [AppAutomationPlaygroundIosHomeTest.java](../samples/src/test/java/io/eot/mockede2e/appium/ios/AppAutomationPlaygroundIosHomeTest.java)
  (single screen) and
  [AppAutomationPlaygroundIosPlannerScenarioTest.java](../samples/src/test/java/io/eot/mockede2e/appium/ios/AppAutomationPlaygroundIosPlannerScenarioTest.java)
  (4-screen flow) — the iOS counterparts of the Android providers above, using the
  same accessibility identifiers (this app is built as a deliberate cross-platform
  automation demo sharing testIDs between platforms).

Back to main [README](../README.md)
