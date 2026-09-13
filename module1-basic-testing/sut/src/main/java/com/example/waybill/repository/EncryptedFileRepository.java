package com.example.waybill.repository;

import com.example.waybill.entity.EncryptedFile;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface EncryptedFileRepository extends JpaRepository<EncryptedFile, Long> {
    List<EncryptedFile> findByWaybillId(String waybillId);
    List<EncryptedFile> findByWaybillIdIn(Collection<String> waybillIds);
    Optional<EncryptedFile> findFirstByWaybillIdOrderByCreatedAtDesc(String waybillId);
}
