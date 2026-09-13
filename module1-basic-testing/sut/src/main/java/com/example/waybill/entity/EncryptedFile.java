package com.example.waybill.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@Entity
@Table(name = "encrypted_files")
public class EncryptedFile {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 64)
    private String waybillId;

    @Column(nullable = false)
    private String originalFilename;

    @Column(length = 128)
    private String contentType;

    private long fileSize;

    @Column(nullable = false, length = 128)
    private String fileHash;

    @Column(nullable = false, length = 128)
    private String encryptedFileHash;

    @Column(nullable = false)
    private String storageUri;

    @Lob
    @Column(nullable = false)
    private String aesKeyEncrypted;

    @Column(nullable = false, length = 64)
    private String gcmIv;

    @Column(length = 64)
    private String gcmTag;

    private Long uploaderId;
    private LocalDateTime createdAt;

    @PrePersist
    void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
