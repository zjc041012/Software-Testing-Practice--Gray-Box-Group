package com.example.waybill.fabric.mock;

import com.example.waybill.entity.ChainName;
import com.example.waybill.entity.WaybillRecord;
import com.example.waybill.entity.WaybillStatus;
import com.example.waybill.fabric.ChainWaybill;
import com.example.waybill.fabric.FabricGateway;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Service
@ConditionalOnProperty(prefix = "app.fabric", name = "mode", havingValue = "mock", matchIfMissing = true)
public class MockFabricGateway implements FabricGateway {
    private final Map<ChainName, Map<String, ChainWaybill>> ledgers = new ConcurrentHashMap<>();
    private final Map<ChainName, Map<String, List<Map<String, Object>>>> histories = new ConcurrentHashMap<>();

    public MockFabricGateway() {
        ledgers.put(ChainName.CHINA, new ConcurrentHashMap<>());
        ledgers.put(ChainName.EUROPE, new ConcurrentHashMap<>());
        histories.put(ChainName.CHINA, new ConcurrentHashMap<>());
        histories.put(ChainName.EUROPE, new ConcurrentHashMap<>());
    }

    @Override
    public String createWaybill(ChainName chain, WaybillRecord record, String fileHash, String encryptedFileHash, String storageUri) {
        ChainWaybill waybill = new ChainWaybill(record.getWaybillId(), record.getSmgsNo(), fileHash, encryptedFileHash,
                storageUri, record.getConsignorOrgId(), record.getSourceChain(), chain, record.getStatus(), new LinkedHashSet<>(), new LinkedHashSet<>(),
                null, null, null, null, null);
        ledgers.get(chain).put(record.getWaybillId(), waybill);
        return tx(chain, record.getWaybillId(), "CreateWaybill", waybill);
    }

    @Override
    public Optional<ChainWaybill> readWaybill(ChainName chain, String waybillId) {
        return Optional.ofNullable(ledgers.get(chain).get(waybillId));
    }

    @Override
    public boolean verifyWaybill(ChainName chain, String waybillId, String fileHash, String encryptedFileHash) {
        return readWaybill(chain, waybillId)
                .map(w -> Objects.equals(w.fileHash(), fileHash) && Objects.equals(w.encryptedFileHash(), encryptedFileHash))
                .orElse(false);
    }

    @Override
    public String authorizeAccess(ChainName chain, String waybillId, Long orgId, Long userId) {
        ChainWaybill current = require(chain, waybillId);
        if (orgId != null) {
            current.authorizedOrgs().add(orgId);
        }
        if (userId != null) {
            current.authorizedUsers().add(userId);
        }
        return tx(chain, waybillId, "AuthorizeAccess", current);
    }

    @Override
    public String revokeAccess(ChainName chain, String waybillId, Long orgId, Long userId) {
        ChainWaybill current = require(chain, waybillId);
        if (orgId != null) {
            current.authorizedOrgs().remove(orgId);
        }
        if (userId != null) {
            current.authorizedUsers().remove(userId);
        }
        return tx(chain, waybillId, "RevokeAccess", current);
    }

    @Override
    public Map<String, Object> receiveCrossChainCredentialMessage(ChainName targetChain, Map<String, Object> message) {
        @SuppressWarnings("unchecked")
        Map<String, Object> credential = (Map<String, Object>) message.get("credential");
        String waybillId = text(credential.get("waybillId"));
        ChainName sourceChain = ChainName.valueOf(text(credential.get("sourceChain")));
        String credentialHash = text(message.get("credentialHash"));
        ChainWaybill shared = new ChainWaybill(
                waybillId,
                text(credential.get("smgsNo")),
                text(credential.get("fileHash")),
                text(credential.get("encryptedFileHash")),
                "",
                longValue(credential.get("consignorOrgId")),
                sourceChain,
                targetChain,
                WaybillStatus.SHARED,
                new LinkedHashSet<>(),
                new LinkedHashSet<>(),
                text(credential.get("expiresAt")),
                text(credential.get("sourceTxId")),
                null,
                credentialHash,
                text(message.get("messageHash"))
        );
        ledgers.get(targetChain).put(waybillId, shared);
        String targetTxId = tx(targetChain, waybillId, "ReceiveCrossChainCredentialMessage", message);
        return new LinkedHashMap<>(Map.of(
                "targetTxId", targetTxId,
                "status", "SUCCESS",
                "receivedAt", LocalDateTime.now().toString(),
                "credentialHash", credentialHash,
                "messageHash", text(message.get("messageHash"))
        ));
    }

    @Override
    public boolean verifySharedWaybill(ChainName targetChain, String waybillId, String fileHash, WaybillStatus status, ChainName sourceChain) {
        return readWaybill(targetChain, waybillId)
                .map(w -> Objects.equals(w.fileHash(), fileHash) && w.sourceChain() == sourceChain && (status == null || w.status() == status))
                .orElse(false);
    }

    @Override
    public String updateStatus(ChainName chain, String waybillId, WaybillStatus status) {
        ChainWaybill w = require(chain, waybillId);
        ChainWaybill updated = new ChainWaybill(w.waybillId(), w.smgsNo(), w.fileHash(), w.encryptedFileHash(), w.storageUri(),
                w.ownerOrgId(), w.sourceChain(), w.currentChain(), status, w.authorizedOrgs(), w.authorizedUsers(),
                w.expiresAt(), w.sourceTxId(), w.targetTxId(),
                w.credentialHash(), w.messageHash());
        ledgers.get(chain).put(waybillId, updated);
        return tx(chain, waybillId, "UpdateWaybillStatus", updated);
    }

    @Override
    public List<Map<String, Object>> history(ChainName chain, String waybillId) {
        return histories.get(chain).getOrDefault(waybillId, List.of());
    }

    private ChainWaybill require(ChainName chain, String waybillId) {
        return readWaybill(chain, waybillId).orElseThrow(() -> new IllegalArgumentException("链上运单不存在"));
    }

    private String tx(ChainName chain, String waybillId, String action, Object value) {
        String txId = chain.name().toLowerCase() + "-" + UUID.randomUUID();
        histories.get(chain).computeIfAbsent(waybillId, ignored -> new ArrayList<>()).add(Map.of(
                "txId", txId,
                "action", action,
                "timestamp", LocalDateTime.now().toString(),
                "value", value
        ));
        return txId;
    }

    private String text(Object value) {
        return value == null ? "" : String.valueOf(value);
    }

    private Long longValue(Object value) {
        if (value == null || text(value).isBlank()) {
            return null;
        }
        if (value instanceof Number number) {
            return number.longValue();
        }
        return Long.parseLong(text(value));
    }
}
