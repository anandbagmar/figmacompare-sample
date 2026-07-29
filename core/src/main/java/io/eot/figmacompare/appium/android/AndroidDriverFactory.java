package io.eot.figmacompare.appium.android;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.time.Duration;

import io.appium.java_client.android.AndroidDriver;
import io.appium.java_client.android.options.UiAutomator2Options;

public class AndroidDriverFactory {

    private AndroidDriverFactory() {
    }

    public static AndroidDriver create(String appiumServerUrl, String apkPath, boolean fullReset) {
        UiAutomator2Options options = new UiAutomator2Options();
        options.setPlatformName("Android");
        options.setAutomationName("UiAutomator2");
        options.setDeviceName("Android");
        options.setPrintPageSourceOnFindFailure(true);
        options.setAutoGrantPermissions(true);
        options.setFullReset(fullReset);
        options.setApp(new File(apkPath).getAbsolutePath());

        System.out.println("UiAutomator2Options:");
        for (String capabilityName : options.getCapabilityNames()) {
            System.out.println("\t" + capabilityName + ": " + options.getCapability(capabilityName));
        }

        try {
            AndroidDriver driver = new AndroidDriver(new URL(appiumServerUrl), options);
            driver.manage().timeouts().implicitlyWait(Duration.ofSeconds(1L));
            return driver;
        } catch (MalformedURLException ex) {
            throw new RuntimeException(
                    "Error creating Appium driver for Android device with capabilities: " + options, ex);
        }
    }
}
