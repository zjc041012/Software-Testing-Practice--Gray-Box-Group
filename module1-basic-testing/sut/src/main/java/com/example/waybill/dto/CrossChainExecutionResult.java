package com.example.waybill.dto;

import com.example.waybill.entity.CrossChainTask;

import java.util.List;

public record CrossChainExecutionResult(
        CrossChainTask task,
        CrossChainWaybillCredential credential,
        CrossChainMessage message,
        CrossChainReceipt receipt,
        CrossChainEvidence evidence,
        List<TimelineItem> timeline
) {
}
