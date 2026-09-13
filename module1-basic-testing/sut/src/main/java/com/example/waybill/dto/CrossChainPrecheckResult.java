package com.example.waybill.dto;

import java.util.List;

public record CrossChainPrecheckResult(boolean passed, List<CrossChainCheckItem> items) {
}
