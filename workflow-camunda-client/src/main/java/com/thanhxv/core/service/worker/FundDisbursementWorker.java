package com.thanhxv.core.service.worker;

import com.thanhxv.core.constant.LoanVariables;
import com.thanhxv.ui.worker.AbstractExternalTask;
import lombok.extern.slf4j.Slf4j;
import org.camunda.bpm.client.spring.annotation.ExternalTaskSubscription;
import org.camunda.bpm.client.task.ExternalTask;
import org.camunda.bpm.client.task.ExternalTaskService;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Worker: fund-disbursement
 *
 * Thực hiện giải ngân khoản vay sau khi:
 * - Hợp đồng tín dụng đã được tạo (create-contract)
 * - Tài khoản Core Banking đã được mở (open-account)
 *
 * Đây là bước cuối cùng trước khi kết thúc quy trình thành công.
 * Nếu giải ngân thất bại, engine sẽ retry theo cấu hình.
 */
@Slf4j
@Component
@ExternalTaskSubscription(
        topicName = "fund-disbursement",
        lockDuration = 60_000,            // longer lock for financial transactions
        variableNames = {
                LoanVariables.APPLICANT_ID,
                LoanVariables.APPLICANT_NAME,
                LoanVariables.LOAN_AMOUNT,
                LoanVariables.CONTRACT_ID,
                LoanVariables.ACCOUNT_NUMBER
        }
)
public class FundDisbursementWorker extends AbstractExternalTask {

    @Override
    public void execute(ExternalTask externalTask, ExternalTaskService externalTaskService) {
        String applicantId    = externalTask.getVariable(LoanVariables.APPLICANT_ID);
        String applicantName  = externalTask.getVariable(LoanVariables.APPLICANT_NAME);
        Long   loanAmount     = externalTask.getVariable(LoanVariables.LOAN_AMOUNT);
        String contractId     = externalTask.getVariable(LoanVariables.CONTRACT_ID);
        String accountNumber  = externalTask.getVariable(LoanVariables.ACCOUNT_NUMBER);

        log.info("[fund-disbursement] START | processInstance={} | applicant={} | contractId={} | account={} | amount={}",
                externalTask.getProcessInstanceId(), applicantId, contractId, accountNumber, loanAmount);

        // Validate all prerequisites are present
        if (contractId == null || accountNumber == null) {
            log.error("[fund-disbursement] MISSING prerequisites: contractId={}, accountNumber={}",
                    contractId, accountNumber);
            externalTaskService.handleFailure(
                    externalTask,
                    "Disbursement prerequisites missing",
                    "Missing contractId or accountNumber",
                    0,  // no retries — this is a data error
                    0L
            );
            return;
        }

        try {
            // === Simulate fund transfer via payment gateway ===
            String disbursementRef = executeDisbursement(contractId, accountNumber, loanAmount);
            String disbursedAt = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);

            log.info("[fund-disbursement] SUCCESS | disbursementRef={} | applicant={} | amount={} | at={}",
                    disbursementRef, applicantName, loanAmount, disbursedAt);

            Map<String, Object> variables = new HashMap<>();
            variables.put(LoanVariables.DISBURSEMENT_REF, disbursementRef);
            variables.put("disbursedAt", disbursedAt);
            variables.put("disbursedAmount", loanAmount);

            externalTaskService.complete(externalTask, variables);

        } catch (Exception e) {
            log.error("[fund-disbursement] FAILED | processInstance={} | error={}",
                    externalTask.getProcessInstanceId(), e.getMessage(), e);

            // Financial operations: retry 3 times with 30s interval
            externalTaskService.handleFailure(
                    externalTask,
                    "Disbursement failed: " + e.getMessage(),
                    e.getClass().getName(),
                    3,
                    30_000L
            );
        }
    }

    private String executeDisbursement(String contractId, String accountNumber, Long amount)
            throws InterruptedException {
        log.debug("[fund-disbursement] Transferring {} VND → account {} for contract {}",
                amount, accountNumber, contractId);
        Thread.sleep(1_000); // Simulate payment gateway latency
        return "DSB-" + UUID.randomUUID().toString().substring(0, 12).toUpperCase();
    }
}
