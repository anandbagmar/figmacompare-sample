# uploadToFigma — Figma → Applitools baseline uploader

Reads a list of Figma share links from an Excel file, downloads each design as an
image, uploads it to Applitools Eyes, and saves it as a visual baseline. Results
(app name, test name, viewport, baseline env name, batch URL, status) are written
to a new output Excel file next to the input.

This program is pure Java — no Selenium/Appium/browser needed.

## Quick start

Everything you need to touch lives in one folder: **[figma-visual-testing/](figma-visual-testing/)**

1. **Copy the config file**: duplicate [figma-visual-testing/config.properties.example](figma-visual-testing/config.properties.example)
   as `figma-visual-testing/config.properties`, and fill in `FIGMA_TOKEN`,
   `APPLITOOLS_API_KEY`, and `APPLITOOLS_SERVER_URL` (see [step 1](#1-one-time-setup) below for where to get these).
2. **Copy the input template**: duplicate [figma-visual-testing/figma_baseline_input_template.xlsx](figma-visual-testing/figma_baseline_input_template.xlsx)
   as `figma-visual-testing/figma_baseline_input.xlsx`, and add one row per
   Figma design you want as a baseline (see [step 2](#2-prepare-the-input-excel-file)).
3. **Run it**:
   ```bash
   ./gradlew uploadToFigma
   ```
4. **Check the results**: open `figma-visual-testing/figma_baseline_input_output.xlsx`
   — it has the same rows, plus a `Status` column and a `Baseline Batch URL` link
   to each uploaded baseline in Applitools.

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
Copy [figma-visual-testing/config.properties.example](figma-visual-testing/config.properties.example)
to `figma-visual-testing/config.properties` (this exact file is gitignored, so your
tokens never get committed) and fill it in:

```properties
FIGMA_TOKEN=figd_xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx
APPLITOOLS_API_KEY=your-applitools-api-key
APPLITOOLS_SERVER_URL=https://eyesapi.applitools.com
APP_NAME=Applitools-Images
FIGMA_CACHE_DIR=downloaded_images/figma-cache
```

Any of these can instead be set as an **environment variable** of the same name
(e.g. `export FIGMA_TOKEN=...`) — an env var always overrides the value in
`config.properties`. This is useful for CI.

## 2. Prepare the input Excel file

Copy [figma-visual-testing/figma_baseline_input_template.xlsx](figma-visual-testing/figma_baseline_input_template.xlsx)
to `figma-visual-testing/figma_baseline_input.xlsx` (the default filename the
program looks for) and fill in one row per page/component to validate.

| Column | Required? | Description |
|---|---|---|
| `Figma URL` | **Yes** | A Figma share link to a specific frame/component, e.g. right-click a frame in Figma → *Copy link to selection*. Must contain a `node-id`. |
| `UAT/Prod URL` | No (used later by `compareWithFigma`) | The corresponding UAT/production URL to compare against. Not used by this program, but keep it here so the same row can be reused later. |
| `Test Name` | No | Overrides the auto-derived test name. If left blank, it's derived from the Figma node's name (sanitized to letters/digits/`-`/`_`). |
| `Viewport` | No | Overrides the viewport size, format `WIDTHxHEIGHT` (e.g. `1280x1024`). If left blank, it's derived from the downloaded image's pixel dimensions. |
| `Scale` | No | Figma export scale, e.g. `1`, `2`, `3`. Defaults to `1` if blank. |
| `Format` | No | Figma export format: `png`, `jpg`, `svg`, `pdf`. Defaults to `png` if blank. |

Only `Figma URL` is required — everything else can be left blank and the program
will fill in sensible defaults.

## 3. Run the program

From the project root:

```bash
./gradlew uploadToFigma
```

This defaults to reading `figma-visual-testing/figma_baseline_input.xlsx`. To use
a different file or force a fresh download of every image, pass properties:

```bash
./gradlew uploadToFigma -PinputExcel=path/to/file.xlsx -PforceRefresh=true
```

**Parameters:**

- `-PinputExcel=<path>` — path to the input Excel file. Defaults to
  `figma-visual-testing/figma_baseline_input.xlsx`.
- `-PforceRefresh=true` — re-downloads every Figma image even if a cached copy
  already exists (useful when the Figma design has changed); defaults to `false`,
  which reuses any cached image already in `FIGMA_CACHE_DIR`.

Alternatively, run it directly from your IDE (IntelliJ/VS Code): open
`UploadToFigma.java` and run its `main` method with Program Arguments set to
`[inputExcelPath] [forceRefresh]` (both optional).

## 4. Check the results

- Downloaded Figma images are cached under `downloaded_images/figma-cache/`
  (configurable via `FIGMA_CACHE_DIR`), named `{fileKey}_{nodeId}_{scale}x.{format}`.
- An output file is written alongside the input, named
  `<input-file-name>_output.xlsx`, containing all the original columns plus:
  `App Name`, `Baseline Env Name`, `Baseline Batch URL`, `Status`, `Error Message`.
- Any row where `Status` is `Failed` will have a message in `Error Message` — check
  the console output for the full stack trace.
- On success, `Baseline Batch URL` links directly to the uploaded baseline in the
  Applitools dashboard.
- The console also prints a one-line summary at the end, e.g. `4 of 5 succeeded.`

## Notes

- Re-running with the same `Figma URL` and no `forceRefresh` reuses the cached
  image — it will not re-hit the Figma API, but it *will* re-upload to Applitools
  and create a new baseline test each run (this program always calls
  `saveNewTests(true)`).
- A Figma URL without a `node-id` (i.e. a link to a whole file/page rather than a
  specific frame) is not currently supported — use *Copy link to selection* on the
  specific frame/component in Figma.
