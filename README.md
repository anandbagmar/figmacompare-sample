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

# [Full step-by-step workflow: Figma → baseline → compare → review](docs/README_FigmaVisualValidation.md)

# [Setup instructions](docs/README_MachineSetupInstructions.md)

# Running the [Appium (Android/iOS) tests](docs/README_Appium_Java.md)

# Running the [web tests with Selenium](docs/README_Web_Selenium.md)

# Uploading [Figma designs as Applitools baselines](docs/README_uploadFromFigma.md)

