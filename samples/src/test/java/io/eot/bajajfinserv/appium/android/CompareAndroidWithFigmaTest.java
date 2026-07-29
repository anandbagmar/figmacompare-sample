package io.eot.bajajfinserv.appium.android;

import java.util.List;

import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.AfterSuite;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.BeforeSuite;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import io.eot.figmacompare.appium.android.AndroidCompareRunner;
import io.eot.figmacompare.appium.android.AndroidScenarioRegistry;
import io.eot.figmacompare.excel.FigmaExcelFile;
import io.eot.figmacompare.excel.FigmaRow;

/**
 * Thin TestNG wrapper around AndroidCompareRunner (in core): bootstraps every known
 * Android scenario provider class (so their static registrations into
 * AndroidScenarioRegistry run), then drives one @Test invocation per Excel row group. See
 * README_FigmaVisualValidation.md for how rows/groups/scenarios work, and
 * AndroidCompareRunner's class comment for the actual orchestration logic.
 *
 * Adding a new provider class only requires adding it to PROVIDER_CLASSES below - core
 * itself has no knowledge of any specific provider.
 */
public class CompareAndroidWithFigmaTest {

    private static final String[] PROVIDER_CLASSES = {
            "io.eot.bajajfinserv.appium.android.BajajFinservAndroidTest",
            "io.eot.bajajfinserv.appium.android.AppAutomationPlaygroundAndroidHomeTest",
            "io.eot.bajajfinserv.appium.android.AppAutomationPlaygroundAndroidPlannerScenarioTest",
    };

    private final AndroidCompareRunner runner = new AndroidCompareRunner();

    @BeforeSuite
    public void beforeSuite() {
        for (String providerClass : PROVIDER_CLASSES) {
            AndroidScenarioRegistry.loadProviderClass(providerClass);
        }
        AndroidCompareRunner.beforeSuite();
    }

    @AfterSuite
    public void afterSuite() {
        AndroidCompareRunner.afterSuite();
    }

    @DataProvider(name = "androidGroups")
    public Object[][] androidGroups() {
        List<List<FigmaRow>> groups = AndroidCompareRunner.loadAndroidGroups(System.getProperty("figmaExcel"));
        Object[][] data = new Object[groups.size()][1];
        for (int i = 0; i < groups.size(); i++) {
            data[i][0] = groups.get(i);
        }
        return data;
    }

    @BeforeMethod
    public void beforeMethod(Object[] testParams) {
        @SuppressWarnings("unchecked")
        List<FigmaRow> group = (List<FigmaRow>) testParams[0];
        runner.createDriverForGroup(group);
    }

    @AfterMethod
    public void afterMethod() {
        runner.quitDriver();
    }

    @Test(dataProvider = "androidGroups")
    public void compareAndroidGroupWithFigmaBaseline(List<FigmaRow> group) {
        String scenarioName = FigmaExcelFile.scenarioNameOf(group.get(0));
        boolean isPass = runner.compareGroup(group);
        Assert.assertTrue(isPass, "Visual differences found for scenario: " + scenarioName);
    }
}
