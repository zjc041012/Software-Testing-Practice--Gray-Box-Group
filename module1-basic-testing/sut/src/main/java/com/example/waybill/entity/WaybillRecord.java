package com.example.waybill.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@Entity
@Table(name = "waybill_records")
public class WaybillRecord {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 64)
    private String waybillId;

    @Column(nullable = false, length = 128)
    private String smgsNo;

    private Long consignorOrgId;
    private Long carrierOrgId;
    private Long consigneeOrgId;
    private Long customsOrgId;
    private Long forwarderOrgId;

    @Column(length = 128)
    private String departureStation;

    @Column(length = 128)
    private String destinationStation;

    @Column(length = 128)
    private String goodsName;

    @Column(length = 128)
    private String containerNo;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private ChainName sourceChain;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private ChainName currentChain;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private WaybillStatus status = WaybillStatus.REGISTERED;

    @Column(length = 128)
    private String chainTxId;

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
