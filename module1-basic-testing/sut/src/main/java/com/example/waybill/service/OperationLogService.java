package com.example.waybill.service;

import com.example.waybill.entity.ChainName;
import com.example.waybill.entity.OperationLog;
import com.example.waybill.entity.User;
import com.example.waybill.repository.OperationLogRepository;
import org.springframework.stereotype.Service;

@Service
public class OperationLogService {
    private final OperationLogRepository repository;

    public OperationLogService(OperationLogRepository repository) {
        this.repository = repository;
    }

    public void success(User user, String type, String targetType, String targetId, ChainName chain, String txId, String message) {
        write(user, type, targetType, targetId, chain, txId, "SUCCESS", message);
    }

    public void failed(User user, String type, String targetType, String targetId, ChainName chain, String message) {
        write(user, type, targetType, targetId, chain, null, "FAILED", message);
    }

    private void write(User user, String type, String targetType, String targetId, ChainName chain, String txId, String result, String message) {
        OperationLog log = new OperationLog();
        if (user != null) {
            log.setUserId(user.getId());
            log.setOrgId(user.getOrganization() == null ? null : user.getOrganization().getId());
        }
        log.setOperationType(type);
        log.setTargetType(targetType);
        log.setTargetId(targetId);
        log.setChain(chain);
        log.setTxId(txId);
        log.setResult(result);
        log.setMessage(message);
        repository.save(log);
    }
}
