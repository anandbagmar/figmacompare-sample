package io.samples.eyes;

import static io.samples.EyesResults.displayVisualValidationResults;

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

    public static boolean recordAndCheckPass(FigmaRow row, TestResultsSummary summary) {
        AtomicBoolean isPass = new AtomicBoolean(true);
        summary.forEach(testResultContainer -> {
            TestResults testResults = testResultContainer.getTestResults();
            System.out.printf("Test: %s%n%s%n", testResults.getName(), testResultContainer);
            displayVisualValidationResults(testResults);
            applyResult(row, testResults);
            if (!isPassingStatus(testResults.getStatus())) {
                isPass.set(false);
            }
        });
        return isPass.get();
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
