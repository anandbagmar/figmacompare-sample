package io.samples.excel;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

public class ExcelHelper {

    private static final DataFormatter DATA_FORMATTER = new DataFormatter();

    private static final String COL_FIGMA_URL = "Figma URL";
    private static final String COL_TARGET_URL = "UAT/Prod URL";
    private static final String COL_TEST_NAME = "Test Name";
    private static final String COL_VIEWPORT = "Viewport";
    private static final String COL_SCALE = "Scale";
    private static final String COL_FORMAT = "Format";
    private static final String COL_APP_NAME = "App Name";
    private static final String COL_BASELINE_ENV_NAME = "Baseline Env Name";
    private static final String COL_BASELINE_BATCH_URL = "Baseline Batch URL";
    private static final String COL_STATUS = "Status";
    private static final String COL_ERROR_MESSAGE = "Error Message";

    private static final List<String> OUTPUT_ONLY_COLUMNS = List.of(
            COL_APP_NAME, COL_BASELINE_ENV_NAME, COL_BASELINE_BATCH_URL, COL_STATUS, COL_ERROR_MESSAGE);

    private ExcelHelper() {
    }

    public static List<FigmaRow> readRows(String inputPath) {
        List<FigmaRow> rows = new ArrayList<>();
        try (FileInputStream fis = new FileInputStream(inputPath); Workbook workbook = new XSSFWorkbook(fis)) {
            Sheet sheet = workbook.getSheetAt(0);
            Map<String, Integer> headerIndex = readHeaderIndex(sheet.getRow(0));

            for (int i = 1; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (null == row || isBlank(getCellValue(row, headerIndex.get(COL_FIGMA_URL)))) {
                    continue;
                }
                FigmaRow figmaRow = new FigmaRow();
                figmaRow.figmaUrl = getCellValue(row, headerIndex.get(COL_FIGMA_URL));
                figmaRow.targetUrl = getCellValue(row, headerIndex.get(COL_TARGET_URL));
                figmaRow.testName = getCellValue(row, headerIndex.get(COL_TEST_NAME));
                figmaRow.viewport = getCellValue(row, headerIndex.get(COL_VIEWPORT));
                figmaRow.scale = getCellValue(row, headerIndex.get(COL_SCALE));
                figmaRow.format = getCellValue(row, headerIndex.get(COL_FORMAT));
                rows.add(figmaRow);
            }
        } catch (IOException ex) {
            throw new RuntimeException("Unable to read input Excel file: " + inputPath, ex);
        }
        return rows;
    }

    public static void writeRows(String inputPath, List<FigmaRow> rows, String outputPath) {
        List<String> headers;
        try (FileInputStream fis = new FileInputStream(inputPath); Workbook inWorkbook = new XSSFWorkbook(fis)) {
            Row headerRow = inWorkbook.getSheetAt(0).getRow(0);
            headers = new ArrayList<>();
            for (Cell cell : headerRow) {
                headers.add(cell.getStringCellValue().trim());
            }
        } catch (IOException ex) {
            throw new RuntimeException("Unable to read input Excel headers: " + inputPath, ex);
        }
        for (String outputColumn : OUTPUT_ONLY_COLUMNS) {
            if (!headers.contains(outputColumn)) {
                headers.add(outputColumn);
            }
        }

        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("Baselines");
            Row headerRow = sheet.createRow(0);
            for (int col = 0; col < headers.size(); col++) {
                headerRow.createCell(col).setCellValue(headers.get(col));
            }

            int rowNum = 1;
            for (FigmaRow figmaRow : rows) {
                Map<String, String> values = toMap(figmaRow);
                Row row = sheet.createRow(rowNum++);
                for (int col = 0; col < headers.size(); col++) {
                    row.createCell(col).setCellValue(values.getOrDefault(headers.get(col), ""));
                }
            }
            for (int col = 0; col < headers.size(); col++) {
                sheet.autoSizeColumn(col);
            }

            try (FileOutputStream fos = new FileOutputStream(outputPath)) {
                workbook.write(fos);
            }
        } catch (IOException ex) {
            throw new RuntimeException("Unable to write output Excel file: " + outputPath, ex);
        }
    }

    public static String deriveOutputPath(String inputPath) {
        File inputFile = new File(inputPath);
        String name = inputFile.getName();
        int dotIndex = name.lastIndexOf('.');
        String base = dotIndex >= 0 ? name.substring(0, dotIndex) : name;
        String ext = dotIndex >= 0 ? name.substring(dotIndex) : ".xlsx";
        File parent = inputFile.getAbsoluteFile().getParentFile();
        return new File(parent, base + "_output" + ext).getPath();
    }

    private static Map<String, String> toMap(FigmaRow row) {
        Map<String, String> map = new LinkedHashMap<>();
        map.put(COL_FIGMA_URL, nullToEmpty(row.figmaUrl));
        map.put(COL_TARGET_URL, nullToEmpty(row.targetUrl));
        map.put(COL_TEST_NAME, nullToEmpty(row.testName));
        map.put(COL_VIEWPORT, nullToEmpty(row.viewport));
        map.put(COL_SCALE, nullToEmpty(row.scale));
        map.put(COL_FORMAT, nullToEmpty(row.format));
        map.put(COL_APP_NAME, nullToEmpty(row.appName));
        map.put(COL_BASELINE_ENV_NAME, nullToEmpty(row.baselineEnvName));
        map.put(COL_BASELINE_BATCH_URL, nullToEmpty(row.baselineBatchUrl));
        map.put(COL_STATUS, nullToEmpty(row.status));
        map.put(COL_ERROR_MESSAGE, nullToEmpty(row.errorMessage));
        return map;
    }

    private static Map<String, Integer> readHeaderIndex(Row headerRow) {
        Map<String, Integer> index = new LinkedHashMap<>();
        for (Cell cell : headerRow) {
            index.put(cell.getStringCellValue().trim(), cell.getColumnIndex());
        }
        return index;
    }

    private static String getCellValue(Row row, Integer columnIndex) {
        if (null == columnIndex) {
            return null;
        }
        Cell cell = row.getCell(columnIndex);
        if (null == cell) {
            return null;
        }
        return DATA_FORMATTER.formatCellValue(cell).trim();
    }

    private static boolean isBlank(String value) {
        return null == value || value.isBlank();
    }

    private static String nullToEmpty(String value) {
        return null == value ? "" : value;
    }
}
