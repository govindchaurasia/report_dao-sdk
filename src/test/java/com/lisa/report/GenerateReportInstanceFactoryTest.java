package com.lisa.report;

import com.lisa.report.model.ReportFormat;
import com.lisa.report.model.ReportResult;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class GenerateReportInstanceFactoryTest {

    private static class StubService implements GenerateReportService {
        private final ReportType type;

        StubService(ReportType type) {
            this.type = type;
        }

        @Override
        public ReportType getSupportedType() {
            return type;
        }

        @Override
        public ReportResult generate(GenerateReportRequestDto request, ReportFormat format) {
            return new ReportResult(new byte[0], "application/json", "x.json");
        }
    }

    @Test
    void resolvesServiceByType() {
        StubService alert = new StubService(ReportType.CONNECTED_CAR_ALERT);
        StubService optOut = new StubService(ReportType.OPT_OUT);
        GenerateReportInstanceFactory factory =
                new GenerateReportInstanceFactory(List.of(alert, optOut));

        assertThat(factory.getInstanceByType(ReportType.OPT_OUT)).isSameAs(optOut);
        assertThat(factory.getInstanceByType(ReportType.CONNECTED_CAR_ALERT)).isSameAs(alert);
    }

    @Test
    void unknownTypeThrows() {
        GenerateReportInstanceFactory factory =
                new GenerateReportInstanceFactory(List.of(new StubService(ReportType.OPT_OUT)));

        assertThatThrownBy(() -> factory.getInstanceByType(ReportType.LEAD_STATUS))
                .isInstanceOf(ReportGenerationException.class)
                .hasMessageContaining("No report service registered for type");
    }
}
