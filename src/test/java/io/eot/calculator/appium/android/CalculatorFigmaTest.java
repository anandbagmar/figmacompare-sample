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

import io.appium.java_client.AppiumDriver;
import io.appium.java_client.service.local.AppiumDriverLocalService;
import io.eot.figmacompare.Baseline;
import io.eot.figmacompare.appium.AppiumServerSupport;
import io.eot.figmacompare.appium.android.AndroidDriverFactory;
import io.eot.figmacompare.eyes.BatchSupport;
import io.eot.figmacompare.eyes.MobileEyesSupport;

class CalculatorFigmaTest {
    private static final String className = CalculatorFigmaTest.class.getSimpleName();
    private static final boolean IS_FULL_RESET = true;
    private static BatchInfo batch;
    private static String APPIUM_SERVER_URL = "http://localhost:4723/wd/hub/";
    private static AppiumDriverLocalService localAppiumServer;
    private static String APK_NAME = "sampleApps" + File.separator + "Calculator_8.4.1.apk";
    private static boolean IS_EYES_ENABLED = true;
    private AppiumDriver driver;
    private Eyes eyes;
    private String testName;
    private String baselineName;

    private CalculatorFigmaTest() {

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
        this.testName = testInfo.getName();
        this.baselineName = testName + "-baseline";
        System.out.printf("Test: %s - BeforeEach%n", testName);
        setUpAndroid(testInfo);
        eyes = MobileEyesSupport.open(driver, batch, className, testName, baselineName, IS_EYES_ENABLED);
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
