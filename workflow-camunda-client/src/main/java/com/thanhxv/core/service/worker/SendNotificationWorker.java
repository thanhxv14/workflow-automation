package com.thanhxv.core.service.worker;

import com.thanhxv.core.constant.LoanVariables;
import com.thanhxv.ui.worker.AbstractExternalTask;
import lombok.extern.slf4j.Slf4j;
import org.camunda.bpm.client.spring.annotation.ExternalTaskSubscription;
import org.camunda.bpm.client.task.ExternalTask;
import org.camunda.bpm.client.task.ExternalTaskService;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * Worker: send-notification
 *
 * Gửi thông báo từ chối tới khách hàng.
 * Task này được kích hoạt từ 2 luồng khác nhau:
 *
 * 1. Path_Reject: DMN quyết định REJECT trực tiếp
 * 2. Flow_Timer:  Timer Boundary Event (PT24H) trên UserTask_Review kích hoạt
 *                 → SLA quá hạn, officer chưa phê duyệt trong 24h
 *
 * Worker đọc biến "rejectionReason" để biết nguyên nhân từ chối.
 */
@Slf4j
@Component
@ExternalTaskSubscription(
        topicName = "send-notification",
        lockDuration = 30_000,
        variableNames = {
                LoanVariables.APPLICANT_ID,
                LoanVariables.APPLICANT_NAME,
                LoanVariables.LOAN_AMOUNT,
                LoanVariables.DECISION,
                LoanVariables.APPROVAL_REASON,
                LoanVariables.REJECTION_REASON
        }
)
public class SendNotificationWorker extends AbstractExternalTask {

    @Override
    public void execute(ExternalTask externalTask, ExternalTaskService externalTaskService) {
        String applicantId     = externalTask.getVariable(LoanVariables.APPLICANT_ID);
        String applicantName   = externalTask.getVariable(LoanVariables.APPLICANT_NAME);
        Long   loanAmount      = externalTask.getVariable(LoanVariables.LOAN_AMOUNT);
        String decision        = externalTask.getVariable(LoanVariables.DECISION);
        String approvalReason  = externalTask.getVariable(LoanVariables.APPROVAL_REASON);
        String rejectionReason = externalTask.getVariable(LoanVariables.REJECTION_REASON);

        // Determine rejection source
        String reason = (rejectionReason != null) ? rejectionReason
                : (approvalReason != null)        ? approvalReason
                : "SLA exceeded — application expired after 24 hours without officer review";

        log.info("[send-notification] Sending rejection notice | processInstance={} | applicant={} | reason={}",
                externalTask.getProcessInstanceId(), applicantId, reason);

        try {
            // === Simulate email/SMS notification ===
            sendEmail(applicantName, loanAmount, reason);
            sendSms(applicantId, reason);

            log.info("[send-notification] Notification sent | applicant={} | decision={}", applicantId, decision);

            Map<String, Object> variables = new HashMap<>();
            variables.put(LoanVariables.NOTIFICATION_SENT, true);
            variables.put("notificationReason", reason);

            externalTaskService.complete(externalTask, variables);

        } catch (Exception e) {
            log.error("[send-notification] Failed to send notification | applicant={} | error={}",
                    applicantId, e.getMessage());

            externalTaskService.handleFailure(
                    externalTask,
                    "Notification failed: " + e.getMessage(),
                    e.getClass().getName(),
                    2,
                    10_000L
            );
        }
    }

    private void sendEmail(String name, Long amount, String reason) throws InterruptedException {
        log.debug("[send-notification] Sending email to {} re: loan {} — reason: {}", name, amount, reason);
        Thread.sleep(200);
        log.debug("[send-notification] Email delivered");
    }

    private void sendSms(String applicantId, String reason) throws InterruptedException {
        log.debug("[send-notification] Sending SMS to applicant {} — {}", applicantId, reason);
        Thread.sleep(100);
        log.debug("[send-notification] SMS delivered");
    }
}
