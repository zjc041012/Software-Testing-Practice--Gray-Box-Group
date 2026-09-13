package com.example.waybill.controller;

import com.example.waybill.dto.ApiResponse;
import com.example.waybill.dto.CrossChainCompareResult;
import com.example.waybill.dto.CrossChainEvidence;
import com.example.waybill.dto.CrossChainExecutionResult;
import com.example.waybill.dto.CrossChainPrecheckResult;
import com.example.waybill.dto.CrossChainTaskDetail;
import com.example.waybill.dto.CrossChainTaskRequest;
import com.example.waybill.dto.CrossChainWaybillCredential;
import com.example.waybill.dto.TimelineItem;
import com.example.waybill.dto.VerifyResult;
import com.example.waybill.entity.CrossChainTask;
import com.example.waybill.service.CrossChainService;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/cross-chain")
public class CrossChainController {
    private final CrossChainService crossChainService;

    public CrossChainController(CrossChainService crossChainService) {
        this.crossChainService = crossChainService;
    }

    @PreAuthorize("hasAnyRole('CROSS_CHAIN_GATEWAY','ADMIN')")
    @PostMapping("/tasks")
    public ApiResponse<CrossChainTask> create(@Valid @RequestBody CrossChainTaskRequest request) {
        return ApiResponse.ok(crossChainService.create(request));
    }

    @PreAuthorize("hasAnyRole('CROSS_CHAIN_GATEWAY','ADMIN')")
    @PostMapping("/precheck")
    public ApiResponse<CrossChainPrecheckResult> precheck(@Valid @RequestBody CrossChainTaskRequest request) {
        return ApiResponse.ok(crossChainService.precheck(request));
    }

    @PreAuthorize("hasAnyRole('CROSS_CHAIN_GATEWAY','ADMIN')")
    @PostMapping("/credential-preview")
    public ApiResponse<CrossChainWaybillCredential> credentialPreview(@Valid @RequestBody CrossChainTaskRequest request) {
        return ApiResponse.ok(crossChainService.credentialPreview(request));
    }

    @PreAuthorize("hasAnyRole('CROSS_CHAIN_GATEWAY','ADMIN')")
    @GetMapping("/tasks")
    public ApiResponse<List<CrossChainTask>> list() {
        return ApiResponse.ok(crossChainService.list());
    }

    @PreAuthorize("hasAnyRole('CROSS_CHAIN_GATEWAY','ADMIN')")
    @GetMapping("/tasks/{taskId}")
    public ApiResponse<CrossChainTask> detail(@PathVariable String taskId) {
        return ApiResponse.ok(crossChainService.detail(taskId));
    }

    @PreAuthorize("hasAnyRole('CROSS_CHAIN_GATEWAY','ADMIN')")
    @GetMapping("/tasks/{taskId}/detail")
    public ApiResponse<CrossChainTaskDetail> taskDetail(@PathVariable String taskId) {
        return ApiResponse.ok(crossChainService.taskDetail(taskId));
    }

    @PreAuthorize("hasAnyRole('CROSS_CHAIN_GATEWAY','ADMIN')")
    @PostMapping("/tasks/{taskId}/credential-preview")
    public ApiResponse<CrossChainWaybillCredential> credentialPreviewTask(@PathVariable String taskId) {
        return ApiResponse.ok(crossChainService.credentialPreview(taskId));
    }

    @PreAuthorize("hasAnyRole('CROSS_CHAIN_GATEWAY','ADMIN')")
    @PostMapping("/tasks/{taskId}/execute")
    public ApiResponse<CrossChainExecutionResult> execute(@PathVariable String taskId) {
        return ApiResponse.ok(crossChainService.execute(taskId));
    }

    @PreAuthorize("hasAnyRole('CROSS_CHAIN_GATEWAY','ADMIN')")
    @PostMapping("/tasks/{taskId}/retry")
    public ApiResponse<CrossChainExecutionResult> retry(@PathVariable String taskId) {
        return ApiResponse.ok(crossChainService.execute(taskId));
    }

    @PreAuthorize("hasAnyRole('CROSS_CHAIN_GATEWAY','ADMIN')")
    @PostMapping("/tasks/{taskId}/verify")
    public ApiResponse<VerifyResult> verify(@PathVariable String taskId) {
        return ApiResponse.ok(crossChainService.verify(taskId));
    }

    @PreAuthorize("hasAnyRole('CROSS_CHAIN_GATEWAY','ADMIN')")
    @GetMapping("/tasks/{taskId}/compare")
    public ApiResponse<CrossChainCompareResult> compare(@PathVariable String taskId) {
        return ApiResponse.ok(crossChainService.compare(taskId));
    }

    @PreAuthorize("hasAnyRole('CROSS_CHAIN_GATEWAY','ADMIN')")
    @GetMapping("/tasks/{taskId}/credential")
    public ApiResponse<CrossChainWaybillCredential> credential(@PathVariable String taskId) {
        return ApiResponse.ok(crossChainService.credential(taskId));
    }

    @PreAuthorize("hasAnyRole('CROSS_CHAIN_GATEWAY','ADMIN')")
    @GetMapping("/tasks/{taskId}/evidence")
    public ApiResponse<CrossChainEvidence> evidence(@PathVariable String taskId) {
        return ApiResponse.ok(crossChainService.evidence(taskId));
    }

    @PreAuthorize("hasAnyRole('CROSS_CHAIN_GATEWAY','ADMIN')")
    @GetMapping("/tasks/{taskId}/timeline")
    public ApiResponse<List<TimelineItem>> timeline(@PathVariable String taskId) {
        return ApiResponse.ok(crossChainService.timeline(taskId));
    }
}
