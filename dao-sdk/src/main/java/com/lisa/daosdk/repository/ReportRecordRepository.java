package com.lisa.daosdk.repository;

import com.lisa.daosdk.entity.ReportRecord;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;

/**
 * Sample Spring Data JPA repository for {@link ReportRecord}.
 * <p>
 * Add your own repositories under this package; they are picked up automatically
 * by {@code DaoSdkAutoConfiguration} via {@code @EnableJpaRepositories}.
 */
public interface ReportRecordRepository extends JpaRepository<ReportRecord, Long> {

    List<ReportRecord> findBySaleDateBetween(LocalDate start, LocalDate end);
}
