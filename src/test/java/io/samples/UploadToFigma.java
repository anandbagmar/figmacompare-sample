package io.samples;

import java.util.List;

import com.applitools.eyes.RectangleSize;

import io.samples.config.AppConfig;
import io.samples.excel.ExcelHelper;
import io.samples.excel.FigmaRow;
import io.samples.figma.FigmaClient;

/**
 * Reads Figma URLs from an input Excel file, uploads each corresponding Figma image to
 * Applitools Eyes as a baseline, and writes the results to an output Excel file.
 *
 * Expected input columns (header row, any order): Figma URL, UAT/Prod URL, Test Name,
 * Viewport, Scale, Format. Only "Figma URL" is required per row; the rest are optional
 * overrides.
 *
 * Usage: UploadToFigma <inputExcelPath> [forceRefresh: true|false]
 */
public class UploadToFigma {

    private static final String DEFAULT_SCALE = "1";
    private static final String DEFAULT_FORMAT = "png";

    public static void main(String[] args) {
        if (args.length < 1) {
            System.out.println("Usage: UploadToFigma <inputExcelPath> [forceRefresh: true|false]");
            System.exit(1);
        }
        String inputPath = args[0];
        boolean forceRefresh = args.length > 1 && Boolean.parseBoolean(args[1]);

        String figmaToken = AppConfig.get("FIGMA_TOKEN");
        String applitoolsApiKey = AppConfig.get("APPLITOOLS_API_KEY");
        String applitoolsServerUrl = AppConfig.get("APPLITOOLS_SERVER_URL");
        String appName = AppConfig.get("APP_NAME", "Applitools-Images");
        String cacheDir = AppConfig.get("FIGMA_CACHE_DIR", "downloaded_images/figma-cache");

        if (null == figmaToken) {
            throw new IllegalStateException("FIGMA_TOKEN is not set (config.properties or env var)");
        }

        FigmaClient figmaClient = new FigmaClient(figmaToken);
        List<FigmaRow> rows = ExcelHelper.readRows(inputPath);
        System.out.println("Loaded " + rows.size() + " row(s) from " + inputPath);

        for (FigmaRow row : rows) {
            processRow(row, figmaClient, appName, applitoolsApiKey, applitoolsServerUrl, cacheDir, forceRefresh);
        }

        String outputPath = ExcelHelper.deriveOutputPath(inputPath);
        ExcelHelper.writeRows(inputPath, rows, outputPath);
        System.out.println("Wrote results to " + outputPath);
    }

    private static void processRow(FigmaRow row, FigmaClient figmaClient, String appName, String applitoolsApiKey,
            String applitoolsServerUrl, String cacheDir, boolean forceRefresh) {
        System.out.println("Processing: " + row.figmaUrl);
        row.appName = appName;
        try {
            String scale = isBlank(row.scale) ? DEFAULT_SCALE : row.scale;
            String format = isBlank(row.format) ? DEFAULT_FORMAT : row.format;

            if (isBlank(row.testName)) {
                row.testName = sanitizeTestName(figmaClient.fetchNodeName(row.figmaUrl));
            }
            row.baselineEnvName = row.testName + "-baseline";

            RectangleSize viewportSize = parseViewport(row.viewport);

            java.io.File imageFile = figmaClient.getCachedImage(row.figmaUrl, format, scale, cacheDir, forceRefresh);

            BaselineUploadResult result = Baseline.uploadImageAndSetAsBaseline(
                    imageFile.getAbsolutePath(), row.baselineEnvName, appName, row.testName, viewportSize,
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

    private static RectangleSize parseViewport(String viewport) {
        if (isBlank(viewport)) {
            return null;
        }
        String[] parts = viewport.toLowerCase().split("x");
        if (parts.length != 2) {
            throw new IllegalArgumentException("Viewport must be in WIDTHxHEIGHT format, got: " + viewport);
        }
        return new RectangleSize(Integer.parseInt(parts[0].trim()), Integer.parseInt(parts[1].trim()));
    }

    private static String sanitizeTestName(String name) {
        return name.trim().replaceAll("[^a-zA-Z0-9_-]+", "_");
    }

    private static boolean isBlank(String value) {
        return null == value || value.isBlank();
    }
}
