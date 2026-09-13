package com.example.waybill.entity;

public enum CrossChainTaskStatus {
    CREATED,
    PENDING,
    ROUTED,
    VERIFIED,
    TIMEOUT,
    CREDENTIAL_CREATED,
    MESSAGE_CREATED,
    RELAYING,
    TARGET_CHAIN_CONFIRMED,
    RECEIPT_CREATED,
    EVIDENCE_CREATED,
    PREPARED,
    SUBMITTED,
    CONFIRMED,
    FAILED
}
