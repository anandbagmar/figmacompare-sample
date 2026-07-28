package io.samples.appium.android;

import java.util.ArrayList;
import java.util.List;

import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.AfterSuite;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.BeforeSuite;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

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
import io.samples.appium.AppiumServerSupport;
import io.samples.config.AppConfig;
import io.samples.eyes.BatchSupport;
import io.samples.eyes.ComparisonResultRecorder;
import io.samples.excel.ExcelHelper;
import io.samples.excel.FigmaExcelFile;
import io.samples.excel.FigmaRow;
import io.samples.excel.FigmaValidation;

/**
 * The one Android runner for the compareWithFigma mobile path (see
 * README_FigmaVisualValidation.md): for every group of "Platform=Android" rows sharing a
 * "Scenario Name", looks up that scenario in AndroidScenarioRegistry - wherever it was
 * registered, e.g. BajajFinservAndroidTest - launches the matching app, runs the
 * registered ScenarioFlow, and writes results back. Scenario Name is required for every
 * Android row (validated up front): reaching even a single mobile screen can need bespoke
 * login/navigation, so there's no generic "just open this screen" runner the way web has.
 *
 * A new app just needs a new provider class (following BajajFinservAndroidTest's pattern)
 * registering its scenarios into AndroidScenarioRegistry, plus one line in
 * AndroidScenarioRegistry.ensureAllProvidersRegistered() - this class doesn't change.
 */
public class CompareAndroidWithFigma {

    private static final boolean IS_FULL_RESET = true;
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

    @BeforeSuite
    public static void beforeSuite() {
        AndroidScenarioRegistry.ensureAllProvidersRegistered();
        localAppiumServer = AppiumServerSupport.start(AppiumServerSupport.defaultLogFileDir());
        appiumServerUrl = localAppiumServer.getUrl().toString();
        batch = BatchSupport.createSuiteBatch(AppConfig.get("APPLITOOLS_BATCH_NAME", "compareAndroidWithFigma"));
    }

    @AfterSuite
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

    @DataProvider(name = "androidGroups")
    public static Object[][] androidGroups() {
        AndroidScenarioRegistry.ensureAllProvidersRegistered();
        figmaExcelPath = FigmaExcelFile.resolvePath(System.getProperty("figmaExcel"));
        allRows = ExcelHelper.readRows(figmaExcelPath);

        List<FigmaRow> androidRows = FigmaExcelFile.filterByPlatform(allRows, "android");
        List<String> errors = new ArrayList<>(FigmaValidation.validate(allRows));
        errors.addAll(FigmaValidation.validateScenarioTests(androidRows, AndroidScenarioRegistry
                .registeredScenarioNames()));
        FigmaValidation.throwIfAny(errors);

        List<List<FigmaRow>> androidGroups = FigmaExcelFile.groupContiguous(androidRows);
        Object[][] data = new Object[androidGroups.size()][1];
        for (int i = 0; i < androidGroups.size(); i++) {
            data[i][0] = androidGroups.get(i);
        }
        return data;
    }

    /**
     * TestNG injects the about-to-run @Test method's own parameters here, so the correct
     * app (APK) can be launched per scenario group before that group's test body runs.
     */
    @BeforeMethod
    public void beforeMethod(Object[] testParams) {
        @SuppressWarnings("unchecked")
        List<FigmaRow> group = (List<FigmaRow>) testParams[0];
        String scenarioName = FigmaExcelFile.scenarioNameOf(group.get(0));
        AndroidScenarioRegistry.Registration registration = AndroidScenarioRegistry.get(scenarioName);
        driver = AndroidDriverFactory.create(appiumServerUrl, registration.apkPath, IS_FULL_RESET);
    }

    @AfterMethod
    public void afterMethod() {
        if (null != driver) {
            driver.quit();
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

    @Test(dataProvider = "androidGroups")
    void compareAndroidScenarioWithFigmaBaseline(List<FigmaRow> group) {
        FigmaRow firstRow = group.get(0);
        String scenarioName = FigmaExcelFile.scenarioNameOf(firstRow);
        String baselineName = isBlank(firstRow.baselineEnvName)
                ? scenarioName + "-baseline"
                : firstRow.baselineEnvName;

        // Guaranteed non-null by pre-flight validation (validateScenarioTests).
        AndroidScenarioRegistry.Registration registration = AndroidScenarioRegistry.get(scenarioName);

        configureEyes(registration.appName, scenarioName, baselineName);
        try {
            registration.flow.run(driver, eyes, group);
            TestResults testResults = eyes.close(false);

            boolean isPass = ComparisonResultRecorder.recordAndCheckPass(group, testResults);
            Assert.assertTrue(isPass, "Visual differences found for scenario: " + scenarioName);
        } catch (RuntimeException ex) {
            for (FigmaRow row : group) {
                row.validationStatus = "Failed";
                row.errorMessage = ex.getMessage();
            }
            eyes.abortIfNotClosed();
            throw ex;
        }
    }

    private static boolean isBlank(String value) {
        return null == value || value.isBlank();
    }
}
