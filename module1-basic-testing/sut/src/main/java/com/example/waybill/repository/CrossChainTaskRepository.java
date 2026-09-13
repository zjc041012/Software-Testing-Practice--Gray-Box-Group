package com.example.waybill.repository;

import com.example.waybill.entity.CrossChainTask;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CrossChainTaskRepository extends JpaRepository<CrossChainTask, Long> {
    Optional<CrossChainTask> findByTaskId(String taskId);
    List<CrossChainTask> findByWaybillId(String waybillId);
    List<CrossChainTask> findAllByOrderByCreatedAtDescIdDesc();
}
