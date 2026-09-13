package com.example.waybill.service;

import com.example.waybill.dto.CrossChainCheckItem;
import com.example.waybill.dto.CrossChainPrecheckResult;
import com.example.waybill.dto.CrossChainTaskRequest;
import com.example.waybill.entity.ChainName;
import com.example.waybill.entity.EncryptedFile;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

class D003BlankHashProbeTest extends CrossChainProbeSupport {
    @Test
    void blankFileHashesMustFailPrecheck() {
        allowOperator();
        stubWaybillAndSource();
        EncryptedFile file = new EncryptedFile();
        file.setFileHash("");
        file.setEncryptedFileHash("");
        when(fileRepository.findFirstByWaybillIdOrderByCreatedAtDesc("WB-001"))
                .thenReturn(Optional.of(file));

        CrossChainTaskRequest request = new CrossChainTaskRequest(
                "WB-001", ChainName.CHINA, ChainName.EUROPE,
                "customs clearance", LocalDateTime.now().plusDays(1));
        CrossChainPrecheckResult result = service.precheck(request);
        CrossChainCheckItem hashCheck = result.items().stream()
                .filter(item -> item.name().equals("文件哈希已生成"))
                .findFirst().orElseThrow();

        System.out.println("D-003 actualHashCheckPassed=" + hashCheck.passed()
                + " overallPassed=" + result.passed());
        assertThat(hashCheck.passed())
                .as("D-003 blank file hashes must fail the hash check")
                .isFalse();
        assertThat(result.passed()).isFalse();
    }
}
