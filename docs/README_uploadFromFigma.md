Back to main [README](../README.md)

# uploadFromFigma — Figma → Applitools baseline uploader

Reads a list of Figma share links from the shared Figma Excel file, downloads each
design as an image, uploads it to Applitools Eyes, and saves it as a visual baseline.
Results (app name, baseline env name, batch URL, status) are written back into the
same file, in place.

This program is pure Java — no Selenium/Appium/browser needed.

This is step 2 of the full workflow — see
[README_FigmaVisualValidation.md](README_FigmaVisualValidation.md) for how this fits
together with the manual review steps and the `compareWithFigma` comparison programs.

## Table of contents

- [Quick start](#quick-start)
- [1. One-time setup](#1-one-time-setup)
  - [1.0 Build and publish figmacompare](#10-build-and-publish-figmacompare)
  - [1.1 Get a Figma personal access token](#11-get-a-figma-personal-access-token)
  - [1.2 Get your Applitools API key and server URL](#12-get-your-applitools-api-key-and-server-url)
  - [1.3 Fill in `config.properties`](#13-fill-in-configproperties)
- [2. Fill in the Figma Excel file](#2-fill-in-the-figma-excel-file)
- [3. Run the program](#3-run-the-program)
  - [Choosing the Excel file path](#choosing-the-excel-file-path)
- [4. Check the results](#4-check-the-results)
- [Cleaning up results](#cleaning-up-results)
- [Notes](#notes)

## Quick start

Templates live in **[figma-visual-testing/templates/](../figma-visual-testing/templates/)**
and are reference-only — never edit them directly. Copy them one level up, into
**figma-visual-testing/**, to create your actual working files.

Run these from the project root:

```bash
cp figma-visual-testing/templates/config.properties.example figma-visual-testing/config.properties
cp figma-visual-testing/templates/figma_visual_tests_template.xlsx figma-visual-testing/figma_visual_tests.xlsx
```

1. **Fill in the config**: open `figma-visual-testing/config.properties` (the copy
   you just made) and fill in `FIGMA_TOKEN`, `APPLITOOLS_API_KEY`, and
   `APPLITOOLS_SERVER_URL` (see [step 1](#1-one-time-setup) below for where to get
   these). This exact file is gitignored, so your tokens never get committed.
2. **Fill in the Excel file**: open `figma-visual-testing/figma_visual_tests.xlsx`
   (the copy you just made) and add one row per Figma design you want as a baseline
   (see [step 2](#2-fill-in-the-figma-excel-file)). This is the **one file** used
   for the entire workflow — `uploadFromFigma` and `compareWithFigma` both read from
   and write back into it, in place.
3. **Run it**:
   ```bash
   ./gradlew uploadFromFigma
   ```
4. **Check the results**: re-open `figma-visual-testing/figma_visual_tests.xlsx`
   — it now has a `Status` column and a `Baseline Batch URL` link to each uploaded
   baseline in Applitools.

Everything below is detail/reference for the steps above.

## 1. One-time setup

### 1.0 Get access to figmacompare

This repo depends on `com.github.anandbagmar:figmacompare`, resolved via
[JitPack](https://jitpack.io), which builds directly from the (public)
[figmacompare](https://github.com/anandbagmar/figmacompare) repo's tags on demand - it
isn't on Maven Central, and **no token is needed** to consume it. `build.gradle` tries
two sources, in order:

1. **`mavenLocal()`** — your local `~/.m2` cache, checked first
2. **JitPack** — the real published releases

Pick whichever matches what you're doing:

**Just running the tests, not changing figmacompare itself:** no setup needed - just run:
```bash
./gradlew compareWebWithFigma   # or any other task
```
With no `-PfigmacompareVersion=...` given, `build.gradle` automatically resolves the
latest release via `scripts/latest-figmacompare-version.sh` (a plain, unauthenticated
GitHub API call). Pass `-PfigmacompareVersion=vX.Y.Z` (note: **include the `v`** - unlike
this repo's own version number, JitPack needs the exact tag) to pin a specific release
instead, e.g. to reproduce an old build.

**Actively iterating on a figmacompare change:** publish it to your local `~/.m2` cache
instead, which `mavenLocal()` picks up automatically, then point this repo at that
version with `-PfigmacompareVersion=...` - **no `build.gradle` edit needed**:
```bash
cd /path/to/figmacompare
./gradlew publishToMavenLocal
# prints the exact version published, e.g. "Published io.eot:figmacompare:0.0.1-local
# to mavenLocal ... To use it from figmacompare-sample, run there with:
# -PfigmacompareVersion=0.0.1-local"

cd /path/to/figmacompare-sample
./gradlew compareWebWithFigma -PfigmacompareVersion=0.0.1-local
```

### 1.1 Get a Figma personal access token
In Figma: **Account Settings → Security → Personal access tokens** → generate a new
token. This token must have access to the file(s) you want to export.

### 1.2 Get your Applitools API key and server URL
From the Applitools dashboard: **Account Settings → API Key**. The server URL is
usually `https://eyesapi.applitools.com` (SaaS) — use your org's URL if you're on a
private/on-prem instance.

### 1.3 Fill in `config.properties`
Copy [figma-visual-testing/templates/config.properties.example](../figma-visual-testing/templates/config.properties.example)
to `figma-visual-testing/config.properties` (this exact file is gitignored, so your
tokens never get committed):

```bash
cp figma-visual-testing/templates/config.properties.example figma-visual-testing/config.properties
```

Then fill it in:

```properties
FIGMA_TOKEN=figd_xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx
APPLITOOLS_API_KEY=your-applitools-api-key
APPLITOOLS_SERVER_URL=https://eyesapi.applitools.com
APP_NAME=Applitools-Images
FIGMA_CACHE_DIR=downloaded_images/figma-cache
FIGMA_EXCEL_FILE=
APPLITOOLS_BATCH_NAME=
```

`APPLITOOLS_BATCH_NAME` groups every baseline uploaded in one `uploadFromFigma` run
under a single named batch in the Applitools dashboard. Leave it blank to default to
`Upload from Figma`.

Any of these can instead be set as an **environment variable** of the same name
(e.g. `export FIGMA_TOKEN=...`) — an env var always overrides the value in
`config.properties`. This is useful for CI.

`FIGMA_EXCEL_FILE` is optional — see [Choosing the Excel file path](#choosing-the-excel-file-path) below.

## 2. Fill in the Figma Excel file

Copy [figma-visual-testing/templates/figma_visual_tests_template.xlsx](../figma-visual-testing/templates/figma_visual_tests_template.xlsx)
to `figma-visual-testing/figma_visual_tests.xlsx` (the default filename both
`uploadFromFigma` and `compareWithFigma` look for):

```bash
cp figma-visual-testing/templates/figma_visual_tests_template.xlsx figma-visual-testing/figma_visual_tests.xlsx
```

Then fill in one row per page/component to validate. This is the same file used all
the way through `compareWithFigma` later — some columns only matter to this step,
others only to the comparison step, and it's fine to leave those blank for now.

| Column | Required? | Description |
|---|---|---|
| `Figma URL` | **Yes** | A Figma share link to a specific frame/component, e.g. right-click a frame in Figma → *Copy link to selection*. Must contain a `node-id`. |
| `Platform` | No (used later by `compareWithFigma`) | `Web`, `Android`, or `iOS`. Not used by this program, but keep it here so the same row can be reused later. |
| `App URL / Screen Name` | No (used later by `compareWithFigma`) | For `Web`: the UAT/production URL. For `Android`/`iOS`: a screen name/identifier. Not used by this program, but keep it here so the same row can be reused later. |
| `Scenario Name` | **Yes for Android/iOS; optional for Web** | Names the Applitools test this row belongs to (this program doesn't need it to be already registered anywhere - that only matters later, for `compareWithFigma`). Shared by **consecutive** rows to upload them as the steps of one multi-step Applitools test instead of one test per row (matches how the Applitools Figma plugin exports a multi-frame scenario). For web, leave blank for a standalone row. For Android/iOS, this is always required, even for a single-row test - see [README_FigmaVisualValidation.md](README_FigmaVisualValidation.md) for why. |
| `Test Name` | No | Overrides the auto-derived test/step name. If left blank, it's derived from the Figma node's name (sanitized to letters/digits/`-`/`_`). For a scenario, this is the step's name within the shared test, not the whole test's name. |
| `Baseline Env Name` | No | Overrides the Applitools baseline environment name. If left blank, it's derived as `{testName}-baseline` (standalone) or `{scenarioName}-baseline` (scenario). For a scenario, only needs to be set on one row — it's shared across the whole group, and re-checked for consistency across the group before anything runs. |
| `Viewport` | No | Overrides the viewport size, format `WIDTHxHEIGHT` (e.g. `1280x1024`). If left blank, it's derived from the downloaded image's pixel dimensions. |
| `Scale` | No | Figma export scale, e.g. `1`, `2`, `3`. Defaults to `1` if blank. |
| `Format` | No | Figma export format: `png`, `jpg`, `svg`, `pdf`. Defaults to `png` if blank. |
| `Skip` | No | Set to `true`/`t`/`yes`/`y`/`skip` (case-insensitive) to exclude this row from a run without deleting it. Blank/anything else means the row runs normally. To run only specific rows, mark everything else as `Skip`. |

Only `Figma URL` is required — everything else can be left blank and the program
will fill in sensible defaults.

## 3. Run the program

From the project root:

```bash
./gradlew uploadFromFigma
```

### Choosing the Excel file path

By default, both programs look for `figma-visual-testing/figma_visual_tests.xlsx`.
You can point at a different file (e.g. separate files per project, or one for web
and one for mobile) in priority order:

1. **Command line** (highest priority): `-PfigmaExcel=<path>`
2. **`config.properties`**: set `FIGMA_EXCEL_FILE=<path>` (itself overridable by a
   `FIGMA_EXCEL_FILE` environment variable)
3. **Built-in default**: `figma-visual-testing/figma_visual_tests.xlsx`

```bash
./gradlew uploadFromFigma -PfigmaExcel=figma-visual-testing/web_project_a.xlsx -PforceRefresh=true
```

**Parameters:**

- `-PfigmaExcel=<path>` — path to the Excel file. Defaults per the priority order above.
- `-PforceRefresh=true` — re-downloads every Figma image even if a cached copy
  already exists (useful when the Figma design has changed); defaults to `false`,
  which reuses any cached image already in `FIGMA_CACHE_DIR`.
- `-Pplatform=Web|Android|iOS` — only upload baselines for that platform's rows (an
  invalid value hard-fails immediately). Defaults to unset, which processes every
  row regardless of platform (prior behavior) - useful while iterating on one
  platform's tests without re-touching baselines for the others.

```bash
./gradlew uploadFromFigma -Pplatform=Web
```

Alternatively, run it directly from your IDE (IntelliJ/VS Code): open
`UploadFromFigma.java` and run its `main` method with Program Arguments set to
`[figmaExcelPath] [forceRefresh] [platform]` (all optional).

## 4. Check the results

- Downloaded Figma images are cached under `downloaded_images/figma-cache/`
  (configurable via `FIGMA_CACHE_DIR`), named `{fileKey}_{nodeId}_{scale}x.{format}`.
- Results are written back into the same Excel file, in place: `App Name`,
  `Baseline Env Name`, `Baseline Batch URL`, `Status`, `Error Message`.
- Any row where `Status` is `Failed` will have a message in `Error Message` — check
  the console output for the full stack trace.
- On success, `Baseline Batch URL` links directly to the uploaded baseline in the
  Applitools dashboard.
- The console also prints a one-line summary at the end, e.g. `4 of 5 succeeded.`
- Rows marked `Skip` are left completely untouched and don't count toward that summary.

## Cleaning up results

```bash
./gradlew cleanFigmaExcel
```

Resets the write-back result columns (`App Name`, `Baseline Batch URL`, `Status`,
`Error Message`, `Comparison Batch URL`, `Validation Status`) back to blank across
every row - a clean slate before a fresh `uploadFromFigma`/`compareWithFigma` run,
without hand-editing cells.

This does **not** touch `Baseline Env Name`, `Viewport`, or `Test Name` - those are
manual-input columns with an auto-derive fallback (blank the first time, then whatever
a run last wrote), not pure results, so they're deliberately left alone. If one of
those is stuck on a stale value - e.g. after renaming a `Scenario Name`, its
`Baseline Env Name` doesn't follow automatically, since it's only derived when blank -
clear that specific cell by hand.

Same `-PfigmaExcel=<path>` override as every other task, e.g.:
```bash
./gradlew cleanFigmaExcel -PfigmaExcel=figma-visual-testing/figma_mockede2e_web_ci.xlsx
```

**This is a destructive, in-place overwrite with no backup** - same as every other
write this project does to the Excel file. If you're tracking the file yourself
(git, Time Machine, a manual copy), that's your safety net, not this tool's.

## Notes

- Re-running with the same `Figma URL` and no `forceRefresh` reuses the cached
  image — it will not re-hit the Figma API, but it *will* re-upload to Applitools
  and create a new baseline test each run (this program always calls
  `saveNewTests(true)`). A first-ever run for a given `Baseline Env Name` will show
  as **New**/**Unresolved** in the Applitools dashboard's Test Results view - that's
  expected, not a failure: there's nothing to compare a brand-new checkpoint
  against, but it is saved as the active baseline (since `saveNewTests` is on). To
  confirm it actually saved, run `compareWebWithFigma` against the same row - it'll
  show Passed/Failed (not New) once a real baseline exists to diff against.
- **Manual workaround for a Figma image that keeps failing** (rate-limited, or
  otherwise): you can place the image directly in the cache instead of waiting on
  the Figma API. `FigmaClient.getCachedImage` only hits the network if the cache
  file doesn't already exist, so a manually-placed file with the right name is used
  as-is:
  1. In Figma, select the frame → **Export** → PNG at the row's `Scale` (default `1`)
  2. Name it `{fileKey}_{nodeId with ":" replaced by "-"}_{scale}x.{format}` - e.g.
     for `node-id=170-61` in file `7kPt5byFnDm1hs2Bd1FlNL`, scale `1`, format `png`:
     `7kPt5byFnDm1hs2Bd1FlNL_170-61_1x.png`
  3. Move it into `FIGMA_CACHE_DIR` (default `downloaded_images/figma-cache/`)
  4. Run without `-PforceRefresh=true`, so the cache is honored

  Make sure the export matches what Figma's own API would have rendered (same
  frame, default export settings) - this image becomes the actual Applitools
  baseline, not just a placeholder.
- A Figma URL without a `node-id` (i.e. a link to a whole file/page rather than a
  specific frame) is not currently supported — use *Copy link to selection* on the
  specific frame/component in Figma.
- Since the Excel file is updated in place, close it in Excel before running —
  a file locked open by another program can't be overwritten.
- Figma's API rate-limits requests per token - if you see repeated `HTTP 429`
  messages, that's Figma throttling, not a bug. `FigmaClient` (in `figmacompare`)
  already retries with backoff and paces requests to reduce how often this happens,
  but heavy back-to-back runs (e.g. several manual CI dispatches while testing) can
  still exhaust the token's budget - if so, just wait before retrying. In CI
  specifically, `downloaded_images/figma-cache/` is cached between runs (see this
  repo's README, "Continuous Integration" § Figma image caching) so repeated runs
  don't re-download unchanged images.
