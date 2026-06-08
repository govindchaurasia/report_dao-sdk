package com.lisa.report;

import com.lisa.report.delivery.ReportDeliveryConfig;
import com.lisa.report.delivery.ReportDeliveryService;
import com.lisa.report.model.ReportFormat;
import com.lisa.report.model.ReportResult;

/**
 * One-call orchestrator: generate a typed report and then deliver it
 * (email / SFTP, optionally password-zipped) in a single step.
 * <p>
 * The host's scheduler maps its {@code ReportConfigMasterEntityDto} onto a
 * {@link GenerateReportRequestDto} (data window) and a {@link ReportDeliveryConfig}
 * (transport), then calls {@link #generateAndDeliver}.
 */
public class SdkGenerateAndDeliverService {

    private final SdkGenerateReportInstanceFactory instanceFactory;
    private final ReportDeliveryService deliveryService;

    public SdkGenerateAndDeliverService(SdkGenerateReportInstanceFactory instanceFactory,
                                        ReportDeliveryService deliveryService) {
        this.instanceFactory = instanceFactory;
        this.deliveryService = deliveryService;
    }

    /**
     * Generate the report for {@code type} in {@code format}, deliver it per
     * {@code deliveryConfig}, and return the rendered result (so the caller can
     * also persist or log it if desired).
     */
    public ReportResult generateAndDeliver(ReportType type,
                                           GenerateReportRequestDto request,
                                           ReportFormat format,
                                           ReportDeliveryConfig deliveryConfig) {
        ReportResult result = instanceFactory.getInstanceByType(type).generate(request, format);
        deliveryService.deliver(result, deliveryConfig);
        return result;
    }
}
