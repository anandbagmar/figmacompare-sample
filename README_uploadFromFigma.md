# uploadFromFigma — Figma → Applitools baseline uploader

Reads a list of Figma share links from the shared Figma Excel file, downloads each
design as an image, uploads it to Applitools Eyes, and saves it as a visual baseline.
Results (app name, baseline env name, batch URL, status) are written back into the
same file, in place.

This program is pure Java — no Selenium/Appium/browser needed.

This is step 2 of the full workflow — see
[README_FigmaVisualValidation.md](README_FigmaVisualValidation.md) for how this fits
together with the manual review steps and the `compareWithFigma` comparison programs.

## Quick start

Templates live in **[figma-visual-testing/templates/](figma-visual-testing/templates/)**
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

### 1.1 Get a Figma personal access token
In Figma: **Account Settings → Security → Personal access tokens** → generate a new
token. This token must have access to the file(s) you want to export.

### 1.2 Get your Applitools API key and server URL
From the Applitools dashboard: **Account Settings → API Key**. The server URL is
usually `https://eyesapi.applitools.com` (SaaS) — use your org's URL if you're on a
private/on-prem instance.

### 1.3 Fill in `config.properties`
Copy [figma-visual-testing/templates/config.properties.example](figma-visual-testing/templates/config.properties.example)
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
```

Any of these can instead be set as an **environment variable** of the same name
(e.g. `export FIGMA_TOKEN=...`) — an env var always overrides the value in
`config.properties`. This is useful for CI.

`FIGMA_EXCEL_FILE` is optional — see [Choosing the Excel file path](#choosing-the-excel-file-path) below.

## 2. Fill in the Figma Excel file

Copy [figma-visual-testing/templates/figma_visual_tests_template.xlsx](figma-visual-testing/templates/figma_visual_tests_template.xlsx)
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
| `Test Name` | No | Overrides the auto-derived test name. If left blank, it's derived from the Figma node's name (sanitized to letters/digits/`-`/`_`). |
| `Baseline Env Name` | No | Overrides the Applitools baseline environment name. If left blank, it's derived as `{testName}-baseline`. Provide this if you need a specific/existing baseline env name instead of the auto-derived one. |
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

Alternatively, run it directly from your IDE (IntelliJ/VS Code): open
`UploadFromFigma.java` and run its `main` method with Program Arguments set to
`[figmaExcelPath] [forceRefresh]` (both optional).

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

## Notes

- Re-running with the same `Figma URL` and no `forceRefresh` reuses the cached
  image — it will not re-hit the Figma API, but it *will* re-upload to Applitools
  and create a new baseline test each run (this program always calls
  `saveNewTests(true)`).
- A Figma URL without a `node-id` (i.e. a link to a whole file/page rather than a
  specific frame) is not currently supported — use *Copy link to selection* on the
  specific frame/component in Figma.
- Since the Excel file is updated in place, close it in Excel before running —
  a file locked open by another program can't be overwritten.
