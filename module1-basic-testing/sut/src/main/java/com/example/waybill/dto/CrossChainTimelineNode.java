package com.example.waybill.dto;

public record CrossChainTimelineNode(
        Integer stepNo,
        String stepKey,
        String key,
        String titleZh,
        String titleEn,
        String label,
        String status,
        String time,
        String description,
        String summary,
        String relatedObjectType,
        String relatedObjectId,
        String errorMessage
) {
}
