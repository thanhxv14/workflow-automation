package com.thanhxv.core.service.worker;

import com.thanhxv.core.constant.LoanVariables;
import com.thanhxv.ui.worker.AbstractExternalTask;
import lombok.extern.slf4j.Slf4j;
import org.camunda.bpm.client.spring.annotation.ExternalTaskSubscription;
import org.camunda.bpm.client.task.ExternalTask;
import org.camunda.bpm.client.task.ExternalTaskService;
import org.springframework.stereotype.Component;

/**
 * Worker: undo-account  (Compensation Task)
 *
 * Đây là compensation handler cho Task_Account.
 * Được kích hoạt bởi Compensation Boundary Event (Comp_Boundary)
 * khi engine cần rollback account đã tạo.
 *
 * QUAN TRỌNG: isForCompensation="true" trong BPMN → task này
 * chỉ chạy khi được trigger bởi compensation event, không chạy trong main flow.
 */
@Slf4j
@Component
@ExternalTaskSubscription(
        topicName = "undo-account",
        lockDuration = 30_000,
        variableNames = {
                LoanVariables.ACCOUNT_NUMBER,
                LoanVariables.APPLICANT_ID
        }
)
public class UndoAccountWorker extends AbstractExternalTask {

    @Override
    public void execute(ExternalTask externalTask, ExternalTaskService externalTaskService) {
        String accountNumber  = externalTask.getVariable(LoanVariables.ACCOUNT_NUMBER);
        String applicantId = externalTask.getVariable(LoanVariables.APPLICANT_ID);

        log.warn("[undo-account] COMPENSATION TRIGGERED | processInstance={} | accountNumber={} | applicant={}",
                externalTask.getProcessInstanceId(), accountNumber, applicantId);

        try {
            // === Simulate account cancellation in core banking ===
            simulateAccountCancellation(accountNumber);

            log.info("[undo-account] Account {} successfully cancelled (compensation complete)", accountNumber);

            externalTaskService.complete(externalTask);

        } catch (Exception e) {
            log.error("[undo-account] Compensation failed | accountId={} | error={}", accountNumber, e.getMessage());

            externalTaskService.handleFailure(
                    externalTask,
                    "Account undo failed: " + e.getMessage(),
                    e.getClass().getName(),
                    1,
                    10_000L
            );
        }
    }

    private void simulateAccountCancellation(String accountNumber) throws InterruptedException {
        log.debug("[undo-account] Sending cancellation request for account {} to core banking ...", accountNumber);
        Thread.sleep(500);
        log.debug("[undo-account] Core banking acknowledged cancellation of {}", accountNumber);
    }
}
