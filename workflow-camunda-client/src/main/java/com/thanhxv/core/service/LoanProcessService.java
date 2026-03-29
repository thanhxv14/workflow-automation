package com.thanhxv.core.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

/**
 * REST client service for interacting with Camunda Engine REST API.
 *
 * Covers:
 * - Starting a new loan process instance
 * - Sending a cancel message to an active instance
 * - Completing a User Task (manual review)
 * - Querying active tasks for the credit_officers group
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class LoanProcessService {

    private final RestTemplate restTemplate;

    @Value("${camunda.bpm.client.base-url}")
    private String engineBaseUrl;

    // =========================================================
    // Start Process
    // =========================================================

    /**
     * Start a new loan origination process instance.
     *
     * @param applicantId    unique ID of the applicant
     * @param applicantName  full name
     * @param loanAmount     requested loan amount (VND)
     * @param creditScore    applicant credit score (e.g. 300–850)
     * @param monthlyIncome  monthly income (VND)
     * @param loanPurpose    purpose description
     * @return processInstanceId
     */
    public String startLoanProcess(String applicantId, String applicantName,
                                   Long loanAmount, Integer creditScore,
                                   Long monthlyIncome, String loanPurpose) {

        Map<String, Object> variables = Map.of(
                "applicantId",    buildVariable("String", applicantId),
                "applicantName",  buildVariable("String", applicantName),
                "loanAmount",     buildVariable("Long", loanAmount),
                "creditScore",    buildVariable("Integer", creditScore),
                "monthlyIncome",  buildVariable("Long", monthlyIncome),
                "loanPurpose",    buildVariable("String", loanPurpose)
        );

        Map<String, Object> body = new HashMap<>();
        body.put("variables", variables);
        body.put("businessKey", applicantId + "-" + System.currentTimeMillis());

        String url = engineBaseUrl + "/process-definition/key/Loan_Origination_Process/start";
        log.info("[LoanProcessService] Starting process for applicant={}", applicantId);

        ResponseEntity<Map> response = restTemplate.postForEntity(url, toHttpEntity(body), Map.class);

        if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
            String instanceId = (String) response.getBody().get("id");
            log.info("[LoanProcessService] Process started | instanceId={}", instanceId);
            return instanceId;
        }
        throw new RuntimeException("Failed to start process: " + response.getStatusCode());
    }

    // =========================================================
    // Send Cancel Message
    // =========================================================

    /**
     * Send the "msg_cancel_loan" message to an active process instance.
     * This triggers the Event-based Sub-process (SubProcess_Cancel).
     *
     * @param processInstanceId target instance
     */
    public void sendCancelMessage(String processInstanceId) {
        Map<String, Object> body = Map.of(
                "messageName",     "msg_cancel_loan",
                "processInstanceId", processInstanceId
        );

        String url = engineBaseUrl + "/message";
        log.info("[LoanProcessService] Sending cancel message to instance={}", processInstanceId);

        ResponseEntity<Void> response = restTemplate.postForEntity(url, toHttpEntity(body), Void.class);

        if (!response.getStatusCode().is2xxSuccessful()) {
            throw new RuntimeException("Failed to send cancel message: " + response.getStatusCode());
        }
        log.info("[LoanProcessService] Cancel message sent successfully");
    }

    // =========================================================
    // User Task: Manual Review
    // =========================================================

    /**
     * List open tasks for the credit_officers group.
     */
    public Map[] getOpenReviewTasks() {
        String url = engineBaseUrl + "/task?candidateGroup=credit_officers&active=true";
        ResponseEntity<Map[]> response = restTemplate.getForEntity(url, Map[].class);
        return response.getBody() != null ? response.getBody() : new Map[0];
    }

    /**
     * Claim a User Task on behalf of an officer.
     */
    public void claimTask(String taskId, String officerId) {
        Map<String, Object> body = Map.of("userId", officerId);
        String url = engineBaseUrl + "/task/" + taskId + "/claim";
        restTemplate.postForEntity(url, toHttpEntity(body), Void.class);
        log.info("[LoanProcessService] Task {} claimed by {}", taskId, officerId);
    }

    /**
     * Complete a User Task (manual review decision).
     *
     * @param taskId   the task to complete
     * @param approved officer decision (true = approve, false = reject)
     * @param notes    officer notes
     */
    public void completeReviewTask(String taskId, boolean approved, String notes) {
        Map<String, Object> variables = Map.of(
                "officerApproved", buildVariable("Boolean", approved),
                "officerNotes",    buildVariable("String", notes)
        );

        Map<String, Object> body = Map.of("variables", variables);
        String url = engineBaseUrl + "/task/" + taskId + "/complete";

        restTemplate.postForEntity(url, toHttpEntity(body), Void.class);
        log.info("[LoanProcessService] Task {} completed | approved={}", taskId, approved);
    }

    // =========================================================
    // Query Instance State
    // =========================================================

    /**
     * Get current active activities for a process instance (useful for debugging).
     */
    public Map getInstanceActivity(String processInstanceId) {
        String url = engineBaseUrl + "/process-instance/" + processInstanceId + "/activity-instances";
        ResponseEntity<Map> response = restTemplate.getForEntity(url, Map.class);
        return response.getBody();
    }

    // =========================================================
    // Helpers
    // =========================================================

    private Map<String, Object> buildVariable(String type, Object value) {
        Map<String, Object> var = new HashMap<>();
        var.put("value", value);
        var.put("type", type);
        return var;
    }

    private <T> HttpEntity<T> toHttpEntity(T body) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        return new HttpEntity<>(body, headers);
    }
}
