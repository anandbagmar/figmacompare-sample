# Figma Visual Validation — full workflow

This is the end-to-end runbook for validating Bajaj Finserv's web/mobile
implementation against approved Figma designs. It ties together two programs
(`uploadToFigma`, `compareWithFigma`) and the manual steps around them, all driven
by a single Excel file that accumulates columns as it moves through the pipeline.

| Status | Program |
|---|---|
| ✅ Implemented | [uploadToFigma](README_uploadToFigma.md) |
| 🚧 Not yet built — plan below, pending confirmation | `compareWithFigma` |

## Roles

- **UI/UX (Prachiti)** — owns the Figma designs, prepares the initial input Excel,
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

## Step 1 — Prepare the input Excel *(Manual — Prachiti)*

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

For each row, open `Baseline Batch URL` and decide what should be validated:

- **Web rows**: leave `Locator` blank for full-page validation, or fill it in with a
  CSS/XPath selector to validate just that component against the Figma baseline.
- **Mobile rows**: always full-page — `Locator` is not used for Android/iOS. Instead,
  make sure `App URL / Screen Name` unambiguously identifies the screen, since QA
  will map it to an Appium flow in Step 4.

## Step 4 — Compare implementation against Figma baseline *(Step 4a automated, 4b semi-automated — QA)* 🚧

Run `compareWithFigma` over the same Excel. It iterates every row and, per row,
opens the app, captures the screen/region, runs the Applitools Eyes comparison
against `Baseline Env Name` from Step 2, and writes back `Comparison Batch URL` +
`Validation Status` (`Passed`/`Unresolved`/`Failed`) — plus a final pass/fail summary,
the same pattern `uploadToFigma` already uses.

**4a. Web rows — fully generic.** Selenium opens `App URL / Screen Name` directly;
no per-row code needed. Full page if `Locator` is blank, otherwise just that region.

**4b. Mobile rows — needs a per-screen Appium flow written by QA.** Unlike a web URL,
a mobile "screen name" can't be navigated to generically — reaching it usually
requires login, menu navigation, or test data setup specific to that app. So for each
distinct `App URL / Screen Name` value used in the sheet, QA writes one small Appium
method (a "screen flow": drive the app to that screen and hand back a screenshot/driver
state) and registers it by name; `compareWithFigma` looks up the matching flow for each
mobile row, runs it, then does the same Applitools comparison + Excel write-back as the
web path. This mirrors what `BajajFinservAndroidTest`/`CalculatorFigmaTest` already do
by hand today (see [docs/README_Appium_Java.md](docs/README_Appium_Java.md)) —
`compareWithFigma` gives that pattern a shared runner + Excel-driven results instead of
one bespoke test class per screen.

## Step 5 — Review and report *(Manual — Prachiti/QA)*

Open the final output Excel, filter rows where `Validation Status` is `Unresolved` or
`Failed`, open each `Comparison Batch URL` in the Applitools dashboard, and use the
Visual AI match algorithms to judge whether a flagged difference is a real
implementation bug. File a Jira issue directly from Applitools for valid discrepancies.

## What's next

Step 4 (`compareWithFigma`) is only a plan right now — nothing has been implemented.
Once this runbook is confirmed, implementation will add:

- The `Platform` and `Locator` columns to the Excel schema (`ExcelHelper`/`FigmaRow`
  and the input template), extending what `uploadToFigma` already reads/writes.
- A `compareWithFigma` Gradle task + main class for the fully-generic web path (4a).
- A small registry/interface for mobile screen flows (4b) that QA implements per app,
  plus one worked example screen flow to copy from.
