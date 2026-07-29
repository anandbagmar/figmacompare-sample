package io.eot.bajajfinserv.web.selenium;

import static io.eot.figmacompare.EyesResults.displayVisualValidationResults;

import java.io.File;
import java.lang.reflect.Method;
import java.util.concurrent.atomic.AtomicBoolean;

import org.openqa.selenium.WebDriver;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.AfterSuite;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.BeforeSuite;
import org.testng.annotations.Test;

import com.applitools.eyes.AccessibilityGuidelinesVersion;
import com.applitools.eyes.AccessibilityLevel;
import com.applitools.eyes.AccessibilitySettings;
import com.applitools.eyes.BatchInfo;
import com.applitools.eyes.MatchLevel;
import com.applitools.eyes.RectangleSize;
import com.applitools.eyes.StdoutLogHandler;
import com.applitools.eyes.TestResultsStatus;
import com.applitools.eyes.TestResultsSummary;
import com.applitools.eyes.selenium.BrowserType;
import com.applitools.eyes.selenium.Configuration;
import com.applitools.eyes.selenium.Eyes;
import com.applitools.eyes.selenium.StitchMode;
import com.applitools.eyes.visualgrid.services.RunnerOptions;
import com.applitools.eyes.visualgrid.services.VisualGridRunner;

import io.eot.figmacompare.Baseline;
import io.eot.figmacompare.web.selenium.Driver;

public class WebFigmaTest {

    private static final String appName = "Applitools-Images";
    private static final String userName = System.getProperty("user.name");
    private static final String APPLITOOLS_API_KEY = System.getenv("APPLITOOLS_API_KEY");
    private final RectangleSize viewportSize = new RectangleSize(1280, 1024);
    private WebDriver driver;
    private String testName;
    private String baselineName;

    @BeforeSuite
    public static void beforeSuite() {
    }

    @AfterSuite
    public static void afterSuite() {
    }

    @BeforeMethod
    public void beforeMethod(Method method) {
        System.out.println("BeforeMethod: Test: " + method.getName());
        driver = Driver.createDriverFor("chrome");
        this.testName = method.getName();
        this.baselineName = testName + "-baseline";
    }

    private VisualGridRunner initVisualGridRunner() {
        VisualGridRunner visualGridRunner = new VisualGridRunner(new RunnerOptions().testConcurrency(10));
        visualGridRunner.setDontCloseBatches(true);
        return visualGridRunner;
    }

    private BatchInfo initBatchInfo() {
        BatchInfo batch = new BatchInfo(userName + "-" + appName);
        batch.setNotifyOnCompletion(false);
        batch.addProperty("REPOSITORY_NAME", new File(System.getProperty("user.dir")).getName());
        batch.addProperty("APP_NAME", appName);
        return batch;
    }

    private void closeBatch(BatchInfo batch) {
        if (null != batch) {
            batch.setCompleted(true);
        }
    }

    private Eyes initialiseEyes(VisualGridRunner visualGridRunner, BatchInfo batch, String baselineName) {
        Eyes eyes = new Eyes(visualGridRunner);
        Configuration config = new Configuration();
        config.setHostOS(System.getProperty("os.name"));
        config.setAppName(appName);
        config.setBaselineEnvName(baselineName);
        config.setApiKey(APPLITOOLS_API_KEY);
        config.setBatch(batch);
        config.setIsDisabled(Boolean.FALSE);
        config.setForceFullPageScreenshot(true);
        config.setStitchMode(StitchMode.CSS);
        config.setSaveNewTests(Boolean.FALSE);
        config.setMatchLevel(MatchLevel.STRICT);
        config.addProperty("username", userName);
        config.setIgnoreDisplacements(true);
        config.setAccessibilityValidation(
                new AccessibilitySettings(AccessibilityLevel.AA, AccessibilityGuidelinesVersion.WCAG_2_1));

        // Add browsers with different viewports
        config.addBrowser(1280, 1024, BrowserType.CHROME);
        // config.addBrowser(1280, 1024, BrowserType.CHROME_ONE_VERSION_BACK);
        // config.addBrowser(1280, 1024, BrowserType.CHROME_TWO_VERSIONS_BACK);
        // config.addBrowser(1920, 1200, BrowserType.CHROME);
        // config.addBrowser(1440, 1024, BrowserType.FIREFOX);
        // config.addBrowser(1320, 1200, BrowserType.CHROME);
        // config.addBrowser(1024, 1024, BrowserType.FIREFOX);

        // // Add mobile emulation devices in Portrait/Landscape mode
        // config.addDeviceEmulation(DeviceName.iPhone_X, ScreenOrientation.PORTRAIT);
        // config.addDeviceEmulation(DeviceName.Pixel_2, ScreenOrientation.PORTRAIT);

        eyes.setConfiguration(config);
        eyes.setLogHandler(new StdoutLogHandler(true));

        return eyes;

    }

    @AfterMethod
    public void afterMethod(Method method) {
        System.out.println("AfterMethod: Test: " + method.getName());
        if (null != driver) {
            driver.quit();
        }
    }

    private void getResults(VisualGridRunner visualGridRunner, Eyes eyes) {
        System.out.println("AfterMethod: Test: " + testName);
        AtomicBoolean isPass = new AtomicBoolean(true);
        if (null != eyes) {
            eyes.closeAsync();
            TestResultsSummary allTestResults = visualGridRunner.getAllTestResults(false);
            allTestResults.forEach(testResultContainer -> {
                System.out.printf("Test: %s\n%s%n",
                        testResultContainer.getTestResults().getName(),
                        testResultContainer);
                displayVisualValidationResults(testResultContainer.getTestResults());
                TestResultsStatus testResultsStatus = testResultContainer.getTestResults().getStatus();
                if (testResultsStatus.equals(TestResultsStatus.Failed)
                        || testResultsStatus.equals(TestResultsStatus.Unresolved)) {
                    isPass.set(false);
                }
            });
        }
        if (null != driver) {
            driver.quit();
        }
        Assert.assertTrue(isPass.get(), "Visual differences found.");
    }

    @Test
    void checkCardsTest() {
        String baselineImagePath = System.getProperty("user.dir") + File.separator + "downloaded_images"
                + File.separator + "Applitools_homepage.png";
        Baseline.uploadImageAndSetAsBaseline(baselineImagePath, baselineName, appName, testName, viewportSize);
        compareWithUploadedImage(baselineName, "https://bfsd.npu.bfsgodirect.com/cards");
    }

    // @Test
    void checkApplitoolsHomePageTest() {
        String baselineImagePath = System.getProperty("user.dir") + File.separator + "downloaded_images"
                + File.separator + "Applitools_homepage.png";
        Baseline.uploadImageAndSetAsBaseline(baselineImagePath, baselineName, appName, testName, viewportSize);
        compareWithUploadedImage(baselineName, "https://applitools.com");
    }

    private void compareWithUploadedImage(String baselineName, String url) {
        driver.get(url);
        VisualGridRunner visualGridRunner = initVisualGridRunner();
        BatchInfo batchInfo = initBatchInfo();
        Eyes eyesSelenium = initialiseEyes(visualGridRunner, batchInfo, baselineName);
        eyesSelenium.open(driver, appName, testName, viewportSize);
        eyesSelenium.checkWindow("home");
        getResults(visualGridRunner, eyesSelenium);
        closeBatch(batchInfo);
        closeVisualGridRunner(visualGridRunner);
    }

    private static void closeVisualGridRunner(VisualGridRunner visualGridRunner) {
        visualGridRunner.close();
    }

}
