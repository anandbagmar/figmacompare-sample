package io.eot.pipeline.web.selenium;

import java.util.List;

import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.AfterSuite;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.BeforeSuite;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import io.eot.figmacompare.excel.FigmaRow;
import io.eot.figmacompare.web.selenium.WebCompareRunner;

/**
 * Thin TestNG wrapper around WebCompareRunner (in core): drives one @Test invocation per
 * Excel row group and asserts the aggregate pass/fail result collected in afterSuite. See
 * WebCompareRunner's class comment for the actual orchestration logic (single shared
 * VisualGridRunner/BatchInfo, async close, results collected once for the whole suite).
 */
public class CompareWebWithFigmaTest {

    private final WebCompareRunner runner = new WebCompareRunner();

    @BeforeSuite
    public void beforeSuite() {
        WebCompareRunner.beforeSuite();
    }

    @AfterSuite
    public void afterSuite() {
        boolean isPass = WebCompareRunner.afterSuite();
        Assert.assertTrue(isPass, "Visual differences found - see the Figma Excel file for details.");
    }

    @DataProvider(name = "webGroups")
    public Object[][] webGroups() {
        List<List<FigmaRow>> groups = WebCompareRunner.loadWebGroups(System.getProperty("figmaExcel"));
        Object[][] data = new Object[groups.size()][1];
        for (int i = 0; i < groups.size(); i++) {
            data[i][0] = groups.get(i);
        }
        return data;
    }

    @BeforeMethod
    public void beforeMethod() {
        runner.createDriver();
    }

    @AfterMethod
    public void afterMethod() {
        runner.quitDriver();
    }

    @Test(dataProvider = "webGroups")
    public void compareWebGroupWithFigmaBaseline(List<FigmaRow> group) {
        runner.compareGroup(group);
    }
}
