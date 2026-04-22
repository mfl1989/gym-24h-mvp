package com.gym24h.application.service;

import com.gym24h.application.command.service.SubscriptionReconciliationService;
import com.gym24h.infrastructure.external.stripe.StripeClient;
import com.stripe.model.Invoice;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ActiveProfiles("test")
@SpringBootTest
@AutoConfigureMockMvc
class SubscriptionReconciliationServiceTest {

    @Autowired
    private SubscriptionReconciliationService subscriptionReconciliationService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @MockBean
    private StripeClient stripeClient;

    @BeforeEach
    void setUp() {
        jdbcTemplate.update("delete from processed_webhook_events");
        jdbcTemplate.update("delete from audit_logs");
        jdbcTemplate.update("delete from invoices");
        jdbcTemplate.update("delete from subscriptions");
        jdbcTemplate.update("delete from users");
    }

    @Test
    void shouldInsertMissingInvoiceAndExtendSubscriptionDuringReconciliation() throws Exception {
        Fixture fixture = createArrearsSubscriptionFixture();
        Instant beforeEnd = currentPeriodEndAt(fixture.subscriptionId());

        Invoice stripeInvoice = Mockito.mock(Invoice.class);
        when(stripeInvoice.getId()).thenReturn("in_reconcile_001");
        when(stripeInvoice.getPaid()).thenReturn(true);
        when(stripeInvoice.getAmountPaid()).thenReturn(3000L);
        when(stripeInvoice.getCurrency()).thenReturn("usd");
        when(stripeInvoice.getHostedInvoiceUrl()).thenReturn("https://stripe.test/invoices/in_reconcile_001");
        when(stripeInvoice.getSubscription()).thenReturn("sub_test_" + fixture.subscriptionId());
        when(stripeInvoice.getCustomer()).thenReturn("cus_test_" + fixture.userId());
        when(stripeInvoice.getCreated()).thenReturn(1710000000L);
        when(stripeInvoice.getDueDate()).thenReturn(1710003600L);
        when(stripeInvoice.getMetadata()).thenReturn(Map.of("userId", fixture.userId().toString(), "subscriptionId", fixture.subscriptionId().toString()));

        when(stripeClient.listInvoicesSince(Mockito.any())).thenReturn(List.of(stripeInvoice));

        int correctedCount = subscriptionReconciliationService.reconcileWithStripe(Instant.now().minusSeconds(3600));

        assertThat(correctedCount).isEqualTo(1);
        assertThat(invoiceCountByStripeInvoiceId("in_reconcile_001")).isEqualTo(1);
        assertThat(invoiceStatusByStripeInvoiceId("in_reconcile_001")).isEqualTo("PAID");
        assertThat(currentSubscriptionStatus(fixture.subscriptionId())).isEqualTo("ACTIVE");
        assertThat(currentPeriodEndAt(fixture.subscriptionId())).isEqualTo(beforeEnd.plusSeconds(30L * 24L * 60L * 60L));
        assertThat(reconciliationAuditCount(fixture.userId())).isEqualTo(1);
    }

    private Fixture createArrearsSubscriptionFixture() {
        UUID userId = UUID.randomUUID();
        UUID subscriptionId = UUID.randomUUID();
        Instant now = Instant.now();

        jdbcTemplate.update(
                """
                insert into users (id, line_user_id, phone_number, display_name, membership_status, created_at, updated_at, version, is_deleted)
                values (?, ?, ?, ?, ?, ?, ?, ?, ?)
                """,
                userId,
                "line-" + userId,
                null,
                "Reconciliation User",
                "ACTIVE",
                now,
                now,
                0,
                false
        );

        jdbcTemplate.update(
                """
                insert into subscriptions (
                    id, user_id, plan_code, billing_cycle, status,
                    stripe_customer_id, stripe_subscription_id,
                    started_at, current_period_start_at, current_period_end_at,
                    canceled_at, cancellation_requested_at,
                    created_at, updated_at, version, is_deleted
                ) values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """,
                subscriptionId,
                userId,
                "STANDARD",
                "THIRTY_DAYS",
                "ARREARS",
                "cus_test_" + userId,
                "sub_test_" + subscriptionId,
                now.minusSeconds(3600),
                now.minusSeconds(3600),
                now.plusSeconds(3600),
                null,
                null,
                now.minusSeconds(3600),
                now.minusSeconds(3600),
                0,
                false
        );

        return new Fixture(userId, subscriptionId);
    }

    private String currentSubscriptionStatus(UUID subscriptionId) {
        return jdbcTemplate.queryForObject(
                "select status from subscriptions where id = ?",
                String.class,
                subscriptionId
        );
    }

    private Instant currentPeriodEndAt(UUID subscriptionId) {
        return jdbcTemplate.queryForObject(
                "select current_period_end_at from subscriptions where id = ?",
                Instant.class,
                subscriptionId
        );
    }

    private int invoiceCountByStripeInvoiceId(String stripeInvoiceId) {
        Integer count = jdbcTemplate.queryForObject(
                "select count(*) from invoices where stripe_invoice_id = ?",
                Integer.class,
                stripeInvoiceId
        );
        return count == null ? 0 : count;
    }

    private String invoiceStatusByStripeInvoiceId(String stripeInvoiceId) {
        return jdbcTemplate.queryForObject(
                "select status from invoices where stripe_invoice_id = ?",
                String.class,
                stripeInvoiceId
        );
    }

    private int reconciliationAuditCount(UUID userId) {
        Integer count = jdbcTemplate.queryForObject(
                "select count(*) from audit_logs where user_id = ? and action = ? and result = ?",
                Integer.class,
                userId,
                "SYSTEM_RECONCILIATION",
                "SUCCESS"
        );
        return count == null ? 0 : count;
    }

    private record Fixture(UUID userId, UUID subscriptionId) {
    }
}