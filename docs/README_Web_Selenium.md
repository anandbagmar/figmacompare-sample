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

[Driver.java](../src/test/java/io/samples/web/selenium/Driver.java) creates a local
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

* [WebFigmaTest.java](../src/test/java/io/samples/web/selenium/WebFigmaTest.java) —
  uploads a locally-stored Figma export as the visual baseline via
  [Baseline.java](../src/test/java/io/samples/Baseline.java), then opens the corresponding
  page with Selenium and runs a full-page Applitools Eyes comparison against it
  ```bash
  ./gradlew test -PtestClass=io.samples.web.selenium.WebFigmaTest
  ```

* [BajajFinservWebTest.java](../src/test/java/io/samples/web/selenium/BajajFinservWebTest.java) —
  opens the Bajaj Finserv Markets personal loan page with Selenium and runs an Applitools
  Eyes region comparison against a previously-uploaded baseline
  ```bash
  ./gradlew test -PtestClass=io.samples.web.selenium.BajajFinservWebTest
  ```

Both test classes configure Eyes via the Visual Grid runner (`VisualGridRunner`), so a
single local browser session fans out to every browser/viewport combination added in
`config.addBrowser(...)` inside each test's `initialiseEyes(...)` method.

### Uploading a Figma design as the baseline

Instead of manually placing an image under `downloaded_images/` and calling
`Baseline.uploadImageAndSetAsBaseline(...)` as `WebFigmaTest` currently does, you can
upload Figma designs in bulk via the
[uploadToFigma](../README_uploadToFigma.md) utility.

Back to main [README](../README.md)
