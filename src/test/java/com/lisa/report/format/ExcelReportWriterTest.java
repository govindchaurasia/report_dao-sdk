package com.lisa.report.format;

import com.lisa.report.model.ReportData;
import com.lisa.report.model.ReportFormat;
import com.lisa.report.model.ReportResult;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class ExcelReportWriterTest {

    private final ExcelReportWriter writer = new ExcelReportWriter();

    @Test
    void reportsItsFormat() {
        assertThat(writer.format()).isEqualTo(ReportFormat.EXCEL);
    }

    @Test
    void writesHeaderAndTypedCells() throws Exception {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("name", "alpha");
        row.put("count", 3);
        row.put("active", true);
        row.put("missing", null);
        ReportData data = new ReportData(List.of("name", "count", "active", "missing"), List.of(row));

        ReportResult result = writer.write("orders", data);

        assertThat(result.getContentType())
                .isEqualTo("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        assertThat(result.getFilename()).isEqualTo("orders.xlsx");

        try (XSSFWorkbook workbook = new XSSFWorkbook(new ByteArrayInputStream(result.getContent()))) {
            Sheet sheet = workbook.getSheetAt(0);
            Row header = sheet.getRow(0);
            assertThat(header.getCell(0).getStringCellValue()).isEqualTo("name");
            int fontIndex = header.getCell(0).getCellStyle().getFontIndexAsInt();
            assertThat(workbook.getFontAt(fontIndex).getBold()).isTrue();

            Row dataRow = sheet.getRow(1);
            assertThat(dataRow.getCell(0).getStringCellValue()).isEqualTo("alpha");
            assertThat(dataRow.getCell(1).getNumericCellValue()).isEqualTo(3.0);
            assertThat(dataRow.getCell(2).getBooleanCellValue()).isTrue();
            Cell missing = dataRow.getCell(3);
            assertThat(missing.getCellType()).isEqualTo(CellType.BLANK);
        }
    }

    @Test
    void sanitizesInvalidSheetNameWithoutFailing() throws Exception {
        ReportData data = new ReportData(List.of("a"), List.of());

        ReportResult result = writer.write("bad/name:with*chars[]", data);

        try (XSSFWorkbook workbook = new XSSFWorkbook(new ByteArrayInputStream(result.getContent()))) {
            assertThat(workbook.getNumberOfSheets()).isEqualTo(1);
            assertThat(workbook.getSheetName(0)).doesNotContain("/", ":", "*", "[", "]");
        }
    }
}
