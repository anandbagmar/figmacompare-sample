package io.samples.appium.android;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import io.samples.appium.ScenarioFlow;

/**
 * Central lookup of every Android "Scenario Name" -> the app (APK) and ScenarioFlow that
 * implement it, regardless of which test class registered it. A Scenario Name used in the
 * shared Figma Excel file can be implemented in any class - CompareAndroidWithFigma is the
 * one runner that finds and executes it by name, so app-specific classes (like
 * BajajFinservAndroidTest) only need to register their scenarios here, not run their own
 * TestNG suite.
 */
public class AndroidScenarioRegistry {

    private static final Map<String, Registration> SCENARIOS = new LinkedHashMap<>();

    private AndroidScenarioRegistry() {
    }

    public static void register(String scenarioName, String apkPath, String appName, ScenarioFlow flow) {
        if (SCENARIOS.containsKey(scenarioName)) {
            throw new IllegalStateException("Scenario \"" + scenarioName + "\" is already registered - "
                    + "check for a duplicate registration across test classes.");
        }
        SCENARIOS.put(scenarioName, new Registration(apkPath, appName, flow));
    }

    public static Registration get(String scenarioName) {
        return SCENARIOS.get(scenarioName);
    }

    public static Set<String> registeredScenarioNames() {
        return SCENARIOS.keySet();
    }

    /**
     * Every class that registers Android scenarios must be listed here, so its static
     * initializer actually runs (and its scenarios get registered) before a test run looks
     * them up - Java only runs a class's static initializer once that class has been
     * loaded/referenced, so an unreferenced provider class would otherwise never register
     * anything. Add one line here for every new Android app test/provider class.
     */
    public static void ensureAllProvidersRegistered() {
        loadClass("io.samples.appium.android.BajajFinservAndroidTest");
        loadClass("io.samples.appium.android.AppAutomationPlaygroundAndroidHomeTest");
        loadClass("io.samples.appium.android.AppAutomationPlaygroundAndroidPlannerScenarioTest");
    }

    private static void loadClass(String className) {
        try {
            Class.forName(className);
        } catch (ClassNotFoundException ex) {
            throw new RuntimeException("Could not load Android scenario provider class: " + className, ex);
        }
    }

    public static class Registration {
        public final String apkPath;
        public final String appName;
        public final ScenarioFlow flow;

        public Registration(String apkPath, String appName, ScenarioFlow flow) {
            this.apkPath = apkPath;
            this.appName = appName;
            this.flow = flow;
        }
    }
}
