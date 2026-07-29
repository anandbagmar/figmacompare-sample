package io.samples;

import com.applitools.eyes.RectangleSize;
import com.applitools.eyes.TestResults;

public class BaselineUploadResult {

    private final TestResults testResults;
    private final RectangleSize viewportSize;

    public BaselineUploadResult(TestResults testResults, RectangleSize viewportSize) {
        this.testResults = testResults;
        this.viewportSize = viewportSize;
    }

    public TestResults getTestResults() {
        return testResults;
    }

    public RectangleSize getViewportSize() {
        return viewportSize;
    }
}
