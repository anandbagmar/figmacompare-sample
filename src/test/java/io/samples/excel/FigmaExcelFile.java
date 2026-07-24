package io.samples.excel;

import java.io.File;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import io.samples.config.AppConfig;

/**
 * Path resolution, platform/skip filtering, and load/save for the single, unified Figma
 * visual-testing Excel file that both uploadFromFigma and compareWithFigma (web/mobile) read
 * from and write back to in place. See README_FigmaVisualValidation.md.
 */
public class FigmaExcelFile {

    private static final String DEFAULT_PATH = "figma-visual-testing/figma_visual_tests.xlsx";
    private static final Set<String> SKIP_VALUES = Set.of("true", "t", "yes", "y", "skip");

    private FigmaExcelFile() {
    }

    /**
     * Resolves the Excel file path, in priority order: an explicit override (e.g. a
     * command-line arg or -PfigmaExcel= passed through as a system property), then the
     * FIGMA_EXCEL_FILE key in config.properties (itself overridable by an env var of the
     * same name), then a built-in default.
     */
    public static String resolvePath(String explicitPathOverride) {
        String path = (null != explicitPathOverride && !explicitPathOverride.isBlank())
                ? explicitPathOverride
                : AppConfig.get("FIGMA_EXCEL_FILE", DEFAULT_PATH);
        if (!new File(path).exists()) {
            throw new IllegalStateException("Figma Excel file not found: " + path + System.lineSeparator()
                    + "Copy " + AppConfig.TEMPLATES_DIR + "/figma_visual_tests_template.xlsx to " + path
                    + ", fill in your rows, and re-run - or point at a different file via -PfigmaExcel=<path>, "
                    + "the FIGMA_EXCEL_FILE environment variable, or FIGMA_EXCEL_FILE in config.properties.");
        }
        return path;
    }

    public static List<FigmaRow> excludeSkipped(List<FigmaRow> allRows) {
        return allRows.stream().filter(row -> !isSkipped(row)).collect(Collectors.toList());
    }

    public static List<FigmaRow> filterByPlatform(List<FigmaRow> allRows, String platform) {
        return allRows.stream()
                .filter(row -> platform.equalsIgnoreCase(row.platform))
                .filter(row -> !isSkipped(row))
                .collect(Collectors.toList());
    }

    public static boolean isSkipped(FigmaRow row) {
        return null != row.skip && SKIP_VALUES.contains(row.skip.trim().toLowerCase());
    }
}
