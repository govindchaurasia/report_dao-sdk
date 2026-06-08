# report_dao-sdk

Self-contained Spring Boot (3.5.3, Java 17) reporting library, packaged as a single dependency JAR (`com.lisa:report_dao-sdk:1.0.0`).

## Build

```bash
mvn -DskipTests install
```

This installs the JAR into your local `~/.m2`, ready for your host Spring Boot app to depend on.

## Layout

- `pom.xml` — the Maven project (at the repo root).
- `src/main/java/com/lisa/daosdk/` — data-access layer (`ReportDataService`, `DaoSdkAutoConfiguration`).
- `src/main/java/com/lisa/report/` — reporting layer (writers, REST controller, Quartz job, typed report services, `model.ReportData`).
- `src/main/resources/META-INF/spring/...AutoConfiguration.imports` — Spring Boot auto-config registration.
