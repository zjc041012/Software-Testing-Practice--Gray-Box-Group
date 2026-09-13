package com.example.waybill.repository;

import com.example.waybill.entity.OperationLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface OperationLogRepository extends JpaRepository<OperationLog, Long> {
    List<OperationLog> findByTargetIdOrderByCreatedAtAsc(String targetId);
}
