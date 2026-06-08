# report_dao-sdk

Self-contained Spring Boot (3.5.3, Java 17) reporting library, packaged as a single dependency JAR (`com.lisa:report_dao-sdk:1.0.0`).

## Build

```bash
mvn -DskipTests install
```

Installs the JAR into your local `~/.m2`, ready for your host Spring Boot app to depend on.

## Layout

- `pom.xml` — the Maven project (repo root).
- `src/main/java/com/lisa/report/` — the whole library under one base package (data-access + reporting, incl. `model.ReportData`).
- `src/main/resources/META-INF/spring/...AutoConfiguration.imports` — Spring Boot auto-config registration.
