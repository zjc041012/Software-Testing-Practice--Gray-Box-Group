package com.example.waybill.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@Entity
@Table(name = "file_key_grants")
public class FileKeyGrant {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 64)
    private String waybillId;

    @Column(nullable = false)
    private Long fileId;

    private Long grantorUserId;
    private Long granteeUserId;
    private Long granteeOrgId;

    @Enumerated(EnumType.STRING)
    @Column(length = 32)
    private Role granteeRole;

    @Lob
    private String encryptedKeyForGrantee;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private GrantStatus grantStatus = GrantStatus.ACTIVE;

    private LocalDateTime grantedAt;
    private LocalDateTime revokedAt;

    @PrePersist
    void onCreate() {
        grantedAt = LocalDateTime.now();
    }
}
