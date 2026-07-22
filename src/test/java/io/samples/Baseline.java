package io.samples;

import java.awt.image.BufferedImage;
import java.io.File;

import javax.imageio.ImageIO;

import com.applitools.eyes.EyesRunner;
import com.applitools.eyes.RectangleSize;
import com.applitools.eyes.TestResults;
import com.applitools.eyes.images.ImageRunner;
import com.applitools.eyes.images.Target;

public class Baseline {

    public static void uploadImageAndSetAsBaseline(String baseLineFilePath, String baselineName, String appName,
            String testName, RectangleSize viewportSize) {
        EyesRunner runner = new ImageRunner();
        com.applitools.eyes.images.Eyes eyesImages = new com.applitools.eyes.images.Eyes(runner);
        eyesImages.setBaselineEnvName(baselineName);
        com.applitools.eyes.config.Configuration config = eyesImages.getConfiguration();
        config.setHostOS(System.getProperty("os.name"));
        config.setHostApp(appName);
        config.setBaselineEnvName(baselineName);
        config.setSaveNewTests(Boolean.TRUE);
        eyesImages.setConfiguration(config);

        try {
            File imageFile = new File(baseLineFilePath);
            System.out.println("Image File '" + imageFile.getName() + "' exists? " + imageFile.exists());
            BufferedImage img = ImageIO.read(imageFile);
            System.out.println("Image read");
            if (null == viewportSize) {
                RectangleSize imageSize = new RectangleSize(
                        img.getWidth(),
                        img.getHeight());
                viewportSize = imageSize;
                System.out.println(
                        "Viewport is not provided. Using provided image's size: " + img.getWidth() + " x "
                                + img.getHeight() + " pixels");
            }
            eyesImages.open(appName, testName, viewportSize);
            eyesImages.check(imageFile.getName(), Target.image(img));
            System.out.println("After eyes.check");
            TestResults testResults = eyesImages.close(false);
            System.out.println("TestResults: " + testResults);
        } catch (Exception ex) {
            System.out.println(ex);
            ex.printStackTrace();
        } finally {
            eyesImages.abortIfNotClosed();
        }
    }
}
