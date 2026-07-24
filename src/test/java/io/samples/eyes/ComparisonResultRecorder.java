package io.samples.eyes;

import static io.samples.EyesResults.displayVisualValidationResults;

import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import com.applitools.eyes.TestResults;
import com.applitools.eyes.TestResultsStatus;
import com.applitools.eyes.TestResultsSummary;

import io.samples.excel.FigmaRow;

/**
 * Records an Applitools comparison result onto a FigmaRow (Comparison Batch URL,
 * Validation Status) and reports whether it passed, for both the Visual Grid (Selenium/web,
 * one test can fan out into several results) and native (Appium/mobile, a single result) cases.
 */
public class ComparisonResultRecorder {

    private ComparisonResultRecorder() {
    }

    /**
     * For a Visual Grid runner shared across many rows: call this once, after every row
     * has submitted its check via closeAsync(), not per row - getAllTestResults() reports
     * cumulatively for every test run on that runner, and results are matched back to
     * their row by test name (the testName each row's eyes.open(...) used).
     */
    public static boolean recordAndCheckPass(Map<String, FigmaRow> rowsByTestName, TestResultsSummary summary) {
        AtomicBoolean allPass = new AtomicBoolean(true);
        summary.forEach(testResultContainer -> {
            TestResults testResults = testResultContainer.getTestResults();
            System.out.printf("Test: %s%n%s%n", testResults.getName(), testResultContainer);
            displayVisualValidationResults(testResults);
            FigmaRow row = rowsByTestName.get(testResults.getName());
            if (null != row) {
                applyResult(row, testResults);
            }
            if (!isPassingStatus(testResults.getStatus())) {
                allPass.set(false);
            }
        });
        return allPass.get();
    }

    public static boolean recordAndCheckPass(FigmaRow row, TestResults testResults) {
        System.out.printf("Test: %s%n%s%n", testResults.getName(), testResults);
        displayVisualValidationResults(testResults);
        applyResult(row, testResults);
        return isPassingStatus(testResults.getStatus());
    }

    private static void applyResult(FigmaRow row, TestResults testResults) {
        row.comparisonBatchUrl = testResults.getUrl();
        row.validationStatus = testResults.getStatus().toString();
    }

    private static boolean isPassingStatus(TestResultsStatus status) {
        return !status.equals(TestResultsStatus.Failed) && !status.equals(TestResultsStatus.Unresolved);
    }
}
