package com.lisa.report.service;

import com.lisa.report.model.ReportData;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import jakarta.persistence.Tuple;
import jakarta.persistence.TupleElement;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReportDataServiceTest {

    @Mock
    private EntityManager entityManager;
    @Mock
    private Query query;

    private ReportDataService newService() {
        ReportDataService service = new ReportDataService();
        ReflectionTestUtils.setField(service, "entityManager", entityManager);
        return service;
    }

    private TupleElement<?> element(String alias) {
        TupleElement<?> el = org.mockito.Mockito.mock(TupleElement.class);
        when(el.getAlias()).thenReturn(alias);
        return el;
    }

    @Test
    void mapsTuplesToColumnsAndRowsAndBindsParameters() {
        ReportDataService service = newService();
        when(entityManager.createNativeQuery("SELECT 1", Tuple.class)).thenReturn(query);

        TupleElement<?> nameElement = element("name");
        TupleElement<?> countElement = element("count");
        Tuple tuple = org.mockito.Mockito.mock(Tuple.class);
        when(tuple.getElements()).thenReturn(List.of(nameElement, countElement));
        when(tuple.get("name")).thenReturn("alpha");
        when(tuple.get("count")).thenReturn(3);
        when(query.getResultList()).thenReturn(List.of(tuple));

        ReportData data = service.runNativeQuery("SELECT 1", Map.of("storeId", 5L));

        assertThat(data.getColumns()).containsExactly("name", "count");
        assertThat(data.getRows()).hasSize(1);
        assertThat(data.getRows().get(0)).containsEntry("name", "alpha").containsEntry("count", 3);
        verify(query).setParameter("storeId", 5L);
    }

    @Test
    void emptyResultSetYieldsEmptyColumnsAndRows() {
        ReportDataService service = newService();
        when(entityManager.createNativeQuery(eq("SELECT 1"), eq(Tuple.class))).thenReturn(query);
        when(query.getResultList()).thenReturn(List.of());

        ReportData data = service.runNativeQuery("SELECT 1", null);

        assertThat(data.getColumns()).isEmpty();
        assertThat(data.getRows()).isEmpty();
    }

    @Test
    void nullParametersAreTolerated() {
        ReportDataService service = newService();
        when(entityManager.createNativeQuery(any(String.class), eq(Tuple.class))).thenReturn(query);
        when(query.getResultList()).thenReturn(List.of());

        service.runNativeQuery("SELECT 1", null);

        verify(query, org.mockito.Mockito.never()).setParameter(any(String.class), any());
    }
}
