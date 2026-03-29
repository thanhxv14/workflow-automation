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
import java.util.UUID;

/**
 * Worker: create-contract
 *
 * Chịu trách nhiệm tạo hợp đồng tín dụng cho khoản vay đã được phê duyệt.
 * Chạy song song với Task_Account (open-account).
 *
 * Nếu thất bại, throw BPMN Error để engine xử lý (ví dụ escalate lên manager).
 * Compensation task (undo-contract) sẽ được kích hoạt nếu có lỗi bù trừ.
 */
@Slf4j
@Component
@ExternalTaskSubscription(
        topicName = "create-contract",
        lockDuration = 30_000,            // 30 seconds lock
        variableNames = {
                LoanVariables.APPLICANT_ID,
                LoanVariables.APPLICANT_NAME,
                LoanVariables.LOAN_AMOUNT,
                LoanVariables.APPROVAL_REASON
        }
)
public class CreateContractWorker extends AbstractExternalTask {

    @Override
    public void execute(ExternalTask externalTask, ExternalTaskService externalTaskService) {
        String applicantId   = externalTask.getVariable(LoanVariables.APPLICANT_ID);
        String applicantName = externalTask.getVariable(LoanVariables.APPLICANT_NAME);
        Long   loanAmount    = externalTask.getVariable(LoanVariables.LOAN_AMOUNT);

        log.info("[create-contract] START | processInstance={} | applicant={} | amount={}",
                externalTask.getProcessInstanceId(), applicantId, loanAmount);

        try {
            // === Simulate contract generation ===
            String contractId = generateContractId(applicantId);
            simulateExternalSystemCall("Contract Service", 500);

            log.info("[create-contract] SUCCESS | contractId={} | applicant={}",
                    contractId, applicantName);

            // Return output variables to engine
            Map<String, Object> variables = new HashMap<>();
            variables.put(LoanVariables.CONTRACT_ID, contractId);

            externalTaskService.complete(externalTask, variables);

        } catch (Exception e) {
            log.error("[create-contract] FAILED | processInstance={} | error={}",
                    externalTask.getProcessInstanceId(), e.getMessage());

            // Report failure with retry logic:
            // retries=2, retryTimeout=5000ms between retries
            externalTaskService.handleFailure(
                    externalTask,
                    "Contract creation failed: " + e.getMessage(),
                    e.getClass().getName(),
                    2,        // retries remaining
                    5_000L    // retry timeout ms
            );
        }
    }

    private String generateContractId(String applicantId) {
        return "CTR-" + applicantId.toUpperCase() + "-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }

    private void simulateExternalSystemCall(String system, long ms) throws InterruptedException {
        log.debug("[create-contract] Calling {} ...", system);
        Thread.sleep(ms);
    }
}
