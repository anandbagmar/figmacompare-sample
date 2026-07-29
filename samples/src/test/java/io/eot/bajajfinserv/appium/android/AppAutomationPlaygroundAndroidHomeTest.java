package io.eot.bajajfinserv.appium.android;

import java.io.File;
import java.time.Duration;

import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import io.appium.java_client.AppiumBy;
import io.eot.figmacompare.appium.android.AndroidScenarioRegistry;
import io.eot.figmacompare.excel.FigmaRow;

/**
 * Scenario provider (not a TestNG test itself) for the "App Automation Playground" Android
 * app's Home screen - a standalone, single-screen scenario. See
 * AppAutomationPlaygroundAndroidPlannerScenarioTest for the multi-screen version of the
 * same app. Registered scenarios are run by CompareAndroidWithFigma.
 */
public class AppAutomationPlaygroundAndroidHomeTest {

    private static final String APP_NAME = "AppAutomationPlayground";
    private static final String APK_NAME = "sampleApps" + File.separator + "App Automation Playground-debug.apk";

    private AppAutomationPlaygroundAndroidHomeTest() {
    }

    static {
        AndroidScenarioRegistry.register("android-app-automation-playground-home", APK_NAME, APP_NAME,
                (driver, eyes, rows) -> {
                    new WebDriverWait(driver, Duration.ofSeconds(15)).until(
                            ExpectedConditions.presenceOfElementLocated(AppiumBy.accessibilityId("home.screen")));
                    eyes.checkWindow(resolveStepName(rows.get(0)));
                });
    }

    private static String resolveStepName(FigmaRow row) {
        return (null == row.testName || row.testName.isBlank()) ? row.appUrlOrScreenName : row.testName;
    }
}
