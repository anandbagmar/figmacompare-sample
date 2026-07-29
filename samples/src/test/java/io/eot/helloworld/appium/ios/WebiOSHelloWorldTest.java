package io.eot.helloworld.appium.ios;

import java.io.File;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.time.Duration;
import java.util.Date;

import org.openqa.selenium.By;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.AfterSuite;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.BeforeSuite;
import org.testng.annotations.Test;

import com.applitools.eyes.BatchInfo;
import com.applitools.eyes.StdoutLogHandler;
import com.applitools.eyes.TestResults;
import com.applitools.eyes.TestResultsStatus;
import com.applitools.eyes.appium.Eyes;
import com.applitools.eyes.appium.Target;
import com.applitools.eyes.selenium.Configuration;

import io.appium.java_client.AppiumDriver;
import io.appium.java_client.ios.options.XCUITestOptions;
import io.appium.java_client.service.local.AppiumDriverLocalService;
import io.eot.figmacompare.appium.AppiumServerSupport;
import io.eot.figmacompare.eyes.BatchSupport;
import io.eot.figmacompare.eyes.EyesConfigSupport;
import static io.eot.figmacompare.Wait.waitFor;

class WebiOSHelloWorldTest {
    private static final String className = WebiOSHelloWorldTest.class.getSimpleName();
    private static final long epochSecond = new Date().toInstant().getEpochSecond();
    private static final String IOS_UDID = "B38642DE-1521-4AF0-B13A-EC710A6807E9";
    private static final String IOS_DEVICE_NAME = "iPhone 16 Pro";
    private static final String IOS_PLATFORM_VERSION = "18.1";
    private static final boolean IS_FULL_RESET = false;
    private static BatchInfo batch;
    private static String APPIUM_SERVER_URL = "http://localhost:4723/wd/hub/";
    private static AppiumDriverLocalService localAppiumServer;
    private static boolean IS_EYES_ENABLED = true;
    private AppiumDriver driver;
    private Eyes eyes;

    private WebiOSHelloWorldTest() {
    }

    @BeforeSuite
    static void beforeAll() {
        localAppiumServer = AppiumServerSupport.start(AppiumServerSupport.defaultLogFileDir());
        APPIUM_SERVER_URL = localAppiumServer.getUrl().toString();
        batch = BatchSupport.createSuiteBatch(className);
        batch.setId(String.valueOf(epochSecond));
    }

    @AfterSuite
    static void afterAll() {
        BatchSupport.closeBatch(batch);
        AppiumServerSupport.stop(localAppiumServer);
    }

    @BeforeMethod
    public void beforeEach(Method testInfo) {
        System.out.printf("Test: %s - BeforeEach%n", testInfo.getName());
        setUpiOS(testInfo);
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

    void setUpiOS(Method testInfo) {
        System.out.println("BeforeEach: Test - " + testInfo.getName());
        System.out.printf("Create AppiumDriver for iOS test - %s%n", APPIUM_SERVER_URL);

        XCUITestOptions xcuiTestOptions = new XCUITestOptions();
        xcuiTestOptions.setPlatformName("iOS");
        xcuiTestOptions.setAutomationName("XCUITest");
        xcuiTestOptions.setPlatformVersion(IOS_PLATFORM_VERSION);
        xcuiTestOptions.setDeviceName(IOS_DEVICE_NAME);
        xcuiTestOptions.setUdid(IOS_UDID);
        xcuiTestOptions.setFullReset(IS_FULL_RESET);
        xcuiTestOptions.setShowXcodeLog(false);
        xcuiTestOptions.setCapability("appium:showIOSLog", false);
        xcuiTestOptions.setPrintPageSourceOnFindFailure(true);
        xcuiTestOptions.setAutoAcceptAlerts(true);
        xcuiTestOptions.setCapability("browserName", "safari");
        xcuiTestOptions.setSafariInitialUrl("https://google.com");

        System.out.println("XCUITestOptions: " + xcuiTestOptions);
        try {
            driver = new AppiumDriver(new URL(APPIUM_SERVER_URL), xcuiTestOptions);
            driver.manage().timeouts().implicitlyWait(Duration.ofSeconds(1L));
        } catch (MalformedURLException e) {
            System.err.println("Error creating Appium driver for iOS device with capabilities: " + xcuiTestOptions);
            throw new RuntimeException(e);
        }

        configureEyes(testInfo);
    }

    private void configureEyes(Method testInfo) {
        System.out.println("Setup Eyes configuration");
        eyes = new Eyes();
        eyes.setLogHandler(new StdoutLogHandler(true));

        Configuration configuration = EyesConfigSupport.baseConfiguration(batch, null);
        configuration.setBranchName("main");
        configuration.setEnvironmentName("prod");
        configuration.setIgnoreCaret(true);
        configuration.setIsDisabled(!IS_EYES_ENABLED);
        configuration.setServerUrl("https://eyes.applitools.com");
        eyes.setConfiguration(configuration);

        eyes.open(driver, className, testInfo.getName());
    }

    @Test
    void runIOSWebTest() {
        driver.get("https://applitools.com/helloworld");
        waitFor(2);
        eyes.checkWindow("App launched");
        for (int stepNumber = 0; stepNumber < 2; stepNumber++) {
            By linkText = By.linkText("?diff1");
            driver.findElement(linkText).click();
            waitFor(1);
            eyes.check("step-" + stepNumber, Target.region(linkText).layout());
            waitFor(1);
        }
        driver.findElement(By.tagName("button")).click();
        eyes.check("Click Me", Target.window().layout());
        Assert.assertTrue(true, "Test completed. Assertions will be done by Applitools");
    }
}
