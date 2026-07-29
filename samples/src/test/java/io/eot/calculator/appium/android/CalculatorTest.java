package io.eot.calculator.appium.android;

import java.io.File;
import java.lang.reflect.Method;

import org.openqa.selenium.By;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.AfterSuite;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.BeforeSuite;
import org.testng.annotations.Test;

import com.applitools.eyes.BatchInfo;
import com.applitools.eyes.TestResults;
import com.applitools.eyes.TestResultsStatus;
import com.applitools.eyes.appium.Eyes;
import com.applitools.eyes.appium.Target;

import io.appium.java_client.AppiumDriver;
import io.appium.java_client.service.local.AppiumDriverLocalService;
import io.eot.figmacompare.appium.AppiumServerSupport;
import io.eot.figmacompare.appium.android.AndroidDriverFactory;
import io.eot.figmacompare.eyes.BatchSupport;
import io.eot.figmacompare.eyes.MobileEyesSupport;

class CalculatorTest {
    private static final String className = CalculatorTest.class.getSimpleName();
    private static final boolean IS_FULL_RESET = true;
    private static BatchInfo batch;
    private static String APPIUM_SERVER_URL = "http://localhost:4723/wd/hub/";
    private static AppiumDriverLocalService localAppiumServer;
    private static String APK_NAME = "sampleApps" + File.separator + "Calculator_8.4.1.apk";
    private static boolean IS_EYES_ENABLED = true;
    private AppiumDriver driver;
    private Eyes eyes;

    private CalculatorTest() {

    }

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

    @BeforeMethod
    public void beforeEach(Method testInfo) {
        System.out.printf("Test: %s - BeforeEach%n", testInfo.getName());
        setUpAndroid(testInfo);
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
        driver = AndroidDriverFactory.create(APPIUM_SERVER_URL, APK_NAME, IS_FULL_RESET);
        eyes = MobileEyesSupport.open(driver, batch, className, testInfo.getName(), null, IS_EYES_ENABLED, true);
    }

    @Test
    void calculatorTest_id() {
        eyes.check("Calculator!-ignoreCaret", Target.window().ignoreCaret(true));
        eyes.checkWindow("Calculator!");

        int p1 = 3;
        int p2 = 5;

        driver.findElement(By.id("digit_" + p1)).click();
        eyes.check("digit_" + p1 + "-byElement", Target.region(By.id("digit_" + p1)));
        eyes.check("digit_" + p1 + "-by", Target.window().layout(By.id("digit_" + p1)));

        driver.findElement(By.id("op_add")).click();
        eyes.check("op_add-byElement", Target.region(By.id("op_add")));

        driver.findElement(By.id("digit_" + p2)).click();
        eyes.check("digit_" + p2 + "-byElement", Target.region(By.id("digit_" + p2)));

        driver.findElement(By.id("eq")).click();
        eyes.check("eq-ignoreCaret", Target.window().ignoreCaret(true));
        eyes.checkWindow("eq");
    }

    // @Test
    void calculatorTest_full() {
        eyes.check("Calculator!-ignoreCaret", Target.window().ignoreCaret(true));
        eyes.checkWindow("Calculator!");

        int p1 = 5;
        int p2 = 6;

        driver.findElement(By.id("digit_" + p1)).click();
        eyes.check("digit_" + p1 + "-byElement", Target.region(driver.findElement(By.id("digit_" + p1))));

        driver.findElement(By.id("op_add")).click();
        eyes.check("op_add-byElement", Target.region(driver.findElement(By.id("op_add"))));

        driver.findElement(By.id("digit_" + p2)).click();
        eyes.check("digit_" + p2 + "-byElement", Target.region(driver.findElement(By.id("digit_" + p2))));

        driver.findElement(By.id("eq")).click();
        eyes.check("eq-ignoreCaret", Target.window().ignoreCaret(true));
        eyes.checkWindow("eq");
    }
}
