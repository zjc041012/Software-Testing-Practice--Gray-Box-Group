package com.example.waybill.dto;

import java.util.List;
import java.util.Map;

public record CrossChainCompareResult(
        boolean passed,
        Map<String, Object> sourceEvidence,
        Map<String, Object> targetEvidence,
        List<CrossChainCompareItem> items
) {
}
