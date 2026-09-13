package com.example.waybill.service;

import com.example.waybill.dto.CrossChainExecutionResult;
import com.example.waybill.dto.CrossChainPrecheckResult;
import com.example.waybill.dto.CrossChainTaskRequest;
import com.example.waybill.entity.ChainName;
import com.example.waybill.entity.CrossChainTask;
import com.example.waybill.entity.CrossChainTaskStatus;
import com.example.waybill.entity.EncryptedFile;
import com.example.waybill.entity.Role;
import com.example.waybill.entity.User;
import com.example.waybill.entity.WaybillRecord;
import com.example.waybill.entity.WaybillStatus;
import com.example.waybill.fabric.ChainWaybill;
import com.example.waybill.fabric.FabricGateway;
import com.example.waybill.repository.CrossChainTaskRepository;
import com.example.waybill.repository.EncryptedFileRepository;
import com.example.waybill.repository.WaybillRecordRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CrossChainServiceTest {
    @Mock
    private CrossChainTaskRepository taskRepository;
    @Mock
    private WaybillRecordRepository waybillRepository;
    @Mock
    private EncryptedFileRepository fileRepository;
    @Mock
    private FabricGateway fabricGateway;
    @Mock
    private CurrentUserService currentUserService;
    @Mock
    private OperationLogService logService;
    @Mock
    private AccessControlService accessControlService;

    private CrossChainService service;
    private User operator;
    private WaybillRecord waybill;
    private ChainWaybill sourceRecord;

    @BeforeEach
    void setUp() {
        service = new CrossChainService(taskRepository, waybillRepository, fileRepository, fabricGateway,
                currentUserService, logService, accessControlService, new ObjectMapper().findAndRegisterModules());

        operator = new User();
        operator.setId(7L);
        operator.setUsername("gateway-user");
        operator.setRole(Role.CROSS_CHAIN_GATEWAY);
        when(currentUserService.user()).thenReturn(operator);
        doNothing().when(accessControlService).requireCrossChainOperator(operator);

        waybill = new WaybillRecord();
        waybill.setWaybillId("WB-001");
        waybill.setSmgsNo("SMGS-001");
        waybill.setDepartureStation("Beijing");
        waybill.setDestinationStation("Berlin");
        waybill.setGoodsName("Machine parts");
        waybill.setContainerNo("CONT-001");
        waybill.setSourceChain(ChainName.CHINA);
        waybill.setCurrentChain(ChainName.CHINA);
        waybill.setStatus(WaybillStatus.IN_TRANSIT);
        waybill.setChainTxId("china-tx-001");

        sourceRecord = new ChainWaybill("WB-001", "SMGS-001", "file-hash-001", "encrypted-hash-001",
                "/storage/WB-001.enc", 10L, ChainName.CHINA, ChainName.CHINA, WaybillStatus.IN_TRANSIT,
                java.util.Set.of(), java.util.Set.of(), null, "china-tx-001", null, null, null);
    }

    @Test
    void precheckRejectsIdenticalSourceAndTargetChains() {
        when(waybillRepository.findByWaybillId("WB-001")).thenReturn(Optional.empty());
        when(fileRepository.findFirstByWaybillIdOrderByCreatedAtDesc("WB-001")).thenReturn(Optional.empty());
        when(fabricGateway.readWaybill(ChainName.CHINA, "WB-001")).thenReturn(Optional.empty());

        CrossChainPrecheckResult result = service.precheck(request(ChainName.CHINA, ChainName.CHINA, future()));

        assertThat(result.passed()).isFalse();
        assertThat(result.items()).anyMatch(item -> item.name().equals("源链和目标链") && !item.passed());
    }

    @Test
    void precheckRejectsAnExpiredRequest() {
        stubValidPrecheckDependencies();

        CrossChainPrecheckResult result = service.precheck(request(ChainName.CHINA, ChainName.EUROPE, past()));

        assertThat(result.passed()).isFalse();
        assertThat(result.items()).anyMatch(item -> item.name().equals("有效期合法") && !item.passed());
    }

    @Test
    void precheckPassesWhenWaybillFileAndSourceChainRecordAreReady() {
        stubValidPrecheckDependencies();

        CrossChainPrecheckResult result = service.precheck(request(ChainName.CHINA, ChainName.EUROPE, future()));

        assertThat(result.passed()).isTrue();
        assertThat(result.items()).hasSize(8).allMatch(item -> item.passed());
    }

    @Test
    void createPersistsAUserOwnedCreatedTaskAfterPrecheck() {
        stubValidPrecheckDependencies();
        when(taskRepository.save(any(CrossChainTask.class))).thenAnswer(invocation -> invocation.getArgument(0));

        CrossChainTask task = service.create(request(ChainName.CHINA, ChainName.EUROPE, future()));

        assertThat(task.getTaskId()).startsWith("CCT-");
        assertThat(task.getCredentialId()).startsWith("XCC-");
        assertThat(task.getWaybillId()).isEqualTo("WB-001");
        assertThat(task.getStatus()).isEqualTo(CrossChainTaskStatus.CREATED);
        assertThat(task.getCreatedBy()).isEqualTo(7L);
        verify(taskRepository).save(any(CrossChainTask.class));
        verify(logService).success(eq(operator), eq("CREATE_CROSS_CHAIN_TASK"), eq("CROSS_CHAIN"),
                anyString(), eq(ChainName.CHINA), eq(null), anyString());
    }

    @Test
    void createRejectsWhenPrecheckFails() {
        when(waybillRepository.findByWaybillId("WB-001")).thenReturn(Optional.of(waybill));
        when(fileRepository.findFirstByWaybillIdOrderByCreatedAtDesc("WB-001")).thenReturn(Optional.empty());
        when(fabricGateway.readWaybill(ChainName.CHINA, "WB-001")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.create(request(ChainName.CHINA, ChainName.EUROPE, future())))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("跨链前置检查未通过");

        verify(taskRepository, never()).save(any(CrossChainTask.class));
    }

    @Test
    void executeMovesTaskThroughSuccessfulStateAndCreatesTargetReceipt() {
        CrossChainTask task = newTask();
        stubTask(task);
        when(waybillRepository.findByWaybillId("WB-001")).thenReturn(Optional.of(waybill));
        when(fabricGateway.readWaybill(ChainName.CHINA, "WB-001")).thenReturn(Optional.of(sourceRecord));
        when(fabricGateway.receiveCrossChainCredentialMessage(eq(ChainName.EUROPE), any())).thenReturn(receiptPayload());
        when(taskRepository.save(any(CrossChainTask.class))).thenAnswer(invocation -> invocation.getArgument(0));

        CrossChainExecutionResult result = service.execute("CCT-001");

        assertThat(result.task().getStatus()).isEqualTo(CrossChainTaskStatus.CONFIRMED);
        assertThat(result.task().getTargetTxId()).isEqualTo("europe-tx-001");
        assertThat(result.task().getCredentialHash()).isNotBlank();
        assertThat(result.task().getMessageHash()).isNotBlank();
        assertThat(result.task().getReceiptHash()).isNotBlank();
        assertThat(result.task().getEvidenceHash()).isNotBlank();
        assertThat(result.receipt().status()).isEqualTo("SUCCESS");
        verify(fabricGateway, times(1)).receiveCrossChainCredentialMessage(eq(ChainName.EUROPE), any());
        verify(logService).success(eq(operator), eq("EXECUTE_CROSS_CHAIN"), eq("CROSS_CHAIN"), eq("CCT-001"),
                eq(ChainName.EUROPE), eq("europe-tx-001"), anyString());
    }

    @Test
    void retryOfAConfirmedTaskIsIdempotent() {
        CrossChainTask task = newTask();
        stubTask(task);
        when(waybillRepository.findByWaybillId("WB-001")).thenReturn(Optional.of(waybill));
        when(fabricGateway.readWaybill(ChainName.CHINA, "WB-001")).thenReturn(Optional.of(sourceRecord));
        when(fabricGateway.receiveCrossChainCredentialMessage(eq(ChainName.EUROPE), any())).thenReturn(receiptPayload());
        when(taskRepository.save(any(CrossChainTask.class))).thenAnswer(invocation -> invocation.getArgument(0));

        service.execute("CCT-001");
        CrossChainExecutionResult retryResult = service.execute("CCT-001");

        assertThat(retryResult.task().getStatus()).isEqualTo(CrossChainTaskStatus.CONFIRMED);
        assertThat(retryResult.task().getTargetTxId()).isEqualTo("europe-tx-001");
        verify(fabricGateway, times(1)).receiveCrossChainCredentialMessage(eq(ChainName.EUROPE), any());
    }

    @Test
    void executeMarksTaskFailedWhenSourceChainRecordIsMissing() {
        CrossChainTask task = newTask();
        stubTask(task);
        when(waybillRepository.findByWaybillId("WB-001")).thenReturn(Optional.of(waybill));
        when(fabricGateway.readWaybill(ChainName.CHINA, "WB-001")).thenReturn(Optional.empty());
        when(taskRepository.save(any(CrossChainTask.class))).thenAnswer(invocation -> invocation.getArgument(0));

        CrossChainExecutionResult result = service.execute("CCT-001");

        assertThat(result.task().getStatus()).isEqualTo(CrossChainTaskStatus.FAILED);
        assertThat(result.task().getErrorMessage()).contains("源链上没有找到");
        verify(fabricGateway, never()).receiveCrossChainCredentialMessage(any(), any());
        verify(logService).failed(eq(operator), eq("EXECUTE_CROSS_CHAIN"), eq("CROSS_CHAIN"), eq("CCT-001"),
                eq(ChainName.EUROPE), anyString());
    }

    @Test
    void verifyRejectsAQueuedTaskAndRecordsTheDecision() {
        CrossChainTask task = newTask();
        stubTask(task);
        when(taskRepository.save(any(CrossChainTask.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var result = service.verify("CCT-001");

        assertThat(result.passed()).isFalse();
        assertThat(result.detail()).contains("尚未确认成功");
        assertThat(task.getLastVerifyPassed()).isFalse();
        assertThat(task.getVerifiedAt()).isNotNull();
        verify(taskRepository).save(task);
    }

    private void stubValidPrecheckDependencies() {
        EncryptedFile file = new EncryptedFile();
        file.setFileHash("file-hash-001");
        file.setEncryptedFileHash("encrypted-hash-001");
        when(waybillRepository.findByWaybillId("WB-001")).thenReturn(Optional.of(waybill));
        when(fileRepository.findFirstByWaybillIdOrderByCreatedAtDesc("WB-001")).thenReturn(Optional.of(file));
        when(fabricGateway.readWaybill(ChainName.CHINA, "WB-001")).thenReturn(Optional.of(sourceRecord));
    }

    private CrossChainTaskRequest request(ChainName source, ChainName target, LocalDateTime expiresAt) {
        return new CrossChainTaskRequest("WB-001", source, target, "customs clearance", expiresAt);
    }

    private CrossChainTask newTask() {
        CrossChainTask task = new CrossChainTask();
        task.setTaskId("CCT-001");
        task.setCredentialId("XCC-001");
        task.setWaybillId("WB-001");
        task.setSourceChain(ChainName.CHINA);
        task.setTargetChain(ChainName.EUROPE);
        task.setPurpose("customs clearance");
        task.setExpiresAt(future());
        task.setStatus(CrossChainTaskStatus.CREATED);
        task.setCreatedBy(7L);
        task.setCreatedAt(LocalDateTime.now());
        return task;
    }

    private void stubTask(CrossChainTask task) {
        when(taskRepository.findByTaskId("CCT-001")).thenReturn(Optional.of(task));
    }

    private Map<String, Object> receiptPayload() {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("targetTxId", "europe-tx-001");
        payload.put("status", "SUCCESS");
        payload.put("receivedAt", LocalDateTime.now().toString());
        payload.put("credentialHash", "ignored-by-service");
        payload.put("messageHash", "ignored-by-service");
        return payload;
    }

    private LocalDateTime future() {
        return LocalDateTime.now().plusDays(1);
    }

    private LocalDateTime past() {
        return LocalDateTime.now().minusMinutes(1);
    }
}
