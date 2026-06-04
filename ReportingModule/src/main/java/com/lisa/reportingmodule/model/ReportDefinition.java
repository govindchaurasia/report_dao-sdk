package com.lisa.reportingmodule.model;

import java.util.Map;

/**
 * A named, server-side report: a stable name mapped to the native SQL that
 * produces it, plus any default parameters.
 * <p>
 * Defining reports on the server (rather than accepting raw SQL from callers)
 * keeps the REST endpoint safe from SQL injection. Register your own
 * definitions via {@code ReportDefinitionRegistry} from the host application.
 */
public class ReportDefinition {

    private final String name;
    private final String sql;
    private final Map<String, Object> defaultParameters;

    public ReportDefinition(String name, String sql, Map<String, Object> defaultParameters) {
        this.name = name;
        this.sql = sql;
        this.defaultParameters = defaultParameters == null ? Map.of() : Map.copyOf(defaultParameters);
    }

    public String getName() {
        return name;
    }

    public String getSql() {
        return sql;
    }

    public Map<String, Object> getDefaultParameters() {
        return defaultParameters;
    }
}
