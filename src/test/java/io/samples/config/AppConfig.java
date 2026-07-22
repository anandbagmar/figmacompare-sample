package io.samples.config;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class AppConfig {

    private static final Properties PROPERTIES = load();

    private AppConfig() {
    }

    private static Properties load() {
        Properties properties = new Properties();
        try (InputStream in = AppConfig.class.getClassLoader().getResourceAsStream("config.properties")) {
            if (null != in) {
                properties.load(in);
            }
        } catch (IOException ex) {
            throw new RuntimeException("Unable to load config.properties", ex);
        }
        return properties;
    }

    public static String get(String key) {
        String envValue = System.getenv(key);
        if (null != envValue && !envValue.isBlank()) {
            return envValue;
        }
        String propValue = PROPERTIES.getProperty(key);
        return (null != propValue && !propValue.isBlank()) ? propValue : null;
    }

    public static String get(String key, String defaultValue) {
        String value = get(key);
        return null != value ? value : defaultValue;
    }
}
