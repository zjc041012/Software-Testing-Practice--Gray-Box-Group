package com.example.waybill.dto;

import com.example.waybill.entity.ChainName;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;

public record CrossChainTaskRequest(
        @NotBlank String waybillId,
        @NotNull ChainName sourceChain,
        @NotNull ChainName targetChain,
        String purpose,
        @NotNull
        LocalDateTime expiresAt
) {
    public CrossChainTaskRequest {
        waybillId = waybillId == null ? null : waybillId.trim();
    }
}
