package com.lisa.report.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration for the reporting module, bound from the {@code reporting.*}
 * namespace in the host application's configuration.
 */
@ConfigurationProperties(prefix = "reporting")
public class ReportingProperties {

    /** Directory where scheduled (Quartz) reports are written. */
    private String outputDirectory = "reports";

    /** Whether the REST endpoint is registered. */
    private boolean exposeRestEndpoint = true;

    /** Base path for the REST endpoint (also read via the controller's request mapping). */
    private String basePath = "/api/reports";

    public String getOutputDirectory() {
        return outputDirectory;
    }

    public void setOutputDirectory(String outputDirectory) {
        this.outputDirectory = outputDirectory;
    }

    public boolean isExposeRestEndpoint() {
        return exposeRestEndpoint;
    }

    public void setExposeRestEndpoint(boolean exposeRestEndpoint) {
        this.exposeRestEndpoint = exposeRestEndpoint;
    }

    public String getBasePath() {
        return basePath;
    }

    public void setBasePath(String basePath) {
        this.basePath = basePath;
    }
}
