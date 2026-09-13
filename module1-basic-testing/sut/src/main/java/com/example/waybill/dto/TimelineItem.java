package com.example.waybill.dto;

import java.util.List;

public record TimelineItem(
        String source,
        String time,
        String action,
        String summary,
        String txId,
        boolean deleted,
        List<TimelineDetail> details
) {
    public record TimelineDetail(String label, String value, boolean hash) {
    }
}
