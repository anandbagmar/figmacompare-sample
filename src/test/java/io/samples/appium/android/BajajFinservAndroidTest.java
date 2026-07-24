package io.samples.appium.android;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

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
import io.samples.eyes.BatchSupport;
import io.samples.eyes.ComparisonResultRecorder;
import io.samples.excel.ExcelHelper;
import io.samples.excel.FigmaExcelFile;
import io.samples.excel.FigmaRow;

/**
 * Mobile (Android) path of the compareWithFigma step described in
 * README_FigmaVisualValidation.md, for the Bajaj Finserv Android app.
 *
 * Unlike the web path (BajajFinservWebTest), a single generic test cannot handle every
 * mobile row: Selenium can open any URL directly, but there is no equivalent "just
 * navigate there" for a native app screen — reaching one usually needs login, menu
 * navigation, or test data setup specific to that app. So this class is the template for
 * one Appium test class per app: it owns a small registry of "screen flows" (one method
 * per distinct "App URL / Screen Name" value used in the shared Figma Excel file for this
 * app), and a data-driven test that looks up and runs the matching flow for each
 * "Platform=Android" row, then does the same Applitools comparison + Excel write-back (in
 * place) as the web path. Rows with "Skip" set are left untouched and not processed.
 */
public class BajajFinservAndroidTest {

    private static final String APP_NAME = "BajajFinservAndroidApp";
    private static final String APK_NAME = "sampleApps" + File.separator + "app_npu_v8.3.17.apk";
    private static final boolean IS_FULL_RESET = true;
    private static final boolean IS_EYES_ENABLED = true;

    private static final String userName = System.getProperty("user.name");
    private static final String APPLITOOLS_API_KEY = System.getenv("APPLITOOLS_API_KEY");

    /**
     * One entry per distinct "App URL / Screen Name" value used for this app in the
     * shared Figma Excel file. Add a new entry here whenever a new screen needs
     * comparing; the flow just has to leave the app on that screen when it returns.
     */
    private static final Map<String, Consumer<AppiumDriver>> SCREEN_FLOWS = new HashMap<>();
    static {
        SCREEN_FLOWS.put("Home Screen", driver -> {
            // The app opens directly on the home screen after launch - nothing to navigate.
        });
    }

    private static AppiumDriverLocalService localAppiumServer;
    private static String appiumServerUrl = "http://localhost:4723/wd/hub/";
    private static BatchInfo batch;
    private static String figmaExcelPath;
    private static List<FigmaRow> allRows;

    private AppiumDriver driver;
    private Eyes eyes;

    @BeforeSuite
    public static void beforeSuite() {
        localAppiumServer = AppiumServerSupport.start(AppiumServerSupport.defaultLogFileDir());
        appiumServerUrl = localAppiumServer.getUrl().toString();
        batch = BatchSupport.createSuiteBatch(APP_NAME);
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

    @DataProvider(name = "androidRows")
    public static Object[][] androidRows() {
        figmaExcelPath = FigmaExcelFile.resolvePath(System.getProperty("figmaExcel"));
        allRows = ExcelHelper.readRows(figmaExcelPath);
        List<FigmaRow> androidRows = FigmaExcelFile.filterByPlatform(allRows, "android");

        Object[][] data = new Object[androidRows.size()][1];
        for (int i = 0; i < androidRows.size(); i++) {
            data[i][0] = androidRows.get(i);
        }
        return data;
    }

    @BeforeMethod
    public void beforeMethod() {
        driver = AndroidDriverFactory.create(appiumServerUrl, APK_NAME, IS_FULL_RESET);
    }

    @AfterMethod
    public void afterMethod() {
        if (null != driver) {
            driver.quit();
        }
    }

    private void configureEyes(String testName, String baselineName) {
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
        eyes.open(driver, APP_NAME, testName);
    }

    @Test(dataProvider = "androidRows")
    void compareAndroidRowWithFigmaBaseline(FigmaRow row) {
        String testName = isBlank(row.testName) ? row.appUrlOrScreenName : row.testName;
        String baselineName = isBlank(row.baselineEnvName) ? testName + "-baseline" : row.baselineEnvName;

        Consumer<AppiumDriver> screenFlow = SCREEN_FLOWS.get(row.appUrlOrScreenName);
        if (null == screenFlow) {
            row.validationStatus = "Failed";
            row.errorMessage = "No screen flow registered for \"" + row.appUrlOrScreenName + "\". Add one to "
                    + "BajajFinservAndroidTest.SCREEN_FLOWS.";
            Assert.fail(row.errorMessage);
            return;
        }

        configureEyes(testName, baselineName);
        try {
            screenFlow.accept(driver);
            eyes.checkWindow(testName);
            TestResults testResults = eyes.close(false);

            boolean isPass = ComparisonResultRecorder.recordAndCheckPass(row, testResults);
            Assert.assertTrue(isPass, "Visual differences found for: " + row.appUrlOrScreenName);
        } catch (RuntimeException ex) {
            row.validationStatus = "Failed";
            row.errorMessage = ex.getMessage();
            eyes.abortIfNotClosed();
            throw ex;
        }
    }

    private static boolean isBlank(String value) {
        return null == value || value.isBlank();
    }
}
