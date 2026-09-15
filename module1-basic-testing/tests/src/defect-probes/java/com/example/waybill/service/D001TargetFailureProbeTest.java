package com.example.waybill.service;

import com.example.waybill.dto.CrossChainExecutionResult;
import com.example.waybill.entity.ChainName;
import com.example.waybill.entity.CrossChainTask;
import com.example.waybill.entity.CrossChainTaskStatus;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

class D001TargetFailureProbeTest extends CrossChainProbeSupport {
    @Test
    void failedTargetReceiptMustNotConfirmTask() {
        allowOperator();
        stubWaybillAndSource();
        CrossChainTask task = createdTask();
        when(taskRepository.findByTaskId("CCT-001")).thenReturn(Optional.of(task));
        when(taskRepository.save(any(CrossChainTask.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        Map<String, Object> failedReceipt = new LinkedHashMap<>();
        failedReceipt.put("targetTxId", "europe-tx-rejected");
        failedReceipt.put("status", "FAILED");
        failedReceipt.put("failureReason", "target-chain rejection");
        failedReceipt.put("receivedAt", LocalDateTime.now().toString());
        when(fabricGateway.receiveCrossChainCredentialMessage(eq(ChainName.EUROPE), any()))
                .thenReturn(failedReceipt);

        CrossChainExecutionResult result = service.execute("CCT-001");
        CrossChainExecutionResult retryResult = service.execute("CCT-001");

        System.out.println("D-001 actualTaskStatus=" + result.task().getStatus()
                + " actualReceiptStatus=" + result.receipt().status()
                + " retryTaskStatus=" + retryResult.task().getStatus());
        assertThat(result.receipt().status()).isEqualTo("FAILED");
        assertThat(result.task().getStatus())
                .as("D-001 failed target receipt must not confirm the task")
                .isEqualTo(CrossChainTaskStatus.FAILED);
        assertThat(retryResult.task().getStatus())
                .as("D-001 retry must not promote a failed receipt to confirmation")
                .isEqualTo(CrossChainTaskStatus.FAILED);
        assertThat(retryResult.receipt().status()).isEqualTo("FAILED");
    }
}
