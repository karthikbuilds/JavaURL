package com.karthik.JavaURL.analytics;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ClickRecordRepository extends JpaRepository<ClickRecord, Long> {

    List<ClickRecord> findTop50ByShortCodeOrderByClickedAtDesc(String shortCode);
}