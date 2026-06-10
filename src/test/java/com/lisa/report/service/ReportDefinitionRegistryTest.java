package com.lisa.report.service;

import com.lisa.report.model.ReportDefinition;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class ReportDefinitionRegistryTest {

    private final ReportDefinitionRegistry registry = new ReportDefinitionRegistry();

    @Test
    void registersAndRetrievesByName() {
        ReportDefinition def = new ReportDefinition("orders", "SELECT 1", Map.of());

        registry.register(def);

        assertThat(registry.get("orders")).isSameAs(def);
    }

    @Test
    void getReturnsNullForUnknownName() {
        assertThat(registry.get("missing")).isNull();
    }

    @Test
    void registeringSameNameOverwrites() {
        registry.register(new ReportDefinition("orders", "SELECT 1", Map.of()));
        registry.register(new ReportDefinition("orders", "SELECT 2", Map.of()));

        assertThat(registry.get("orders").getSql()).isEqualTo("SELECT 2");
    }

    @Test
    void namesAreReturnedSorted() {
        registry.register(new ReportDefinition("zeta", "SELECT 1", Map.of()));
        registry.register(new ReportDefinition("alpha", "SELECT 1", Map.of()));

        assertThat(registry.names()).containsExactly("alpha", "zeta");
    }

    @Test
    void allReturnsEveryDefinition() {
        registry.register(new ReportDefinition("a", "SELECT 1", Map.of()));
        registry.register(new ReportDefinition("b", "SELECT 1", Map.of()));

        assertThat(registry.all()).hasSize(2);
    }
}
