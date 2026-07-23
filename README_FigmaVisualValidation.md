# Figma Visual Validation — full workflow

This is the end-to-end runbook for validating Bajaj Finserv's web/mobile
implementation against approved Figma designs. It ties together two programs
(`uploadToFigma`, `compareWithFigma`) and the manual steps around them, all driven
by a single Excel file that accumulates columns as it moves through the pipeline.

| Status | Program |
|---|---|
| ✅ Implemented | [uploadToFigma](README_uploadToFigma.md) |
| ✅ Implemented | `compareWithFigma` web path — [BajajFinservWebTest.java](src/test/java/io/samples/web/selenium/BajajFinservWebTest.java), see [docs/README_Web_Selenium.md](docs/README_Web_Selenium.md) |
| ✅ Implemented (Android; template for other apps/iOS) | `compareWithFigma` mobile path — [BajajFinservAndroidTest.java](src/test/java/io/samples/appium/android/BajajFinservAndroidTest.java), see [docs/README_Appium_Java.md](docs/README_Appium_Java.md) |

## Roles

- **UI/UX Team** — owns the Figma designs, prepares the initial input Excel,
  reviews final visual differences, files Jira issues.
- **QA** — runs both programs, fills in the `Locator` column for web components,
  writes the small per-screen Appium navigation flows mobile rows need, and helps
  triage failures.

## The Excel file, end to end

One row = one Figma design (a full page or a single component) paired 1-to-1 with
the corresponding place to find it in the real app — a **URL** for web, or a
**screen name** for mobile (since a mobile app has no URL to navigate to directly).
The file can have any number of rows; both programs iterate every row.

The same file gains columns as it passes through each step:

```
Input                          → after uploadToFigma        → after QA review        → after compareWithFigma
─────────────────────────────────────────────────────────────────────────────────────────────────────────────
Figma URL                        (unchanged)                   (unchanged)              (unchanged)
Platform                         (unchanged)                   (unchanged)              (unchanged)
App URL / Screen Name            (unchanged)                   (unchanged)              (unchanged)
Test Name (optional)             filled in if blank             (unchanged)              (unchanged)
Viewport (optional, web)         filled in if blank             (unchanged)              (unchanged)
Scale / Format (optional)        (unchanged)                   (unchanged)              (unchanged)
                                  App Name                                               (unchanged)
                                  Baseline Env Name                                       (unchanged)
                                  Baseline Batch URL                                      (unchanged)
                                  Status / Error Message                                  (unchanged)
                                                                Locator (web only)        (unchanged)
                                                                                          Comparison Batch URL
                                                                                          Validation Status
                                                                                          Error Message
```

## Step 1 — Prepare the input Excel *(Manually prepared by the UI/UX team)*

One row per Figma design to validate:

| Column | Required? | Notes |
|---|---|---|
| `Figma URL` | Yes | Share link to a specific frame/component (must contain `node-id`) |
| `Platform` | Yes | `Web`, `Android`, or `iOS` |
| `App URL / Screen Name` | Yes | For `Web`: the UAT/production URL. For `Android`/`iOS`: a screen name/identifier — not a URL, since reaching a mobile screen usually needs app navigation |
| `Test Name` | No | Auto-derived from the Figma node name if blank |
| `Viewport` | No, web only | `WIDTHxHEIGHT`; auto-derived from the image if blank |
| `Scale` / `Format` | No | Figma export options; default `1` / `png` |

## Step 2 — Upload Figma designs as baselines *(Automated — QA)* ✅

Run `./gradlew uploadToFigma`. Details: [README_uploadToFigma.md](README_uploadToFigma.md).

Produces an output Excel with `App Name`, `Baseline Env Name`, `Baseline Batch URL`,
`Status` added — this becomes the input to Step 3.

## Step 3 — Identify validation scope *(Manual — QA)*

Copy `uploadToFigma`'s output into the compare input file `compareWithFigma` expects
(this becomes your working copy — leave the actual output file alone in case you need
to re-check it later):

```bash
cp figma-visual-testing/figma_baseline_input_output.xlsx figma-visual-testing/figma_compare_input.xlsx
```

Then, for each row in `figma_compare_input.xlsx`, open `Baseline Batch URL` and decide
what should be validated:

- **Web rows**: leave `Locator` blank for full-page validation, or fill it in with a
  CSS/XPath selector to validate just that component against the Figma baseline.
- **Mobile rows**: always full-page — `Locator` is not used for Android/iOS. Instead,
  make sure `App URL / Screen Name` unambiguously identifies the screen, since QA
  will map it to an Appium flow in Step 4.

## Step 4 — Compare implementation against Figma baseline

`compareWithFigma` iterates every row and, per row, opens the app, captures the
screen/region, runs the Applitools Eyes comparison against `Baseline Env Name` from
Step 2, and writes back `Comparison Batch URL` + `Validation Status`
(`Passed`/`Unresolved`/`Failed`) — plus a final pass/fail summary, the same pattern
`uploadToFigma` already uses.

**4a. Web rows — fully generic. ✅ Implemented** as
[BajajFinservWebTest.java](src/test/java/io/samples/web/selenium/BajajFinservWebTest.java):
a TestNG test, data-driven from an Excel file (default
`figma-visual-testing/figma_compare_input.xlsx`, override with `-PcompareExcel=<path>`),
one invocation per `Platform=Web` row. Selenium opens `App URL / Screen Name` directly —
no per-row code needed. Full page if `Locator` is blank, otherwise just that region.
Run it with:
```bash
./gradlew test -PtestClass=io.samples.web.selenium.BajajFinservWebTest
```
See [docs/README_Web_Selenium.md](docs/README_Web_Selenium.md) for details. The input
file is `uploadToFigma`'s output Excel with `Locator` filled in per Step 3 — use
[figma-visual-testing/templates/figma_compare_input_template.xlsx](figma-visual-testing/templates/figma_compare_input_template.xlsx)
as a reference for the expected shape.

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
./gradlew test -PtestClass=io.samples.appium.android.BajajFinservAndroidTest
```
A new Android/iOS app means a new test class following this same template — reusing the
shared [AppiumServerSupport](src/test/java/io/samples/appium/AppiumServerSupport.java),
[AndroidDriverFactory](src/test/java/io/samples/appium/android/AndroidDriverFactory.java),
[BatchSupport](src/test/java/io/samples/eyes/BatchSupport.java),
[ComparisonResultRecorder](src/test/java/io/samples/eyes/ComparisonResultRecorder.java),
and [CompareRows](src/test/java/io/samples/excel/CompareRows.java) utilities — only its
`SCREEN_FLOWS` entries and Eyes configuration specifics need to be written from scratch.
iOS has no equivalent driver factory/test class yet, but would follow the same pattern.

## Step 5 — Review and report *(UI/UX team will manually review the results)*

Open the final output Excel, filter rows where `Validation Status` is `Unresolved` or
`Failed`, open each `Comparison Batch URL` in the Applitools dashboard, and use the
Visual AI match algorithms to judge whether a flagged difference is a real
implementation bug. File a Jira issue directly from Applitools for valid discrepancies.

## What's next

Steps 1–5 all have a working implementation or documented manual process for Web and
Android. What's left:

- **iOS**: no `AppiumDriver` factory or comparison test class yet — follow
  `BajajFinservAndroidTest`'s template (own `SCREEN_FLOWS` registry + the shared
  `AppiumServerSupport`/`BatchSupport`/`ComparisonResultRecorder`/`CompareRows` utilities)
  once there's an iOS app and screens to validate.
- **New apps/screens**: each new Android/iOS app needs its own test class copying
  `BajajFinservAndroidTest`'s structure; each new screen just needs one more
  `SCREEN_FLOWS` entry in the relevant app's test class.
