package com.example.waybill.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@Entity
@Table(name = "cross_chain_tasks")
public class CrossChainTask {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 64)
    private String taskId;

    @Column(nullable = false, length = 64)
    private String waybillId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private ChainName sourceChain;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private ChainName targetChain;

    @Column(length = 255)
    private String purpose;

    @Column(length = 128)
    private String sourceTxId;

    @Column(length = 128)
    private String targetTxId;

    private LocalDateTime expiresAt;

    @Column(length = 64)
    private String credentialId;

    @Column(length = 64)
    private String messageId;

    @Column(length = 64)
    private String receiptId;

    @Column(length = 64)
    private String evidenceId;

    @Column(length = 128)
    private String credentialHash;

    @Column(length = 128)
    private String messageHash;

    @Column(length = 128)
    private String receiptHash;

    @Column(length = 128)
    private String evidenceHash;

    @Lob
    private String credentialSnapshot;

    @Lob
    private String messageSnapshot;

    @Lob
    private String receiptSnapshot;

    @Lob
    private String evidenceSnapshot;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private CrossChainTaskStatus status = CrossChainTaskStatus.CREATED;

    @Lob
    private String errorMessage;

    private LocalDateTime precheckedAt;
    private LocalDateTime previewedAt;
    private LocalDateTime sourcePreparedAt;
    private LocalDateTime credentialCreatedAt;
    private LocalDateTime messageCreatedAt;
    private LocalDateTime receiptCreatedAt;
    private LocalDateTime evidenceCreatedAt;
    private LocalDateTime targetConfirmedAt;
    private LocalDateTime comparedAt;
    private LocalDateTime verifiedAt;

    private Boolean lastVerifyPassed;

    @Lob
    private String lastVerifyMessage;

    private Long createdBy;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @PrePersist
    void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = createdAt;
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
