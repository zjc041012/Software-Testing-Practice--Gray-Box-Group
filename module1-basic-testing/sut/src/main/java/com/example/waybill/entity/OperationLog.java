package com.example.waybill.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@Entity
@Table(name = "operation_logs")
public class OperationLog {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long userId;
    private Long orgId;

    @Column(length = 64)
    private String operationType;

    @Column(length = 64)
    private String targetType;

    @Column(length = 128)
    private String targetId;

    @Enumerated(EnumType.STRING)
    @Column(length = 32)
    private ChainName chain;

    @Column(length = 128)
    private String txId;

    @Column(length = 32)
    private String result;

    @Lob
    private String message;

    @Column(length = 64)
    private String ipAddress;

    private LocalDateTime createdAt;

    @PrePersist
    void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
