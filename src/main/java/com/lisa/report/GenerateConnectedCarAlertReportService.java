package com.lisa.report;

import com.lisa.report.model.ReportFormat;
import com.lisa.report.model.ReportResult;
import com.lisa.report.service.ReportingService;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Connected Car Alert report: a daily per-store engagement funnel for connected-car
 * alert interactions ({@code planned_service_type LIKE '%ALERT%'}) over
 * {@code jag_future_service_interaction}, joined to {@code jag_store_master} for the
 * store / ASD attributes and to {@code svc_messages_instance} for the central / dealer
 * e-mail handoff counts. Windowed by {@code actual_i_date} and optionally scoped to a
 * single enterprise and/or store.
 * <p>
 * Library-owned type in package {@code com.lisa.report}; if the host
 * component-scans a same-named report service, give the beans explicit names to
 * disambiguate.
 * <p>
 * The SQL lives in the {@code connected-car-alerts} {@code ReportDefinition} (see
 * {@code ReportingModuleAutoConfiguration}); this service maps the typed request
 * onto that definition's parameters and delegates rendering to {@link ReportingService}.
 * Only non-null request values are passed so the definition's defaults apply
 * otherwise (enterprise {@code 0} = all enterprises, store {@code 0} = all stores,
 * open date range).
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
            if (request.getEnterpriseId() != null) {
                parameters.put("enterpriseId", request.getEnterpriseId());
            }
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
