Back to [README_FigmaVisualValidation.md](../README_FigmaVisualValidation.md)

# How to add a new test / scenario

## Table of contents

- [A. A new standalone web page/component — no code](#a-a-new-standalone-web-pagecomponent--no-code)
- [B. A new web scenario (multiple pages as one test) — no code](#b-a-new-web-scenario-multiple-pages-as-one-test--no-code)
- [C. A new scenario for an existing Android/iOS app — small amount of code](#c-a-new-scenario-for-an-existing-androidios-app--small-amount-of-code)
- [D. A brand-new Android app — one new class + one registry line](#d-a-brand-new-android-app--one-new-class--one-registry-line)
- [E. iOS — same as C/D, using the iOS equivalents](#e-ios--same-as-cd-using-the-ios-equivalents)
- [F. Plugging in your own existing Appium tests](#f-plugging-in-your-own-existing-appium-tests)

See [README_Scenarios.md](README_Scenarios.md) for how `Scenario Name` and the
web/mobile registries work before adding to them here.

## A. A new standalone web page/component — no code

1. Add one row: `Figma URL`, `Platform=Web`, `App URL / Screen Name`. Leave
   `Scenario Name` blank.
2. `./gradlew uploadFromFigma`.
3. In Step 3, optionally set `Locator` if it's a component, not a full page.
4. `./gradlew compareWebWithFigma`.

## B. A new web scenario (multiple pages as one test) — no code

1. Add N **consecutive** rows, each with its own `Figma URL`/`App URL / Screen
   Name`/`Test Name` (the step name), all sharing the same `Scenario Name`.
2. `./gradlew uploadFromFigma` — uploads all N steps as one Applitools test.
3. Optionally set `Locator` per row in Step 3.
4. `./gradlew compareWebWithFigma`.

## C. A new scenario for an **existing** Android/iOS app — small amount of code

Example: adding a new screen/flow to the Bajaj Finserv app.

1. Open that app's provider class,
   [BajajFinservAndroidTest.java](../samples/src/test/java/io/eot/bajajfinserv/appium/android/BajajFinservAndroidTest.java).
2. Add a new registration in its static block:
   ```java
   AndroidScenarioRegistry.register("your-new-scenario-name", APK_NAME, APP_NAME, (driver, eyes, rows) -> {
       // whatever login/navigation this scenario's screen(s) need, e.g.:
       // driver.findElement(...).click();
       eyes.checkWindow(resolveStepName(rows.get(0)));
       // for a multi-screen scenario, navigate further and call eyes.checkWindow(...) again per step
   });
   ```
3. Add matching row(s) to the Excel: `Platform=Android`, `Scenario Name` set to the
   **exact same string** you just registered, one row per step (contiguous if more
   than one).
4. `./gradlew uploadFromFigma`, then `./gradlew compareAndroidWithFigma`.

## D. A brand-new Android app — one new class + one registry line

1. Create a new provider class under its own per-app package, e.g.
   `samples/src/test/java/io/eot/<yourapp>/appium/android/` (see
   `io.eot.bajajfinserv`, `io.eot.calculator`, `io.eot.mockede2e` for existing
   examples - each app/client gets its own top-level package under `io.eot`),
   copying `BajajFinservAndroidTest.java`'s shape (private constructor, `APP_NAME`/
   `APK_NAME` constants, a static block calling `AndroidScenarioRegistry.register(...)`
   for each of that app's scenarios).
2. Add its `.apk` under `sampleApps/`.
3. Add **one line** to the `PROVIDER_CLASSES` list in
   [CompareAndroidWithFigmaTest.java](../samples/src/test/java/io/eot/pipeline/appium/android/CompareAndroidWithFigmaTest.java)
   loading your new class — without this, its static block (and therefore its
   registrations) never runs.
4. Add Excel rows (`Platform=Android`, `Scenario Name` matching what you registered).
5. `./gradlew uploadFromFigma`, then `./gradlew compareAndroidWithFigma`.

`AndroidCompareRunner.java` itself never needs to change for C or D — it only
ever looks things up by `Scenario Name` in the shared registry.

## E. iOS — same as C/D, using the iOS equivalents

iOS mirrors Android exactly:
`IosScenarioRegistry` (in figmacompare),
`IosCompareRunner` (in figmacompare), and
`IosDriverFactory` (in figmacompare) —
follow steps C/D above but:
- create your provider class under its own per-app package (`samples/src/test/java/io/eot/<yourapp>/appium/ios/`; e.g.
  [`AppAutomationPlaygroundIosPlannerScenarioTest`](../samples/src/test/java/io/eot/mockede2e/appium/ios/AppAutomationPlaygroundIosPlannerScenarioTest.java) lives under `io.eot.mockede2e`),
- register into `IosScenarioRegistry` with an app **path** (a `.app` bundle
  directory under `sampleApps/`, not a `.zip` — unzip it once) instead of an APK,
- add your class to the `PROVIDER_CLASSES` list in
  [CompareIosWithFigmaTest.java](../samples/src/test/java/io/eot/pipeline/appium/ios/CompareIosWithFigmaTest.java),
- use `Platform=iOS` in the Excel,
- run `./gradlew compareIosWithFigma` instead of `compareAndroidWithFigma`.

## F. Plugging in your own existing Appium tests

If you already have Appium tests for an app and want them driving these visual
comparisons, this is really the same as C/D — you're not adopting a new test
framework, just porting your existing interaction code into a `ScenarioFlow`.

**Pre-requisites:**

1. **The app binary** under `sampleApps/` — an `.apk` for Android, or an unzipped
   `.app` bundle (not a `.zip`) for iOS.
2. **Figma designs** for whatever screens/steps you want validated — each needs
   its own Figma URL with a `node-id`.
3. **Your test logic as plain Appium driver calls**
   (`driver.findElement(AppiumBy...).click()`, waits, etc.) using
   `io.appium.java_client.AppiumDriver`/`AppiumBy` — whatever your existing tests
   already use. It doesn't matter what test runner they currently run under
   (JUnit, Cucumber, raw TestNG) — you're reusing the interaction code, not the
   test classes or annotations.
4. **Decided checkpoints** — exactly which points in your existing flow should
   become an `eyes.checkWindow(...)` call, i.e. which of your screens map to a
   Figma frame. Not every step of an existing test needs to become a check, only
   the ones you want validated against a design.
5. `config.properties` already set up (Figma token, Applitools creds) — same as
   everywhere else in this repo.
6. **One known gap to check first:**
   `AndroidDriverFactory` (in figmacompare)/
   `IosDriverFactory` (in figmacompare)
   currently only take `(apkPath/appPath, fullReset)` — a fixed, minimal
   capability set. If your app needs extra Appium capabilities, or your
   comparisons need different Eyes config (batch/match-level/etc. are currently
   global per platform, not per-app), those factories/runners would need a small
   extension first.

**Steps** — same as C/D: create one provider class per app (or scenario), port
your interaction code into its `ScenarioFlow`, calling `eyes.checkWindow(...)`
once per Figma row you want checked, in the same order those rows appear in the
Excel; add one line to that platform's `CompareAndroidWithFigmaTest`/
`CompareIosWithFigmaTest` `PROVIDER_CLASSES` list; add the
matching Excel rows; run `uploadFromFigma` then `compareAndroidWithFigma`/
`compareIosWithFigma`. See
[`AppAutomationPlaygroundAndroidPlannerScenarioTest.java`](../samples/src/test/java/io/eot/mockede2e/appium/android/AppAutomationPlaygroundAndroidPlannerScenarioTest.java)
for a worked multi-step example ported from real app exploration.

Back to [README_FigmaVisualValidation.md](../README_FigmaVisualValidation.md)
