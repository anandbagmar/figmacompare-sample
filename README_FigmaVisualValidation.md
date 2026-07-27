# Figma Visual Validation — full workflow

This is the end-to-end runbook for validating Bajaj Finserv's web/mobile
implementation against approved Figma designs. It ties together two programs
(`uploadFromFigma`, `compareWithFigma`) and the manual steps around them, all driven
by **one shared Excel file** that accumulates columns as it moves through the
pipeline — the same file is read from and written back to (in place) at every
stage, so there's no copying between stage-specific files.

| Status | Program |
|---|---|
| ✅ Implemented | [uploadFromFigma](README_uploadFromFigma.md) |
| ✅ Implemented | `compareWithFigma` web path — [BajajFinservWebTest.java](src/test/java/io/samples/web/selenium/BajajFinservWebTest.java), see [docs/README_Web_Selenium.md](docs/README_Web_Selenium.md) |
| ✅ Implemented (Android; template for other apps/iOS) | `compareWithFigma` mobile path — [BajajFinservAndroidTest.java](src/test/java/io/samples/appium/android/BajajFinservAndroidTest.java), see [docs/README_Appium_Java.md](docs/README_Appium_Java.md) |

## Roles

- **UI/UX Team** — owns the Figma designs, prepares the initial Excel rows,
  reviews final visual differences, files Jira issues.
- **QA** — runs both programs, fills in the `Locator` column for web components,
  writes the small per-screen Appium navigation flows mobile rows need, and helps
  triage failures.

## Single vs. multi-step tests ("scenarios")

Most rows are standalone: one Figma export = one full page or component = one
Applitools test. But the Applitools Figma plugin also supports exporting several
Figma frames together as the steps of **one** multi-step test (confirmed by
inspecting its own network traffic: one `eyes.open()`, one match call per frame
with its own step name, one close). This project supports the same thing via the
`Scenario Name` column:

- Blank → the row stands alone (today's normal behavior).
- A value shared by several **consecutive** rows → those rows become the ordered
  steps of one Applitools test, named after the scenario. Order is exactly the
  order the rows appear in the sheet.

For web, this needs no extra code — `BajajFinservWebTest` just does `driver.get()`
+ `check()` per row in a continuous browser session. For mobile, each step still
runs through the same `SCREEN_FLOWS` registry as a standalone row would, just in
one continuous app session across the whole scenario instead of relaunching
between steps. See `BajajFinservAndroidTest`'s class comment for the rule this
implies for how screen flows must be written.

## Pre-flight validation ("dry run")

Before `uploadFromFigma` or `compareWithFigma` do any real work (Figma/Applitools
API calls, browser/app launches), they validate the *entire* file and report every
problem found at once — nothing runs partially. Checks include:

- `Figma URL` (with a `node-id`), `Platform` (`Web`/`Android`/`iOS`), and
  `App URL / Screen Name` present on every non-`Skip` row; `Viewport` (if set)
  matches `WIDTHxHEIGHT`.
- **Scenario rows are contiguous** — a `Scenario Name` reused by non-adjacent rows
  is a hard error naming the offending row numbers.
- **Scenario metadata is consistent** — `Baseline Env Name`/`App Name`/`Viewport`
  must agree across every row in a scenario; a hard error names the conflicting
  values and rows.
- **Step names are unique within a scenario.**
- For `compareAndroidWithFigma` specifically: every Android row's
  `App URL / Screen Name` has a matching `SCREEN_FLOWS` entry.

This isn't a separate command — it's the first thing each of `uploadFromFigma`,
`compareWebWithFigma`, and `compareAndroidWithFigma` does.

## The Excel file, end to end

One row = one Figma design (a full page or a single component) paired 1-to-1 with
the corresponding place to find it in the real app — a **URL** for web, or a
**screen name** for mobile (since a mobile app has no URL to navigate to
directly). The file can have any number of rows; both programs iterate every row,
skipping any row marked `Skip`.

It's **one file for the whole pipeline**, by default `figma-visual-testing/figma_visual_tests.xlsx`
(configurable — see [Choosing the Excel file path](README_uploadFromFigma.md#choosing-the-excel-file-path)).
Some columns only matter to one stage; it's fine to leave those blank until that
stage runs:

| Column | Filled in by |
|---|---|
| `Figma URL`, `Platform`, `App URL / Screen Name` | Step 1 (UI/UX team) |
| `Scenario Name` | Step 1, optional — groups this row with others sharing the same value into one multi-step test |
| `Test Name`, `Baseline Env Name`, `Viewport`, `Scale`, `Format`, `Skip` | Step 1, optional — auto-derived by Step 2 if left blank |
| `App Name`, `Baseline Batch URL`, `Status`, `Error Message` | Step 2 (`uploadFromFigma`) |
| `Locator` | Step 3 (QA), web rows only |
| `Comparison Batch URL`, `Validation Status` | Step 4 (`compareWithFigma`) |

## Step 1 — Prepare the Excel file *(Manually prepared by the UI/UX team)*

Copy the template and fill in one row per Figma design to validate — see
[README_uploadFromFigma.md § 2](README_uploadFromFigma.md#2-fill-in-the-figma-excel-file)
for the full column reference. The columns worth calling out here:

| Column | Required? | Notes |
|---|---|---|
| `Figma URL` | Yes | Share link to a specific frame/component (must contain `node-id`) |
| `Platform` | Yes | `Web`, `Android`, or `iOS` |
| `App URL / Screen Name` | Yes | For `Web`: the UAT/production URL. For `Android`/`iOS`: a screen name/identifier — not a URL, since reaching a mobile screen usually needs app navigation |
| `Scenario Name` | No | Shared by consecutive rows to group them into one multi-step test (see above). Leave blank for a standalone row |
| `Baseline Env Name` | No | Provide your own baseline env name, or leave blank to auto-derive `{testName}-baseline` (standalone) / `{scenarioName}-baseline` (scenario) |
| `Skip` | No | `true`/`t`/`yes`/`y`/`skip` (case-insensitive) excludes this row from a run without removing it — useful for running only a subset |

## Step 2 — Upload Figma designs as baselines *(Automated — QA)* ✅

Run `./gradlew uploadFromFigma`. Details: [README_uploadFromFigma.md](README_uploadFromFigma.md).

For a standalone row, this is one Applitools test with one step. For a scenario,
every row in the group is downloaded and uploaded as the steps of **one**
Applitools test — `App Name`, `Baseline Env Name`, `Baseline Batch URL`, `Status`
are written back onto every row in the group (they describe the one shared test,
not the individual step).

## Step 3 — Identify validation scope *(Manual — QA)*

Open the same file (`figma_visual_tests.xlsx`) — no copying needed. For each row,
open `Baseline Batch URL` and decide what should be validated:

- **Web rows**: leave `Locator` blank for full-page validation, or fill it in with a
  CSS/XPath selector to validate just that component against the Figma baseline.
  This applies per step in a scenario too — each step can independently be
  full-page or a specific component.
- **Mobile rows**: always full-page — `Locator` is not used for Android/iOS. Instead,
  make sure `App URL / Screen Name` unambiguously identifies the screen, since QA
  will map it to an Appium flow in Step 4.

## Step 4 — Compare implementation against Figma baseline

`compareWithFigma` iterates every non-`Skip` row/scenario and runs the Applitools
Eyes comparison against `Baseline Env Name` from Step 2, writing back
`Comparison Batch URL` + `Validation Status` (`Passed`/`Unresolved`/`Failed`) into
the same file, in place — plus a final pass/fail summary. For a scenario, one
result is written onto every row in that group, since it's one Applitools test.

**4a. Web rows — fully generic. ✅ Implemented** as
[BajajFinservWebTest.java](src/test/java/io/samples/web/selenium/BajajFinservWebTest.java):
a TestNG test, data-driven from the shared Excel file, one invocation per group of
`Platform=Web` rows (a standalone row is a group of one). Selenium opens
`App URL / Screen Name` directly for each row/step in the group, in the same
continuous browser session — no per-row code needed even for a multi-step
scenario. Full page if `Locator` is blank, otherwise just that region. One
`VisualGridRunner`/`BatchInfo` pair is shared for the whole run, so groups just
submit their checks; results are collected once at the end (`@AfterSuite`),
matched back to each group's rows by test/scenario name, written to the Excel
file, and the suite fails there if anything mismatched. Run it with:
```bash
./gradlew compareWebWithFigma
# or against a specific file:
./gradlew compareWebWithFigma -PfigmaExcel=path/to/file.xlsx
```
See [docs/README_Web_Selenium.md](docs/README_Web_Selenium.md) for details.

**4b. Mobile rows — needs a per-screen Appium flow written by QA. ✅ Implemented for
Android** as
[BajajFinservAndroidTest.java](src/test/java/io/samples/appium/android/BajajFinservAndroidTest.java).
Unlike a web URL, a mobile "screen name" can't be navigated to generically — reaching
it usually requires login, menu navigation, or test data setup specific to that app. So
this class is **one test class per app**, not one generic runner: it owns a small
`SCREEN_FLOWS` registry mapping each distinct `App URL / Screen Name` value used by that
app to a short Appium method that leaves the app on that screen. The data-driven test
runs one invocation per group of `Platform=Android` rows; for each row/step in the
group it looks up and runs the matching flow (in one continuous app session for a
scenario, no relaunch between steps), then does the same Applitools comparison +
Excel write-back as the web path. **Every `SCREEN_FLOWS` entry must be
self-contained** — able to reach its target screen regardless of what ran before
it — since the same entry may run standalone (fresh launch) or as any step of any
scenario.
```bash
./gradlew compareAndroidWithFigma
```
A new Android/iOS app means a new test class following this same template — reusing the
shared [AppiumServerSupport](src/test/java/io/samples/appium/AppiumServerSupport.java),
[AndroidDriverFactory](src/test/java/io/samples/appium/android/AndroidDriverFactory.java),
[BatchSupport](src/test/java/io/samples/eyes/BatchSupport.java),
[ComparisonResultRecorder](src/test/java/io/samples/eyes/ComparisonResultRecorder.java),
[FigmaExcelFile](src/test/java/io/samples/excel/FigmaExcelFile.java), and
[FigmaValidation](src/test/java/io/samples/excel/FigmaValidation.java) utilities — only
its `SCREEN_FLOWS` entries and Eyes configuration specifics need to be written from scratch.
iOS has no equivalent driver factory/test class yet, but would follow the same pattern.

## Step 5 — Review and report *(UI/UX team will manually review the results)*

Open the same Excel file, filter rows where `Validation Status` is `Unresolved` or
`Failed`, open each `Comparison Batch URL` in the Applitools dashboard, and use the
Visual AI match algorithms to judge whether a flagged difference is a real
implementation bug. File a Jira issue directly from Applitools for valid discrepancies.

## What's next

Steps 1–5 all have a working implementation or documented manual process for Web and
Android. What's left:

- **iOS**: no `AppiumDriver` factory or comparison test class yet — follow
  `BajajFinservAndroidTest`'s template (own `SCREEN_FLOWS` registry + the shared
  `AppiumServerSupport`/`BatchSupport`/`ComparisonResultRecorder`/`FigmaExcelFile`
  utilities) once there's an iOS app and screens to validate.
- **New apps/screens**: each new Android/iOS app needs its own test class copying
  `BajajFinservAndroidTest`'s structure; each new screen just needs one more
  `SCREEN_FLOWS` entry in the relevant app's test class.
