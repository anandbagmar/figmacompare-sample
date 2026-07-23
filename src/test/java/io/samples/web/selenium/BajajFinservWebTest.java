package io.samples.web.selenium;

import java.util.List;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.AfterSuite;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.BeforeSuite;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import com.applitools.eyes.AccessibilityGuidelinesVersion;
import com.applitools.eyes.AccessibilityLevel;
import com.applitools.eyes.AccessibilitySettings;
import com.applitools.eyes.BatchInfo;
import com.applitools.eyes.MatchLevel;
import com.applitools.eyes.RectangleSize;
import com.applitools.eyes.StdoutLogHandler;
import com.applitools.eyes.selenium.BrowserType;
import com.applitools.eyes.selenium.Configuration;
import com.applitools.eyes.selenium.Eyes;
import com.applitools.eyes.selenium.StitchMode;
import com.applitools.eyes.selenium.fluent.Target;
import com.applitools.eyes.visualgrid.services.RunnerOptions;
import com.applitools.eyes.visualgrid.services.VisualGridRunner;

import io.samples.eyes.BatchSupport;
import io.samples.eyes.ComparisonResultRecorder;
import io.samples.excel.CompareRows;
import io.samples.excel.ExcelHelper;
import io.samples.excel.FigmaRow;

/**
 * Web path of the compareWithFigma step described in README_FigmaVisualValidation.md:
 * for every "Web" platform row in the compare input Excel, opens "App URL / Screen Name"
 * with Selenium and compares it against the Figma baseline uploaded by uploadToFigma,
 * using "Locator" (if present) to scope the check to a single component instead of the
 * full page. Results (Comparison Batch URL, Validation Status) are written back to an
 * output Excel next to the input, plus a pass/fail summary.
 *
 * This class is the template for any generic web comparison: unlike mobile, a single
 * data-driven test can handle every web row, since Selenium can navigate to any URL
 * directly (see BajajFinservAndroidTest for why mobile needs one test per app instead).
 */
public class BajajFinservWebTest {

    private static final String DEFAULT_APP_NAME = "Applitools-Images";
    private static final RectangleSize DEFAULT_VIEWPORT = new RectangleSize(1280, 1024);
    private static final String DEFAULT_COMPARE_INPUT_PATH = "figma-visual-testing/figma_compare_input.xlsx";
    private static final String COMPARE_EXCEL_PROPERTY = "compareExcel";

    private static final String userName = System.getProperty("user.name");
    private static final String APPLITOOLS_API_KEY = System.getenv("APPLITOOLS_API_KEY");

    private static String compareExcelPath;
    private static List<FigmaRow> allRows;

    private WebDriver driver;

    @BeforeSuite
    public static void beforeSuite() {
    }

    @AfterSuite
    public static void afterSuite() {
        if (null == allRows || allRows.isEmpty()) {
            return;
        }
        CompareRows.writeResultsAndSummary(compareExcelPath, allRows);
    }

    @DataProvider(name = "webRows")
    public static Object[][] webRows() {
        compareExcelPath = CompareRows.resolveExcelPath(COMPARE_EXCEL_PROPERTY, DEFAULT_COMPARE_INPUT_PATH);
        allRows = ExcelHelper.readRows(compareExcelPath);
        List<FigmaRow> webRows = CompareRows.filterByPlatform(allRows, "web");

        Object[][] data = new Object[webRows.size()][1];
        for (int i = 0; i < webRows.size(); i++) {
            data[i][0] = webRows.get(i);
        }
        return data;
    }

    @BeforeMethod
    public void beforeMethod() {
        driver = Driver.createDriverFor("chrome");
    }

    @AfterMethod
    public void afterMethod() {
        if (null != driver) {
            driver.quit();
        }
    }

    private VisualGridRunner initVisualGridRunner() {
        VisualGridRunner visualGridRunner = new VisualGridRunner(new RunnerOptions().testConcurrency(10));
        visualGridRunner.setDontCloseBatches(true);
        return visualGridRunner;
    }

    private Eyes initialiseEyes(VisualGridRunner visualGridRunner, BatchInfo batch, String appName,
            String baselineName, RectangleSize viewportSize) {
        Eyes eyes = new Eyes(visualGridRunner);
        Configuration config = new Configuration();
        config.setHostOS(System.getProperty("os.name"));
        config.setAppName(appName);
        config.setBaselineEnvName(baselineName);
        config.setApiKey(APPLITOOLS_API_KEY);
        config.setBatch(batch);
        config.setIsDisabled(Boolean.FALSE);
        config.setForceFullPageScreenshot(true);
        config.setStitchMode(StitchMode.CSS);
        config.setSaveNewTests(Boolean.FALSE);
        config.setMatchLevel(MatchLevel.STRICT);
        config.addProperty("username", userName);
        config.setIgnoreDisplacements(true);
        config.setAccessibilityValidation(
                new AccessibilitySettings(AccessibilityLevel.AA, AccessibilityGuidelinesVersion.WCAG_2_1));

        // Match the Figma baseline's viewport size, so the Visual Grid renders the
        // checkpoint at the same size instead of a fixed default.
        config.addBrowser(viewportSize.getWidth(), viewportSize.getHeight(), BrowserType.CHROME);

        eyes.setConfiguration(config);
        eyes.setLogHandler(new StdoutLogHandler(true));

        return eyes;
    }

    @Test(dataProvider = "webRows")
    void compareWebRowWithFigmaBaseline(FigmaRow row) {
        String appName = isBlank(row.appName) ? DEFAULT_APP_NAME : row.appName;
        String testName = isBlank(row.testName) ? row.appUrlOrScreenName : row.testName;
        String baselineName = isBlank(row.baselineEnvName) ? testName + "-baseline" : row.baselineEnvName;
        RectangleSize viewportSize = ExcelHelper.parseViewport(row.viewport);
        if (null == viewportSize) {
            viewportSize = DEFAULT_VIEWPORT;
        }

        VisualGridRunner visualGridRunner = initVisualGridRunner();
        BatchInfo batchInfo = BatchSupport.createBatch(appName, userName);
        Eyes eyesSelenium = initialiseEyes(visualGridRunner, batchInfo, appName, baselineName, viewportSize);
        try {
            driver.get(row.appUrlOrScreenName);
            eyesSelenium.open(driver, appName, testName, viewportSize);
            if (isBlank(row.locator)) {
                eyesSelenium.check(testName, Target.window());
            } else {
                eyesSelenium.check(testName, Target.region(parseLocator(row.locator)));
            }
            eyesSelenium.closeAsync();

            boolean isPass = ComparisonResultRecorder.recordAndCheckPass(row,
                    visualGridRunner.getAllTestResults(false));
            BatchSupport.closeBatch(batchInfo);
            visualGridRunner.close();
            Assert.assertTrue(isPass, "Visual differences found for: " + row.appUrlOrScreenName);
        } catch (RuntimeException ex) {
            row.validationStatus = "Failed";
            row.errorMessage = ex.getMessage();
            BatchSupport.closeBatch(batchInfo);
            eyesSelenium.abortIfNotClosed();
            throw ex;
        }
    }

    private static By parseLocator(String locator) {
        String trimmed = locator.trim();
        return trimmed.startsWith("/") ? By.xpath(trimmed) : By.cssSelector(trimmed);
    }

    private static boolean isBlank(String value) {
        return null == value || value.isBlank();
    }
}
