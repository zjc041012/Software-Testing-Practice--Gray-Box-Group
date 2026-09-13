package com.example.waybill.fabric;

import com.example.waybill.entity.ChainName;
import com.example.waybill.entity.WaybillStatus;

import java.util.Set;

public record ChainWaybill(
        String waybillId,
        String smgsNo,
        String fileHash,
        String encryptedFileHash,
        String storageUri,
        Long ownerOrgId,
        ChainName sourceChain,
        ChainName currentChain,
        WaybillStatus status,
        Set<Long> authorizedOrgs,
        Set<Long> authorizedUsers,
        String expiresAt,
        String sourceTxId,
        String targetTxId,
        String credentialHash,
        String messageHash
) {
}
