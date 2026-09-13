package com.example.waybill.repository;

import com.example.waybill.entity.WaybillRecord;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface WaybillRecordRepository extends JpaRepository<WaybillRecord, Long> {
    Optional<WaybillRecord> findByWaybillId(String waybillId);
    boolean existsByWaybillId(String waybillId);
}
