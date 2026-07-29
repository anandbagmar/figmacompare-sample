package io.eot.bajajfinserv.appium.android;

import java.io.File;

import io.eot.figmacompare.appium.android.AndroidScenarioRegistry;
import io.eot.figmacompare.excel.FigmaRow;

/**
 * Scenario provider (not a TestNG test class itself) for the Bajaj Finserv Android app.
 * Registers this app's scenarios into AndroidScenarioRegistry - the actual test execution
 * happens in CompareAndroidWithFigma, which discovers and runs any registered scenario
 * regardless of which provider class registered it. See README_FigmaVisualValidation.md.
 *
 * Every scenario here must be fully self-contained: reaching even a single mobile screen
 * can require login/navigation specific to that screen, so each entry does whatever the
 * real app requires to reach its screen(s) from a fresh launch, then calls
 * eyes.checkWindow(...) once per step it wants recorded.
 */
public class BajajFinservAndroidTest {

    private static final String APP_NAME = "BajajFinservAndroidApp";
    private static final String APK_NAME = "sampleApps" + File.separator + "app_npu_v8.3.17.apk";

    private BajajFinservAndroidTest() {
    }

    static {
        AndroidScenarioRegistry.register("android-home-screen", APK_NAME, APP_NAME, (driver, eyes, rows) -> {
            // The app opens directly on the home screen after launch - nothing to navigate.
            eyes.checkWindow(resolveStepName(rows.get(0)));
        });
    }

    private static String resolveStepName(FigmaRow row) {
        return (null == row.testName || row.testName.isBlank()) ? row.appUrlOrScreenName : row.testName;
    }
}
