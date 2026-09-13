package com.example.waybill.service;

import com.example.waybill.entity.*;
import com.example.waybill.repository.EncryptedFileRepository;
import com.example.waybill.repository.FileKeyGrantRepository;
import com.example.waybill.repository.WaybillRecordRepository;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;

@Service
public class AccessControlService {
    private final CurrentUserService currentUserService;
    private final WaybillRecordRepository waybillRepository;
    private final EncryptedFileRepository fileRepository;
    private final FileKeyGrantRepository grantRepository;

    public AccessControlService(CurrentUserService currentUserService,
                                WaybillRecordRepository waybillRepository,
                                EncryptedFileRepository fileRepository,
                                FileKeyGrantRepository grantRepository) {
        this.currentUserService = currentUserService;
        this.waybillRepository = waybillRepository;
        this.fileRepository = fileRepository;
        this.grantRepository = grantRepository;
    }

    public User currentUser() {
        return currentUserService.user();
    }

    public Long orgId(User user) {
        return user.getOrganization() == null ? null : user.getOrganization().getId();
    }

    public boolean isAdmin(User user) {
        return user.getRole() == Role.ADMIN;
    }

    public WaybillRecord requireWaybill(String waybillId) {
        return waybillRepository.findByWaybillId(waybillId).orElseThrow(() -> new IllegalArgumentException("运单不存在"));
    }

    public void requireCanCreateWaybill(User user, WaybillRecord record) {
        Long orgId = orgId(user);
        if (isAdmin(user)) {
            return;
        }
        if (orgId == null) {
            deny();
        }
        boolean allowed = switch (user.getRole()) {
            case CONSIGNOR -> same(orgId, record.getConsignorOrgId());
            case FORWARDER -> record.getConsignorOrgId() != null && same(orgId, record.getForwarderOrgId());
            case CARRIER -> same(orgId, record.getCarrierOrgId());
            default -> false;
        };
        if (!allowed) {
            deny();
        }
    }

    public boolean canViewWaybill(User user, WaybillRecord record) {
        if (isAdmin(user)) {
            return true;
        }
        Long orgId = orgId(user);
        if (orgId == null) {
            return false;
        }
        return switch (user.getRole()) {
            case CONSIGNOR -> same(orgId, record.getConsignorOrgId()) || hasWaybillGrant(user, record.getWaybillId());
            case FORWARDER -> same(orgId, record.getForwarderOrgId()) || hasWaybillGrant(user, record.getWaybillId());
            case CARRIER -> same(orgId, record.getCarrierOrgId());
            case CUSTOMS -> same(orgId, record.getCustomsOrgId()) || hasWaybillGrant(user, record.getWaybillId());
            case CONSIGNEE -> same(orgId, record.getConsigneeOrgId()) || hasWaybillGrant(user, record.getWaybillId());
            case CROSS_CHAIN_GATEWAY -> false;
            case ADMIN -> true;
        };
    }

    public void requireCanViewWaybill(User user, WaybillRecord record) {
        if (!canViewWaybill(user, record)) {
            deny();
        }
    }

    public List<WaybillRecord> filterVisibleWaybills(User user, List<WaybillRecord> records) {
        if (isAdmin(user)) {
            return records;
        }
        if (user.getRole() == Role.CROSS_CHAIN_GATEWAY) {
            return List.of();
        }
        return records.stream().filter(record -> canViewWaybill(user, record)).toList();
    }

    public void requireCanUpload(User user, WaybillRecord record) {
        if (isAdmin(user)) {
            return;
        }
        Long orgId = orgId(user);
        boolean allowed = orgId != null && switch (user.getRole()) {
            case CONSIGNOR -> same(orgId, record.getConsignorOrgId());
            case FORWARDER -> same(orgId, record.getForwarderOrgId()) || hasWaybillGrant(user, record.getWaybillId());
            case CARRIER -> same(orgId, record.getCarrierOrgId());
            default -> false;
        };
        if (!allowed) {
            deny();
        }
    }

    public boolean canDownload(User user, EncryptedFile file) {
        if (isAdmin(user)) {
            return true;
        }
        if (user.getRole() == Role.CROSS_CHAIN_GATEWAY) {
            return false;
        }
        if (file.getUploaderId() != null && file.getUploaderId().equals(user.getId())) {
            return true;
        }
        Long orgId = orgId(user);
        return grantRepository.existsByFileIdAndGrantStatusAndGranteeUserId(file.getId(), GrantStatus.ACTIVE, user.getId())
                || (orgId != null && grantRepository.existsByFileIdAndGrantStatusAndGranteeOrgId(file.getId(), GrantStatus.ACTIVE, orgId))
                || grantRepository.existsByFileIdAndGrantStatusAndGranteeRole(file.getId(), GrantStatus.ACTIVE, user.getRole());
    }

    public void requireCanListFiles(User user, String waybillId) {
        requireCanViewWaybill(user, requireWaybill(waybillId));
    }

    public void requireCanGrant(User user, WaybillRecord record, EncryptedFile file) {
        if (isAdmin(user)) {
            return;
        }
        Long orgId = orgId(user);
        boolean allowed = file.getUploaderId() != null && file.getUploaderId().equals(user.getId());
        if (!allowed && orgId != null) {
            allowed = switch (user.getRole()) {
                case CONSIGNOR -> same(orgId, record.getConsignorOrgId());
                case FORWARDER -> same(orgId, record.getForwarderOrgId()) && hasWaybillGrant(user, record.getWaybillId());
                default -> false;
            };
        }
        if (!allowed) {
            deny();
        }
    }

    public void requireAllowedGrantTarget(Role granteeRole) {
        if (granteeRole == null) {
            return;
        }
        if (granteeRole == Role.CONSIGNEE || granteeRole == Role.CUSTOMS || granteeRole == Role.FORWARDER) {
            return;
        }
        deny();
    }

    public void requireCanRevokeGrant(User user, FileKeyGrant grant) {
        WaybillRecord record = requireWaybill(grant.getWaybillId());
        EncryptedFile file = fileRepository.findById(grant.getFileId()).orElseThrow(() -> new IllegalArgumentException("文件不存在"));
        requireCanGrant(user, record, file);
    }

    public void requireCanUpdateStatus(User user, WaybillRecord record) {
        Long orgId = orgId(user);
        if (isAdmin(user) || (user.getRole() == Role.CARRIER && orgId != null && same(orgId, record.getCarrierOrgId()))) {
            return;
        }
        deny();
    }

    public void requireCrossChainOperator(User user) {
        if (user.getRole() == Role.ADMIN || user.getRole() == Role.CROSS_CHAIN_GATEWAY) {
            return;
        }
        deny();
    }

    public boolean hasWaybillGrant(User user, String waybillId) {
        Long orgId = orgId(user);
        return grantRepository.findByWaybillId(waybillId).stream()
                .filter(grant -> grant.getGrantStatus() == GrantStatus.ACTIVE)
                .anyMatch(grant -> same(user.getId(), grant.getGranteeUserId())
                        || (orgId != null && same(orgId, grant.getGranteeOrgId()))
                        || user.getRole() == grant.getGranteeRole());
    }

    private boolean same(Long left, Long right) {
        return left != null && Objects.equals(left, right);
    }

    private void deny() {
        throw new AccessDeniedException("无权执行该操作");
    }
}
