package com.lisa.report.model;

import java.util.List;
import java.util.Map;

/**
 * Schema-agnostic container for a report result set.
 * <p>
 * {@code columns} holds the ordered column names (native query aliases) and
 * {@code rows} holds one map per row, keyed by column name. Keeping the shape
 * generic lets the reporting layer format any query into JSON, Excel or CSV
 * without knowing the underlying entity.
 */
public class ReportData {

    private final List<String> columns;
    private final List<Map<String, Object>> rows;

    public ReportData(List<String> columns, List<Map<String, Object>> rows) {
        this.columns = columns;
        this.rows = rows;
    }

    public List<String> getColumns() {
        return columns;
    }

    public List<Map<String, Object>> getRows() {
        return rows;
    }
}
