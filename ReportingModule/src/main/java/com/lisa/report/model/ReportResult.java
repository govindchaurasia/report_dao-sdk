package com.lisa.report.model;

/**
 * The rendered output of a report: the raw bytes plus the metadata needed to
 * serve it over HTTP or write it to disk.
 */
public class ReportResult {

    private final byte[] content;
    private final String contentType;
    private final String filename;

    public ReportResult(byte[] content, String contentType, String filename) {
        this.content = content;
        this.contentType = contentType;
        this.filename = filename;
    }

    public byte[] getContent() {
        return content;
    }

    public String getContentType() {
        return contentType;
    }

    public String getFilename() {
        return filename;
    }
}
