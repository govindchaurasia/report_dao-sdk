package com.lisa.report;

import com.lisa.report.model.ReportFormat;
import com.lisa.report.model.ReportResult;
import com.lisa.reportingmodule.service.ReportingService;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Connected Car Alert report: rows in {@code fsi_custom_search_outbox} that carry a
 * connected-car service alert ({@code serviceAlertId} present), windowed by
 * {@code planner_run_date} and optionally scoped to a single store.
 * <p>
 * The SQL lives in the {@code connected-car-alerts} {@code ReportDefinition} (see
 * {@code ReportingModuleAutoConfiguration}); this service maps the typed request
 * onto that definition's parameters and delegates rendering to {@link ReportingService}.
 * Only non-null request values are passed so the definition's defaults apply
 * otherwise (store {@code 0} = all stores, open date range).
 */
public class GenerateConnectedCarAlertReportService implements GenerateReportService {

    public static final String REPORT_NAME = "connected-car-alerts";

    private final ReportingService reportingService;

    public GenerateConnectedCarAlertReportService(ReportingService reportingService) {
        this.reportingService = reportingService;
    }

    @Override
    public ReportType getSupportedType() {
        return ReportType.CONNECTED_CAR_ALERT;
    }

    @Override
    public ReportResult generate(GenerateReportRequestDto request, ReportFormat format) {
        Map<String, Object> parameters = new LinkedHashMap<>();
        if (request != null) {
            if (request.getStoreIdFK() != null) {
                parameters.put("storeId", request.getStoreIdFK());
            }
            if (request.getStartDate() != null) {
                parameters.put("startDate", request.getStartDate());
            }
            if (request.getEndDate() != null) {
                parameters.put("endDate", request.getEndDate());
            }
        }
        return reportingService.generate(REPORT_NAME, parameters, format);
    }
}
