package com.lisa.report.format;

import com.lisa.daosdk.model.ReportData;
import com.lisa.report.model.ReportFormat;
import com.lisa.report.model.ReportResult;

/**
 * Renders {@link ReportData} into a concrete output format. One implementation
 * exists per {@link ReportFormat}; they are discovered automatically and keyed
 * by {@link #format()}.
 */
public interface ReportWriter {

    ReportFormat format();

    ReportResult write(String reportName, ReportData data);
}
