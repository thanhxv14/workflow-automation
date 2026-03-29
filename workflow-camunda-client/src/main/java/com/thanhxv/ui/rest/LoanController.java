package com.thanhxv.ui.rest;

import com.thanhxv.core.service.LoanProcessService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * REST Controller — exposes endpoints for the loan origination demo.
 *
 * Endpoints:
 *   POST /api/loans/start        → Start a new loan process instance
 *   POST /api/loans/{id}/cancel  → Send cancel message to running instance
 *   GET  /api/loans/tasks        → List open review tasks for credit officers
 *   POST /api/loans/tasks/{taskId}/claim    → Officer claims a task
 *   POST /api/loans/tasks/{taskId}/complete → Officer completes review
 *   GET  /api/loans/{id}/activity           → Debug: view current activity
 */
@Slf4j
@RestController
@RequestMapping("/api/loans")
@RequiredArgsConstructor
public class LoanController {

    private final LoanProcessService loanProcessService;

    // ----------------------------------------------------------
    // Start process
    // ----------------------------------------------------------

    @PostMapping("/start")
    public ResponseEntity<Map<String, Object>> startLoan(@RequestBody StartLoanRequest req) {
        log.info("[LoanController] POST /api/loans/start | applicant={}", req.applicantId());

        String instanceId = loanProcessService.startLoanProcess(
                req.applicantId(),
                req.applicantName(),
                req.loanAmount(),
                req.creditScore(),
                req.monthlyIncome(),
                req.loanPurpose()
        );

        return ResponseEntity.ok(Map.of(
                "processInstanceId", instanceId,
                "message", "Loan process started successfully"
        ));
    }

    // ----------------------------------------------------------
    // Cancel process via Message Event
    // ----------------------------------------------------------

    @PostMapping("/{processInstanceId}/cancel")
    public ResponseEntity<Map<String, String>> cancelLoan(
            @PathVariable String processInstanceId) {

        log.info("[LoanController] POST /api/loans/{}/cancel", processInstanceId);
        loanProcessService.sendCancelMessage(processInstanceId);

        return ResponseEntity.ok(Map.of(
                "processInstanceId", processInstanceId,
                "message", "Cancel message sent — SubProcess_Cancel will execute cleanup"
        ));
    }

    // ----------------------------------------------------------
    // User Task management (Manual Review)
    // ----------------------------------------------------------

    @GetMapping("/tasks")
    public ResponseEntity<Map[]> getOpenTasks() {
        Map[] tasks = loanProcessService.getOpenReviewTasks();
        log.info("[LoanController] GET /api/loans/tasks | found={}", tasks.length);
        return ResponseEntity.ok(tasks);
    }

    @PostMapping("/tasks/{taskId}/claim")
    public ResponseEntity<Map<String, String>> claimTask(
            @PathVariable String taskId,
            @RequestBody ClaimTaskRequest req) {

        loanProcessService.claimTask(taskId, req.officerId());
        return ResponseEntity.ok(Map.of(
                "taskId", taskId,
                "claimedBy", req.officerId(),
                "message", "Task claimed successfully"
        ));
    }

    @PostMapping("/tasks/{taskId}/complete")
    public ResponseEntity<Map<String, Object>> completeTask(
            @PathVariable String taskId,
            @RequestBody CompleteTaskRequest req) {

        loanProcessService.completeReviewTask(taskId, req.approved(), req.notes());
        return ResponseEntity.ok(Map.of(
                "taskId", taskId,
                "approved", req.approved(),
                "message", "Review task completed"
        ));
    }

    // ----------------------------------------------------------
    // Debug: Activity snapshot
    // ----------------------------------------------------------

    @GetMapping("/{processInstanceId}/activity")
    public ResponseEntity<Map> getActivity(@PathVariable String processInstanceId) {
        Map activity = loanProcessService.getInstanceActivity(processInstanceId);
        return ResponseEntity.ok(activity);
    }

    // ----------------------------------------------------------
    // Request records
    // ----------------------------------------------------------

    public record StartLoanRequest(
            String applicantId,
            String applicantName,
            Long loanAmount,
            Integer creditScore,
            Long monthlyIncome,
            String loanPurpose
    ) {}

    public record ClaimTaskRequest(String officerId) {}

    public record CompleteTaskRequest(boolean approved, String notes) {}
}
