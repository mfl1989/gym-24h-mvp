package com.gym24h.domain.repository;

import com.gym24h.domain.model.invoice.Invoice;
import com.gym24h.domain.model.user.UserId;

import java.util.List;
import java.util.Optional;

public interface InvoiceRepository {

    Optional<Invoice> findByStripeInvoiceId(String stripeInvoiceId);

    List<Invoice> findByUserIdOrderByBilledAtDesc(UserId userId);

    Invoice save(Invoice invoice);
}
