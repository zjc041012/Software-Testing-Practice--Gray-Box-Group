package com.example.waybill.fabric;

import com.example.waybill.entity.ChainName;
import com.example.waybill.entity.WaybillRecord;
import com.example.waybill.entity.WaybillStatus;

import java.util.List;
import java.util.Map;
import java.util.Optional;

public interface FabricGateway {
    String createWaybill(ChainName chain, WaybillRecord record, String fileHash, String encryptedFileHash, String storageUri);

    Optional<ChainWaybill> readWaybill(ChainName chain, String waybillId);

    boolean verifyWaybill(ChainName chain, String waybillId, String fileHash, String encryptedFileHash);

    String authorizeAccess(ChainName chain, String waybillId, Long orgId, Long userId);

    String revokeAccess(ChainName chain, String waybillId, Long orgId, Long userId);

    Map<String, Object> receiveCrossChainCredentialMessage(ChainName targetChain, Map<String, Object> message);

    boolean verifySharedWaybill(ChainName targetChain, String waybillId, String fileHash, WaybillStatus status, ChainName sourceChain);

    String updateStatus(ChainName chain, String waybillId, WaybillStatus status);

    List<Map<String, Object>> history(ChainName chain, String waybillId);
}
