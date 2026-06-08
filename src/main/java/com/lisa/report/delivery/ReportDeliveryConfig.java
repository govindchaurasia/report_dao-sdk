package com.lisa.report.delivery;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Library-owned delivery configuration. The host maps its own
 * {@code ReportConfigMasterEntityDto} onto this immutable object so the library
 * stays self-contained (it never depends on host VO/DTO types).
 * <p>
 * The connection block ({@code host}/{@code port}/{@code username}/{@code password})
 * is reused per method: for {@link DeliveryMethod#EMAIL} it is the SMTP server, for
 * {@link DeliveryMethod#SFTP} it is the SFTP server. Build via {@link #builder()}.
 */
public class ReportDeliveryConfig {

    private final DeliveryMethod method;
    private final String fileName;
    private final String zipPassword;

    // Connection block (SMTP for EMAIL, SFTP server for SFTP).
    private final String host;
    private final Integer port;
    private final String username;
    private final String password;

    // Email-specific.
    private final String toAddresses;
    private final String fromAddress;
    private final String senderName;
    private final String subject;
    private final String body;
    private final boolean startTls;
    private final Map<String, String> mailProperties;

    // SFTP-specific.
    private final String remoteDirectory;
    private final boolean strictHostKeyChecking;

    private ReportDeliveryConfig(Builder builder) {
        this.method = builder.method;
        this.fileName = builder.fileName;
        this.zipPassword = builder.zipPassword;
        this.host = builder.host;
        this.port = builder.port;
        this.username = builder.username;
        this.password = builder.password;
        this.toAddresses = builder.toAddresses;
        this.fromAddress = builder.fromAddress;
        this.senderName = builder.senderName;
        this.subject = builder.subject;
        this.body = builder.body;
        this.startTls = builder.startTls;
        this.mailProperties = builder.mailProperties == null
                ? Collections.emptyMap()
                : Collections.unmodifiableMap(new LinkedHashMap<>(builder.mailProperties));
        this.remoteDirectory = builder.remoteDirectory;
        this.strictHostKeyChecking = builder.strictHostKeyChecking;
    }

    public DeliveryMethod getMethod() {
        return method;
    }

    public String getFileName() {
        return fileName;
    }

    public String getZipPassword() {
        return zipPassword;
    }

    public String getHost() {
        return host;
    }

    public Integer getPort() {
        return port;
    }

    public String getUsername() {
        return username;
    }

    public String getPassword() {
        return password;
    }

    public String getToAddresses() {
        return toAddresses;
    }

    public String getFromAddress() {
        return fromAddress;
    }

    public String getSenderName() {
        return senderName;
    }

    public String getSubject() {
        return subject;
    }

    public String getBody() {
        return body;
    }

    public boolean isStartTls() {
        return startTls;
    }

    public Map<String, String> getMailProperties() {
        return mailProperties;
    }

    public String getRemoteDirectory() {
        return remoteDirectory;
    }

    public boolean isStrictHostKeyChecking() {
        return strictHostKeyChecking;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private DeliveryMethod method;
        private String fileName;
        private String zipPassword;
        private String host;
        private Integer port;
        private String username;
        private String password;
        private String toAddresses;
        private String fromAddress;
        private String senderName;
        private String subject;
        private String body;
        private boolean startTls = true;
        private Map<String, String> mailProperties;
        private String remoteDirectory;
        private boolean strictHostKeyChecking = false;

        public Builder method(DeliveryMethod method) {
            this.method = method;
            return this;
        }

        public Builder fileName(String fileName) {
            this.fileName = fileName;
            return this;
        }

        public Builder zipPassword(String zipPassword) {
            this.zipPassword = zipPassword;
            return this;
        }

        public Builder host(String host) {
            this.host = host;
            return this;
        }

        public Builder port(Integer port) {
            this.port = port;
            return this;
        }

        public Builder username(String username) {
            this.username = username;
            return this;
        }

        public Builder password(String password) {
            this.password = password;
            return this;
        }

        public Builder toAddresses(String toAddresses) {
            this.toAddresses = toAddresses;
            return this;
        }

        public Builder fromAddress(String fromAddress) {
            this.fromAddress = fromAddress;
            return this;
        }

        public Builder senderName(String senderName) {
            this.senderName = senderName;
            return this;
        }

        public Builder subject(String subject) {
            this.subject = subject;
            return this;
        }

        public Builder body(String body) {
            this.body = body;
            return this;
        }

        public Builder startTls(boolean startTls) {
            this.startTls = startTls;
            return this;
        }

        public Builder mailProperties(Map<String, String> mailProperties) {
            this.mailProperties = mailProperties;
            return this;
        }

        public Builder remoteDirectory(String remoteDirectory) {
            this.remoteDirectory = remoteDirectory;
            return this;
        }

        public Builder strictHostKeyChecking(boolean strictHostKeyChecking) {
            this.strictHostKeyChecking = strictHostKeyChecking;
            return this;
        }

        public ReportDeliveryConfig build() {
            return new ReportDeliveryConfig(this);
        }
    }
}
