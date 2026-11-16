package net.mooctest.refund.domain;

public record RefundRequest(
        PurchaseSnapshot purchase,
        RefundReason reason,
        boolean fullRefundRequested,
        int requestedPercentage,
        boolean evidenceProvided,
        boolean technicalIssueConfirmed,
        boolean contentComplaintUpheld,
        boolean medicalDocumentVerified,
        boolean duplicatePaymentConfirmed,
        boolean bankDetailsPresent,
        boolean fraudSuspected,
        boolean adminOverride
) {
}
