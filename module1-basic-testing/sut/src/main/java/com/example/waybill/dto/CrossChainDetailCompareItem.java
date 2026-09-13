package com.example.waybill.dto;

public record CrossChainDetailCompareItem(
        String category,
        String field,
        Object sourceValue,
        Object targetValue,
        String result
) {
}
