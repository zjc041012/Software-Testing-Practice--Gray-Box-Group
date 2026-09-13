package com.example.waybill.dto;

import com.example.waybill.entity.ChainName;

import java.time.LocalDateTime;

public record CrossChainWaybillCredential(
        String credentialId,
        String taskId,
        String credentialType,
        String waybillId,
        String smgsNo,
        String departureStation,
        String destinationStation,
        String route,
        String goodsName,
        String containerNo,
        String transportStatus,
        String consignorOrgId,
        String carrierOrgId,
        String consigneeOrgId,
        String customsOrgId,
        String forwarderOrgId,
        String fileHash,
        String encryptedFileHash,
        ChainName sourceChain,
        ChainName targetChain,
        String sourceTxId,
        String targetTxId,
        LocalDateTime issuedAt,
        LocalDateTime receivedAt,
        LocalDateTime expiresAt,
        String credentialHash
) {
}
