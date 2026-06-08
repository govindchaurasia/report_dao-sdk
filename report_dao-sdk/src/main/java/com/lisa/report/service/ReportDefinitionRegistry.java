package com.lisa.report.service;

import com.lisa.report.model.ReportDefinition;

import java.util.Collection;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Thread-safe registry of named report definitions.
 * <p>
 * Autowire this from the host application to register your own reports:
 * <pre>
 * registry.register(new ReportDefinition("monthly-orders", "SELECT ...", Map.of()));
 * </pre>
 */
public class ReportDefinitionRegistry {

    private final ConcurrentMap<String, ReportDefinition> definitions = new ConcurrentHashMap<>();

    public void register(ReportDefinition definition) {
        definitions.put(definition.getName(), definition);
    }

    public ReportDefinition get(String name) {
        return definitions.get(name);
    }

    public Collection<String> names() {
        return new TreeSet<>(definitions.keySet());
    }

    public Collection<ReportDefinition> all() {
        return definitions.values();
    }
}
