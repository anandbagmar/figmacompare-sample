package io.samples.appium.ios;

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
import io.samples.appium.ScenarioFlow;
import io.samples.config.AppConfig;
import io.samples.eyes.BatchSupport;
import io.samples.eyes.ComparisonResultRecorder;
import io.samples.excel.ExcelHelper;
import io.samples.excel.FigmaExcelFile;
import io.samples.excel.FigmaRow;
import io.samples.excel.FigmaValidation;

/**
 * The one iOS runner for the compareWithFigma mobile path - mirrors
 * CompareAndroidWithFigma.java exactly; see its class comment and
 * README_FigmaVisualValidation.md for the full rationale (Scenario Name is required for
 * every iOS row, dispatched through IosScenarioRegistry regardless of which class
 * registered it).
 */
public class CompareIosWithFigma {

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

    @BeforeSuite
    public static void beforeSuite() {
        IosScenarioRegistry.ensureAllProvidersRegistered();
        localAppiumServer = AppiumServerSupport.start(AppiumServerSupport.defaultLogFileDir());
        appiumServerUrl = localAppiumServer.getUrl().toString();
        batch = BatchSupport.createSuiteBatch(AppConfig.get("APPLITOOLS_BATCH_NAME", "compareIosWithFigma"));
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

    @DataProvider(name = "iosGroups")
    public static Object[][] iosGroups() {
        IosScenarioRegistry.ensureAllProvidersRegistered();
        figmaExcelPath = FigmaExcelFile.resolvePath(System.getProperty("figmaExcel"));
        allRows = ExcelHelper.readRows(figmaExcelPath);

        List<FigmaRow> iosRows = FigmaExcelFile.filterByPlatform(allRows, "ios");
        List<String> errors = new ArrayList<>(FigmaValidation.validate(allRows));
        errors.addAll(FigmaValidation.validateScenarioTests(iosRows, IosScenarioRegistry.registeredScenarioNames()));
        FigmaValidation.throwIfAny(errors);

        List<List<FigmaRow>> iosGroups = FigmaExcelFile.groupContiguous(iosRows);
        Object[][] data = new Object[iosGroups.size()][1];
        for (int i = 0; i < iosGroups.size(); i++) {
            data[i][0] = iosGroups.get(i);
        }
        return data;
    }

    /**
     * TestNG injects the about-to-run @Test method's own parameters here, so the correct
     * app can be launched per scenario group before that group's test body runs.
     */
    @BeforeMethod
    public void beforeMethod(Object[] testParams) {
        @SuppressWarnings("unchecked")
        List<FigmaRow> group = (List<FigmaRow>) testParams[0];
        String scenarioName = FigmaExcelFile.scenarioNameOf(group.get(0));
        IosScenarioRegistry.Registration registration = IosScenarioRegistry.get(scenarioName);
        driver = IosDriverFactory.create(appiumServerUrl, registration.appPath, IOS_UDID, IOS_DEVICE_NAME,
                IOS_PLATFORM_VERSION, IS_FULL_RESET);
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

    @Test(dataProvider = "iosGroups")
    void compareIosScenarioWithFigmaBaseline(List<FigmaRow> group) {
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
