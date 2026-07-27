package io.samples.web.selenium;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

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
import com.applitools.eyes.TestResultsSummary;
import com.applitools.eyes.selenium.BrowserType;
import com.applitools.eyes.selenium.Configuration;
import com.applitools.eyes.selenium.Eyes;
import com.applitools.eyes.selenium.StitchMode;
import com.applitools.eyes.selenium.fluent.Target;
import com.applitools.eyes.visualgrid.services.RunnerOptions;
import com.applitools.eyes.visualgrid.services.VisualGridRunner;

import io.samples.eyes.BatchSupport;
import io.samples.eyes.ComparisonResultRecorder;
import io.samples.excel.ExcelHelper;
import io.samples.excel.FigmaExcelFile;
import io.samples.excel.FigmaRow;
import io.samples.excel.FigmaValidation;

/**
 * Web path of the compareWithFigma step described in README_FigmaVisualValidation.md:
 * for every "Web" platform row in the shared Figma Excel file, opens "App URL / Screen
 * Name" with Selenium and compares it against the Figma baseline uploaded by
 * uploadFromFigma, using "Locator" (if present) to scope the check to a single component
 * instead of the full page. Results (Comparison Batch URL, Validation Status) are
 * written back to the same file in place, plus a pass/fail summary. Rows with "Skip" set
 * are left untouched and not processed.
 *
 * Rows sharing the same non-blank "Scenario Name" (consecutively, in sheet order) run as
 * one multi-step test in a single continuous browser session: one eyes.open(), one
 * driver.get()+check() per row/step, one close(). A standalone row (blank Scenario Name)
 * is just a scenario of one step - there's a single code path for both cases, since
 * Selenium can navigate to any URL directly whether it's one step or several (unlike
 * mobile, see BajajFinservAndroidTest for why that needs one test per app instead).
 *
 * One VisualGridRunner and one BatchInfo are shared for the whole run (creating one per
 * group would repeatedly start/stop the Ultrafast Grid's background process and hang).
 * Because of that, groups only submit their checks via closeAsync() - actual results are
 * collected once, in @AfterSuite, and matched back to each group's rows by test name.
 */
public class BajajFinservWebTest {

    private static final String DEFAULT_APP_NAME = "Applitools-Images";
    private static final RectangleSize DEFAULT_VIEWPORT = new RectangleSize(1280, 1024);

    private static final String userName = System.getProperty("user.name");
    private static final String APPLITOOLS_API_KEY = System.getenv("APPLITOOLS_API_KEY");

    private static String figmaExcelPath;
    private static List<FigmaRow> allRows;
    private static List<List<FigmaRow>> webGroups;
    private static VisualGridRunner visualGridRunner;
    private static BatchInfo batchInfo;

    private WebDriver driver;

    @BeforeSuite
    public static void beforeSuite() {
        visualGridRunner = new VisualGridRunner(new RunnerOptions().testConcurrency(10));
        visualGridRunner.setDontCloseBatches(true);
        batchInfo = BatchSupport.createBatch(DEFAULT_APP_NAME, userName);
    }

    @AfterSuite
    public static void afterSuite() {
        try {
            if (null == webGroups || webGroups.isEmpty()) {
                return;
            }
            Map<String, List<FigmaRow>> rowsByTestName = new LinkedHashMap<>();
            for (List<FigmaRow> group : webGroups) {
                rowsByTestName.put(resolveScenarioTestName(group), group);
            }

            TestResultsSummary summary = visualGridRunner.getAllTestResults(false);
            boolean isPass = ComparisonResultRecorder.recordAndCheckPass(rowsByTestName, summary);

            ExcelHelper.writeRows(figmaExcelPath, allRows);
            long passed = allRows.stream().filter(row -> "Passed".equals(row.validationStatus)).count();
            System.out.println();
            System.out.println(passed + " of " + allRows.size() + " row(s) passed. Results written to "
                    + figmaExcelPath);

            Assert.assertTrue(isPass, "Visual differences found - see " + figmaExcelPath + " for details.");
        } finally {
            BatchSupport.closeBatch(batchInfo);
            visualGridRunner.close();
        }
    }

    @DataProvider(name = "webGroups")
    public static Object[][] webGroups() {
        figmaExcelPath = FigmaExcelFile.resolvePath(System.getProperty("figmaExcel"));
        allRows = ExcelHelper.readRows(figmaExcelPath);
        FigmaValidation.throwIfAny(FigmaValidation.validate(allRows));

        List<FigmaRow> webRows = FigmaExcelFile.filterByPlatform(allRows, "web");
        webGroups = FigmaExcelFile.groupContiguous(webRows);

        Object[][] data = new Object[webGroups.size()][1];
        for (int i = 0; i < webGroups.size(); i++) {
            data[i][0] = webGroups.get(i);
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

    private Eyes initialiseEyes(String appName, String baselineName, RectangleSize viewportSize) {
        Eyes eyes = new Eyes(visualGridRunner);
        Configuration config = new Configuration();
        config.setHostOS(System.getProperty("os.name"));
        config.setAppName(appName);
        config.setBaselineEnvName(baselineName);
        config.setApiKey(APPLITOOLS_API_KEY);
        config.setBatch(batchInfo);
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

    @Test(dataProvider = "webGroups")
    void compareWebGroupWithFigmaBaseline(List<FigmaRow> group) {
        FigmaRow firstRow = group.get(0);
        String appName = isBlank(firstRow.appName) ? DEFAULT_APP_NAME : firstRow.appName;
        String scenarioTestName = resolveScenarioTestName(group);
        String baselineName = isBlank(firstRow.baselineEnvName)
                ? scenarioTestName + "-baseline"
                : firstRow.baselineEnvName;
        RectangleSize viewportSize = ExcelHelper.parseViewport(firstRow.viewport);
        if (null == viewportSize) {
            viewportSize = DEFAULT_VIEWPORT;
        }

        Eyes eyesSelenium = initialiseEyes(appName, baselineName, viewportSize);
        try {
            eyesSelenium.open(driver, appName, scenarioTestName, viewportSize);
            for (FigmaRow row : group) {
                String stepName = resolveStepName(row);
                driver.get(row.appUrlOrScreenName);
                if (isBlank(row.locator)) {
                    eyesSelenium.check(stepName, Target.window());
                } else {
                    eyesSelenium.check(stepName, Target.region(parseLocator(row.locator)));
                }
            }
            eyesSelenium.closeAsync();
        } catch (RuntimeException ex) {
            for (FigmaRow row : group) {
                row.validationStatus = "Failed";
                row.errorMessage = ex.getMessage();
            }
            eyesSelenium.abortIfNotClosed();
            throw ex;
        }
    }

    private static String resolveScenarioTestName(List<FigmaRow> group) {
        String scenarioName = FigmaExcelFile.scenarioNameOf(group.get(0));
        return null != scenarioName ? scenarioName : resolveStepName(group.get(0));
    }

    private static String resolveStepName(FigmaRow row) {
        return isBlank(row.testName) ? row.appUrlOrScreenName : row.testName;
    }

    private static By parseLocator(String locator) {
        String trimmed = locator.trim();
        return trimmed.startsWith("/") ? By.xpath(trimmed) : By.cssSelector(trimmed);
    }

    private static boolean isBlank(String value) {
        return null == value || value.isBlank();
    }
}
