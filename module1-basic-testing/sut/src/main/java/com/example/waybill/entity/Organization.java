package com.example.waybill.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@Entity
@Table(name = "organizations")
public class Organization {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 64)
    private String orgCode;

    @Column(nullable = false, length = 128)
    private String orgName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private Role orgType;

    @Column(length = 64)
    private String countryRegion;

    @Column(length = 128)
    private String fabricMspId;

    @Column(nullable = false, length = 32)
    private String status = "ACTIVE";

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
