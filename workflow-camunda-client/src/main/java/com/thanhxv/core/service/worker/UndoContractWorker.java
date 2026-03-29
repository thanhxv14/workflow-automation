package com.thanhxv.core.service.worker;

import com.thanhxv.core.constant.LoanVariables;
import com.thanhxv.ui.worker.AbstractExternalTask;
import lombok.extern.slf4j.Slf4j;
import org.camunda.bpm.client.spring.annotation.ExternalTaskSubscription;
import org.camunda.bpm.client.task.ExternalTask;
import org.camunda.bpm.client.task.ExternalTaskService;
import org.springframework.stereotype.Component;

/**
 * Worker: undo-contract  (Compensation Task)
 *
 * Đây là compensation handler cho Task_Contract.
 * Được kích hoạt bởi Compensation Boundary Event (Comp_Boundary)
 * khi engine cần rollback hợp đồng đã tạo.
 *
 * QUAN TRỌNG: isForCompensation="true" trong BPMN → task này
 * chỉ chạy khi được trigger bởi compensation event, không chạy trong main flow.
 */
@Slf4j
@Component
@ExternalTaskSubscription(
        topicName = "undo-contract",
        lockDuration = 30_000,
        variableNames = {
                LoanVariables.CONTRACT_ID,
                LoanVariables.APPLICANT_ID
        }
)
public class UndoContractWorker extends AbstractExternalTask {

    @Override
    public void execute(ExternalTask externalTask, ExternalTaskService externalTaskService) {
        String contractId  = externalTask.getVariable(LoanVariables.CONTRACT_ID);
        String applicantId = externalTask.getVariable(LoanVariables.APPLICANT_ID);

        log.warn("[undo-contract] COMPENSATION TRIGGERED | processInstance={} | contractId={} | applicant={}",
                externalTask.getProcessInstanceId(), contractId, applicantId);

        try {
            // === Simulate contract cancellation in core banking ===
            simulateContractCancellation(contractId);

            log.info("[undo-contract] Contract {} successfully cancelled (compensation complete)", contractId);

            externalTaskService.complete(externalTask);

        } catch (Exception e) {
            log.error("[undo-contract] Compensation failed | contractId={} | error={}", contractId, e.getMessage());

            externalTaskService.handleFailure(
                    externalTask,
                    "Contract undo failed: " + e.getMessage(),
                    e.getClass().getName(),
                    1,
                    10_000L
            );
        }
    }

    private void simulateContractCancellation(String contractId) throws InterruptedException {
        log.debug("[undo-contract] Sending cancellation request for contract {} to core banking ...", contractId);
        Thread.sleep(300);
        log.debug("[undo-contract] Core banking acknowledged cancellation of {}", contractId);
    }
}
