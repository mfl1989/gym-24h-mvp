package com.gym24h.infrastructure.persistence.repository;

import com.gym24h.domain.model.invoice.Invoice;
import com.gym24h.domain.model.invoice.InvoiceId;
import com.gym24h.domain.model.user.UserId;
import com.gym24h.domain.repository.InvoiceRepository;
import com.gym24h.domain.model.subscription.SubscriptionId;
import com.gym24h.infrastructure.persistence.entity.InvoiceEntity;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Repository
public class InvoiceRepositoryImpl implements InvoiceRepository {

    private final JpaInvoiceRepository jpaInvoiceRepository;

    public InvoiceRepositoryImpl(JpaInvoiceRepository jpaInvoiceRepository) {
        this.jpaInvoiceRepository = jpaInvoiceRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Invoice> findByStripeInvoiceId(String stripeInvoiceId) {
        return jpaInvoiceRepository.findFirstByStripeInvoiceIdAndDeletedFalse(stripeInvoiceId)
                .map(this::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Invoice> findByUserIdOrderByBilledAtDesc(UserId userId) {
        return jpaInvoiceRepository.findByUserIdAndDeletedFalseOrderByBilledAtDesc(userId.value()).stream()
                .map(this::toDomain)
                .toList();
    }

    @Override
    @Transactional
    public Invoice save(Invoice invoice) {
        InvoiceEntity savedEntity = jpaInvoiceRepository.save(toEntity(invoice));
        return toDomain(savedEntity);
    }

    private Invoice toDomain(InvoiceEntity entity) {
        return Invoice.builder()
                .id(new InvoiceId(entity.getId()))
                .subscriptionId(new SubscriptionId(entity.getSubscriptionId()))
                .userId(new UserId(entity.getUserId()))
                .stripeInvoiceId(entity.getStripeInvoiceId())
                .stripeEventId(entity.getStripeEventId())
                .amountPaid(entity.getAmount())
                .currency(entity.getCurrency())
                .status(entity.getStatus())
                .invoiceUrl(entity.getInvoiceUrl())
                .billedAt(entity.getBilledAt())
                .paidAt(entity.getPaidAt())
                .dueAt(entity.getDueAt())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .version(entity.getVersion())
                .deleted(entity.isDeleted())
                .build();
    }

    private InvoiceEntity toEntity(Invoice invoice) {
        InvoiceEntity entity = new InvoiceEntity();
        entity.setId(invoice.getId().value());
        entity.setSubscriptionId(invoice.getSubscriptionId().value());
        entity.setUserId(invoice.getUserId().value());
        entity.setStripeInvoiceId(invoice.getStripeInvoiceId());
        entity.setStripeEventId(invoice.getStripeEventId());
        entity.setAmount(invoice.getAmountPaid());
        entity.setCurrency(invoice.getCurrency());
        entity.setStatus(invoice.getStatus());
        entity.setInvoiceUrl(invoice.getInvoiceUrl());
        entity.setBilledAt(invoice.getBilledAt());
        entity.setPaidAt(invoice.getPaidAt());
        entity.setDueAt(invoice.getDueAt());
        entity.setCreatedAt(invoice.getCreatedAt());
        entity.setUpdatedAt(invoice.getUpdatedAt());
        entity.setVersion(invoice.getVersion());
        entity.setDeleted(invoice.isDeleted());
        return entity;
    }
}
