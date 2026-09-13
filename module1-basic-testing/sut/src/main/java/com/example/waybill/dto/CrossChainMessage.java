package com.example.waybill.dto;

import com.example.waybill.entity.ChainName;

import java.time.LocalDateTime;

public record CrossChainMessage(
        String messageId,
        String taskId,
        String messageType,
        ChainName sourceChain,
        ChainName targetChain,
        CrossChainWaybillCredential credential,
        String credentialHash,
        LocalDateTime sentAt,
        String messageHash
) {
}
