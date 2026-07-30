Back to [README_FigmaVisualValidation.md](README_FigmaVisualValidation.md)

# Single vs. multi-step tests ("scenarios")

## Table of contents

- [Web — `Scenario Name` is optional](#web)
- [Android/iOS — `Scenario Name` is required, always](#androidios)

Most rows are standalone: one Figma export = one full page or component = one
Applitools test. But the Applitools Figma plugin also supports exporting several
Figma frames together as the steps of **one** multi-step test (confirmed by
inspecting its own network traffic: one `eyes.open()`, one match call per frame
with its own step name, one close). This project supports the same thing via the
`Scenario Name` column — but web and mobile treat it very differently:

## Web

**`Scenario Name` is optional.** A blank value means the row stands alone
(today's normal behavior, fully generic). A value shared by several **consecutive**
rows groups them into the ordered steps of one Applitools test. Either way,
`WebCompareRunner` is the same generic code: it just does `driver.get()` +
`check()` per row, in one continuous browser session for a scenario. No code to
write, ever, for any web row.

## Android/iOS

**`Scenario Name` is required, always**, whether the row covers one
Figma export or several. Reaching even a *single* mobile screen can need bespoke
login or navigation, so there's no generic "just open this screen" runner for
mobile the way there is for web — every mobile test is inherently a hand-written
procedure. `Scenario Name` is the key QA uses to dispatch to that procedure:

- `AndroidCompareRunner` (plain-Java, in figmacompare), driven by
  [`CompareAndroidWithFigmaTest`](../src/test/java/io/eot/pipeline/appium/android/CompareAndroidWithFigmaTest.java)
  (thin TestNG shim, in this repo), is the **one** runner for every Android row,
  regardless of app.
- It looks up each group's `Scenario Name` in
  `AndroidScenarioRegistry` (in figmacompare)
  — a shared, static registry that any class can register into.
- App-specific classes like
  [`BajajFinservAndroidTest`](../src/test/java/io/eot/bajajfinserv/appium/android/BajajFinservAndroidTest.java)
  aren't TestNG tests themselves — they're **scenario providers**: their static
  initializer registers `(scenarioName, apkPath, appName, ScenarioFlow)` tuples
  into the registry. `AndroidCompareRunner` finds and runs whichever one
  matches, launching the right app for it, **regardless of which class file
  registered it**.
- A `ScenarioFlow` owns its whole scenario: whatever login/navigation the real app
  needs, then one `eyes.checkWindow(...)` call per step it wants recorded, in
  whatever order makes sense for that flow. It is not a generic "look up a screen
  and check it" function — it's the actual bespoke test.

See [README_AddingTests.md](README_AddingTests.md) for how to register a new
scenario.

Back to [README_FigmaVisualValidation.md](README_FigmaVisualValidation.md)
