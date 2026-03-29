package com.thanhxv.core.constant;

/**
 * Centralized constants for Camunda process variable names.
 * Avoids magic strings scattered across workers.
 */
public final class LoanVariables {

    // Input variables (set when starting the process)
    public static final String APPLICANT_ID       = "applicantId";
    public static final String APPLICANT_NAME     = "applicantName";
    public static final String LOAN_AMOUNT        = "loanAmount";
    public static final String CREDIT_SCORE       = "creditScore";
    public static final String MONTHLY_INCOME     = "monthlyIncome";
    public static final String LOAN_PURPOSE       = "loanPurpose";

    // DMN output
    public static final String DECISION           = "decision";
    public static final String APPROVAL_REASON    = "approvalReason";

    // Output variables set by workers
    public static final String CONTRACT_ID        = "contractId";
    public static final String ACCOUNT_NUMBER     = "accountNumber";
    public static final String DISBURSEMENT_REF   = "disbursementRef";
    public static final String NOTIFICATION_SENT  = "notificationSent";
    public static final String CLEANUP_DONE       = "cleanupDone";

    // Rejection reason (set by timer boundary or reject path)
    public static final String REJECTION_REASON   = "rejectionReason";

    private LoanVariables() {}
}
