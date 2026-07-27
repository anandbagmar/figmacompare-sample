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

* [BajajFinservAndroidTest.java](../src/test/java/io/samples/appium/android/BajajFinservAndroidTest.java) —
  the **mobile (Android) path of `compareWithFigma`** for the Bajaj Finserv app
  (`app_npu_v8.3.17.apk`); see
  [README_FigmaVisualValidation.md](../README_FigmaVisualValidation.md). Data-driven from
  the same shared Figma Excel file as `BajajFinservWebTest` (default
  `figma-visual-testing/figma_visual_tests.xlsx`, override with `-PfigmaExcel=<path>`),
  one invocation per group of non-`Skip` `Platform=Android` rows — a group is either one
  standalone row, or several consecutive rows sharing the same `Scenario Name`. Unlike
  web, there's no generic way to navigate a native app to an arbitrary screen, so this
  class owns a small `SCREEN_FLOWS` registry mapping each distinct `App URL / Screen Name`
  value to a short Appium method that leaves the app on that screen — add an entry here
  for every new screen you want to validate. For a scenario, each row/step's flow runs in
  turn within one continuous app session (no relaunch between steps), so **every flow must
  be self-contained** — able to reach its target screen regardless of what ran before it -
  since the same entry may run standalone (fresh launch) or as any step of any scenario.
  It then runs an Applitools Eyes full-page comparison against the group's
  `Baseline Env Name` baseline, and writes `Comparison Batch URL` + `Validation Status`
  back into the same file in place, same as the web path.
  ```bash
  ./gradlew compareAndroidWithFigma
  ```
  (a shortcut for `./gradlew test -PtestClass=io.samples.appium.android.BajajFinservAndroidTest`,
  which still works too if you want it.) A new app's test class should get its own
  `compare<App>WithFigma` task in `build.gradle`, following the same pattern.

  This is the template for any other app's native Appium comparison test: start the
  Appium server, create the driver, and record/write back results using the shared
  [AppiumServerSupport](../src/test/java/io/samples/appium/AppiumServerSupport.java),
  [AndroidDriverFactory](../src/test/java/io/samples/appium/android/AndroidDriverFactory.java),
  [BatchSupport](../src/test/java/io/samples/eyes/BatchSupport.java),
  [ComparisonResultRecorder](../src/test/java/io/samples/eyes/ComparisonResultRecorder.java),
  [FigmaExcelFile](../src/test/java/io/samples/excel/FigmaExcelFile.java), and
  [FigmaValidation](../src/test/java/io/samples/excel/FigmaValidation.java) utilities —
  only the screen flows and Eyes configuration specifics need to be app-specific.

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
