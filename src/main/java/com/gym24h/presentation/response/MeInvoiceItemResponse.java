package com.gym24h.presentation.response;

import com.gym24h.domain.model.invoice.InvoiceStatus;

import java.time.Instant;

public record MeInvoiceItemResponse(
        InvoiceStatus status,
        String amount,
        String currency,
        String invoiceUrl,
        Instant billedAt,
        Instant paidAt,
        Instant dueAt
) {
}
