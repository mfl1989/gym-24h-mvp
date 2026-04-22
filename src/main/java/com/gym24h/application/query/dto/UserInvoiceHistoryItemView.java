package com.gym24h.application.query.dto;

import com.gym24h.domain.model.invoice.InvoiceStatus;

import java.time.Instant;

public record UserInvoiceHistoryItemView(
        InvoiceStatus status,
        String amount,
        String currency,
        String invoiceUrl,
        Instant billedAt,
        Instant paidAt,
        Instant dueAt
) {
}