package io.eot.bajajfinserv.appium.android;

import java.io.File;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.time.Duration;

import org.openqa.selenium.By;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.AfterSuite;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.BeforeSuite;
import org.testng.annotations.Test;

import com.applitools.eyes.BatchInfo;
import com.applitools.eyes.MatchLevel;
import com.applitools.eyes.StdoutLogHandler;
import com.applitools.eyes.TestResults;
import com.applitools.eyes.TestResultsStatus;
import com.applitools.eyes.appium.Eyes;
import com.applitools.eyes.config.MobileOptions;
import com.applitools.eyes.selenium.Configuration;
import com.applitools.eyes.selenium.StitchMode;

import io.appium.java_client.AppiumDriver;
import io.appium.java_client.android.AndroidDriver;
import io.appium.java_client.android.options.UiAutomator2Options;
import io.appium.java_client.service.local.AppiumDriverLocalService;
import io.appium.java_client.service.local.AppiumServiceBuilder;
import io.appium.java_client.service.local.flags.GeneralServerFlag;
import io.eot.figmacompare.Baseline;

class CalculatorFigmaTest {
    private static final String className = CalculatorFigmaTest.class.getSimpleName();
    private static final String userName = System.getProperty("user.name");
    private static final boolean IS_FULL_RESET = true;
    private static BatchInfo batch;
    private static String APPIUM_SERVER_URL = "http://localhost:4723/wd/hub/";
    private static AppiumDriverLocalService localAppiumServer;
    private static String APK_NAME = "sampleApps" + File.separator + "Calculator_8.4.1.apk";
    private static boolean IS_EYES_ENABLED = true;
    private final String APPLITOOLS_API_KEY = System.getenv("APPLITOOLS_API_KEY");
    private AppiumDriver driver;
    private Eyes eyes;
    private String testName;
    private String baselineName;
    private static final String LOG_FILE_DIR = System.getenv("LOG_DIR") == null ? "appium-server.log"
            : System.getenv("LOG_DIR") + "/appium_logs.txt";

    private CalculatorFigmaTest() {

    }

    @BeforeSuite
    static void beforeAll() {
        startAppiumServer();
        String localBatchName = className;
        String ciBatchName = System.getenv("APPLITOOLS_BATCH_NAME");
        String applitoolsBatchName = ciBatchName == null ? localBatchName : ciBatchName;
        batch = new BatchInfo(applitoolsBatchName);
        // If the test runs via Jenkins, set the batch ID accordingly.
        batch.addProperty("REPOSITORY_NAME", new File(System.getProperty("user.dir")).getName());
        System.out.println("Create AppiumRunner");
        System.out.printf("Batch name: %s%n", batch.getName());
        System.out.printf("Batch startedAt: %s%n", batch.getStartedAt().getTime());
        System.out.printf("Batch BatchId: %s%n", batch.getId());
    }

    @AfterSuite
    static void afterAll() {
        System.out.printf("AfterAll: Stopping the local Appium server running on: '%s'%n", APPIUM_SERVER_URL);
        if (null != batch) {
            batch.setCompleted(true);
        }
        if (null != localAppiumServer) {
            localAppiumServer.stop();
            System.out.printf("Is Appium server running? %s%n", localAppiumServer.isRunning());
        }
    }

    private static void startAppiumServer() {
        System.out.println("Start local Appium server");
        AppiumServiceBuilder serviceBuilder = new AppiumServiceBuilder();
        // Use any port, in case the default 4723 is already taken (maybe by another
        // Appium server)
        serviceBuilder.usingAnyFreePort();
        serviceBuilder.withAppiumJS(new File("./node_modules/appium/build/lib/main.js"));
        serviceBuilder.withLogFile(new File(LOG_FILE_DIR));
        serviceBuilder.withArgument(GeneralServerFlag.ALLOW_INSECURE, "adb_shell");
        serviceBuilder.withArgument(GeneralServerFlag.RELAXED_SECURITY);

        // Appium 2.x
        localAppiumServer = AppiumDriverLocalService.buildService(serviceBuilder);

        localAppiumServer.start();
        APPIUM_SERVER_URL = localAppiumServer.getUrl().toString();
        System.out.printf("Appium server started on url: '%s'%n", localAppiumServer.getUrl().toString());
    }

    @BeforeMethod
    public void beforeEach(Method testInfo) {
        this.testName = testInfo.getName();
        this.baselineName = testName + "-baseline";
        System.out.printf("Test: %s - BeforeEach%n", testName);
        setUpAndroid(testInfo);
        configureEyes(testInfo);
    }

    @AfterMethod
    void tearDown(Method testInfo) {
        System.out.println("AfterEach: Test - " + testInfo.getName());
        boolean isPass = true;
        if (IS_EYES_ENABLED) {
            TestResults testResults = eyes.close(false);
            System.out.printf("Test: %s\n%s%n", testResults.getName(), testResults);
            if (testResults.getStatus().equals(TestResultsStatus.Failed)
                    || testResults.getStatus().equals(TestResultsStatus.Unresolved)) {
                isPass = false;
            }
        }
        if (null != driver) {
            driver.quit();
        }
        Assert.assertTrue(isPass, "Visual differences found.");
    }

    void setUpAndroid(Method testInfo) {
        System.out.println("BeforeEach: Test - " + testInfo.getName());
        System.out.printf("Create AppiumDriver for android test - %s%n", APPIUM_SERVER_URL);
        // Appium 2.x
        UiAutomator2Options uiAutomator2Options = new UiAutomator2Options();
        uiAutomator2Options.setPlatformName("Android");

        uiAutomator2Options.setAutomationName("UiAutomator2");
        uiAutomator2Options.setDeviceName("Android");
        uiAutomator2Options.setPrintPageSourceOnFindFailure(true);
        uiAutomator2Options.setAutoGrantPermissions(true);
        uiAutomator2Options.setFullReset(IS_FULL_RESET);
        uiAutomator2Options.setApp(new File(APK_NAME).getAbsolutePath());
        System.out.println("UiAutomator2Options:");
        for (String capabilityName : uiAutomator2Options.getCapabilityNames()) {
            System.out.println("\t" + capabilityName + ": " + uiAutomator2Options.getCapability(capabilityName));
        }

        try {
            driver = new AndroidDriver(new URL(APPIUM_SERVER_URL), uiAutomator2Options);
            driver.manage().timeouts().implicitlyWait(Duration.ofSeconds(1L));
        } catch (MalformedURLException e) {
            System.err.println(
                    "Error creating Appium driver for android device with capabilities: " + uiAutomator2Options);
            throw new RuntimeException(e);
        }
        System.out.printf("Created AppiumDriver for - %s%n", APPIUM_SERVER_URL);
    }

    private void configureEyes(Method testInfo) {
        System.out.println("Setup Eyes configuration");
        eyes = new Eyes();
        eyes.setLogHandler(new StdoutLogHandler(true));
        Configuration configuration = eyes.getConfiguration();
        if (null != baselineName) {
            configuration.setBaselineEnvName(baselineName);
        }
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
        configuration.setSaveNewTests(true);
        configuration.setServerUrl("https://eyes.applitools.com");
        configuration.setStitchMode(StitchMode.CSS);
        eyes.setConfiguration(configuration);
        eyes.setConfiguration(eyes.getConfiguration().setMobileOptions(MobileOptions.keepNavigationBar(false)));
        eyes.open(driver, className, testInfo.getName());
    }

    @Test
    void calculatorTest_id() {
        String baselineName = testName + "-baseline";
        String baselineImagePath = System.getProperty("user.dir") + File.separator + "downloaded_images"
                + File.separator + "calculator.png";
        Baseline.uploadImageAndSetAsBaseline(baselineImagePath, baselineName, className, testName, null);

        int p1 = 3;
        int p2 = 5;

        driver.findElement(By.id("digit_" + p1)).click();
        driver.findElement(By.id("op_add")).click();
        driver.findElement(By.id("digit_" + p2)).click();
        driver.findElement(By.id("eq")).click();
        eyes.checkWindow("eq");
    }
}
