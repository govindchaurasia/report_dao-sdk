package com.lisa.reportingmodule.format;

import com.lisa.daosdk.model.ReportData;
import com.lisa.reportingmodule.ReportGenerationException;
import com.lisa.reportingmodule.model.ReportFormat;
import com.lisa.reportingmodule.model.ReportResult;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.ByteArrayOutputStream;
import java.util.List;
import java.util.Map;

/**
 * Renders a report as an .xlsx workbook using Apache POI.
 */
public class ExcelReportWriter implements ReportWriter {

    private static final String XLSX_CONTENT_TYPE =
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";

    @Override
    public ReportFormat format() {
        return ReportFormat.EXCEL;
    }

    @Override
    public ReportResult write(String reportName, ReportData data) {
        List<String> columns = data.getColumns();
        try (XSSFWorkbook workbook = new XSSFWorkbook();
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {

            Sheet sheet = workbook.createSheet(safeSheetName(reportName));

            CellStyle headerStyle = workbook.createCellStyle();
            Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerStyle.setFont(headerFont);

            Row headerRow = sheet.createRow(0);
            for (int c = 0; c < columns.size(); c++) {
                Cell cell = headerRow.createCell(c);
                cell.setCellValue(columns.get(c));
                cell.setCellStyle(headerStyle);
                // Fixed width avoids autoSizeColumn, which needs AWT fonts (unreliable headless).
                sheet.setColumnWidth(c, 20 * 256);
            }

            int rowIndex = 1;
            for (Map<String, Object> row : data.getRows()) {
                Row excelRow = sheet.createRow(rowIndex++);
                for (int c = 0; c < columns.size(); c++) {
                    Cell cell = excelRow.createCell(c);
                    Object value = row.get(columns.get(c));
                    if (value == null) {
                        cell.setBlank();
                    } else if (value instanceof Number number) {
                        cell.setCellValue(number.doubleValue());
                    } else if (value instanceof Boolean bool) {
                        cell.setCellValue(bool);
                    } else {
                        cell.setCellValue(String.valueOf(value));
                    }
                }
            }

            workbook.write(out);
            return new ReportResult(out.toByteArray(), XLSX_CONTENT_TYPE, reportName + ".xlsx");
        } catch (Exception ex) {
            throw new ReportGenerationException("Failed to render Excel report '" + reportName + "'", ex);
        }
    }

    private String safeSheetName(String name) {
        String sanitized = name == null ? "Report" : name.replaceAll("[\\\\/?*\\[\\]:]", " ").trim();
        if (sanitized.isEmpty()) {
            sanitized = "Report";
        }
        return sanitized.length() > 31 ? sanitized.substring(0, 31) : sanitized;
    }
}
