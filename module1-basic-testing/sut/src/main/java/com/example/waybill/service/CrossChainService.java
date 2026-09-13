package com.example.waybill.service;

import com.example.waybill.dto.CrossChainCheckItem;
import com.example.waybill.dto.CrossChainCompareItem;
import com.example.waybill.dto.CrossChainCompareResult;
import com.example.waybill.dto.CrossChainEvidence;
import com.example.waybill.dto.CrossChainExecutionResult;
import com.example.waybill.dto.CrossChainMessage;
import com.example.waybill.dto.CrossChainPrecheckResult;
import com.example.waybill.dto.CrossChainReceipt;
import com.example.waybill.dto.CrossChainDetailCompareItem;
import com.example.waybill.dto.CrossChainTaskDetail;
import com.example.waybill.dto.CrossChainTaskRequest;
import com.example.waybill.dto.CrossChainTimelineNode;
import com.example.waybill.dto.CrossChainWaybillCredential;
import com.example.waybill.dto.TimelineItem;
import com.example.waybill.dto.VerifyResult;
import com.example.waybill.entity.ChainName;
import com.example.waybill.entity.CrossChainTask;
import com.example.waybill.entity.CrossChainTaskStatus;
import com.example.waybill.entity.EncryptedFile;
import com.example.waybill.entity.User;
import com.example.waybill.entity.WaybillRecord;
import com.example.waybill.fabric.ChainWaybill;
import com.example.waybill.fabric.FabricGateway;
import com.example.waybill.repository.CrossChainTaskRepository;
import com.example.waybill.repository.EncryptedFileRepository;
import com.example.waybill.repository.WaybillRecordRepository;
import com.example.waybill.util.HashUtils;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

@Service
public class CrossChainService {
    private static final String CREDENTIAL_TYPE = "SMGS_WAYBILL";
    private static final String MESSAGE_TYPE = "WAYBILL_CREDENTIAL_SHARE";

    private final CrossChainTaskRepository taskRepository;
    private final WaybillRecordRepository waybillRepository;
    private final EncryptedFileRepository fileRepository;
    private final FabricGateway fabricGateway;
    private final CurrentUserService currentUserService;
    private final OperationLogService logService;
    private final AccessControlService accessControlService;
    private final ObjectMapper objectMapper;

    public CrossChainService(CrossChainTaskRepository taskRepository,
                             WaybillRecordRepository waybillRepository,
                             EncryptedFileRepository fileRepository,
                             FabricGateway fabricGateway,
                             CurrentUserService currentUserService,
                             OperationLogService logService,
                             AccessControlService accessControlService,
                             ObjectMapper objectMapper) {
        this.taskRepository = taskRepository;
        this.waybillRepository = waybillRepository;
        this.fileRepository = fileRepository;
        this.fabricGateway = fabricGateway;
        this.currentUserService = currentUserService;
        this.logService = logService;
        this.accessControlService = accessControlService;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public CrossChainTask create(CrossChainTaskRequest request) {
        validateTaskRequest(request);
        CrossChainPrecheckResult precheck = precheck(request);
        if (!precheck.passed()) {
            throw new IllegalArgumentException("跨链前置检查未通过：" + failedDetails(precheck.items()));
        }

        User user = currentUserService.user();
        CrossChainTask task = new CrossChainTask();
        task.setTaskId("CCT-" + shortId());
        task.setCredentialId("XCC-" + shortId());
        task.setWaybillId(request.waybillId());
        task.setSourceChain(request.sourceChain());
        task.setTargetChain(request.targetChain());
        task.setPurpose(hasText(request.purpose()) ? request.purpose().trim() : "跨链运单凭证共享");
        task.setExpiresAt(request.expiresAt());
        task.setPrecheckedAt(LocalDateTime.now());
        task.setCreatedBy(user.getId());
        CrossChainTask saved = taskRepository.save(task);
        logService.success(user, "CREATE_CROSS_CHAIN_TASK", "CROSS_CHAIN", saved.getTaskId(), request.sourceChain(), null,
                "创建跨链运单凭证共享任务");
        return saved;
    }

    public List<CrossChainTask> list() {
        accessControlService.requireCrossChainOperator(currentUserService.user());
        List<CrossChainTask> tasks = taskRepository.findAllByOrderByCreatedAtDescIdDesc();
        boolean changed = false;
        for (CrossChainTask task : tasks) {
            changed |= ensureTaskDefaults(task);
        }
        return changed ? taskRepository.saveAll(tasks) : tasks;
    }

    public CrossChainTask detail(String taskId) {
        accessControlService.requireCrossChainOperator(currentUserService.user());
        CrossChainTask task = taskRepository.findByTaskId(taskId).orElseThrow(() -> new IllegalArgumentException("跨链任务不存在"));
        if (ensureTaskDefaults(task)) {
            return taskRepository.save(task);
        }
        return task;
    }

    public CrossChainTaskDetail taskDetail(String taskId) {
        CrossChainTask task = detail(taskId);
        boolean repaired = hydrateMissingEvidenceChain(task);
        if (repaired) {
            task = taskRepository.save(task);
        }
        return new CrossChainTaskDetail(
                task,
                currentStep(task),
                failureStage(task),
                task.getStatus() == CrossChainTaskStatus.FAILED ? text(task.getErrorMessage()) : "",
                task.getStatus() == CrossChainTaskStatus.FAILED ? failureSuggestion(task) : "",
                readCredentialSnapshot(task),
                readMessageSnapshot(task),
                readReceiptSnapshot(task),
                readEvidenceSnapshot(task),
                detailTimeline(task),
                detailComparison(task)
        );
    }

    public CrossChainPrecheckResult precheck(CrossChainTaskRequest request) {
        accessControlService.requireCrossChainOperator(currentUserService.user());
        List<CrossChainCheckItem> items = new ArrayList<>();

        if (request.sourceChain() == request.targetChain()) {
            items.add(new CrossChainCheckItem("源链和目标链", false, "源链和目标链不能相同"));
        } else {
            items.add(new CrossChainCheckItem("源链和目标链", true, request.sourceChain() + " -> " + request.targetChain()));
        }

        WaybillRecord waybill = waybillRepository.findByWaybillId(request.waybillId()).orElse(null);
        items.add(new CrossChainCheckItem("运单存在", waybill != null, waybill == null ? "MySQL 中没有找到该运单" : waybill.getWaybillId()));
        if (waybill != null) {
            boolean chainMatched = waybill.getCurrentChain() == request.sourceChain();
            items.add(new CrossChainCheckItem("源链选择正确", chainMatched,
                    chainMatched ? "运单当前链为 " + waybill.getCurrentChain() : "运单当前链为 " + waybill.getCurrentChain()));
            items.add(new CrossChainCheckItem("源链交易记录", hasText(waybill.getChainTxId()),
                    hasText(waybill.getChainTxId()) ? shortText(waybill.getChainTxId()) : "运单缺少源链交易 ID"));
        }

        EncryptedFile file = fileRepository.findFirstByWaybillIdOrderByCreatedAtDesc(request.waybillId()).orElse(null);
        items.add(new CrossChainCheckItem("文件哈希已生成", file != null,
                file == null ? "该运单尚未上传文件" : "原文哈希和密文哈希已生成"));

        try {
            ChainWaybill chainWaybill = fabricGateway.readWaybill(request.sourceChain(), request.waybillId()).orElse(null);
            items.add(new CrossChainCheckItem("源链运单已存证", chainWaybill != null,
                    chainWaybill == null ? "Fabric 源链没有该运单" : request.sourceChain() + " 已找到链上运单记录"));
        } catch (Exception e) {
            items.add(new CrossChainCheckItem("源链运单已存证", false, e.getMessage()));
        }

        boolean expiresOk = request.expiresAt() != null && request.expiresAt().isAfter(LocalDateTime.now());
        items.add(new CrossChainCheckItem("有效期合法", expiresOk,
                request.expiresAt() == null ? "必须设置有效期" : request.expiresAt().toString()));
        items.add(new CrossChainCheckItem("共享对象", true, "CrossChainWaybillCredential"));

        return new CrossChainPrecheckResult(items.stream().allMatch(CrossChainCheckItem::passed), items);
    }

    public CrossChainWaybillCredential credentialPreview(CrossChainTaskRequest request) {
        accessControlService.requireCrossChainOperator(currentUserService.user());
        validateTaskRequest(request);
        WaybillRecord waybill = waybillRepository.findByWaybillId(request.waybillId())
                .orElseThrow(() -> new IllegalArgumentException("运单不存在，请先在运单管理中登记运单"));
        ChainWaybill source = fabricGateway.readWaybill(request.sourceChain(), request.waybillId())
                .orElseThrow(() -> new IllegalArgumentException("源链上没有找到该运单，请先上传文件并完成链上存证"));
        return buildCredential("XCC-PREVIEW", "PREVIEW", waybill, source, request.targetChain(), request.expiresAt(), LocalDateTime.now(), null, null);
    }

    @Transactional
    public CrossChainWaybillCredential credentialPreview(String taskId) {
        CrossChainTask task = detail(taskId);
        WaybillRecord waybill = waybillRepository.findByWaybillId(task.getWaybillId())
                .orElseThrow(() -> new IllegalArgumentException("运单不存在，请先在运单管理中登记运单"));
        ChainWaybill source = fabricGateway.readWaybill(task.getSourceChain(), task.getWaybillId())
                .orElseThrow(() -> new IllegalArgumentException("源链上没有找到该运单，请先上传文件并完成链上存证"));
        validateTaskRequest(requestFrom(task));
        CrossChainWaybillCredential credential = buildCredential(task.getCredentialId(), task.getTaskId(), waybill, source,
                task.getTargetChain(), task.getExpiresAt(), LocalDateTime.now(), task.getTargetTxId(), task.getTargetConfirmedAt());
        task.setPreviewedAt(LocalDateTime.now());
        task.setCredentialHash(credential.credentialHash());
        task.setCredentialSnapshot(json(credential));
        taskRepository.save(task);
        return credential;
    }

    @Transactional
    public CrossChainExecutionResult execute(String taskId) {
        User user = currentUserService.user();
        accessControlService.requireCrossChainOperator(user);
        CrossChainTask task = detail(taskId);
        if (task.getStatus() == CrossChainTaskStatus.CONFIRMED && hasCompleteCrossChainHashes(task)) {
            return resultFromTask(task);
        }
        try {
            WaybillRecord waybill = waybillRepository.findByWaybillId(task.getWaybillId())
                    .orElseThrow(() -> new IllegalArgumentException("运单不存在，请先在运单管理中登记运单"));
            ChainWaybill source = fabricGateway.readWaybill(task.getSourceChain(), task.getWaybillId())
                    .orElseThrow(() -> new IllegalArgumentException("源链上没有找到该运单，请先上传文件并完成链上存证"));

            task.setErrorMessage(null);

            LocalDateTime issuedAt = task.getCredentialCreatedAt() == null ? LocalDateTime.now() : task.getCredentialCreatedAt();
            CrossChainWaybillCredential credential = readCredentialSnapshot(task);
            if (credential == null || !Objects.equals(credential.taskId(), task.getTaskId())
                    || !Objects.equals(credential.credentialId(), task.getCredentialId())
                    || !Objects.equals(credential.credentialHash(), task.getCredentialHash())) {
                credential = buildCredential(task.getCredentialId(), task.getTaskId(), waybill, source,
                        task.getTargetChain(), task.getExpiresAt(), issuedAt, null, null);
                task.setSourceTxId(credential.sourceTxId());
                task.setCredentialHash(credential.credentialHash());
                task.setCredentialSnapshot(json(credential));
                task.setCredentialCreatedAt(issuedAt);
            }
            if (!hasText(task.getSourceTxId())) {
                task.setSourceTxId(credential.sourceTxId());
            }
            task.setStatus(CrossChainTaskStatus.PENDING);

            LocalDateTime sentAt = task.getMessageCreatedAt() == null ? LocalDateTime.now() : task.getMessageCreatedAt();
            CrossChainMessage message = readMessageSnapshot(task);
            if (message == null || !Objects.equals(message.taskId(), task.getTaskId())
                    || !Objects.equals(message.credentialHash(), task.getCredentialHash())
                    || !Objects.equals(message.messageHash(), task.getMessageHash())) {
                message = buildMessage(task, credential, sentAt);
                task.setMessageId(message.messageId());
                task.setMessageHash(message.messageHash());
                task.setMessageSnapshot(json(message));
                task.setMessageCreatedAt(sentAt);
            }
            task.setStatus(CrossChainTaskStatus.ROUTED);

            CrossChainReceipt receipt = readReceiptSnapshot(task);
            LocalDateTime receivedAt;
            if (hasText(task.getTargetTxId())) {
                receivedAt = task.getTargetConfirmedAt() == null ? LocalDateTime.now() : task.getTargetConfirmedAt();
                if (receipt == null || !Objects.equals(receipt.taskId(), task.getTaskId())
                        || !Objects.equals(receipt.credentialHash(), task.getCredentialHash())
                        || !Objects.equals(receipt.messageHash(), task.getMessageHash())) {
                    Map<String, Object> receiptPayload = new LinkedHashMap<>();
                    receiptPayload.put("targetTxId", task.getTargetTxId());
                    receiptPayload.put("status", "SUCCESS");
                    receiptPayload.put("receivedAt", timeText(receivedAt));
                    receipt = buildReceipt(task, receiptPayload, receivedAt);
                    task.setReceiptId(receipt.receiptId());
                    task.setReceiptHash(receipt.receiptHash());
                    task.setReceiptSnapshot(json(receipt));
                    task.setReceiptCreatedAt(task.getReceiptCreatedAt() == null ? LocalDateTime.now() : task.getReceiptCreatedAt());
                }
            } else {
                Map<String, Object> receiptPayload = fabricGateway.receiveCrossChainCredentialMessage(task.getTargetChain(), map(message));
                task.setTargetTxId(requiredText(receiptPayload, "targetTxId"));
                task.setTargetConfirmedAt(LocalDateTime.now());
                receivedAt = parseTime(text(receiptPayload.get("receivedAt")), task.getTargetConfirmedAt());
                receipt = buildReceipt(task, receiptPayload, receivedAt);
                task.setReceiptId(receipt.receiptId());
                task.setReceiptHash(receipt.receiptHash());
                task.setReceiptSnapshot(json(receipt));
                task.setReceiptCreatedAt(LocalDateTime.now());
            }
            task.setStatus(CrossChainTaskStatus.VERIFIED);

            CrossChainWaybillCredential receivedCredential = withTargetConfirmation(credential, task.getTargetTxId(), receivedAt);
            task.setCredentialSnapshot(json(receivedCredential));

            CrossChainEvidence evidence = readEvidenceSnapshot(task);
            if (evidence == null || !Objects.equals(evidence.taskId(), task.getTaskId())
                    || !Objects.equals(evidence.credentialHash(), task.getCredentialHash())
                    || !Objects.equals(evidence.messageHash(), task.getMessageHash())
                    || !Objects.equals(evidence.receiptHash(), task.getReceiptHash())) {
                evidence = buildEvidence(task, receipt, task.getEvidenceCreatedAt() == null ? LocalDateTime.now() : task.getEvidenceCreatedAt());
                task.setEvidenceId(evidence.evidenceId());
                task.setEvidenceHash(evidence.evidenceHash());
                task.setEvidenceSnapshot(json(evidence));
                task.setEvidenceCreatedAt(evidence.createdAt());
            }
            task.setStatus(CrossChainTaskStatus.CONFIRMED);
            task.setErrorMessage(null);
            CrossChainTask saved = taskRepository.save(task);
            logService.success(user, "EXECUTE_CROSS_CHAIN", "CROSS_CHAIN", taskId, task.getTargetChain(), task.getTargetTxId(),
                    "跨链运单凭证已写入目标链并生成跨链存证");
            return new CrossChainExecutionResult(saved, receivedCredential, message, receipt, evidence, timeline(taskId));
        } catch (Exception e) {
            task.setStatus(CrossChainTaskStatus.FAILED);
            task.setErrorMessage(e.getMessage());
            CrossChainTask saved = taskRepository.save(task);
            logService.failed(user, "EXECUTE_CROSS_CHAIN", "CROSS_CHAIN", taskId, task.getTargetChain(), e.getMessage());
            return new CrossChainExecutionResult(saved, readCredentialSnapshot(task), readMessageSnapshot(task),
                    readReceiptSnapshot(task), readEvidenceSnapshot(task), timeline(taskId));
        }
    }

    @Transactional
    public VerifyResult verify(String taskId) {
        accessControlService.requireCrossChainOperator(currentUserService.user());
        CrossChainTask task = detail(taskId);
        if (task.getStatus() != CrossChainTaskStatus.CONFIRMED) {
            VerifyResult result = new VerifyResult(false, "该跨链任务尚未确认成功，不能核验跨链凭证链路。");
            saveVerifyResult(task, result);
            return result;
        }
        CrossChainCompareResult compare = compare(taskId);
        VerifyResult result = new VerifyResult(compare.passed(), compare.passed()
                ? "核验通过：源链存证、跨链凭证、目标链确认和跨链存证链路一致。"
                : "核验失败：跨链凭证链路存在不一致，请查看对比明细。");
        saveVerifyResult(task, result);
        return result;
    }

    @Transactional
    public CrossChainCompareResult compare(String taskId) {
        CrossChainTask task = detail(taskId);
        CrossChainCompareResult result = buildCompareResult(task);
        task.setComparedAt(LocalDateTime.now());
        taskRepository.save(task);
        return result;
    }

    private CrossChainCompareResult buildCompareResult(CrossChainTask task) {
        CrossChainWaybillCredential credential = credential(task.getTaskId());
        ChainWaybill source = fabricGateway.readWaybill(task.getSourceChain(), task.getWaybillId())
                .orElseThrow(() -> new IllegalArgumentException("源链上没有找到该运单"));
        ChainWaybill target = fabricGateway.readWaybill(task.getTargetChain(), task.getWaybillId())
                .orElseThrow(() -> new IllegalArgumentException("目标链上没有找到该跨链凭证，请先执行任务"));

        List<CrossChainCompareItem> items = new ArrayList<>();
        items.add(compareItem("source.fileHash == credential.fileHash", source.fileHash(), credential.fileHash()));
        items.add(compareItem("source.encryptedFileHash == credential.encryptedFileHash", source.encryptedFileHash(), credential.encryptedFileHash()));
        items.add(compareItem("task.credentialHash == recomputedCredentialHash", task.getCredentialHash(), credentialHash(credential)));
        items.add(compareItem("target.credentialHash == task.credentialHash", target.credentialHash(), task.getCredentialHash()));
        items.add(compareItem("target.messageHash == task.messageHash", target.messageHash(), task.getMessageHash()));
        items.add(compareItem("evidence.credentialHash == task.credentialHash", readEvidenceSnapshot(task) == null ? "" : readEvidenceSnapshot(task).credentialHash(), task.getCredentialHash()));

        Map<String, Object> sourceView = new LinkedHashMap<>();
        sourceView.put("sourceChain", task.getSourceChain());
        sourceView.put("sourceTxId", task.getSourceTxId());
        sourceView.put("fileHash", source.fileHash());
        sourceView.put("encryptedFileHash", source.encryptedFileHash());

        Map<String, Object> targetView = new LinkedHashMap<>();
        targetView.put("targetChain", task.getTargetChain());
        targetView.put("targetTxId", task.getTargetTxId());
        targetView.put("credentialHash", target.credentialHash());
        targetView.put("messageHash", target.messageHash());

        return new CrossChainCompareResult(items.stream().allMatch(CrossChainCompareItem::matched),
                sourceView, targetView, items);
    }

    public CrossChainWaybillCredential credential(String taskId) {
        CrossChainTask task = detail(taskId);
        CrossChainWaybillCredential snapshot = readCredentialSnapshot(task);
        if (snapshot != null) {
            return snapshot;
        }
        return credentialPreview(requestFrom(task));
    }

    public CrossChainEvidence evidence(String taskId) {
        CrossChainTask task = detail(taskId);
        CrossChainEvidence evidence = readEvidenceSnapshot(task);
        if (evidence == null) {
            throw new IllegalArgumentException("该跨链任务尚未生成跨链存证");
        }
        return evidence;
    }

    public List<TimelineItem> timeline(String taskId) {
        CrossChainTask task = detail(taskId);
        List<TimelineItem> items = new ArrayList<>();
        items.add(new TimelineItem("业务系统", timeText(task.getCreatedAt()), "创建跨链任务",
                "创建 " + task.getSourceChain() + " -> " + task.getTargetChain() + " 运单凭证共享任务", null, false,
                List.of(detail("共享对象", "CrossChainWaybillCredential", false))));
        if (task.getPrecheckedAt() != null) {
            items.add(new TimelineItem("业务系统", timeText(task.getPrecheckedAt()), "跨链前置检查",
                    "完成运单、链路、源链存证和有效期检查", null, false,
                    List.of(detail("有效期", timeText(task.getExpiresAt()), false))));
        }
        if (task.getCredentialCreatedAt() != null || hasText(task.getCredentialHash())) {
            items.add(new TimelineItem("业务系统", timeText(task.getCredentialCreatedAt() == null ? task.getUpdatedAt() : task.getCredentialCreatedAt()),
                    "生成跨链运单凭证", "生成 CrossChainWaybillCredential 并计算 credentialHash", null, false,
                    List.of(detail("credentialHash", task.getCredentialHash(), true))));
        }
        if (task.getMessageCreatedAt() != null || hasText(task.getMessageHash())) {
            items.add(new TimelineItem("业务系统", timeText(task.getMessageCreatedAt() == null ? task.getUpdatedAt() : task.getMessageCreatedAt()),
                    "生成跨链消息", "封装 WAYBILL_CREDENTIAL_SHARE 消息并计算 messageHash", null, false,
                    List.of(detail("messageHash", task.getMessageHash(), true))));
        }
        if (hasText(task.getTargetTxId())) {
            items.add(simpleTimeline("Fabric 目标链", task.getTargetConfirmedAt() == null ? task.getUpdatedAt() : task.getTargetConfirmedAt(),
                    "ReceiveCrossChainCredentialMessage", "目标链接收并校验跨链运单凭证", task.getTargetTxId()));
        }
        if (task.getReceiptCreatedAt() != null || hasText(task.getReceiptHash())) {
            items.add(new TimelineItem("业务系统", timeText(task.getReceiptCreatedAt() == null ? task.getUpdatedAt() : task.getReceiptCreatedAt()),
                    "生成跨链回执", "根据目标链确认结果生成 CrossChainReceipt", null, false,
                    List.of(detail("receiptHash", task.getReceiptHash(), true))));
        }
        if (task.getEvidenceCreatedAt() != null || hasText(task.getEvidenceHash())) {
            items.add(new TimelineItem("业务系统", timeText(task.getEvidenceCreatedAt() == null ? task.getUpdatedAt() : task.getEvidenceCreatedAt()),
                    "生成跨链存证", "聚合凭证、消息和回执生成 CrossChainEvidence", null, false,
                    List.of(detail("evidenceHash", task.getEvidenceHash(), true))));
        }
        if (task.getComparedAt() != null) {
            items.add(simpleTimeline("业务系统", task.getComparedAt(), "双链凭证链路对比",
                    "源链存证、跨链凭证与目标链确认完成一致性对比", null));
        }
        if (task.getVerifiedAt() != null) {
            items.add(simpleTimeline("业务系统", task.getVerifiedAt(), "跨链核验",
                    task.getLastVerifyPassed() == Boolean.TRUE ? "核验通过" : "核验失败：" + text(task.getLastVerifyMessage()), null));
        }
        if (task.getStatus() == CrossChainTaskStatus.FAILED && hasText(task.getErrorMessage())) {
            items.add(simpleTimeline("业务系统", task.getUpdatedAt(), "跨链任务失败", task.getErrorMessage(), null));
        }
        return items;
    }

    private CrossChainExecutionResult resultFromTask(CrossChainTask task) {
        return new CrossChainExecutionResult(task, readCredentialSnapshot(task), readMessageSnapshot(task),
                readReceiptSnapshot(task), readEvidenceSnapshot(task), timeline(task.getTaskId()));
    }

    private CrossChainWaybillCredential buildCredential(String credentialId, String taskId, WaybillRecord waybill, ChainWaybill source,
                                                       ChainName targetChain, LocalDateTime expiresAt, LocalDateTime issuedAt,
                                                       String targetTxId, LocalDateTime receivedAt) {
        CrossChainWaybillCredential unsigned = new CrossChainWaybillCredential(
                credentialId,
                taskId,
                CREDENTIAL_TYPE,
                waybill.getWaybillId(),
                waybill.getSmgsNo(),
                waybill.getDepartureStation(),
                waybill.getDestinationStation(),
                route(waybill),
                waybill.getGoodsName(),
                waybill.getContainerNo(),
                waybill.getStatus().name(),
                text(waybill.getConsignorOrgId()),
                text(waybill.getCarrierOrgId()),
                text(waybill.getConsigneeOrgId()),
                text(waybill.getCustomsOrgId()),
                text(waybill.getForwarderOrgId()),
                source.fileHash(),
                source.encryptedFileHash(),
                source.currentChain(),
                targetChain,
                text(waybill.getChainTxId()),
                targetTxId,
                issuedAt,
                receivedAt,
                expiresAt,
                null
        );
        return withCredentialHash(unsigned, credentialHash(unsigned));
    }

    private CrossChainMessage buildMessage(CrossChainTask task, CrossChainWaybillCredential credential, LocalDateTime sentAt) {
        CrossChainMessage unsigned = new CrossChainMessage(
                hasText(task.getMessageId()) ? task.getMessageId() : "XCM-" + shortId(),
                task.getTaskId(),
                MESSAGE_TYPE,
                task.getSourceChain(),
                task.getTargetChain(),
                credential,
                credential.credentialHash(),
                sentAt,
                null
        );
        return new CrossChainMessage(unsigned.messageId(), unsigned.taskId(), unsigned.messageType(), unsigned.sourceChain(),
                unsigned.targetChain(), unsigned.credential(), unsigned.credentialHash(), unsigned.sentAt(), messageHash(unsigned));
    }

    private CrossChainReceipt buildReceipt(CrossChainTask task, Map<String, Object> payload, LocalDateTime receivedAt) {
        CrossChainReceipt unsigned = new CrossChainReceipt(
                hasText(task.getReceiptId()) ? task.getReceiptId() : "XCR-" + shortId(),
                task.getMessageId(),
                task.getTaskId(),
                task.getSourceChain(),
                task.getTargetChain(),
                task.getTargetTxId(),
                text(payload.getOrDefault("status", "SUCCESS")),
                text(payload.get("failureReason")),
                receivedAt,
                task.getCredentialHash(),
                task.getMessageHash(),
                null
        );
        return new CrossChainReceipt(unsigned.receiptId(), unsigned.messageId(), unsigned.taskId(), unsigned.sourceChain(),
                unsigned.targetChain(), unsigned.targetTxId(), unsigned.status(), unsigned.failureReason(), unsigned.receivedAt(),
                unsigned.credentialHash(), unsigned.messageHash(), receiptHash(unsigned));
    }

    private CrossChainEvidence buildEvidence(CrossChainTask task, CrossChainReceipt receipt, LocalDateTime createdAt) {
        CrossChainEvidence unsigned = new CrossChainEvidence(
                hasText(task.getEvidenceId()) ? task.getEvidenceId() : "XCE-" + shortId(),
                task.getTaskId(),
                task.getCredentialId(),
                task.getMessageId(),
                receipt.receiptId(),
                task.getSourceChain(),
                task.getTargetChain(),
                task.getSourceTxId(),
                task.getTargetTxId(),
                task.getCredentialHash(),
                task.getMessageHash(),
                receipt.receiptHash(),
                null,
                createdAt
        );
        return new CrossChainEvidence(unsigned.evidenceId(), unsigned.taskId(), unsigned.credentialId(), unsigned.messageId(),
                unsigned.receiptId(), unsigned.sourceChain(), unsigned.targetChain(), unsigned.sourceTxId(), unsigned.targetTxId(),
                unsigned.credentialHash(), unsigned.messageHash(), unsigned.receiptHash(), evidenceHash(unsigned), unsigned.createdAt());
    }

    private CrossChainWaybillCredential withTargetConfirmation(CrossChainWaybillCredential c, String targetTxId, LocalDateTime receivedAt) {
        return new CrossChainWaybillCredential(c.credentialId(), c.taskId(), c.credentialType(), c.waybillId(), c.smgsNo(),
                c.departureStation(), c.destinationStation(), c.route(), c.goodsName(), c.containerNo(), c.transportStatus(),
                c.consignorOrgId(), c.carrierOrgId(), c.consigneeOrgId(), c.customsOrgId(), c.forwarderOrgId(),
                c.fileHash(), c.encryptedFileHash(), c.sourceChain(), c.targetChain(), c.sourceTxId(), targetTxId,
                c.issuedAt(), receivedAt, c.expiresAt(), c.credentialHash());
    }

    private CrossChainWaybillCredential withCredentialHash(CrossChainWaybillCredential c, String hash) {
        return new CrossChainWaybillCredential(c.credentialId(), c.taskId(), c.credentialType(), c.waybillId(), c.smgsNo(),
                c.departureStation(), c.destinationStation(), c.route(), c.goodsName(), c.containerNo(), c.transportStatus(),
                c.consignorOrgId(), c.carrierOrgId(), c.consigneeOrgId(), c.customsOrgId(), c.forwarderOrgId(),
                c.fileHash(), c.encryptedFileHash(), c.sourceChain(), c.targetChain(), c.sourceTxId(), c.targetTxId(),
                c.issuedAt(), c.receivedAt(), c.expiresAt(), hash);
    }

    private String credentialHash(CrossChainWaybillCredential c) {
        return sha256(String.join("\n", text(c.credentialId()), text(c.taskId()), text(c.credentialType()),
                text(c.waybillId()), text(c.smgsNo()), text(c.departureStation()), text(c.destinationStation()),
                text(c.route()), text(c.goodsName()), text(c.containerNo()), text(c.transportStatus()),
                text(c.consignorOrgId()), text(c.carrierOrgId()), text(c.consigneeOrgId()), text(c.customsOrgId()),
                text(c.forwarderOrgId()), text(c.fileHash()), text(c.encryptedFileHash()), text(c.sourceChain()),
                text(c.targetChain()), text(c.sourceTxId()), timeText(c.issuedAt()), timeText(c.expiresAt())));
    }

    private String messageHash(CrossChainMessage m) {
        return sha256(String.join("\n", text(m.messageId()), text(m.taskId()), text(m.messageType()),
                text(m.sourceChain()), text(m.targetChain()), text(m.credentialHash()), timeText(m.sentAt())));
    }

    private String receiptHash(CrossChainReceipt r) {
        return sha256(String.join("\n", text(r.receiptId()), text(r.messageId()), text(r.taskId()), text(r.targetChain()),
                text(r.targetTxId()), text(r.status()), timeText(r.receivedAt()), text(r.credentialHash()), text(r.messageHash())));
    }

    private String evidenceHash(CrossChainEvidence e) {
        return sha256(String.join("\n", text(e.taskId()), text(e.credentialHash()), text(e.messageHash()), text(e.receiptHash()),
                text(e.sourceTxId()), text(e.targetTxId()), timeText(e.createdAt())));
    }

    private void validateTaskRequest(CrossChainTaskRequest request) {
        if (request.sourceChain() == request.targetChain()) {
            throw new IllegalArgumentException("源链和目标链不能相同");
        }
        WaybillRecord waybill = waybillRepository.findByWaybillId(request.waybillId())
                .orElseThrow(() -> new IllegalArgumentException("运单不存在，请先在运单管理中登记运单"));
        if (waybill.getCurrentChain() != request.sourceChain()) {
            throw new IllegalArgumentException("源链选择不正确。该运单当前在 " + waybill.getCurrentChain()
                    + "，请把源链改为 " + waybill.getCurrentChain() + " 后再创建跨链任务");
        }
        if (request.expiresAt() == null || !request.expiresAt().isAfter(LocalDateTime.now())) {
            throw new IllegalArgumentException("有效期必须晚于当前时间");
        }
        accessControlService.requireCrossChainOperator(currentUserService.user());
    }

    private CrossChainTaskRequest requestFrom(CrossChainTask task) {
        return new CrossChainTaskRequest(task.getWaybillId(), task.getSourceChain(), task.getTargetChain(),
                task.getPurpose(), task.getExpiresAt());
    }

    private void saveVerifyResult(CrossChainTask task, VerifyResult result) {
        task.setVerifiedAt(LocalDateTime.now());
        task.setLastVerifyPassed(result.passed());
        task.setLastVerifyMessage(result.detail());
        taskRepository.save(task);
    }

    private boolean hasCompleteCrossChainHashes(CrossChainTask task) {
        return hasText(task.getSourceTxId()) && hasText(task.getTargetTxId()) && hasText(task.getCredentialHash())
                && hasText(task.getMessageHash()) && hasText(task.getReceiptHash()) && hasText(task.getEvidenceHash());
    }

    private boolean ensureTaskDefaults(CrossChainTask task) {
        boolean changed = false;
        if (!hasText(task.getCredentialId())) {
            task.setCredentialId("XCC-" + shortId());
            changed = true;
        }
        if (task.getStatus() == CrossChainTaskStatus.CONFIRMED && hasText(task.getErrorMessage())) {
            task.setErrorMessage(null);
            changed = true;
        }
        return changed;
    }

    private String currentStep(CrossChainTask task) {
        if (task.getStatus() == CrossChainTaskStatus.FAILED) {
            return failureStage(task);
        }
        if (hasText(task.getEvidenceHash())) {
            return "CROSS_CHAIN_EVIDENCE_CREATED";
        }
        if (hasText(task.getReceiptHash())) {
            return "RECEIPT_RETURNED";
        }
        if (hasText(task.getTargetTxId())) {
            return "CROSS_CHAIN_EVIDENCE_CREATED";
        }
        if (task.getStatus() == CrossChainTaskStatus.RELAYING || task.getStatus() == CrossChainTaskStatus.ROUTED) {
            return "RELAY_ROUTING";
        }
        if (hasText(task.getMessageHash())) {
            return "CROSS_CHAIN_MESSAGE_CREATED";
        }
        if (hasText(task.getCredentialHash())) {
            return "WAYBILL_CREDENTIAL_CREATED";
        }
        return "SOURCE_CHAIN_CONFIRMED";
    }

    private String failureStage(CrossChainTask task) {
        if (task.getStatus() != CrossChainTaskStatus.FAILED) {
            return "";
        }
        String message = text(task.getErrorMessage());
        if (message.contains("源链上没有找到")) {
            return "SOURCE_CHAIN_CONFIRMED";
        }
        if (!hasText(task.getCredentialHash())) {
            return "WAYBILL_CREDENTIAL_CREATED";
        }
        if (!hasText(task.getMessageHash())) {
            return "CROSS_CHAIN_MESSAGE_CREATED";
        }
        if (!hasText(task.getTargetTxId())) {
            return message.contains("Fabric transaction failed") || message.contains("链上")
                    ? "TARGET_CHAIN_WRITE"
                    : "TARGET_CHAIN_RECEIVED";
        }
        if (!hasText(task.getReceiptHash())) {
            return "RECEIPT_RETURNED";
        }
        if (!hasText(task.getEvidenceHash())) {
            return "CROSS_CHAIN_EVIDENCE_CREATED";
        }
        return "RELAY_ROUTING";
    }

    private String failureSuggestion(CrossChainTask task) {
        String message = text(task.getErrorMessage());
        if (message.contains("源链上没有找到")) {
            return "确认运单已经上传文件并完成源链存证，然后重新执行跨链任务。";
        }
        if (message.contains("invalid credential hash") || message.contains("invalid message hash")) {
            return "重新生成凭证和跨链消息后重试，检查目标链链码版本是否已更新。";
        }
        if (message.contains("Fabric transaction failed")) {
            return "检查 Fabric peer、orderer 和链码容器状态，恢复后点击重试。";
        }
        return "检查失败阶段对应的链路和日志，修复后点击重试执行。";
    }

    private boolean hydrateMissingEvidenceChain(CrossChainTask task) {
        if (hasCompleteCrossChainHashes(task)) {
            return false;
        }
        boolean changed = false;
        try {
            WaybillRecord waybill = waybillRepository.findByWaybillId(task.getWaybillId()).orElse(null);
            ChainWaybill source = fabricGateway.readWaybill(task.getSourceChain(), task.getWaybillId()).orElse(null);
            if (waybill == null || source == null) {
                return false;
            }
            LocalDateTime issuedAt = task.getCredentialCreatedAt() == null ? task.getCreatedAt() : task.getCredentialCreatedAt();
            if (issuedAt == null) {
                issuedAt = LocalDateTime.now();
            }
            CrossChainWaybillCredential credential = readCredentialSnapshot(task);
            if (credential == null || !hasText(task.getCredentialHash()) || !Objects.equals(credential.taskId(), task.getTaskId())
                    || !Objects.equals(credential.credentialId(), task.getCredentialId())) {
                credential = buildCredential(task.getCredentialId(), task.getTaskId(), waybill, source,
                        task.getTargetChain(), task.getExpiresAt(), issuedAt, task.getTargetTxId(), task.getTargetConfirmedAt());
                task.setSourceTxId(hasText(task.getSourceTxId()) ? task.getSourceTxId() : credential.sourceTxId());
                task.setCredentialHash(credential.credentialHash());
                task.setCredentialSnapshot(json(credential));
                task.setCredentialCreatedAt(issuedAt);
                changed = true;
            }
            CrossChainMessage message = readMessageSnapshot(task);
            if (message == null || !hasText(task.getMessageHash()) || !Objects.equals(message.taskId(), task.getTaskId())
                    || !Objects.equals(message.credentialHash(), task.getCredentialHash())) {
                LocalDateTime sentAt = task.getMessageCreatedAt() == null ? issuedAt : task.getMessageCreatedAt();
                message = buildMessage(task, credential, sentAt);
                task.setMessageId(message.messageId());
                task.setMessageHash(message.messageHash());
                task.setMessageSnapshot(json(message));
                task.setMessageCreatedAt(sentAt);
                changed = true;
            }
            if (hasText(task.getTargetTxId())) {
                CrossChainReceipt receipt = readReceiptSnapshot(task);
                if (receipt == null || !hasText(task.getReceiptHash()) || !Objects.equals(receipt.taskId(), task.getTaskId())
                        || !Objects.equals(receipt.credentialHash(), task.getCredentialHash())
                        || !Objects.equals(receipt.messageHash(), task.getMessageHash())) {
                    Map<String, Object> payload = new LinkedHashMap<>();
                    payload.put("targetTxId", task.getTargetTxId());
                    payload.put("status", "SUCCESS");
                    payload.put("receivedAt", timeText(task.getTargetConfirmedAt() == null ? LocalDateTime.now() : task.getTargetConfirmedAt()));
                    receipt = buildReceipt(task, payload, task.getTargetConfirmedAt() == null ? LocalDateTime.now() : task.getTargetConfirmedAt());
                    task.setReceiptId(receipt.receiptId());
                    task.setReceiptHash(receipt.receiptHash());
                    task.setReceiptSnapshot(json(receipt));
                    task.setReceiptCreatedAt(task.getReceiptCreatedAt() == null ? LocalDateTime.now() : task.getReceiptCreatedAt());
                    changed = true;
                }
                CrossChainEvidence evidence = readEvidenceSnapshot(task);
                if ((task.getStatus() == CrossChainTaskStatus.CONFIRMED || hasText(task.getEvidenceHash()))
                        && (evidence == null || !hasText(task.getEvidenceHash()) || !Objects.equals(evidence.taskId(), task.getTaskId())
                        || !Objects.equals(evidence.credentialHash(), task.getCredentialHash())
                        || !Objects.equals(evidence.messageHash(), task.getMessageHash())
                        || !Objects.equals(evidence.receiptHash(), task.getReceiptHash()))) {
                    evidence = buildEvidence(task, receipt, task.getEvidenceCreatedAt() == null ? LocalDateTime.now() : task.getEvidenceCreatedAt());
                    task.setEvidenceId(evidence.evidenceId());
                    task.setEvidenceHash(evidence.evidenceHash());
                    task.setEvidenceSnapshot(json(evidence));
                    task.setEvidenceCreatedAt(evidence.createdAt());
                    changed = true;
                }
            }
        } catch (Exception ignored) {
            return changed;
        }
        return changed;
    }

    private List<CrossChainTimelineNode> detailTimeline(CrossChainTask task) {
        String failed = failureStage(task);
        boolean sourceConfirmed = !"SOURCE_CHAIN_CONFIRMED".equals(failed);
        return List.of(
                timelineNode(task, 1, "SOURCE_CHAIN_CONFIRMED", "源链确认", "Source Confirmed", sourceConfirmed, task.getSourcePreparedAt(), "源链已存在运单存证记录", "SOURCE", task.getWaybillId(), failed),
                timelineNode(task, 2, "WAYBILL_CREDENTIAL_CREATED", "生成凭证", "Credential Generated", hasText(task.getCredentialHash()), task.getCredentialCreatedAt(), "生成 CrossChainWaybillCredential 并计算 credentialHash", "CREDENTIAL", task.getCredentialId(), failed),
                timelineNode(task, 3, "CROSS_CHAIN_MESSAGE_CREATED", "生成消息", "Message Generated", hasText(task.getMessageHash()) || hasText(task.getMessageId()), task.getMessageCreatedAt(), "生成 CrossChainMessage 并计算 messageHash", "MESSAGE", task.getMessageId(), failed),
                timelineNode(task, 4, "RELAY_ROUTING", "中继路由", "Relay Routed", relayCompleted(task, failed), task.getMessageCreatedAt(), "CrossChainRelayService 根据 targetChain 路由消息", "RELAY", "CrossChainRelayService", failed),
                timelineNode(task, 5, "TARGET_CHAIN_RECEIVED", "目标链接收", "Target Received", hasText(task.getTargetTxId()), task.getTargetConfirmedAt(), "目标链接收并校验跨链凭证", "TARGET", task.getTargetTxId(), failed),
                timelineNode(task, 6, "RECEIPT_RETURNED", "返回回执", "Receipt Returned", hasText(task.getReceiptHash()) || hasText(task.getTargetTxId()), task.getReceiptCreatedAt() == null ? task.getTargetConfirmedAt() : task.getReceiptCreatedAt(), "目标链返回接收回执并计算 receiptHash", "RECEIPT", task.getReceiptId(), failed),
                timelineNode(task, 7, "CROSS_CHAIN_EVIDENCE_CREATED", "生成存证", "Evidence Generated", hasText(task.getEvidenceHash()), task.getEvidenceCreatedAt(), "生成 CrossChainEvidence 并计算 evidenceHash", "EVIDENCE", task.getEvidenceId(), failed)
        );
    }

    private boolean relayCompleted(CrossChainTask task, String failedStage) {
        return hasText(task.getMessageHash()) && (hasText(task.getTargetTxId())
                || task.getStatus() == CrossChainTaskStatus.RELAYING
                || task.getStatus() == CrossChainTaskStatus.ROUTED
                || "TARGET_CHAIN_WRITE".equals(failedStage)
                || "TARGET_CHAIN_RECEIVED".equals(failedStage));
    }

    private CrossChainTimelineNode timelineNode(CrossChainTask task, int stepNo, String key, String titleZh, String titleEn,
                                                boolean completed, LocalDateTime time, String summary,
                                                String relatedObjectType, String relatedObjectId, String failedStage) {
        String status;
        if (task.getStatus() == CrossChainTaskStatus.FAILED && (key.equals(failedStage) || ("TARGET_CHAIN_WRITE".equals(failedStage) && "TARGET_CHAIN_RECEIVED".equals(key)))) {
            status = "failed";
        } else if (completed) {
            status = "completed";
        } else if (key.equals(currentStep(task))) {
            status = "active";
        } else {
            status = "pending";
        }
        String errorMessage = "failed".equals(status) ? text(task.getErrorMessage()) : "";
        return new CrossChainTimelineNode(stepNo, key, key, titleZh, titleEn, titleZh + " " + titleEn, status,
                timeText(time), summary, summary, relatedObjectType, relatedObjectId, errorMessage);
    }

    private List<CrossChainDetailCompareItem> detailComparison(CrossChainTask task) {
        CrossChainWaybillCredential credential = readCredentialSnapshot(task);
        CrossChainEvidence evidence = readEvidenceSnapshot(task);
        List<CrossChainDetailCompareItem> rows = new ArrayList<>();
        if (credential == null) {
            rows.add(new CrossChainDetailCompareItem("凭证身份信息", "credential", "-", "-", "缺失"));
            rows.add(new CrossChainDetailCompareItem("运输业务摘要", "businessSummary", "-", "-", "缺失"));
            rows.add(new CrossChainDetailCompareItem("参与方摘要", "participantSummary", "-", "-", "缺失"));
            rows.add(new CrossChainDetailCompareItem("链上证据信息", "onChainEvidence", "-", "-", "缺失"));
            return rows;
        }
        String targetCredentialHash = hasText(task.getTargetTxId()) ? task.getCredentialHash() : "";
        rows.add(detailCompare("凭证身份信息", "credentialId", credential.credentialId(), task.getCredentialId()));
        rows.add(detailCompare("凭证身份信息", "waybillId", credential.waybillId(), task.getWaybillId()));
        rows.add(detailCompare("凭证身份信息", "credentialHash", credential.credentialHash(), targetCredentialHash));
        rows.add(detailCompare("运输业务摘要", "route", credential.route(), credential.route()));
        rows.add(detailCompare("运输业务摘要", "goodsName", credential.goodsName(), credential.goodsName()));
        rows.add(detailCompare("运输业务摘要", "containerNo", credential.containerNo(), credential.containerNo()));
        rows.add(detailCompare("参与方摘要", "consignorOrgId", credential.consignorOrgId(), credential.consignorOrgId()));
        rows.add(detailCompare("参与方摘要", "carrierOrgId", credential.carrierOrgId(), credential.carrierOrgId()));
        rows.add(detailCompare("参与方摘要", "consigneeOrgId", credential.consigneeOrgId(), credential.consigneeOrgId()));
        rows.add(detailCompare("链上证据信息", "fileHash", credential.fileHash(), credential.fileHash()));
        rows.add(detailCompare("链上证据信息", "sourceTxId", credential.sourceTxId(), task.getSourceTxId()));
        rows.add(detailCompare("链上证据信息", "targetTxId", credential.targetTxId(), task.getTargetTxId()));
        rows.add(detailCompare("链上证据信息", "evidenceHash", evidence == null ? "" : evidence.evidenceHash(), task.getEvidenceHash()));
        return rows;
    }

    private CrossChainDetailCompareItem detailCompare(String category, String field, Object source, Object target) {
        String result;
        if (!hasText(text(source)) || !hasText(text(target))) {
            result = "缺失";
        } else if (Objects.equals(text(source), text(target))) {
            result = "一致";
        } else {
            result = "不一致";
        }
        return new CrossChainDetailCompareItem(category, field, source, target, result);
    }

    private CrossChainCompareItem compareItem(String field, Object source, Object target) {
        return new CrossChainCompareItem(field, source, target, Objects.equals(text(source), text(target)));
    }

    private String requiredText(Map<String, Object> values, String key) {
        String value = text(values.get(key));
        if (!hasText(value)) {
            throw new IllegalStateException("Fabric 返回结果缺少 " + key);
        }
        return value;
    }

    private Map<String, Object> map(Object value) {
        return objectMapper.convertValue(value, new TypeReference<>() {
        });
    }

    private String json(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("序列化跨链对象失败", e);
        }
    }

    private <T> T read(String json, Class<T> type) {
        if (!hasText(json)) {
            return null;
        }
        try {
            return objectMapper.readValue(json, type);
        } catch (Exception e) {
            return null;
        }
    }

    private CrossChainWaybillCredential readCredentialSnapshot(CrossChainTask task) {
        return read(task.getCredentialSnapshot(), CrossChainWaybillCredential.class);
    }

    private CrossChainMessage readMessageSnapshot(CrossChainTask task) {
        return read(task.getMessageSnapshot(), CrossChainMessage.class);
    }

    private CrossChainReceipt readReceiptSnapshot(CrossChainTask task) {
        return read(task.getReceiptSnapshot(), CrossChainReceipt.class);
    }

    private CrossChainEvidence readEvidenceSnapshot(CrossChainTask task) {
        return read(task.getEvidenceSnapshot(), CrossChainEvidence.class);
    }

    private LocalDateTime parseTime(String value, LocalDateTime fallback) {
        if (!hasText(value)) {
            return fallback;
        }
        try {
            return LocalDateTime.parse(value.replace("Z", ""));
        } catch (Exception e) {
            return fallback;
        }
    }

    private String route(WaybillRecord waybill) {
        return text(waybill.getDepartureStation()) + " -> " + text(waybill.getDestinationStation());
    }

    private TimelineItem simpleTimeline(String source, LocalDateTime time, String action, String summary, String txId) {
        return new TimelineItem(source, timeText(time), action, summary, txId, false, List.of());
    }

    private TimelineItem.TimelineDetail detail(String label, String value, boolean hash) {
        return new TimelineItem.TimelineDetail(label, text(value), hash);
    }

    private String failedDetails(List<CrossChainCheckItem> items) {
        return items.stream().filter(item -> !item.passed()).map(item -> item.name() + "：" + item.detail()).toList().toString();
    }

    private String shortText(String value) {
        if (!hasText(value)) {
            return "-";
        }
        return value.length() <= 24 ? value : value.substring(0, 12) + "..." + value.substring(value.length() - 8);
    }

    private String shortId() {
        return UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }

    private String sha256(String value) {
        return HashUtils.sha256Hex(value.getBytes(StandardCharsets.UTF_8));
    }

    private String timeText(LocalDateTime value) {
        return value == null ? "" : value.format(java.time.format.DateTimeFormatter.ISO_LOCAL_DATE_TIME);
    }

    private String text(Object value) {
        return value == null ? "" : String.valueOf(value);
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank() && !"null".equalsIgnoreCase(value);
    }
}
