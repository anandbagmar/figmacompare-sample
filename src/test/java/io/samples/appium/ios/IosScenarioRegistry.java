package io.samples.appium.ios;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import io.samples.appium.ScenarioFlow;

/**
 * Central lookup of every iOS "Scenario Name" -> the app (.app bundle) and ScenarioFlow
 * that implement it, regardless of which test class registered it. Mirrors
 * AndroidScenarioRegistry - see its class comment for the full rationale.
 */
public class IosScenarioRegistry {

    private static final Map<String, Registration> SCENARIOS = new LinkedHashMap<>();

    private IosScenarioRegistry() {
    }

    public static void register(String scenarioName, String appPath, String appName, ScenarioFlow flow) {
        if (SCENARIOS.containsKey(scenarioName)) {
            throw new IllegalStateException("Scenario \"" + scenarioName + "\" is already registered - "
                    + "check for a duplicate registration across test classes.");
        }
        SCENARIOS.put(scenarioName, new Registration(appPath, appName, flow));
    }

    public static Registration get(String scenarioName) {
        return SCENARIOS.get(scenarioName);
    }

    public static Set<String> registeredScenarioNames() {
        return SCENARIOS.keySet();
    }

    /**
     * Every class that registers iOS scenarios must be listed here, so its static
     * initializer actually runs before a test run looks its scenarios up - see
     * AndroidScenarioRegistry.ensureAllProvidersRegistered() for why. Add one line here
     * for every new iOS app test/provider class.
     */
    public static void ensureAllProvidersRegistered() {
        loadClass("io.samples.appium.ios.AppAutomationPlaygroundIosHomeTest");
        loadClass("io.samples.appium.ios.AppAutomationPlaygroundIosPlannerScenarioTest");
    }

    private static void loadClass(String className) {
        try {
            Class.forName(className);
        } catch (ClassNotFoundException ex) {
            throw new RuntimeException("Could not load iOS scenario provider class: " + className, ex);
        }
    }

    public static class Registration {
        public final String appPath;
        public final String appName;
        public final ScenarioFlow flow;

        public Registration(String appPath, String appName, ScenarioFlow flow) {
            this.appPath = appPath;
            this.appName = appName;
            this.flow = flow;
        }
    }
}
