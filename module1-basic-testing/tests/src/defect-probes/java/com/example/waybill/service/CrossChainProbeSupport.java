package com.example.waybill.service;

import com.example.waybill.entity.ChainName;
import com.example.waybill.entity.CrossChainTask;
import com.example.waybill.entity.CrossChainTaskStatus;
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

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.Set;

import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

abstract class CrossChainProbeSupport {
    protected final CrossChainTaskRepository taskRepository = mock(CrossChainTaskRepository.class);
    protected final WaybillRecordRepository waybillRepository = mock(WaybillRecordRepository.class);
    protected final EncryptedFileRepository fileRepository = mock(EncryptedFileRepository.class);
    protected final FabricGateway fabricGateway = mock(FabricGateway.class);
    protected final CurrentUserService currentUserService = mock(CurrentUserService.class);
    protected final OperationLogService logService = mock(OperationLogService.class);
    protected final AccessControlService accessControlService = mock(AccessControlService.class);
    protected final CrossChainService service = new CrossChainService(
            taskRepository, waybillRepository, fileRepository, fabricGateway,
            currentUserService, logService, accessControlService,
            new ObjectMapper().findAndRegisterModules());

    protected final User operator = operator();
    protected final WaybillRecord waybill = waybill();
    protected final ChainWaybill sourceRecord = sourceRecord();

    protected void allowOperator() {
        when(currentUserService.user()).thenReturn(operator);
        doNothing().when(accessControlService).requireCrossChainOperator(operator);
    }

    protected void stubWaybillAndSource() {
        when(waybillRepository.findByWaybillId("WB-001")).thenReturn(Optional.of(waybill));
        when(fabricGateway.readWaybill(ChainName.CHINA, "WB-001"))
                .thenReturn(Optional.of(sourceRecord));
    }

    protected CrossChainTask createdTask() {
        CrossChainTask task = new CrossChainTask();
        task.setTaskId("CCT-001");
        task.setCredentialId("XCC-001");
        task.setWaybillId("WB-001");
        task.setSourceChain(ChainName.CHINA);
        task.setTargetChain(ChainName.EUROPE);
        task.setPurpose("customs clearance");
        task.setExpiresAt(LocalDateTime.now().plusDays(1));
        task.setStatus(CrossChainTaskStatus.CREATED);
        task.setCreatedBy(7L);
        task.setCreatedAt(LocalDateTime.now());
        return task;
    }

    private static User operator() {
        User user = new User();
        user.setId(7L);
        user.setUsername("gateway-user");
        user.setRole(Role.CROSS_CHAIN_GATEWAY);
        return user;
    }

    private static WaybillRecord waybill() {
        WaybillRecord record = new WaybillRecord();
        record.setWaybillId("WB-001");
        record.setSmgsNo("SMGS-001");
        record.setDepartureStation("Beijing");
        record.setDestinationStation("Berlin");
        record.setGoodsName("Machine parts");
        record.setContainerNo("CONT-001");
        record.setSourceChain(ChainName.CHINA);
        record.setCurrentChain(ChainName.CHINA);
        record.setStatus(WaybillStatus.IN_TRANSIT);
        record.setChainTxId("china-tx-001");
        return record;
    }

    private static ChainWaybill sourceRecord() {
        return new ChainWaybill("WB-001", "SMGS-001", "file-hash-001", "encrypted-hash-001",
                "/storage/WB-001.enc", 10L, ChainName.CHINA, ChainName.CHINA,
                WaybillStatus.IN_TRANSIT, Set.of(), Set.of(), null, "china-tx-001", null, null, null);
    }
}
