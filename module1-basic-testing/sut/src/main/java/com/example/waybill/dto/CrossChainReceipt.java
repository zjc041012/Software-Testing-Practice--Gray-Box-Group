package com.example.waybill.dto;

import com.example.waybill.entity.ChainName;

import java.time.LocalDateTime;

public record CrossChainReceipt(
        String receiptId,
        String messageId,
        String taskId,
        ChainName sourceChain,
        ChainName targetChain,
        String targetTxId,
        String status,
        String failureReason,
        LocalDateTime receivedAt,
        String credentialHash,
        String messageHash,
        String receiptHash
) {
}
