Back to main [README](../README.md)

# Running web tests with Selenium

For `compareWebWithFigma`'s actual behavior (scenario grouping, viewport matching,
`Locator`, headless/browser options), see figmacompare's
[CompareWithFigma.md](https://github.com/anandbagmar/figmacompare/blob/main/docs/CompareWithFigma.md).
This page covers this repo's own example test files and local run setup.

## Table of contents

- [Usage](#usage)
  - [Set your APPLITOOLS_API_KEY](#set-your-applitools_api_key)
  - [Launch the tests](#launch-the-tests)
- [Example Test Source Files](#example-test-source-files)

## Usage

### Set your APPLITOOLS_API_KEY

First,
[retrieve your API key](https://applitools.com/docs/topics/overview/obtain-api-key.html)
for your user account from the Eyes Dashboard UI, and set it as an environment variable —
every test class in this project reads it via `System.getenv("APPLITOOLS_API_KEY")`:

```bash
export APPLITOOLS_API_KEY=<your-api-key>
```

### Launch the tests

```bash
./gradlew test -PtestClass=<fully.qualified.TestClassName>
```

Or run a specific test class directly from your IDE (IntelliJ/Eclipse) by right-clicking the
class and choosing **Run**.

## Example Test Source Files

* [WebFigmaTest.java](../src/test/java/io/eot/bajajfinserv/web/selenium/WebFigmaTest.java) —
  uploads a locally-stored Figma export as the visual baseline via
  `Baseline.java` (in figmacompare), then opens the corresponding
  page with Selenium and runs a full-page Applitools Eyes comparison against it
  ```bash
  ./gradlew test -PtestClass=io.eot.bajajfinserv.web.selenium.WebFigmaTest
  ```

* `WebCompareRunner` (in figmacompare) +
  [CompareWebWithFigmaTest.java](../src/test/java/io/eot/pipeline/web/selenium/CompareWebWithFigmaTest.java)
  (thin TestNG shim, in this repo) — together the **web path of `compareWithFigma`**,
  data-driven from the shared Figma Excel file. See figmacompare's
  [CompareWithFigma.md](https://github.com/anandbagmar/figmacompare/blob/main/docs/CompareWithFigma.md)
  for how it behaves.
  ```bash
  ./gradlew compareWebWithFigma
  # or against a specific file:
  ./gradlew compareWebWithFigma -PfigmaExcel=path/to/file.xlsx
  ```
  (`compareWebWithFigma` is a shortcut for
  `./gradlew test -PtestClass=io.eot.pipeline.web.selenium.CompareWebWithFigmaTest`,
  which still works too if you want it.)

Instead of manually placing an image under `downloaded_images/` and calling
`Baseline.uploadImageAndSetAsBaseline(...)` as `WebFigmaTest` currently does, you can
upload Figma designs in bulk via [uploadFromFigma](README_uploadFromFigma.md) — its
output Excel (with `Locator` filled in per row) is exactly what
`CompareWebWithFigmaTest`/`WebCompareRunner` expects as input.

Back to main [README](../README.md)
