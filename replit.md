# ReportingModule

A multi-format (JSON / Excel / CSV) Spring Boot reporting library, designed to be dropped into an existing Spring Boot application as a dependency JAR. It pulls data from the host app's database (AWS RDS, MySQL/MariaDB) and can be triggered both via a REST endpoint and via a Quartz-scheduled job.

## Projects

Two standalone Maven projects (Java 17, Spring Boot 3.5.3), built as plain dependency JARs (no executable repackaging):

- **`dao-sdk`** (`com.lisa:dao-sdk`) — the data-access layer and single dependency hub. Holds Spring Data JPA + Hibernate, the MySQL/MariaDB driver, and the report-format libraries (Apache POI, OpenCSV, Jackson). These flow transitively into `ReportingModule`.
- **`ReportingModule`** (`com.lisa:reporting-module`) — depends on `dao-sdk`; generates reports in JSON/Excel/CSV, exposes a REST controller, and ships a Quartz job + service beans for scheduled generation.

Both modules use Spring Boot auto-configuration (`META-INF/spring/.../AutoConfiguration.imports`), so the host app just adds the JAR — no manual `@ComponentScan` / `@EntityScan` / `@EnableJpaRepositories`.

## Build & Operate

The Java toolchain is not on the default PATH in non-login shells. Export it first:

```bash
export JAVA_HOME=/nix/store/chcmn5mpj3l5jzxfs2krqximz3276i8v-openjdk-19.0.2+7/lib/openjdk
export PATH="/nix/store/4lslm4qgsyjmpdw64w0a8q5bdmlzjvjd-apache-maven-3.8.6/maven/bin:$JAVA_HOME/bin:$PATH"
```

Then build in dependency order (`dao-sdk` first, so `ReportingModule` can resolve it from the local `~/.m2` repo):

```bash
mvn -f dao-sdk/pom.xml -DskipTests install
mvn -f ReportingModule/pom.xml -DskipTests install
```

- JDK is GraalVM/OpenJDK 19, but both modules compile with `--release 17`, so the JARs target Java 17 and run in your host's Java 17 runtime.

## Stack

- Java 17, Maven, Spring Boot 3.5.3
- Data: Spring Data JPA + Hibernate, MySQL/MariaDB (`com.mysql:mysql-connector-j`)
- Reports: Apache POI (Excel), OpenCSV (CSV), Jackson (JSON)
- Web: `spring-boot-starter-web` (`provided` in ReportingModule — host supplies the runtime)
- Scheduling: `spring-boot-starter-quartz` (`provided` — host owns the scheduler)

## Where things live

- `dao-sdk/src/main/java/com/lisa/daosdk/`
  - `service/ReportDataService` — runs read-only native SQL, returns schema-agnostic rows
  - `entity/`, `repository/` — sample JPA entity + repository (replace with your real tables)
  - `config/DaoSdkAutoConfiguration` — entity scan, repositories, `ReportDataService` bean
- `ReportingModule/src/main/java/com/lisa/reportingmodule/`
  - `service/ReportingService` — orchestrates fetch → render; called by REST and Quartz
  - `service/ReportDefinitionRegistry` — named report definitions (host registers its own)
  - `format/` — `JsonReportWriter`, `ExcelReportWriter`, `CsvReportWriter`
  - `web/ReportController` — `GET ${reporting.base-path:/api/reports}` and `/{name}?format=`
  - `scheduling/ReportGenerationJob` — Quartz job writing reports to disk
  - `config/ReportingModuleAutoConfiguration`, `config/ReportingProperties`

## How the host app uses it

1. Add `com.lisa:reporting-module:1.0.0` as a dependency (it transitively brings `dao-sdk`).
2. Configure the datasource (`spring.datasource.*`) for your AWS RDS instance as usual.
3. Register report definitions by autowiring `ReportDefinitionRegistry` and calling `register(...)`.
4. Trigger reports via `GET /api/reports/{name}?format=excel`, or schedule `ReportGenerationJob` in Quartz with a `JobDataMap` containing `reportName` (+ optional `format` and SQL params).

Configurable via `reporting.*`: `reporting.output-directory`, `reporting.expose-rest-endpoint`, `reporting.base-path`.

## Architecture decisions

- `dao-sdk` is the dependency hub by design: all shared/report libraries live there in `compile` scope so they reach `ReportingModule` transitively (per the user's request).
- Reports are defined server-side (name → SQL in `ReportDefinitionRegistry`) rather than accepting raw SQL over REST, to avoid SQL injection.
- Report data is kept schema-agnostic (`List<Map<String,Object>>` via native-query `Tuple`s), so the formatters work for any query without coupling to entities.
- `spring-boot-starter-web` and `-quartz` are `provided` in `ReportingModule` — the host already supplies both runtimes, avoiding a duplicate embedded server/scheduler.

## Notes

- The repo also contains an unrelated pnpm/Node template scaffold (`artifacts/`, `lib/`) from the workspace bootstrap; it is not part of the reporting modules.

## User preferences

- Build tool: Maven. Java 17 + Spring Boot 3.5.3. Data access: Spring Data JPA (Hibernate). DB: MySQL/MariaDB (AWS RDS). Wiring: Spring Boot auto-configuration (starter-style).
- groupId `com.lisa`; base packages `com.lisa.daosdk` and `com.lisa.reportingmodule`.
