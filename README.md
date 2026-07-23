# Bajaj Finserv Figma-to-Production Visual Validation POC

This project validates that Bajaj Finserv's live web and mobile applications match
their approved Figma designs, using [Applitools Eyes](https://applitools.com/platform/eyes/)
as the visual comparison engine. The workflow:

1. **Upload Figma designs as baselines** — the [uploadToFigma](README_uploadToFigma.md)
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

# [Setup instructions](docs/README_MachineSetupInstructions.md)

# Running the [Appium (Android/iOS) tests](docs/README_Appium_Java.md)

# Running the [web tests with Selenium](docs/README_Web_Selenium.md)

# Running the [Appium-Java tests in local Jenkins](docs/README_Jenkins.md)

# Uploading [Figma designs as Applitools baselines](README_uploadToFigma.md)

