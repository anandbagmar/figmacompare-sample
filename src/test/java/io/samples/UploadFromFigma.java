package io.samples;

import java.util.List;

import com.applitools.eyes.EyesRunner;
import com.applitools.eyes.RectangleSize;
import com.applitools.eyes.images.ImageRunner;

import io.samples.config.AppConfig;
import io.samples.excel.ExcelHelper;
import io.samples.excel.FigmaExcelFile;
import io.samples.excel.FigmaRow;
import io.samples.figma.FigmaClient;

/**
 * Reads Figma URLs from the shared Figma Excel file, uploads each corresponding Figma
 * image to Applitools Eyes as a baseline, and writes the results back to the same file.
 *
 * Expected columns (header row, any order): Figma URL, Platform, App URL / Screen Name,
 * Test Name, Baseline Env Name, Viewport, Scale, Format, Skip. Only "Figma URL" is
 * required per row; the rest are optional. "Baseline Env Name" is auto-derived as
 * "{testName}-baseline" if left blank, or used as-is if provided. Rows with "Skip" set to
 * true/t/yes/y/skip (case-insensitive) are left untouched and not processed.
 *
 * Usage: UploadFromFigma [figmaExcelPath] [forceRefresh: true|false]
 * figmaExcelPath, if omitted, falls back to -DfigmaExcel, then FIGMA_EXCEL_FILE in
 * config.properties/env, then a built-in default (see FigmaExcelFile).
 */
public class UploadFromFigma {

    private static final String DEFAULT_SCALE = "1";
    private static final String DEFAULT_FORMAT = "png";

    public static void main(String[] args) {
        String pathOverride = args.length > 0 ? args[0] : System.getProperty("figmaExcel");
        String figmaExcelPath = FigmaExcelFile.resolvePath(pathOverride);
        boolean forceRefresh = args.length > 1 ? Boolean.parseBoolean(args[1]) : Boolean.getBoolean("forceRefresh");

        String figmaToken = AppConfig.get("FIGMA_TOKEN");
        String applitoolsApiKey = AppConfig.get("APPLITOOLS_API_KEY");
        String applitoolsServerUrl = AppConfig.get("APPLITOOLS_SERVER_URL");
        String appName = AppConfig.get("APP_NAME", "Applitools-Images");
        String cacheDir = AppConfig.get("FIGMA_CACHE_DIR", "downloaded_images/figma-cache");

        String configFilePath = AppConfig.CONFIG_DIR + "/" + AppConfig.CONFIG_FILE_NAME;
        if (null == figmaToken) {
            throw new IllegalStateException("FIGMA_TOKEN is not set. Open " + configFilePath
                    + " and fill in FIGMA_TOKEN (or set it as an environment variable).");
        }
        if (null == applitoolsApiKey) {
            throw new IllegalStateException("APPLITOOLS_API_KEY is not set. Open " + configFilePath
                    + " and fill in APPLITOOLS_API_KEY (or set it as an environment variable).");
        }
        if (null == applitoolsServerUrl) {
            throw new IllegalStateException("APPLITOOLS_SERVER_URL is not set. Open " + configFilePath
                    + " and fill in APPLITOOLS_SERVER_URL (or set it as an environment variable).");
        }

        FigmaClient figmaClient = new FigmaClient(figmaToken);
        List<FigmaRow> allRows = ExcelHelper.readRows(figmaExcelPath);
        List<FigmaRow> toProcess = FigmaExcelFile.excludeSkipped(allRows);
        System.out.println("Loaded " + allRows.size() + " row(s) from " + figmaExcelPath + " ("
                + (allRows.size() - toProcess.size()) + " skipped)");

        // One EyesRunner (and its background "universal core" process) shared across the
        // whole batch, instead of starting/stopping it per row.
        EyesRunner runner = new ImageRunner();
        try {
            for (FigmaRow row : toProcess) {
                processRow(runner, row, figmaClient, appName, applitoolsApiKey, applitoolsServerUrl, cacheDir,
                        forceRefresh);
            }
        } finally {
            runner.close();
        }

        ExcelHelper.writeRows(figmaExcelPath, allRows);

        long succeeded = toProcess.stream().filter(r -> "Success".equals(r.status)).count();
        System.out.println();
        System.out.println(succeeded + " of " + toProcess.size() + " succeeded. Results written to "
                + figmaExcelPath);
    }

    private static void processRow(EyesRunner runner, FigmaRow row, FigmaClient figmaClient, String appName,
            String applitoolsApiKey, String applitoolsServerUrl, String cacheDir, boolean forceRefresh) {
        System.out.println("Processing: " + row.figmaUrl);
        row.appName = appName;
        try {
            String scale = isBlank(row.scale) ? DEFAULT_SCALE : row.scale;
            String format = isBlank(row.format) ? DEFAULT_FORMAT : row.format;

            if (isBlank(row.testName)) {
                row.testName = sanitizeTestName(figmaClient.fetchNodeName(row.figmaUrl));
            }
            if (isBlank(row.baselineEnvName)) {
                row.baselineEnvName = row.testName + "-baseline";
            }

            RectangleSize viewportSize = ExcelHelper.parseViewport(row.viewport);

            java.io.File imageFile = figmaClient.getCachedImage(row.figmaUrl, format, scale, cacheDir, forceRefresh);

            BaselineUploadResult result = Baseline.uploadImageAndSetAsBaseline(
                    runner, imageFile.getAbsolutePath(), row.baselineEnvName, appName, row.testName, viewportSize,
                    applitoolsApiKey, applitoolsServerUrl);

            row.viewport = result.getViewportSize().getWidth() + "x" + result.getViewportSize().getHeight();
            if (null != result.getTestResults()) {
                row.baselineBatchUrl = result.getTestResults().getUrl();
                row.status = "Success";
            } else {
                row.status = "Failed";
                row.errorMessage = "See console/log output for details";
            }
        } catch (Exception ex) {
            row.status = "Failed";
            row.errorMessage = ex.getMessage();
            System.out.println(ex);
            ex.printStackTrace();
        }
    }

    private static String sanitizeTestName(String name) {
        return name.trim().replaceAll("[^a-zA-Z0-9_-]+", "_");
    }

    private static boolean isBlank(String value) {
        return null == value || value.isBlank();
    }
}
