package io.samples.excel;

import java.io.File;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Shared loading/write-back for the compareWithFigma step (see README_FigmaVisualValidation.md),
 * used by both the Selenium (web) and Appium (mobile) comparison test classes.
 */
public class CompareRows {

    private CompareRows() {
    }

    public static String resolveExcelPath(String systemPropertyName, String defaultPath) {
        String path = System.getProperty(systemPropertyName, defaultPath);
        if (!new File(path).exists()) {
            throw new IllegalStateException("Compare input Excel file not found: " + path
                    + ". Run uploadToFigma first, fill in the Locator column (web rows) for each row you want "
                    + "scoped to a component, save the file at this path, and re-run (or pass -D"
                    + systemPropertyName + "=<path>).");
        }
        return path;
    }

    public static List<FigmaRow> filterByPlatform(List<FigmaRow> allRows, String platform) {
        return allRows.stream()
                .filter(row -> platform.equalsIgnoreCase(row.platform))
                .collect(Collectors.toList());
    }

    public static void writeResultsAndSummary(String inputPath, List<FigmaRow> allRows) {
        String outputPath = ExcelHelper.deriveOutputPath(inputPath);
        ExcelHelper.writeRows(inputPath, allRows, outputPath);
        long passed = allRows.stream().filter(row -> "Passed".equals(row.validationStatus)).count();
        System.out.println();
        System.out.println(passed + " of " + allRows.size() + " row(s) passed. Results written to " + outputPath);
    }
}
