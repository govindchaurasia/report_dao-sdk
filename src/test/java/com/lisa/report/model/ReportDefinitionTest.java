package com.lisa.report.model;

import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ReportDefinitionTest {

    @Test
    void exposesNameAndSql() {
        ReportDefinition def = new ReportDefinition("orders", "SELECT 1", Map.of("k", "v"));

        assertThat(def.getName()).isEqualTo("orders");
        assertThat(def.getSql()).isEqualTo("SELECT 1");
        assertThat(def.getDefaultParameters()).containsEntry("k", "v");
    }

    @Test
    void nullDefaultsBecomeEmptyMap() {
        ReportDefinition def = new ReportDefinition("orders", "SELECT 1", null);

        assertThat(def.getDefaultParameters()).isEmpty();
    }

    @Test
    void defensivelyCopiesDefaultsAndIsImmutable() {
        Map<String, Object> source = new HashMap<>();
        source.put("k", "v");

        ReportDefinition def = new ReportDefinition("orders", "SELECT 1", source);
        source.put("mutated", "after");

        assertThat(def.getDefaultParameters()).containsOnlyKeys("k");
        assertThatThrownBy(() -> def.getDefaultParameters().put("x", "y"))
                .isInstanceOf(UnsupportedOperationException.class);
    }
}
