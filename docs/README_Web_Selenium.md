Back to main [README](../README.md)

# Running web tests with Selenium

## Usage

These tests drive a browser with Selenium and use the Applitools Eyes Selenium SDK
(Ultrafast Grid) to run visual checks across multiple browsers/viewports from a single
local browser session.

### Set your APPLITOOLS_API_KEY

First,
[retrieve your API key](https://applitools.com/docs/topics/overview/obtain-api-key.html)
for your user account from the Eyes Dashboard UI, and set it as an environment variable —
every test class in this project reads it via `System.getenv("APPLITOOLS_API_KEY")`:

```bash
export APPLITOOLS_API_KEY=<your-api-key>
```

### Choose a browser

[Driver.java](../core/src/main/java/io/eot/figmacompare/web/selenium/Driver.java) creates a local
`WebDriver` for Chrome, Firefox, Edge, or Safari. It defaults to Chrome, or reads the
`BROWSER` environment variable if set:

```bash
export BROWSER=chrome   # or firefox, edge, safari
```

Make sure the corresponding browser is installed locally — Selenium Manager will resolve
the matching driver automatically.

### Launch the tests

```bash
./gradlew test -PtestClass=<fully.qualified.TestClassName>
```

Or run a specific test class directly from your IDE (IntelliJ/Eclipse) by right-clicking the
class and choosing **Run**.

## Example Test Source Files

* [WebFigmaTest.java](../samples/src/test/java/io/eot/bajajfinserv/web/selenium/WebFigmaTest.java) —
  uploads a locally-stored Figma export as the visual baseline via
  [Baseline.java](../core/src/main/java/io/eot/figmacompare/Baseline.java), then opens the corresponding
  page with Selenium and runs a full-page Applitools Eyes comparison against it
  ```bash
  ./gradlew test -PtestClass=io.eot.bajajfinserv.web.selenium.WebFigmaTest
  ```

* [WebCompareRunner.java](../core/src/main/java/io/eot/figmacompare/web/selenium/WebCompareRunner.java)
  (plain-Java orchestration, in `core`) +
  [CompareWebWithFigmaTest.java](../samples/src/test/java/io/eot/bajajfinserv/web/selenium/CompareWebWithFigmaTest.java)
  (thin TestNG shim, in `samples`) — together the **web path of `compareWithFigma`** (see
  [README_FigmaVisualValidation.md](../README_FigmaVisualValidation.md)). This is not a
  fixed single-page test: it's data-driven from the shared Figma Excel file (default
  `figma-visual-testing/figma_visual_tests.xlsx`, override with `-PfigmaExcel=<path>`),
  one TestNG invocation per group of non-`Skip` `Platform=Web` rows — a group is either
  one standalone row, or several consecutive rows sharing the same `Scenario Name`. For
  each row/step in the group it opens `App URL / Screen Name` with Selenium (in one
  continuous browser session for a scenario) and submits an Applitools Eyes comparison
  against the group's `Baseline Env Name` baseline (full page if `Locator` is blank,
  otherwise just that CSS/XPath-selected region).
  ```bash
  ./gradlew compareWebWithFigma
  # or against a specific file:
  ./gradlew compareWebWithFigma -PfigmaExcel=path/to/file.xlsx
  ```
  (`compareWebWithFigma` is a shortcut for
  `./gradlew test -PtestClass=io.eot.bajajfinserv.web.selenium.CompareWebWithFigmaTest`,
  which still works too if you want it.)

  One `VisualGridRunner` and one `BatchInfo` are shared across every group for the whole
  run — creating a new one per group would repeatedly start/stop the Ultrafast Grid's
  background process and hang after the first one. Because results from a shared
  runner are only available once every submitted check has finished, groups just
  submit their checks via `closeAsync()`; the actual pass/fail, `Comparison Batch
  URL`, and `Validation Status` are collected once in `@AfterSuite` (matched back to
  each group's rows by test/scenario name), written to the Excel file, and the suite
  fails there if any group had a visual difference.

`WebFigmaTest` configures Eyes via its own per-test `VisualGridRunner` (it only ever
runs one check, so that's fine); `WebCompareRunner` configures browsers/viewports
via `config.addBrowser(...)` inside its shared `initialiseEyes(...)` method.

### Uploading a Figma design as the baseline

Instead of manually placing an image under `downloaded_images/` and calling
`Baseline.uploadImageAndSetAsBaseline(...)` as `WebFigmaTest` currently does, you can
upload Figma designs in bulk via the
[uploadFromFigma](../README_uploadFromFigma.md) utility — its output Excel (with `Locator`
filled in per row) is exactly what `CompareWebWithFigmaTest`/`WebCompareRunner` expects as input.

Back to main [README](../README.md)
