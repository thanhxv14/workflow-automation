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
 * Worker: open-account
 *
 * Mở tài khoản Core Banking cho khách hàng vay.
 * Chạy song song với create-contract trong Parallel Gateway.
 *
 * Cả hai tasks (create-contract và open-account) phải hoàn thành
 * trước khi Parallel Join gateway cho phép tiếp tục sang fund-disbursement.
 */
@Slf4j
@Component
@ExternalTaskSubscription(
        topicName = "open-account",
        lockDuration = 30_000,
        variableNames = {
                LoanVariables.APPLICANT_ID,
                LoanVariables.APPLICANT_NAME,
                LoanVariables.LOAN_AMOUNT
        }
)
public class OpenAccountWorker extends AbstractExternalTask {

    @Override
    public void execute(ExternalTask externalTask, ExternalTaskService externalTaskService) {
        String applicantId   = externalTask.getVariable(LoanVariables.APPLICANT_ID);
        String applicantName = externalTask.getVariable(LoanVariables.APPLICANT_NAME);
        Long   loanAmount    = externalTask.getVariable(LoanVariables.LOAN_AMOUNT);

        log.info("[open-account] START | processInstance={} | applicant={} | loanAmount={}",
                externalTask.getProcessInstanceId(), applicantId, loanAmount);

        try {
            // === Simulate core banking account creation ===
            String accountNumber = generateAccountNumber(applicantId);
            simulateCoreSystemCall("Core Banking API", 700);

            if (applicantName.contains("ERR_CORE_BANK")) {
                throw new RuntimeException("Simulated core banking failure for testing");
            }

            log.info("[open-account] SUCCESS | accountNumber={} | applicant={}",
                    accountNumber, applicantName);

            Map<String, Object> variables = new HashMap<>();
            variables.put(LoanVariables.ACCOUNT_NUMBER, accountNumber);

            externalTaskService.complete(externalTask, variables);

        } catch (Exception e) {
            log.error("[open-account] FAILED | processInstance={} | error={}",
                    externalTask.getProcessInstanceId(), e.getMessage());

//            externalTaskService.handleFailure(
//                    externalTask,
//                    "Account opening failed: " + e.getMessage(),
//                    e.getClass().getName(),
//                    2,
//                    5_000L
//            );

            externalTaskService.handleBpmnError(externalTask, "ERR_CORE_BANK");
        }
    }

    private String generateAccountNumber(String applicantId) {
        // Simulate core banking account number format: VN + 12 digits
        long suffix = (applicantId.hashCode() & 0xFFFFFFFFL) % 1_000_000_000_000L;
        return String.format("VN%012d", suffix);
    }

    private void simulateCoreSystemCall(String system, long ms) throws InterruptedException {
        log.debug("[open-account] Calling {} ...", system);
        Thread.sleep(ms);
    }
}
