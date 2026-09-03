Back to main [README](../README.md)

# uploadFromFigma — Figma → Applitools baseline uploader

This page covers **this repo's own setup** for running `uploadFromFigma`. For what
the program actually does - the Excel column schema, `saveNewTests` behavior,
rate-limiting, `cleanFigmaExcel` - see figmacompare's own docs:
[UploadFromFigma.md](https://github.com/anandbagmar/figmacompare/blob/main/docs/UploadFromFigma.md) ·
[ExcelSchema.md](https://github.com/anandbagmar/figmacompare/blob/main/docs/ExcelSchema.md) ·
[Configuration.md](https://github.com/anandbagmar/figmacompare/blob/main/docs/Configuration.md).

This is step 2 of the full workflow — see
[README_FigmaVisualValidation.md](README_FigmaVisualValidation.md) for how this fits
together with the manual review steps and the `compareWithFigma` comparison programs.

## Table of contents

- [Quick start](#quick-start)
- [1. One-time setup](#1-one-time-setup)
  - [1.0 Get access to figmacompare](#10-get-access-to-figmacompare)
  - [1.1 Fill in `config.properties`](#11-fill-in-configproperties)
- [2. Fill in the Figma Excel file](#2-fill-in-the-figma-excel-file)
- [3. Run the program](#3-run-the-program)
- [4. Check the results / clean up](#4-check-the-results--clean-up)

## Quick start

Templates live in **[figma-visual-testing/templates/](../figma-visual-testing/templates/)**
and are reference-only — never edit them directly. Copy them one level up, into
**figma-visual-testing/**, to create your actual working files.

**macOS / Linux:**
```bash
cp figma-visual-testing/templates/config.properties.example figma-visual-testing/config.properties
cp figma-visual-testing/templates/figma_visual_tests_template.xlsx figma-visual-testing/figma_visual_tests.xlsx
```

**Windows (Command Prompt):**
```
copy figma-visual-testing\templates\config.properties.example figma-visual-testing\config.properties
copy figma-visual-testing\templates\figma_visual_tests_template.xlsx figma-visual-testing\figma_visual_tests.xlsx
```

**Windows (PowerShell):**
```powershell
Copy-Item figma-visual-testing\templates\config.properties.example figma-visual-testing\config.properties
Copy-Item figma-visual-testing\templates\figma_visual_tests_template.xlsx figma-visual-testing\figma_visual_tests.xlsx
```

1. Fill in `figma-visual-testing/config.properties` — `FIGMA_TOKEN`, `APPLITOOLS_API_KEY`,
   `APPLITOOLS_SERVER_URL` (gitignored, so tokens never get committed).
2. Fill in `figma-visual-testing/figma_visual_tests.xlsx` — one row per Figma design
   (see figmacompare's [ExcelSchema.md](https://github.com/anandbagmar/figmacompare/blob/main/docs/ExcelSchema.md)
   for the full column reference).
3. Run it (macOS/Linux: `./gradlew uploadFromFigma`; Windows: `gradlew.bat uploadFromFigma`):
   ```bash
   ./gradlew uploadFromFigma
   ```
4. Re-open the Excel file — it now has `Status`/`Baseline Batch URL` filled in.

> **Windows note:** every `./gradlew ...` command on this page works the same way on
> Windows — just run `gradlew.bat ...` instead, from Command Prompt or PowerShell.

## 1. One-time setup

### 1.0 Get access to figmacompare

This repo depends on `com.github.anandbagmar:figmacompare`, resolved via
[JitPack](https://jitpack.io), which builds directly from the (public)
[figmacompare](https://github.com/anandbagmar/figmacompare) repo's tags on demand —
**no token needed**. `build.gradle` tries two sources, in order: `mavenLocal()` (your
local `~/.m2` cache), then JitPack.

**Just running the tests:** no setup needed - just run:
```bash
./gradlew compareWebWithFigma   # or any other task
```
With no `-PfigmacompareVersion=...` given, `build.gradle` automatically resolves the
latest release via `scripts/latest-figmacompare-version.sh`. Pass
`-PfigmacompareVersion=vX.Y.Z` (include the `v` - JitPack needs the exact tag) to pin
a specific release instead.

**Actively iterating on a figmacompare change:** publish it to your local `~/.m2`
cache, which `mavenLocal()` picks up automatically, then point this repo at that
version - no `build.gradle` edit needed:

**macOS / Linux:**
```bash
cd /path/to/figmacompare
./gradlew publishToMavenLocal
# prints the exact coordinate published and the -PfigmacompareVersion to use

cd /path/to/figmacompare-sample
./gradlew compareWebWithFigma -PfigmacompareVersion=0.0.1-local
```

**Windows (Command Prompt or PowerShell):**
```
cd C:\path\to\figmacompare
gradlew.bat publishToMavenLocal
REM prints the exact coordinate published and the -PfigmacompareVersion to use

cd C:\path\to\figmacompare-sample
gradlew.bat compareWebWithFigma -PfigmacompareVersion=0.0.1-local
```

### 1.1 Fill in `config.properties`

Copy [figma-visual-testing/templates/config.properties.example](../figma-visual-testing/templates/config.properties.example)
to `figma-visual-testing/config.properties` (gitignored):

**macOS / Linux:**
```bash
cp figma-visual-testing/templates/config.properties.example figma-visual-testing/config.properties
```

**Windows (Command Prompt):**
```
copy figma-visual-testing\templates\config.properties.example figma-visual-testing\config.properties
```

**Windows (PowerShell):**
```powershell
Copy-Item figma-visual-testing\templates\config.properties.example figma-visual-testing\config.properties
```

See figmacompare's [Configuration.md](https://github.com/anandbagmar/figmacompare/blob/main/docs/Configuration.md)
for what each property means, where to get `FIGMA_TOKEN`/`APPLITOOLS_API_KEY`, and
which ones can instead be set as environment variables (useful for CI - see this
repo's own [Continuous Integration](../README.md#continuous-integration) section).

## 2. Fill in the Figma Excel file

Copy [figma-visual-testing/templates/figma_visual_tests_template.xlsx](../figma-visual-testing/templates/figma_visual_tests_template.xlsx)
to `figma-visual-testing/figma_visual_tests.xlsx` (the default filename both
`uploadFromFigma` and `compareWithFigma` look for - this repo's own working file is
actually `figma_mockede2e_web.xlsx`, set via `FIGMA_EXCEL_FILE` in
`config.properties` - see [Choosing the Excel file path](#3-run-the-program) below):

**macOS / Linux:**
```bash
cp figma-visual-testing/templates/figma_visual_tests_template.xlsx figma-visual-testing/figma_visual_tests.xlsx
```

**Windows (Command Prompt):**
```
copy figma-visual-testing\templates\figma_visual_tests_template.xlsx figma-visual-testing\figma_visual_tests.xlsx
```

**Windows (PowerShell):**
```powershell
Copy-Item figma-visual-testing\templates\figma_visual_tests_template.xlsx figma-visual-testing\figma_visual_tests.xlsx
```

See figmacompare's [ExcelSchema.md](https://github.com/anandbagmar/figmacompare/blob/main/docs/ExcelSchema.md)
for the full column reference — which are required, which auto-derive, and which are
"sticky" once a run writes them.

## 3. Run the program

From the project root:

```bash
./gradlew uploadFromFigma
```

By default, this resolves the Excel file path in priority order: `-PfigmaExcel=<path>`,
then `FIGMA_EXCEL_FILE` in `config.properties`/env, then the built-in default. Common
overrides:

```bash
./gradlew uploadFromFigma -PfigmaExcel=figma-visual-testing/web_project_a.xlsx -PforceRefresh=true -Pplatform=Web
```

See figmacompare's [UploadFromFigma.md](https://github.com/anandbagmar/figmacompare/blob/main/docs/UploadFromFigma.md)
for what `forceRefresh`/`platform` do and what gets written back.

Alternatively, run it directly from your IDE: open `UploadFromFigma.java` (in
figmacompare) and run its `main` method with Program Arguments set to
`[figmaExcelPath] [forceRefresh] [platform]` (all optional).

## 4. Check the results / clean up

Re-open the Excel file - `Status`/`Baseline Batch URL`/etc. are filled in (see
figmacompare's [ExcelSchema.md](https://github.com/anandbagmar/figmacompare/blob/main/docs/ExcelSchema.md)
for the full write-back reference). The console also prints a one-line summary, e.g.
`4 of 5 succeeded.`

To reset those result columns for a clean slate:
```bash
./gradlew cleanFigmaExcel
```
See figmacompare's [UploadFromFigma.md](https://github.com/anandbagmar/figmacompare/blob/main/docs/UploadFromFigma.md#cleanfigmaexcel---resetting-results)
for exactly what this does (and doesn't) touch.
