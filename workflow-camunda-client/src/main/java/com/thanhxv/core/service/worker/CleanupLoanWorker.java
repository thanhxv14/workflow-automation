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
 * Worker: cleanup-loan
 *
 * Chạy trong Event-based Sub-process "SubProcess_Cancel".
 * Được kích hoạt khi có Message Event "msg_cancel_loan" được gửi tới process instance.
 *
 * Điều này xảy ra khi khách hàng yêu cầu hủy hồ sơ vay đang trong quá trình xử lý.
 * Sub-process này interrupt main flow bất kể đang ở bước nào.
 *
 * Cleanup bao gồm:
 * - Xóa dữ liệu tạm thời
 * - Hủy reservation trong các hệ thống liên quan
 * - Ghi log audit
 */
@Slf4j
@Component
@ExternalTaskSubscription(
        topicName = "cleanup-loan",
        lockDuration = 30_000,
        variableNames = {
                LoanVariables.APPLICANT_ID,
                LoanVariables.APPLICANT_NAME,
                LoanVariables.LOAN_AMOUNT,
                LoanVariables.CONTRACT_ID,       // may or may not exist depending on stage
                LoanVariables.ACCOUNT_NUMBER     // may or may not exist
        }
)
public class CleanupLoanWorker extends AbstractExternalTask {

    @Override
    public void execute(ExternalTask externalTask, ExternalTaskService externalTaskService) {
        String applicantId   = externalTask.getVariable(LoanVariables.APPLICANT_ID);
        String applicantName = externalTask.getVariable(LoanVariables.APPLICANT_NAME);
        String contractId    = externalTask.getVariable(LoanVariables.CONTRACT_ID);
        String accountNumber = externalTask.getVariable(LoanVariables.ACCOUNT_NUMBER);

        log.warn("[cleanup-loan] CANCELLATION REQUESTED | processInstance={} | applicant={}",
                externalTask.getProcessInstanceId(), applicantId);

        try {
            performCleanup(applicantId, applicantName, contractId, accountNumber);

            log.info("[cleanup-loan] Cleanup complete | applicant={}", applicantName);

            Map<String, Object> variables = new HashMap<>();
            variables.put(LoanVariables.CLEANUP_DONE, true);
            variables.put("cancelledAt", java.time.LocalDateTime.now().toString());

            externalTaskService.complete(externalTask, variables);

        } catch (Exception e) {
            log.error("[cleanup-loan] Cleanup failed | applicant={} | error={}", applicantId, e.getMessage());

            externalTaskService.handleFailure(
                    externalTask,
                    "Cleanup failed: " + e.getMessage(),
                    e.getClass().getName(),
                    1,
                    5_000L
            );
        }
    }

    private void performCleanup(String applicantId, String applicantName,
                                String contractId, String accountNumber) throws InterruptedException {
        log.debug("[cleanup-loan] Cleaning up data for applicant {}", applicantId);

        if (contractId != null) {
            log.debug("[cleanup-loan] Releasing contract reservation: {}", contractId);
            Thread.sleep(200);
        }

        if (accountNumber != null) {
            log.debug("[cleanup-loan] Releasing account reservation: {}", accountNumber);
            Thread.sleep(200);
        }

        log.debug("[cleanup-loan] Writing audit log for cancellation by {}", applicantName);
        Thread.sleep(100);
    }
}
