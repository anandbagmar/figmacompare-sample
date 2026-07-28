Back to main [README](../README.md)

# Running tests with Appium-Java

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

* `sampleApps/Calculator_8.4.1.apk` — used by `CalculatorTest`/`CalculatorFigmaTest`
* `sampleApps/app_npu_v8.3.17.apk` — the Bajaj Finserv Android app, used by `BajajFinservAndroidTest`
* `sampleApps/HelloWorldiOS.app` — used by `HelloWorldTest`

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

* [CalculatorTest.java](../src/test/java/io/samples/appium/android/CalculatorTest.java) — a
  basic native Android Appium test against the Calculator sample app, with Applitools Eyes
  ```bash
  ./gradlew test -PtestClass=io.samples.appium.android.CalculatorTest
  ```

* [CalculatorFigmaTest.java](../src/test/java/io/samples/appium/android/CalculatorFigmaTest.java) —
  same Calculator app test, but the visual baseline is uploaded from a locally-stored Figma
  export via [Baseline.java](../src/test/java/io/samples/Baseline.java) before comparing
  ```bash
  ./gradlew test -PtestClass=io.samples.appium.android.CalculatorFigmaTest
  ```

* [CompareAndroidWithFigma.java](../src/test/java/io/samples/appium/android/CompareAndroidWithFigma.java) —
  the **one runner** for the mobile (Android) path of `compareWithFigma`; see
  [README_FigmaVisualValidation.md](../README_FigmaVisualValidation.md). Data-driven from
  the same shared Figma Excel file as `BajajFinservWebTest` (default
  `figma-visual-testing/figma_visual_tests.xlsx`, override with `-PfigmaExcel=<path>`),
  one invocation per group of non-`Skip` `Platform=Android` rows sharing a `Scenario Name`
  (mandatory for every Android/iOS row — see below). For each group it looks up that
  `Scenario Name` in [AndroidScenarioRegistry](../src/test/java/io/samples/appium/android/AndroidScenarioRegistry.java),
  launches the registered app (APK), and runs the registered `ScenarioFlow` in one
  continuous app session (no relaunch between steps), then does an Applitools Eyes
  comparison against the group's `Baseline Env Name` baseline and writes
  `Comparison Batch URL` + `Validation Status` back into the file in place, same as the
  web path.
  ```bash
  ./gradlew compareAndroidWithFigma
  ```
  (a shortcut for `./gradlew test -PtestClass=io.samples.appium.android.CompareAndroidWithFigma`,
  which still works too if you want it.)

* [BajajFinservAndroidTest.java](../src/test/java/io/samples/appium/android/BajajFinservAndroidTest.java) —
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
  `CompareAndroidWithFigma` doesn't know or care where a scenario came from.

  A new app means a new provider class following this same pattern, plus one line added to
  `AndroidScenarioRegistry.ensureAllProvidersRegistered()` so its registrations actually run
  (Java only executes a class's static initializer once that class is loaded/referenced).
  `CompareAndroidWithFigma` itself never needs to change.

  Both classes reuse the shared
  [AppiumServerSupport](../src/test/java/io/samples/appium/AppiumServerSupport.java),
  [AndroidDriverFactory](../src/test/java/io/samples/appium/android/AndroidDriverFactory.java),
  [BatchSupport](../src/test/java/io/samples/eyes/BatchSupport.java),
  [ComparisonResultRecorder](../src/test/java/io/samples/eyes/ComparisonResultRecorder.java),
  [FigmaExcelFile](../src/test/java/io/samples/excel/FigmaExcelFile.java), and
  [FigmaValidation](../src/test/java/io/samples/excel/FigmaValidation.java) utilities.

### iOS

* [HelloWorldTest.java](../src/test/java/io/samples/appium/ios/HelloWorldTest.java) — a basic
  native iOS Appium test (`HelloWorldiOS.app`) with Applitools Eyes
  ```bash
  ./gradlew test -PtestClass=io.samples.appium.ios.HelloWorldTest
  ```

* [WebiOSHelloWorldTest.java](../src/test/java/io/samples/appium/ios/WebiOSHelloWorldTest.java) —
  an Appium test that drives Safari on an iOS Simulator (mobile web), with Applitools Eyes
  ```bash
  ./gradlew test -PtestClass=io.samples.appium.ios.WebiOSHelloWorldTest
  ```

Back to main [README](../README.md)
