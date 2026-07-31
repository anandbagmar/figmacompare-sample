# figmacompare-sample: Figma-to-Production Visual Validation

This project validates that a set of sample web and mobile apps (plus the real Bajaj
Finserv app) match their approved Figma designs, using
[Applitools Eyes](https://applitools.com/platform/eyes/) as the visual comparison
engine. The reusable framework — Excel/Figma/Eyes/Appium plumbing, the plain-Java
`compareWithFigma` runners — lives in a separate repo, `figmacompare`
(`io.eot:figmacompare`, built and published to `mavenLocal` from there); this repo is a
client of it, holding the scenario providers, Excel file, and thin TestNG shims for
several sample apps (Bajaj Finserv, Calculator, App Automation Playground, Applitools'
own Hello World demo). The workflow:

1. **Upload Figma designs as baselines** — the [uploadFromFigma](docs/README_uploadFromFigma.md)
   utility reads a list of Figma share links from an Excel file, downloads each design,
   and uploads it to Applitools Eyes as the approved visual baseline. Pure Java, no
   browser/device needed.
2. **Capture the real implementation** — [Selenium](docs/README_Web_Selenium.md) drives
   the web (UAT/production) pages, and [Appium](docs/README_Appium_Java.md) drives the
   native Android/iOS apps, to capture the corresponding screens.
3. **Compare** — the [Eyes Selenium Java SDK](https://applitools.com/docs/api-ref/category/selenium-java)
   and [Eyes Appium Java SDK](https://applitools.com/docs/api-ref/category/appium-java)
   run the visual comparison against the Figma baseline and report differences in the
   Applitools dashboard.

This lets the UI/UX team (who own the Figma designs) and the QA team (who own the
Selenium/Appium automation) collaborate on catching design-vs-implementation drift,
without either side needing to touch the other's tooling.

## Table of contents

- [Full step-by-step workflow: Figma → baseline → compare → review](#full-step-by-step-workflow-figma--baseline--compare--review)
- [Setup instructions](#setup-instructions)
- [Running the Appium (Android/iOS) tests](#running-the-appium-androidios-tests)
- [Running the web tests with Selenium](#running-the-web-tests-with-selenium)
- [Uploading Figma designs as Applitools baselines](#uploading-figma-designs-as-applitools-baselines)
- [Continuous Integration](#continuous-integration)

# [Full step-by-step workflow: Figma → baseline → compare → review](docs/README_FigmaVisualValidation.md)

# [Setup instructions](docs/README_MachineSetupInstructions.md)

# Running the [Appium (Android/iOS) tests](docs/README_Appium_Java.md)

# Running the [web tests with Selenium](docs/README_Web_Selenium.md)

# Uploading [Figma designs as Applitools baselines](docs/README_uploadFromFigma.md)

## Continuous Integration

`.github/workflows/gradle.yml` runs on every push/PR to `main`:

1. **Build with Gradle** — compiles and resolves `io.eot:figmacompare` from GitHub
   Packages (needs `FIGMACOMPARE_PAT`, since that repo is private). Doesn't run any
   tests itself (`-x test`).
2. **Restore CI Figma Excel file from secret** — decodes the `FIGMA_CI_EXCEL_B64`
   secret into `figma-visual-testing/figma_mockede2e_web_ci.xlsx`.
3. **Run web Figma visual tests** — runs `compareWebWithFigma` in headless Chrome
   (`HEADLESS=true`) against that file, using `APPLITOOLS_API_KEY` to authenticate
   with Eyes.

`uploadFromFigma` is **not** run in CI — it's a deliberate, manual step (baselines are
uploaded once, then compared against repeatedly), so `FIGMA_TOKEN` isn't needed as a
CI secret. Android/iOS `compare*WithFigma` also aren't wired into CI yet (would need an
emulator/device farm in the runner).

### Required repo secrets (Settings → Secrets and variables → Actions)

| Secret | Used for |
|---|---|
| `FIGMACOMPARE_PAT` | Resolving `io.eot:figmacompare` from GitHub Packages |
| `APPLITOOLS_API_KEY` | Authenticating with Applitools Eyes |
| `FIGMA_CI_EXCEL_B64` | Base64 of the CI-scoped Figma Excel file (see below) |

### Generating `FIGMA_CI_EXCEL_B64`

`figma-visual-testing/*.xlsx` is gitignored — it holds your own working data,
including run results. CI instead uses a **separate, CI-scoped** file containing only
the `Web`-platform rows with the result columns (`Status`, `Baseline Batch URL`, etc.)
left blank, so nothing stale gets committed or reused across runs. This file is not
committed either — it's stored as a base64-encoded secret and reconstructed at the
start of each CI run:

```bash
base64 -i figma-visual-testing/figma_mockede2e_web_ci.xlsx | pbcopy
```

Paste the clipboard contents as the `FIGMA_CI_EXCEL_B64` secret's value. Regenerate
and re-set the secret whenever the `Web` rows change (new scenario step, new
component locator, etc.).

