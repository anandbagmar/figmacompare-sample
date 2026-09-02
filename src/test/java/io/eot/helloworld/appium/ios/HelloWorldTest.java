package io.eot.helloworld.appium.ios;

import static io.eot.figmacompare.Wait.waitFor;

import java.io.File;
import java.lang.reflect.Method;
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
import com.applitools.eyes.StdoutLogHandler;
import com.applitools.eyes.TestResults;
import com.applitools.eyes.TestResultsStatus;
import com.applitools.eyes.appium.Eyes;
import com.applitools.eyes.appium.Target;
import com.applitools.eyes.selenium.Configuration;

import io.appium.java_client.AppiumBy;
import io.appium.java_client.AppiumDriver;
import io.appium.java_client.service.local.AppiumDriverLocalService;
import io.eot.figmacompare.appium.AppiumServerSupport;
import io.eot.figmacompare.appium.ios.IosDriverFactory;
import io.eot.figmacompare.eyes.BatchSupport;
import io.eot.figmacompare.eyes.EyesConfigSupport;

public class HelloWorldTest {
    private static final String className = HelloWorldTest.class.getSimpleName();
    private static final String IOS_UDID = "B38642DE-1521-4AF0-B13A-EC710A6807E9";
    private static final String IOS_DEVICE_NAME = "iPhone 16 Pro";
    private static final String IOS_PLATFORM_VERSION = "18.1";
    private static final boolean IS_FULL_RESET = false;
    private static BatchInfo batch;
    private static String APPIUM_SERVER_URL = "http://localhost:4723/wd/hub/";
    private static AppiumDriverLocalService localAppiumServer;
    private static String APP_NAME = "sampleApps" + File.separator + "HelloWorldiOS.app";
    private static boolean IS_EYES_ENABLED = true;
    private AppiumDriver driver;
    private Eyes eyes;

    // AppiumNativeiOSHelloWorldEyesNMLTest() {
    // }

    @BeforeSuite
    static void beforeAll() {
        localAppiumServer = AppiumServerSupport.start(AppiumServerSupport.defaultLogFileDir());
        APPIUM_SERVER_URL = localAppiumServer.getUrl().toString();
        batch = BatchSupport.createSuiteBatch(className);
    }

    @AfterSuite
    static void afterAll() {
        BatchSupport.closeBatch(batch);
        AppiumServerSupport.stop(localAppiumServer);
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
        driver = IosDriverFactory.create(APPIUM_SERVER_URL, APP_NAME, IOS_UDID, IOS_DEVICE_NAME,
                IOS_PLATFORM_VERSION, IS_FULL_RESET);
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
