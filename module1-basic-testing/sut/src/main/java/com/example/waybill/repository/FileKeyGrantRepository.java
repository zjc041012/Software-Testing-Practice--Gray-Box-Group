package com.example.waybill.repository;

import com.example.waybill.entity.FileKeyGrant;
import com.example.waybill.entity.GrantStatus;
import com.example.waybill.entity.Role;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface FileKeyGrantRepository extends JpaRepository<FileKeyGrant, Long> {
    List<FileKeyGrant> findByWaybillId(String waybillId);
    List<FileKeyGrant> findByFileIdAndGrantStatus(Long fileId, GrantStatus status);
    boolean existsByFileIdAndGrantStatusAndGranteeUserId(Long fileId, GrantStatus status, Long granteeUserId);
    boolean existsByFileIdAndGrantStatusAndGranteeOrgId(Long fileId, GrantStatus status, Long granteeOrgId);
    boolean existsByFileIdAndGrantStatusAndGranteeRole(Long fileId, GrantStatus status, Role role);
}
