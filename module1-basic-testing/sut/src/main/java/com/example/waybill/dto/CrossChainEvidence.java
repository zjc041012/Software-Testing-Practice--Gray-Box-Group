package com.example.waybill.dto;

import com.example.waybill.entity.ChainName;

import java.time.LocalDateTime;

public record CrossChainEvidence(
        String evidenceId,
        String taskId,
        String credentialId,
        String messageId,
        String receiptId,
        ChainName sourceChain,
        ChainName targetChain,
        String sourceTxId,
        String targetTxId,
        String credentialHash,
        String messageHash,
        String receiptHash,
        String evidenceHash,
        LocalDateTime createdAt
) {
}
