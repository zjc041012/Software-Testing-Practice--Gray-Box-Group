package com.example.waybill.dto;

import com.example.waybill.entity.CrossChainTask;

import java.util.List;

public record CrossChainTaskDetail(
        CrossChainTask task,
        String currentStep,
        String failureStage,
        String failureReason,
        String suggestedAction,
        CrossChainWaybillCredential credential,
        CrossChainMessage message,
        CrossChainReceipt receipt,
        CrossChainEvidence evidence,
        List<CrossChainTimelineNode> timeline,
        List<CrossChainDetailCompareItem> comparison
) {
}
