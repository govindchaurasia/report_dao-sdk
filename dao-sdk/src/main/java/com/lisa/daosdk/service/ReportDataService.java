package com.lisa.daosdk.service;

import com.lisa.daosdk.model.ReportData;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import jakarta.persistence.Tuple;
import jakarta.persistence.TupleElement;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Central data-access entry point for reporting.
 * <p>
 * Runs read-only native SQL queries against the configured datasource (the same
 * AWS RDS instance the host application uses) and returns the result as a
 * {@link ReportData} that the reporting layer can render into any output format.
 */
public class ReportDataService {

    private final EntityManager entityManager;

    public ReportDataService(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    /**
     * Execute a read-only native query and map it to generic rows.
     *
     * @param sql        the native SQL to run; column aliases become the row keys
     * @param parameters named query parameters (may be {@code null} or empty)
     */
    @Transactional(readOnly = true)
    public ReportData runNativeQuery(String sql, Map<String, Object> parameters) {
        Query query = entityManager.createNativeQuery(sql, Tuple.class);
        if (parameters != null) {
            parameters.forEach(query::setParameter);
        }

        @SuppressWarnings("unchecked")
        List<Tuple> tuples = query.getResultList();

        List<String> columns = new ArrayList<>();
        List<Map<String, Object>> rows = new ArrayList<>();

        if (!tuples.isEmpty()) {
            for (TupleElement<?> element : tuples.get(0).getElements()) {
                columns.add(element.getAlias());
            }
            for (Tuple tuple : tuples) {
                Map<String, Object> row = new LinkedHashMap<>();
                for (String column : columns) {
                    row.put(column, tuple.get(column));
                }
                rows.add(row);
            }
        }

        return new ReportData(columns, rows);
    }
}
