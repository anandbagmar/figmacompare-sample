package io.eot.pipeline.appium.ios;

import java.util.List;

import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.AfterSuite;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.BeforeSuite;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import io.eot.figmacompare.appium.ios.IosCompareRunner;
import io.eot.figmacompare.appium.ios.IosScenarioRegistry;
import io.eot.figmacompare.excel.FigmaExcelFile;
import io.eot.figmacompare.excel.FigmaRow;

/**
 * Thin TestNG wrapper around IosCompareRunner (in core): bootstraps every known iOS
 * scenario provider class (so their static registrations into IosScenarioRegistry run),
 * then drives one @Test invocation per Excel row group. See CompareAndroidWithFigmaTest
 * (the Android equivalent) for the full rationale.
 *
 * Adding a new provider class only requires adding it to PROVIDER_CLASSES below - core
 * itself has no knowledge of any specific provider.
 */
public class CompareIosWithFigmaTest {

    private static final String[] PROVIDER_CLASSES = {
            "io.eot.mockede2e.appium.ios.AppAutomationPlaygroundIosHomeTest",
            "io.eot.mockede2e.appium.ios.AppAutomationPlaygroundIosPlannerScenarioTest",
    };

    private final IosCompareRunner runner = new IosCompareRunner();

    @BeforeSuite
    public void beforeSuite() {
        for (String providerClass : PROVIDER_CLASSES) {
            IosScenarioRegistry.loadProviderClass(providerClass);
        }
        IosCompareRunner.beforeSuite();
    }

    @AfterSuite
    public void afterSuite() {
        IosCompareRunner.afterSuite();
    }

    @DataProvider(name = "iosGroups")
    public Object[][] iosGroups() {
        List<List<FigmaRow>> groups = IosCompareRunner.loadIosGroups(System.getProperty("figmaExcel"));
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

    @Test(dataProvider = "iosGroups")
    public void compareIosGroupWithFigmaBaseline(List<FigmaRow> group) {
        String scenarioName = FigmaExcelFile.scenarioNameOf(group.get(0));
        boolean isPass = runner.compareGroup(group);
        Assert.assertTrue(isPass, "Visual differences found for scenario: " + scenarioName);
    }
}
