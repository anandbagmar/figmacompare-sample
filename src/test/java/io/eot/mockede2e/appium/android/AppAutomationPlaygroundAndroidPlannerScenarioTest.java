package io.eot.mockede2e.appium.android;

import java.io.File;
import java.time.Duration;

import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import io.appium.java_client.AppiumBy;
import io.eot.figmacompare.appium.android.AndroidScenarioRegistry;
import io.eot.figmacompare.excel.FigmaRow;

/**
 * Scenario provider (not a TestNG test itself) for the "App Automation
 * Playground" Android
 * app's Community Meeting Planner flow - a 4-screen scenario in one continuous
 * app
 * session: Home -> Choose the planner flow -> Meeting Planner (Step 1) ->
 * Native Detail
 * (Step 2A). Registered scenarios are run by CompareAndroidWithFigma.
 *
 * The Excel rows for this scenario must be listed in exactly this screen order,
 * since
 * eyes.checkWindow(...) is called once per row in rows.get(i) order as this
 * flow
 * navigates forward - there is no going back to an earlier step.
 */
public class AppAutomationPlaygroundAndroidPlannerScenarioTest {

    private static final String APP_NAME = "AppAutomationPlayground";
    private static final String APK_NAME = "sampleApps" + File.separator + "App Automation Playground-debug.apk";
    private static final Duration WAIT_TIMEOUT = Duration.ofSeconds(15);

    private AppAutomationPlaygroundAndroidPlannerScenarioTest() {
    }

    static {
        AndroidScenarioRegistry.register("android-app-automation-playground-planner-scenario", APK_NAME, APP_NAME,
                (driver, eyes, rows) -> {
                    WebDriverWait wait = new WebDriverWait(driver, WAIT_TIMEOUT);

                    // Step 1: Home screen.
                    wait.until(ExpectedConditions.presenceOfElementLocated(AppiumBy.accessibilityId("home.screen")));
                    eyes.checkWindow(resolveStepName(rows.get(0)));

                    // Step 2: tap "Community Meeting Planner" -> "Choose the planner flow" dialog.
                    driver.findElement(AppiumBy.accessibilityId("home.button.planner")).click();
                    wait.until(ExpectedConditions.presenceOfElementLocated(
                            AppiumBy.accessibilityId("planner.mode.button.original")));

                    // Step 3: tap "Open Original Flow" -> Meeting Planner, Step 1 "Build the event
                    // mood".
                    driver.findElement(AppiumBy.accessibilityId("planner.mode.button.original")).click();
                    wait.until(ExpectedConditions.presenceOfElementLocated(
                            AppiumBy.accessibilityId("planner.button.next.native")));
                    eyes.checkWindow(resolveStepName(rows.get(1)));

                    // Step 4: tap "Next: Native Detail" -> Step 2A native screen.
                    driver.findElement(AppiumBy.accessibilityId("planner.button.next.native")).click();
                    wait.until(ExpectedConditions.presenceOfElementLocated(
                            AppiumBy.accessibilityId("nativeJourney.button.continue")));
                    eyes.checkWindow(resolveStepName(rows.get(2)));
                });
    }

    private static String resolveStepName(FigmaRow row) {
        return (null == row.testName || row.testName.isBlank()) ? row.appUrlOrScreenName : row.testName;
    }
}
