package io.samples.web.selenium;

import java.io.File;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

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
import com.applitools.eyes.TestResults;
import com.applitools.eyes.TestResultsStatus;
import com.applitools.eyes.TestResultsSummary;
import com.applitools.eyes.selenium.BrowserType;
import com.applitools.eyes.selenium.Configuration;
import com.applitools.eyes.selenium.Eyes;
import com.applitools.eyes.selenium.StitchMode;
import com.applitools.eyes.selenium.fluent.Target;
import com.applitools.eyes.visualgrid.services.RunnerOptions;
import com.applitools.eyes.visualgrid.services.VisualGridRunner;

import io.samples.excel.ExcelHelper;
import io.samples.excel.FigmaRow;

import static io.samples.EyesResults.displayVisualValidationResults;

/**
 * Web path of the compareWithFigma step described in README_FigmaVisualValidation.md:
 * for every "Web" platform row in the compare input Excel, opens "App URL / Screen Name"
 * with Selenium and compares it against the Figma baseline uploaded by uploadToFigma,
 * using "Locator" (if present) to scope the check to a single component instead of the
 * full page. Results (Comparison Batch URL, Validation Status) are written back to an
 * output Excel next to the input, plus a pass/fail summary.
 */
public class BajajFinservWebTest {

    private static final String DEFAULT_APP_NAME = "Applitools-Images";
    private static final RectangleSize DEFAULT_VIEWPORT = new RectangleSize(1280, 1024);
    private static final String DEFAULT_COMPARE_INPUT_PATH = "figma-visual-testing"
            + File.separator + "figma_compare_input.xlsx";

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
        String outputPath = ExcelHelper.deriveOutputPath(compareExcelPath);
        ExcelHelper.writeRows(compareExcelPath, allRows, outputPath);
        long passed = allRows.stream().filter(row -> "Passed".equals(row.validationStatus)).count();
        System.out.println();
        System.out.println(passed + " of " + allRows.size() + " row(s) passed. Results written to " + outputPath);
    }

    @DataProvider(name = "webRows")
    public static Object[][] webRows() {
        compareExcelPath = System.getProperty("compareExcel", DEFAULT_COMPARE_INPUT_PATH);
        if (!new File(compareExcelPath).exists()) {
            throw new IllegalStateException("Compare input Excel file not found: " + compareExcelPath
                    + ". Run uploadToFigma first, fill in the Locator column for each row you want scoped to a "
                    + "component, save the file at this path, and re-run (or pass -DcompareExcel=<path>).");
        }
        allRows = ExcelHelper.readRows(compareExcelPath);
        List<FigmaRow> webRows = allRows.stream()
                .filter(row -> "web".equalsIgnoreCase(row.platform))
                .collect(Collectors.toList());

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

    private BatchInfo initBatchInfo(String appName) {
        BatchInfo batch = new BatchInfo(userName + "-" + appName);
        batch.setNotifyOnCompletion(false);
        batch.addProperty("REPOSITORY_NAME", new File(System.getProperty("user.dir")).getName());
        batch.addProperty("APP_NAME", appName);
        return batch;
    }

    private void closeBatch(BatchInfo batch) {
        if (null != batch) {
            batch.setCompleted(true);
        }
    }

    private Eyes initialiseEyes(VisualGridRunner visualGridRunner, BatchInfo batch, String appName,
            String baselineName) {
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

        config.addBrowser(1280, 1024, BrowserType.CHROME);

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
        BatchInfo batchInfo = initBatchInfo(appName);
        Eyes eyesSelenium = initialiseEyes(visualGridRunner, batchInfo, appName, baselineName);
        try {
            driver.get(row.appUrlOrScreenName);
            eyesSelenium.open(driver, appName, testName, viewportSize);
            if (isBlank(row.locator)) {
                eyesSelenium.check(testName, Target.window());
            } else {
                eyesSelenium.check(testName, Target.region(parseLocator(row.locator)));
            }
            eyesSelenium.closeAsync();

            AtomicBoolean isPass = new AtomicBoolean(true);
            TestResultsSummary allTestResults = visualGridRunner.getAllTestResults(false);
            allTestResults.forEach(testResultContainer -> {
                TestResults testResults = testResultContainer.getTestResults();
                System.out.printf("Test: %s%n%s%n", testResults.getName(), testResultContainer);
                displayVisualValidationResults(testResults);
                row.comparisonBatchUrl = testResults.getUrl();
                row.validationStatus = testResults.getStatus().toString();
                TestResultsStatus status = testResults.getStatus();
                if (status.equals(TestResultsStatus.Failed) || status.equals(TestResultsStatus.Unresolved)) {
                    isPass.set(false);
                }
            });
            closeBatch(batchInfo);
            visualGridRunner.close();
            Assert.assertTrue(isPass.get(), "Visual differences found for: " + row.appUrlOrScreenName);
        } catch (RuntimeException ex) {
            row.validationStatus = "Failed";
            row.errorMessage = ex.getMessage();
            closeBatch(batchInfo);
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
