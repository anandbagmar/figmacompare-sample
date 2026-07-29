package io.samples.appium.ios;

import java.io.File;
import java.time.Duration;

import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import io.appium.java_client.AppiumBy;
import io.samples.excel.FigmaRow;

/**
 * Scenario provider (not a TestNG test itself) for the "App Automation Playground" iOS
 * app's Home screen - a standalone, single-screen scenario. See
 * AppAutomationPlaygroundIosPlannerScenarioTest for the multi-screen version of the same
 * app. Registered scenarios are run by CompareIosWithFigma.
 *
 * Uses the same accessibility identifiers as the Android build
 * (AppAutomationPlaygroundAndroidHomeTest) - this app is built as a deliberate
 * cross-platform automation demo, sharing testIDs between platforms. Verify these
 * identifiers actually resolve the first time this runs against the real app; adjust if
 * the iOS build doesn't expose one.
 */
public class AppAutomationPlaygroundIosHomeTest {

    private static final String APP_NAME = "AppAutomationPlayground";
    private static final String APP_PATH = "sampleApps" + File.separator + "MockedE2EDemo.app";

    private AppAutomationPlaygroundIosHomeTest() {
    }

    static {
        IosScenarioRegistry.register("ios-app-automation-playground-home", APP_PATH, APP_NAME,
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
