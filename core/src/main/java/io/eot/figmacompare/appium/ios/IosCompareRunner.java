package io.eot.figmacompare.appium.ios;

import java.util.ArrayList;
import java.util.List;

import com.applitools.eyes.BatchInfo;
import com.applitools.eyes.MatchLevel;
import com.applitools.eyes.StdoutLogHandler;
import com.applitools.eyes.TestResults;
import com.applitools.eyes.appium.Eyes;
import com.applitools.eyes.config.MobileOptions;
import com.applitools.eyes.selenium.Configuration;
import com.applitools.eyes.selenium.StitchMode;

import io.appium.java_client.AppiumDriver;
import io.appium.java_client.service.local.AppiumDriverLocalService;
import io.eot.figmacompare.appium.AppiumServerSupport;
import io.eot.figmacompare.config.AppConfig;
import io.eot.figmacompare.eyes.BatchSupport;
import io.eot.figmacompare.eyes.ComparisonResultRecorder;
import io.eot.figmacompare.excel.ExcelHelper;
import io.eot.figmacompare.excel.FigmaExcelFile;
import io.eot.figmacompare.excel.FigmaRow;
import io.eot.figmacompare.excel.FigmaValidation;

/**
 * Plain-Java orchestration for the iOS path of compareWithFigma - mirrors
 * AndroidCompareRunner.java exactly; see its class comment and
 * README_FigmaVisualValidation.md for the full rationale. No TestNG (or any other test
 * framework) dependency - see CompareIosWithFigmaTest in the samples module for a thin
 * TestNG wrapper around it.
 */
public class IosCompareRunner {

    private static final String IOS_UDID = "B38642DE-1521-4AF0-B13A-EC710A6807E9";
    private static final String IOS_DEVICE_NAME = "iPhone 16 Pro";
    private static final String IOS_PLATFORM_VERSION = "18.1";
    private static final boolean IS_FULL_RESET = false;
    private static final boolean IS_EYES_ENABLED = true;

    private static final String userName = System.getProperty("user.name");
    private static final String APPLITOOLS_API_KEY = System.getenv("APPLITOOLS_API_KEY");

    private static AppiumDriverLocalService localAppiumServer;
    private static String appiumServerUrl = "http://localhost:4723/wd/hub/";
    private static BatchInfo batch;
    private static String figmaExcelPath;
    private static List<FigmaRow> allRows;

    private AppiumDriver driver;
    private Eyes eyes;

    public static void beforeSuite() {
        localAppiumServer = AppiumServerSupport.start(AppiumServerSupport.defaultLogFileDir());
        appiumServerUrl = localAppiumServer.getUrl().toString();
        batch = BatchSupport.createSuiteBatch(AppConfig.get("APPLITOOLS_BATCH_NAME", "compareIosWithFigma"));
    }

    public static void afterSuite() {
        BatchSupport.closeBatch(batch);
        AppiumServerSupport.stop(localAppiumServer);
        if (null != allRows && !allRows.isEmpty()) {
            ExcelHelper.writeRows(figmaExcelPath, allRows);
            long passed = allRows.stream().filter(row -> "Passed".equals(row.validationStatus)).count();
            System.out.println();
            System.out.println(passed + " of " + allRows.size() + " row(s) passed. Results written to "
                    + figmaExcelPath);
        }
    }

    /**
     * Reads, validates, and groups the shared Excel file's iOS rows. Callers must ensure
     * every scenario provider class they care about has already been loaded (e.g. via
     * IosScenarioRegistry.loadProviderClass(...)) before calling this.
     */
    public static List<List<FigmaRow>> loadIosGroups(String figmaExcelPathOverride) {
        figmaExcelPath = FigmaExcelFile.resolvePath(figmaExcelPathOverride);
        allRows = ExcelHelper.readRows(figmaExcelPath);

        List<FigmaRow> iosRows = FigmaExcelFile.filterByPlatform(allRows, "ios");
        List<String> errors = new ArrayList<>(FigmaValidation.validate(allRows));
        errors.addAll(FigmaValidation.validateScenarioTests(iosRows, IosScenarioRegistry.registeredScenarioNames()));
        FigmaValidation.throwIfAny(errors);

        return FigmaExcelFile.groupContiguous(iosRows);
    }

    /** Launches the app registered for this group's scenario. Call once per group. */
    public void createDriverForGroup(List<FigmaRow> group) {
        String scenarioName = FigmaExcelFile.scenarioNameOf(group.get(0));
        IosScenarioRegistry.Registration registration = IosScenarioRegistry.get(scenarioName);
        driver = IosDriverFactory.create(appiumServerUrl, registration.appPath, IOS_UDID, IOS_DEVICE_NAME,
                IOS_PLATFORM_VERSION, IS_FULL_RESET);
    }

    public void quitDriver() {
        if (null != driver) {
            driver.quit();
        }
    }

    /**
     * Runs the group's scenario against the already-launched app (see
     * createDriverForGroup) and records the result onto every row in the group. Returns
     * whether it passed; throws on an automation/runtime error (not a visual mismatch,
     * which is recorded but does not throw).
     */
    public boolean compareGroup(List<FigmaRow> group) {
        FigmaRow firstRow = group.get(0);
        String scenarioName = FigmaExcelFile.scenarioNameOf(firstRow);
        String baselineName = isBlank(firstRow.baselineEnvName)
                ? scenarioName + "-baseline"
                : firstRow.baselineEnvName;

        // Guaranteed non-null by pre-flight validation (validateScenarioTests).
        IosScenarioRegistry.Registration registration = IosScenarioRegistry.get(scenarioName);

        configureEyes(registration.appName, scenarioName, baselineName);
        try {
            registration.flow.run(driver, eyes, group);
            TestResults testResults = eyes.close(false);
            return ComparisonResultRecorder.recordAndCheckPass(group, testResults);
        } catch (RuntimeException ex) {
            for (FigmaRow row : group) {
                row.validationStatus = "Failed";
                row.errorMessage = ex.getMessage();
            }
            eyes.abortIfNotClosed();
            throw ex;
        }
    }

    private void configureEyes(String appName, String testName, String baselineName) {
        eyes = new Eyes();
        eyes.setLogHandler(new StdoutLogHandler(true));
        Configuration configuration = eyes.getConfiguration();
        configuration.setBaselineEnvName(baselineName);
        configuration.addProperty("username", userName);
        configuration.setApiKey(APPLITOOLS_API_KEY);
        configuration.setBatch(batch);
        configuration.setBranchName("main");
        configuration.setCaptureStatusBar(true);
        configuration.setDisableBrowserFetching(true);
        configuration.setEnablePatterns(true);
        configuration.setEnvironmentName("prod");
        configuration.setHideCaret(true);
        configuration.setIgnoreCaret(true);
        configuration.setIgnoreDisplacements(true);
        configuration.setIsDisabled(!IS_EYES_ENABLED);
        configuration.setMatchLevel(MatchLevel.STRICT);
        configuration.setSaveNewTests(false);
        configuration.setServerUrl("https://eyes.applitools.com");
        configuration.setStitchMode(StitchMode.CSS);
        eyes.setConfiguration(configuration);
        eyes.setConfiguration(eyes.getConfiguration().setMobileOptions(MobileOptions.keepNavigationBar(false)));
        eyes.open(driver, appName, testName);
    }

    private static boolean isBlank(String value) {
        return null == value || value.isBlank();
    }
}
