package io.samples;

import java.awt.image.BufferedImage;
import java.io.File;

import javax.imageio.ImageIO;

import com.applitools.eyes.BatchInfo;
import com.applitools.eyes.EyesRunner;
import com.applitools.eyes.RectangleSize;
import com.applitools.eyes.TestResults;
import com.applitools.eyes.images.ImageRunner;
import com.applitools.eyes.images.Target;

public class Baseline {

    public static void uploadImageAndSetAsBaseline(String baseLineFilePath, String baselineName, String appName,
            String testName, RectangleSize viewportSize) {
        EyesRunner runner = new ImageRunner();
        try {
            doUpload(runner, baseLineFilePath, baselineName, appName, testName, viewportSize, null, null, null);
        } finally {
            runner.close();
        }
    }

    public static BaselineUploadResult uploadImageAndSetAsBaseline(String baseLineFilePath, String baselineName,
            String appName, String testName, RectangleSize viewportSize, String apiKey, String serverUrl) {
        EyesRunner runner = new ImageRunner();
        try {
            return uploadImageAndSetAsBaseline(runner, baseLineFilePath, baselineName, appName, testName,
                    viewportSize, apiKey, serverUrl, null);
        } finally {
            runner.close();
        }
    }

    /**
     * Same as the other overload, but reuses a caller-supplied, caller-owned EyesRunner
     * instead of creating (and closing) a new one per call, and lets the caller pin an
     * explicit batch name. Applitools' EyesRunner starts a background "universal core"
     * process on creation - reuse one runner across many uploads (e.g. a batch loop)
     * instead of repeatedly starting/stopping it per image. Without an explicit batch
     * name, the SDK lazily creates a default batch on the first eyes.open() call and
     * reuses it for every later test on the same runner - so when sharing a runner across
     * a loop, always pass a real batchName here, or every row ends up sharing whichever
     * batch got auto-created for the first row. The caller is responsible for calling
     * runner.close() once, after all uploads.
     */
    public static BaselineUploadResult uploadImageAndSetAsBaseline(EyesRunner runner, String baseLineFilePath,
            String baselineName, String appName, String testName, RectangleSize viewportSize, String apiKey,
            String serverUrl, String batchName) {
        if (null == apiKey || apiKey.isBlank()) {
            throw new IllegalStateException("APPLITOOLS_API_KEY is required but was null/blank. "
                    + "Set it in config.properties or as an environment variable.");
        }
        if (null == serverUrl || serverUrl.isBlank()) {
            throw new IllegalStateException("APPLITOOLS_SERVER_URL is required but was null/blank. "
                    + "Set it in config.properties or as an environment variable.");
        }
        return doUpload(runner, baseLineFilePath, baselineName, appName, testName, viewportSize, apiKey, serverUrl,
                batchName);
    }

    private static BaselineUploadResult doUpload(EyesRunner runner, String baseLineFilePath, String baselineName,
            String appName, String testName, RectangleSize viewportSize, String apiKey, String serverUrl,
            String batchName) {
        com.applitools.eyes.images.Eyes eyesImages = new com.applitools.eyes.images.Eyes(runner);
        eyesImages.setBaselineEnvName(baselineName);
        com.applitools.eyes.config.Configuration config = eyesImages.getConfiguration();
        config.setHostOS(System.getProperty("os.name"));
        config.setHostApp(appName);
        config.setBaselineEnvName(baselineName);
        config.setSaveNewTests(Boolean.TRUE);
        if (null != apiKey) {
            config.setApiKey(apiKey);
        }
        if (null != serverUrl) {
            config.setServerUrl(serverUrl);
        }
        if (null != batchName && !batchName.isBlank()) {
            config.setBatch(new BatchInfo(batchName));
        }
        eyesImages.setConfiguration(config);

        try {
            File imageFile = new File(baseLineFilePath);
            System.out.println("Image File '" + imageFile.getName() + "' exists? " + imageFile.exists());
            BufferedImage img = ImageIO.read(imageFile);
            System.out.println("Image read");
            if (null == viewportSize) {
                viewportSize = new RectangleSize(img.getWidth(), img.getHeight());
                System.out.println(
                        "Viewport is not provided. Using provided image's size: " + img.getWidth() + " x "
                                + img.getHeight() + " pixels");
            }
            eyesImages.open(appName, testName, viewportSize);
            eyesImages.check(imageFile.getName(), Target.image(img));
            System.out.println("After eyes.check");
            TestResults testResults = eyesImages.close(false);
            System.out.println("TestResults: " + testResults);
            return new BaselineUploadResult(testResults, viewportSize);
        } catch (Exception ex) {
            System.out.println(ex);
            ex.printStackTrace();
            return new BaselineUploadResult(null, viewportSize);
        } finally {
            eyesImages.abortIfNotClosed();
        }
    }
}
