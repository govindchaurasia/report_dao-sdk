package com.lisa.report.web;

import com.lisa.report.model.ReportFormat;
import com.lisa.report.model.ReportResult;
import com.lisa.report.service.ReportingService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * REST entry point for on-demand report generation.
 * <p>
 * The base path defaults to {@code /api/reports} and can be overridden with the
 * {@code reporting.base-path} property. Any query parameters other than
 * {@code format} are passed through as named SQL parameters.
 * <p>
 * Named {@code ReportSdkController} (bean name {@code reportSdkController}) so it
 * does not collide with a host application's own {@code ReportController} bean.
 */
@RestController
@RequestMapping("${reporting.base-path:/api/reports}")
public class ReportSdkController {

    private final ReportingService reportingService;

    public ReportSdkController(ReportingService reportingService) {
        this.reportingService = reportingService;
    }

    @GetMapping
    public Collection<String> listReports() {
        return reportingService.availableReports();
    }

    @GetMapping("/{name}")
    public ResponseEntity<byte[]> generate(@PathVariable String name,
                                           @RequestParam(name = "format", defaultValue = "JSON") String format,
                                           @RequestParam Map<String, String> queryParams) {
        Map<String, Object> parameters = new LinkedHashMap<>(queryParams);
        parameters.remove("format");

        ReportFormat reportFormat = ReportFormat.fromString(format);
        ReportResult result = reportingService.generate(name, parameters, reportFormat);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + result.getFilename() + "\"")
                .contentType(MediaType.parseMediaType(result.getContentType()))
                .body(result.getContent());
    }
}
