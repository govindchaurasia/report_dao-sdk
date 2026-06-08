# Integrating `report_dao-sdk` into the host app

This guide shows how to wire the reporting framework into your Spring Boot host
application. All of the code below is **host-side glue** that lives in *your*
project — the JAR only provides the framework beans via auto-configuration.

The library never depends on host types (host depends on the JAR), so it ships
its own `ReportType`, `ReportFormat`, `ReportFrequency`, request/config/delivery
models. Every library class that mirrors a host concept carries an `Sdk` prefix
to avoid `ConflictingBeanDefinitionException` on the host classpath.

---

## 1. Add the dependency

```xml
<!-- pom.xml of the host app -->
<dependency>
    <groupId>com.lisa</groupId>
    <artifactId>report_dao-sdk</artifactId>
    <version>1.0.0</version>
</dependency>
```

Your existing `spring.datasource.*` (AWS RDS) config is reused — nothing else to
wire. Auto-configuration exposes `SdkGenerateAndDeliverService`,
`SdkReportRequestFactory`, `SdkGenerateReportInstanceFactory`, and the delivery
beans. All are `@ConditionalOnMissingBean`, so you can override any of them.

---

## 2. Plug a host report into the framework

Each host report becomes a Spring bean implementing the library's
`SdkGenerateReportService`. It **returns the rendered bytes** (`ReportResult`) —
the framework handles delivery, so you remove the old SFTP/email code from inside
the report.

```java
package com.lisa.service.report.sdk; // your host package

import com.lisa.report.GenerateReportRequestDto;
import com.lisa.report.ReportType;
import com.lisa.report.SdkGenerateReportService;
import com.lisa.report.model.ReportFormat;
import com.lisa.report.model.ReportResult;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class PerformanceSummarySdkReportService implements SdkGenerateReportService {

    // Your existing host services / DAOs — re-fetch host entities from the IDs in the request.
    private final EnterpriseService enterpriseService;
    private final StoreService storeService;
    private final JaguarFutureServiceInteractionService fsiService;

    @Override
    public ReportType getSupportedType() {
        return ReportType.PERFORMANCE_SUMMARY;
    }

    @Override
    public ReportResult generate(GenerateReportRequestDto request, ReportFormat format) {
        // Use the library's window + targeting params:
        Long enterpriseId = request.getEnterpriseId();
        Long storeId      = request.getStoreIdFK();
        java.util.Date start = request.getStartDate();
        java.util.Date end   = request.getEndDate();
        String level      = request.getReportLevel();   // ENTERPRISE / STORE

        // ... run your existing aggregation against host services/entities ...
        byte[] bytes = buildExcelWithPoi(/* aggregated rows */);

        String filename = "performance-summary-" + enterpriseId + ".xlsx";
        String contentType =
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
        return new ReportResult(bytes, contentType, filename);
    }

    private byte[] buildExcelWithPoi(/* rows */) { /* your POI code */ return new byte[0]; }
}
```

Add one such bean per `ReportType` you need. `SdkGenerateReportInstanceFactory`
auto-indexes them by type — no factory edits required.

> The only report that ships with a library implementation is
> `CONNECTED_CAR_ALERT` (`SdkGenerateConnectedCarAlertReportService`). Every other
> `ReportType` is registered by the host as a bean like the one above.

---

## 3. The Quartz job: map your config row → one call

This replaces the large `switch` + `buildRequest(...)` that used to live in your
scheduler. The library does the frequency date-window math **and** the delivery.

```java
package com.lisa.scheduling;

import com.lisa.report.SdkGenerateAndDeliverService;
import com.lisa.report.model.ReportConfigMasterDto; // YOUR host dto
import com.lisa.report.model.ReportResult;
import com.lisa.report.model.SdkReportConfig;
import lombok.RequiredArgsConstructor;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class GenerateReportQuartzJob implements Job {

    private final SdkGenerateAndDeliverService sdkGenerateAndDeliverService;
    private final ReportConfigMasterService reportConfigMasterService; // your persistence

    @Override
    public void execute(JobExecutionContext context) {
        Long configId = context.getMergedJobDataMap().getLong("reportConfigId");
        ReportConfigMasterDto config = reportConfigMasterService.findById(configId);

        SdkReportConfig sdkConfig = SdkReportConfig.builder()
                // what / when
                .reportType(config.getReportType().name())            // -> library ReportType
                .reportFrequency(config.getReportFrequency().name())  // -> ReportFrequency window
                .reportFormat("EXCEL")                                // JSON / EXCEL / CSV
                .reportLevel(config.getReportLevel())                 // ENTERPRISE / STORE
                .enterpriseId(config.getEnterpriseId())
                .serviceInteractionId(config.getServiceInteractionId())
                .serviceInteractionName(config.getServiceInteractionName())
                .appointmentDaysRange(config.getAppointmentDaysRange())
                .consolidated(config.isConsolidated())
                .startDate(config.getStartDate())   // only used when frequency = CUSTOM
                .endDate(config.getEndDate())       // only used when frequency = CUSTOM
                .reportFileName(config.getReportFileName())
                // delivery
                .sendReportType(config.getSendReportType())           // EMAIL / SFTP
                .hostname(config.getHostname())
                .port(config.getPort())
                .username(config.getUsername())
                .password(config.getPassword())
                .zipFilePassword(config.getZipFilePassword())         // null/blank => not zipped
                // email
                .toEmailAddress(config.getToEmailAddress())
                .senderFirstName(config.getSenderFirstName())
                .senderLastName(config.getSenderLastName())
                .emailSubject("Your scheduled report")
                .emailBody("Please find the attached report.")
                // sftp
                .sftpFilePath(config.getSftpFilePath())
                .knownHostsPath(config.getKnownHostsPath())           // required unless you opt out
                // .strictHostKeyChecking(false)                      // opt out (not recommended)
                .build();

        ReportResult result = sdkGenerateAndDeliverService.generateAndDeliver(sdkConfig);
        // optional: persist/log result.getFilename(), result.getContent().length
    }
}
```

---

## 4. Register the Quartz job from the cron (host-owned, unchanged)

This stays exactly as you had it — the admin panel writes the config row + cron,
and you schedule a trigger that fires the job above.

```java
JobDetail job = JobBuilder.newJob(GenerateReportQuartzJob.class)
        .withIdentity("report-" + config.getId(), "reports")
        .usingJobData("reportConfigId", config.getId())
        .build();

Trigger trigger = TriggerBuilder.newTrigger()
        .withIdentity("trigger-" + config.getId(), "reports")
        .withSchedule(CronScheduleBuilder.cronSchedule(config.getCronExpression()))
        .build();

scheduler.scheduleJob(job, trigger);
```

---

## Notes on the boundary

- **SFTP is secure-by-default:** `strictHostKeyChecking` is `true`, so supply
  `knownHostsPath` (or set `.strictHostKeyChecking(false)` to opt out — not
  recommended).
- **Zip:** set `zipFilePassword` and the file is AES-256 password-zipped before
  sending; leave it null to send the raw file.
- **`CUSTOM` frequency** is the only one that reads `startDate`/`endDate` from the
  config; all other frequencies compute the window for you. Use
  `buildRequest(config, zoneId)` semantics by setting `timeZone` on the config to
  pin the window's zone.
- **Lower-level overload:** if you already build the request/delivery yourself,
  `generateAndDeliver(ReportType, GenerateReportRequestDto, ReportFormat, ReportDeliveryConfig)`
  is still available.
- **Adding a report** is just adding another `SdkGenerateReportService` bean for
  the relevant `ReportType` — no framework changes.
