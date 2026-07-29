package io.eot.helloworld.appium.ios;

import static io.eot.figmacompare.Wait.waitFor;

import java.io.File;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.time.Duration;
import java.util.List;
import java.util.Random;

import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
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
import com.applitools.eyes.appium.Target;

import io.appium.java_client.AppiumBy;
import io.appium.java_client.AppiumDriver;
import io.appium.java_client.ios.IOSDriver;
import io.appium.java_client.ios.options.XCUITestOptions;
import io.appium.java_client.service.local.AppiumDriverLocalService;
import io.appium.java_client.service.local.AppiumServiceBuilder;
import io.appium.java_client.service.local.flags.GeneralServerFlag;

;

public class HelloWorldTest {
    private static final String className = HelloWorldTest.class.getSimpleName();
    private static final String userName = System.getProperty("user.name");
    private static final String IOS_UDID = "B38642DE-1521-4AF0-B13A-EC710A6807E9";
    private static final String IOS_DEVICE_NAME = "iPhone 16 Pro";
    private static final String IOS_PLATFORM_VERSION = "18.1";
    private static final boolean IS_FULL_RESET = false;
    private static BatchInfo batch;
    private static String APPIUM_SERVER_URL = "http://localhost:4723/wd/hub/";
    private static AppiumDriverLocalService localAppiumServer;
    private static String APP_NAME = "sampleApps" + File.separator + "HelloWorldiOS.app";
    private static boolean IS_EYES_ENABLED = true;
    private final String APPLITOOLS_API_KEY = System.getenv("APPLITOOLS_API_KEY");
    private AppiumDriver driver;
    private Eyes eyes;
    private static final String LOG_FILE_DIR = System.getenv("LOG_DIR") == null ? "appium-server.log"
            : System.getenv("LOG_DIR") + "/appium_logs.txt";

    // AppiumNativeiOSHelloWorldEyesNMLTest() {
    // }

    @BeforeSuite
    static void beforeAll() {
        startAppiumServer();
        String batchName = className;
        batch = new BatchInfo(batchName);
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

    private static void generateRandomNumber(AppiumDriver driver) {
        int numberOfClicks = new Random().nextInt(100) % 10;
        System.out.printf("Click on get random number %d times%n", numberOfClicks);
        for (int i = 0; i < numberOfClicks; i++) {
            driver.findElement(AppiumBy.accessibilityId("MakeRandomNumberCheckbox")).click();
            waitFor(1);
        }
    }

    private static WebElement getRandomNumberElement(AppiumDriver driver, Eyes eyes) {
        List<WebElement> webElementList = driver.findElements(AppiumBy.xpath("//XCUIElementTypeStaticText[@name]"));
        for (WebElement element : webElementList) {
            String text = element.getText();
            System.out.println(text);
            try {
                long randomNumber = Long.parseLong(text);
                System.out.println("Random number: " + randomNumber);
                return element;
            } catch (NumberFormatException e) {
                System.out.println("Not the element we are looking for");
            }
        }
        throw new RuntimeException("Random number not available");
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

        // Appium 2.x
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
        xcuiTestOptions.setApp(new File(APP_NAME).getAbsolutePath());

        System.out.println("XCUITestOptions: " + xcuiTestOptions);
        try {
            driver = new IOSDriver(new URL(APPIUM_SERVER_URL), xcuiTestOptions);
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
        eyes.setBatch(batch);
        eyes.setBranchName("main");
        eyes.setEnvName("prod");
        eyes.addProperty("username", userName);
        eyes.setApiKey(APPLITOOLS_API_KEY);
        eyes.setServerUrl("https://eyes.applitools.com");
        eyes.setMatchLevel(MatchLevel.STRICT);
        eyes.setIsDisabled(!IS_EYES_ENABLED);
        eyes.setIgnoreCaret(true);
        eyes.setIgnoreDisplacements(true);
        eyes.setSaveNewTests(true);
        eyes.open(driver, className, testInfo.getName());
    }

    @Test
    public void runIOSNativeAppTest() {
        eyes.checkWindow("App launched - checkWindow");
        eyes.check("App launched - target.window", Target.window());
        driver.findElement(AppiumBy.accessibilityId("Make the number below random.")).click();
        waitFor(1);
        eyes.check("MakeRandomNumberCheckbox-window-strict", Target.window().strict());
        eyes.check("MakeRandomNumberCheckbox-window-layout", Target.window().layout());
        WebElement randomNumberElement = getRandomNumberElement(driver, eyes);
        generateRandomNumber(driver);
        waitFor(1);
        randomNumberElement = getRandomNumberElement(driver, eyes);
        eyes.check("MakeRandomNumberCheckbox-region-strict-randomNumber", Target.region(randomNumberElement).strict());
        eyes.check("MakeRandomNumberCheckbox-region-layout-randomNumber", Target.region(randomNumberElement).layout());

        driver.findElement(AppiumBy.accessibilityId("SimulateDiffsCheckbox")).click();
        waitFor(1);

        eyes.check("SimulateDiffsCheckbox-layout", Target.window().layout());
        eyes.check("SimulateDiffsCheckbox-strict", Target.window().strict());
        driver.findElement(By.xpath("//XCUIElementTypeStaticText[@name=\"Click me!\"]")).click();
        waitFor(1);
        eyes.checkWindow("Click me!");
        Assert.assertTrue(true, "Test completed. Assertions will be done by Applitools");
    }

}
