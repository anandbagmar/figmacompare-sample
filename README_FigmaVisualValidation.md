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
| `Test Name`, `Baseline Env Name`, `Viewport`, `Scale`, `Format`, `Skip` | Step 1, optional — auto-derived by Step 2 if left blank |
| `App Name`, `Baseline Batch URL`, `Status`, `Error Message` | Step 2 (`uploadFromFigma`) |
| `Locator` | Step 3 (QA), web rows only |
| `Comparison Batch URL`, `Validation Status` | Step 4 (`compareWithFigma`) |

## Step 1 — Prepare the Excel file *(Manually prepared by the UI/UX team)*

Copy the template and fill in one row per Figma design to validate — see
[README_uploadFromFigma.md § 2](README_uploadFromFigma.md#2-fill-in-the-figma-excel-file)
for the full column reference. The two columns worth calling out here:

| Column | Required? | Notes |
|---|---|---|
| `Figma URL` | Yes | Share link to a specific frame/component (must contain `node-id`) |
| `Platform` | Yes | `Web`, `Android`, or `iOS` |
| `App URL / Screen Name` | Yes | For `Web`: the UAT/production URL. For `Android`/`iOS`: a screen name/identifier — not a URL, since reaching a mobile screen usually needs app navigation |
| `Baseline Env Name` | No | Provide your own baseline env name, or leave blank to auto-derive `{testName}-baseline` |
| `Skip` | No | `true`/`t`/`yes`/`y`/`skip` (case-insensitive) excludes this row from a run without removing it — useful for running only a subset |

## Step 2 — Upload Figma designs as baselines *(Automated — QA)* ✅

Run `./gradlew uploadFromFigma`. Details: [README_uploadFromFigma.md](README_uploadFromFigma.md).

Writes `App Name`, `Baseline Env Name` (if not already provided), `Baseline Batch URL`,
`Status` back into the same file, in place.

## Step 3 — Identify validation scope *(Manual — QA)*

Open the same file (`figma_visual_tests.xlsx`) — no copying needed. For each row,
open `Baseline Batch URL` and decide what should be validated:

- **Web rows**: leave `Locator` blank for full-page validation, or fill it in with a
  CSS/XPath selector to validate just that component against the Figma baseline.
- **Mobile rows**: always full-page — `Locator` is not used for Android/iOS. Instead,
  make sure `App URL / Screen Name` unambiguously identifies the screen, since QA
  will map it to an Appium flow in Step 4.

## Step 4 — Compare implementation against Figma baseline

`compareWithFigma` iterates every non-`Skip` row and, per row, opens the app,
captures the screen/region, runs the Applitools Eyes comparison against
`Baseline Env Name` from Step 2, and writes back `Comparison Batch URL` +
`Validation Status` (`Passed`/`Unresolved`/`Failed`) into the same file, in place —
plus a final pass/fail summary, the same pattern `uploadFromFigma` already uses.

**4a. Web rows — fully generic. ✅ Implemented** as
[BajajFinservWebTest.java](src/test/java/io/samples/web/selenium/BajajFinservWebTest.java):
a TestNG test, data-driven from the shared Excel file, one invocation per
`Platform=Web` row. Selenium opens `App URL / Screen Name` directly — no per-row
code needed. Full page if `Locator` is blank, otherwise just that region. One
`VisualGridRunner`/`BatchInfo` pair is shared for the whole run, so individual rows
just submit their check; results are collected once at the end (`@AfterSuite`),
matched back to each row by test name, written to the Excel file, and the suite
fails there if anything mismatched. Run it with:
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
looks up the matching flow for each `Platform=Android` row, runs it, then does the same
Applitools comparison + Excel write-back as the web path.
```bash
./gradlew compareAndroidWithFigma
```
A new Android/iOS app means a new test class following this same template — reusing the
shared [AppiumServerSupport](src/test/java/io/samples/appium/AppiumServerSupport.java),
[AndroidDriverFactory](src/test/java/io/samples/appium/android/AndroidDriverFactory.java),
[BatchSupport](src/test/java/io/samples/eyes/BatchSupport.java),
[ComparisonResultRecorder](src/test/java/io/samples/eyes/ComparisonResultRecorder.java),
and [FigmaExcelFile](src/test/java/io/samples/excel/FigmaExcelFile.java) utilities — only
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
