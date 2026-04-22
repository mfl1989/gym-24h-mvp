package com.gym24h.infrastructure.persistence.repository;

import com.gym24h.infrastructure.persistence.entity.InvoiceEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface JpaInvoiceRepository extends JpaRepository<InvoiceEntity, UUID> {

    Optional<InvoiceEntity> findFirstByStripeInvoiceIdAndDeletedFalse(String stripeInvoiceId);

    List<InvoiceEntity> findByUserIdAndDeletedFalseOrderByBilledAtDesc(UUID userId);
}
