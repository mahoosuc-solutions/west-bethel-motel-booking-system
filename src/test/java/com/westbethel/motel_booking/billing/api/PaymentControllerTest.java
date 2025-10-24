package com.westbethel.motel_booking.billing.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.westbethel.motel_booking.billing.domain.Invoice;
import com.westbethel.motel_booking.billing.domain.InvoiceLineItem;
import com.westbethel.motel_booking.billing.domain.InvoiceStatus;
import com.westbethel.motel_booking.billing.domain.Payment;
import com.westbethel.motel_booking.billing.repository.PaymentRepository;
import com.westbethel.motel_booking.common.model.Money;
import com.westbethel.motel_booking.common.model.PaymentStatus;
import com.westbethel.motel_booking.guest.domain.Guest;
import com.westbethel.motel_booking.inventory.domain.RoomType;
import com.westbethel.motel_booking.pricing.domain.RatePlan;
import com.westbethel.motel_booking.property.domain.Property;
import com.westbethel.motel_booking.reservation.domain.Booking;
import com.westbethel.motel_booking.testutil.BaseIntegrationTest;
import com.westbethel.motel_booking.testutil.TestDataBuilder;
import com.westbethel.motel_booking.testutil.TestDataBuilder.DateRange;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

/**
 * Integration tests for PaymentController.
 * Tests payment authorization, capture, refund, and void flows.
 *
 * Test Coverage:
 * - Authorize payment with valid data
 * - Capture authorized payment
 * - Refund captured payment
 * - Void authorized payment
 * - Validation errors (missing fields, invalid amounts, etc.)
 * - Payment status transitions
 * - Invoice balance updates
 * - Complete payment workflows
 */
@DisplayName("Payment Controller Integration Tests")
class PaymentControllerTest extends BaseIntegrationTest {

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private PaymentRepository paymentRepository;

    private Property property;
    private RoomType roomType;
    private Guest guest;
    private Booking booking;
    private Invoice invoice;
    private RatePlan ratePlan;

    @BeforeEach
    void setup() {
        // Arrange: Create complete test data hierarchy
        property = propertyRepository.save(TestDataBuilder.createProperty());

        roomType = roomTypeRepository.save(
                TestDataBuilder.createRoomType(property.getId(), "KING", "King Room",
                                              2, new BigDecimal("150.00")));

        roomRepository.save(
                TestDataBuilder.createRoom(property.getId(), roomType.getId(), "101"));

        guest = guestRepository.save(TestDataBuilder.createGuest());

        ratePlan = ratePlanRepository.save(
                TestDataBuilder.createRatePlan(property.getId(), roomType.getId(),
                                              new BigDecimal("150.00")));

        DateRange dateRange = DateRange.futureRange(5, 2);
        booking = bookingRepository.save(
                TestDataBuilder.createBooking(property.getId(), guest.getId(), ratePlan.getId(),
                                             dateRange.start, dateRange.end,
                                             new BigDecimal("300.00")));

        invoice = createInvoice(booking, new BigDecimal("300.00"));
    }

    @Nested
    @DisplayName("Payment Authorization")
    class PaymentAuthorization {

        @Test
        @DisplayName("Should authorize payment with valid card token")
        void authorizePaymentSuccessfully() throws Exception {
            // Arrange
            String authorizePayload = buildAuthorizePayload("300.00", "tok_visa_valid");

            // Act & Assert
            MvcResult result = mockMvc.perform(post("/api/v1/invoices/" + invoice.getId() + "/payments/authorize")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(authorizePayload))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("AUTHORIZED"))
                    .andExpect(jsonPath("$.paymentId").exists())
                    .andReturn();

            // Verify payment was created
            JsonNode json = objectMapper.readTree(result.getResponse().getContentAsString());
            UUID paymentId = UUID.fromString(json.get("paymentId").asText());

            Payment payment = paymentRepository.findById(paymentId).orElseThrow();
            assertThat(payment.getStatus()).isEqualTo(PaymentStatus.AUTHORIZED);
            assertThat(payment.getAmount().getAmount()).isEqualByComparingTo(new BigDecimal("300.00"));
        }

        @Test
        @DisplayName("Should authorize partial payment amount")
        void authorizePartialPayment() throws Exception {
            // Arrange - Authorize only half the invoice amount
            String authorizePayload = buildAuthorizePayload("150.00", "tok_partial");

            // Act & Assert
            mockMvc.perform(post("/api/v1/invoices/" + invoice.getId() + "/payments/authorize")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(authorizePayload))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("AUTHORIZED"));

            // Verify invoice balance is updated
            Invoice refreshed = invoiceRepository.findById(invoice.getId()).orElseThrow();
            assertThat(refreshed.getBalanceDue().getAmount())
                    .isEqualByComparingTo(new BigDecimal("150.00").setScale(2));
        }

        @Test
        @DisplayName("Should handle multiple authorizations for same invoice")
        void authorizeMultiplePayments() throws Exception {
            // Arrange
            String firstAuth = buildAuthorizePayload("100.00", "tok_first");
            String secondAuth = buildAuthorizePayload("100.00", "tok_second");

            // Act & Assert - First authorization
            MvcResult result1 = mockMvc.perform(post("/api/v1/invoices/" + invoice.getId() + "/payments/authorize")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(firstAuth))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("AUTHORIZED"))
                    .andReturn();

            // Act & Assert - Second authorization
            MvcResult result2 = mockMvc.perform(post("/api/v1/invoices/" + invoice.getId() + "/payments/authorize")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(secondAuth))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("AUTHORIZED"))
                    .andReturn();

            // Verify different payment IDs
            UUID paymentId1 = UUID.fromString(
                    objectMapper.readTree(result1.getResponse().getContentAsString())
                            .get("paymentId").asText());
            UUID paymentId2 = UUID.fromString(
                    objectMapper.readTree(result2.getResponse().getContentAsString())
                            .get("paymentId").asText());

            assertThat(paymentId1).isNotEqualTo(paymentId2);
        }

        @Test
        @DisplayName("Should reject authorization with missing payment token")
        void rejectMissingPaymentToken() throws Exception {
            // Arrange
            String payload = "{\"method\":\"CARD_NOT_PRESENT\",\"initiatedBy\":\"TEST\",\"amount\":{\"amount\":300.00,\"currency\":\"USD\"}}";

            // Act & Assert
            mockMvc.perform(post("/api/v1/invoices/" + invoice.getId() + "/payments/authorize")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(payload))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("Should reject authorization with invalid amount")
        void rejectInvalidAmount() throws Exception {
            // Arrange
            String payload = "{\"method\":\"CARD_NOT_PRESENT\",\"paymentToken\":\"tok_test\",\"initiatedBy\":\"TEST\",\"amount\":{\"amount\":-100.00,\"currency\":\"USD\"}}";

            // Act & Assert
            mockMvc.perform(post("/api/v1/invoices/" + invoice.getId() + "/payments/authorize")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(payload))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("Should reject authorization for non-existent invoice")
        void rejectNonExistentInvoice() throws Exception {
            // Arrange
            String authorizePayload = buildAuthorizePayload("300.00", "tok_test");

            // Act & Assert
            mockMvc.perform(post("/api/v1/invoices/" + UUID.randomUUID() + "/payments/authorize")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(authorizePayload))
                    .andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("Payment Capture")
    class PaymentCapture {

        @Test
        @DisplayName("Should capture authorized payment successfully")
        void capturePaymentSuccessfully() throws Exception {
            // Arrange - Authorize payment first
            String authorizePayload = buildAuthorizePayload("300.00", "tok_capture");
            MvcResult authResult = mockMvc.perform(post("/api/v1/invoices/" + invoice.getId() + "/payments/authorize")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(authorizePayload))
                    .andExpect(status().isOk())
                    .andReturn();

            UUID paymentId = UUID.fromString(
                    objectMapper.readTree(authResult.getResponse().getContentAsString())
                            .get("paymentId").asText());

            // Act & Assert - Capture the payment
            mockMvc.perform(post("/api/v1/payments/" + paymentId + "/capture"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("CAPTURED"))
                    .andExpect(jsonPath("$.paymentId").value(paymentId.toString()));

            // Verify payment status
            Payment captured = paymentRepository.findById(paymentId).orElseThrow();
            assertThat(captured.getStatus()).isEqualTo(PaymentStatus.CAPTURED);
        }

        @Test
        @DisplayName("Should update invoice status to PAID when full amount captured")
        void updateInvoiceStatusWhenFullAmountCaptured() throws Exception {
            // Arrange - Authorize payment
            String authorizePayload = buildAuthorizePayload("300.00", "tok_full");
            MvcResult authResult = mockMvc.perform(post("/api/v1/invoices/" + invoice.getId() + "/payments/authorize")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(authorizePayload))
                    .andExpect(status().isOk())
                    .andReturn();

            UUID paymentId = UUID.fromString(
                    objectMapper.readTree(authResult.getResponse().getContentAsString())
                            .get("paymentId").asText());

            // Act - Capture the payment
            mockMvc.perform(post("/api/v1/payments/" + paymentId + "/capture"))
                    .andExpect(status().isOk());

            // Assert - Invoice should be marked as PAID
            Invoice refreshed = invoiceRepository.findById(invoice.getId()).orElseThrow();
            assertThat(refreshed.getStatus()).isEqualTo(InvoiceStatus.PAID);
            assertThat(refreshed.getBalanceDue().getAmount())
                    .isEqualByComparingTo(BigDecimal.ZERO.setScale(2));
        }

        @Test
        @DisplayName("Should handle partial captures")
        void capturePartialPayments() throws Exception {
            // Arrange - Authorize two payments
            String firstAuth = buildAuthorizePayload("150.00", "tok_partial1");
            String secondAuth = buildAuthorizePayload("150.00", "tok_partial2");

            MvcResult auth1 = mockMvc.perform(post("/api/v1/invoices/" + invoice.getId() + "/payments/authorize")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(firstAuth))
                    .andExpect(status().isOk())
                    .andReturn();

            MvcResult auth2 = mockMvc.perform(post("/api/v1/invoices/" + invoice.getId() + "/payments/authorize")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(secondAuth))
                    .andExpect(status().isOk())
                    .andReturn();

            UUID paymentId1 = UUID.fromString(
                    objectMapper.readTree(auth1.getResponse().getContentAsString())
                            .get("paymentId").asText());
            UUID paymentId2 = UUID.fromString(
                    objectMapper.readTree(auth2.getResponse().getContentAsString())
                            .get("paymentId").asText());

            // Act - Capture first payment only
            mockMvc.perform(post("/api/v1/payments/" + paymentId1 + "/capture"))
                    .andExpect(status().isOk());

            // Assert - Invoice should still have balance
            Invoice afterFirst = invoiceRepository.findById(invoice.getId()).orElseThrow();
            assertThat(afterFirst.getBalanceDue().getAmount())
                    .isEqualByComparingTo(new BigDecimal("150.00").setScale(2));

            // Act - Capture second payment
            mockMvc.perform(post("/api/v1/payments/" + paymentId2 + "/capture"))
                    .andExpect(status().isOk());

            // Assert - Invoice should be fully paid
            Invoice afterBoth = invoiceRepository.findById(invoice.getId()).orElseThrow();
            assertThat(afterBoth.getStatus()).isEqualTo(InvoiceStatus.PAID);
            assertThat(afterBoth.getBalanceDue().getAmount())
                    .isEqualByComparingTo(BigDecimal.ZERO.setScale(2));
        }

        @Test
        @DisplayName("Should reject capture of non-existent payment")
        void rejectNonExistentPaymentCapture() throws Exception {
            // Act & Assert
            mockMvc.perform(post("/api/v1/payments/" + UUID.randomUUID() + "/capture"))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("Should reject capture of already captured payment")
        void rejectDuplicateCapture() throws Exception {
            // Arrange - Authorize and capture
            String authorizePayload = buildAuthorizePayload("300.00", "tok_duplicate");
            MvcResult authResult = mockMvc.perform(post("/api/v1/invoices/" + invoice.getId() + "/payments/authorize")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(authorizePayload))
                    .andExpect(status().isOk())
                    .andReturn();

            UUID paymentId = UUID.fromString(
                    objectMapper.readTree(authResult.getResponse().getContentAsString())
                            .get("paymentId").asText());

            mockMvc.perform(post("/api/v1/payments/" + paymentId + "/capture"))
                    .andExpect(status().isOk());

            // Act & Assert - Try to capture again
            mockMvc.perform(post("/api/v1/payments/" + paymentId + "/capture"))
                    .andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("Payment Refund")
    class PaymentRefund {

        @Test
        @DisplayName("Should refund captured payment successfully")
        void refundPaymentSuccessfully() throws Exception {
            // Arrange - Authorize and capture payment
            UUID paymentId = authorizeAndCapturePayment("300.00");

            // Act & Assert - Refund the payment
            String refundPayload = "{\"method\":\"CARD_NOT_PRESENT\",\"amount\":{\"amount\":300.00,\"currency\":\"USD\"}}";
            mockMvc.perform(post("/api/v1/payments/" + paymentId + "/refund")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(refundPayload))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("REFUNDED"));

            // Verify payment status
            Payment refunded = paymentRepository.findById(paymentId).orElseThrow();
            assertThat(refunded.getStatus()).isEqualTo(PaymentStatus.REFUNDED);
        }

        @Test
        @DisplayName("Should handle partial refunds")
        void refundPartialAmount() throws Exception {
            // Arrange - Authorize and capture full amount
            UUID paymentId = authorizeAndCapturePayment("300.00");

            // Act & Assert - Refund partial amount
            String refundPayload = "{\"method\":\"CARD_NOT_PRESENT\",\"amount\":{\"amount\":150.00,\"currency\":\"USD\"}}";
            mockMvc.perform(post("/api/v1/payments/" + paymentId + "/refund")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(refundPayload))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("REFUNDED"));

            // Verify invoice balance increased
            Invoice refreshed = invoiceRepository.findById(invoice.getId()).orElseThrow();
            assertThat(refreshed.getBalanceDue().getAmount())
                    .isEqualByComparingTo(new BigDecimal("150.00").setScale(2));
        }

        @Test
        @DisplayName("Should reject refund of non-captured payment")
        void rejectRefundOfUnauthorizedPayment() throws Exception {
            // Arrange - Only authorize, don't capture
            String authorizePayload = buildAuthorizePayload("300.00", "tok_refund_fail");
            MvcResult authResult = mockMvc.perform(post("/api/v1/invoices/" + invoice.getId() + "/payments/authorize")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(authorizePayload))
                    .andExpect(status().isOk())
                    .andReturn();

            UUID paymentId = UUID.fromString(
                    objectMapper.readTree(authResult.getResponse().getContentAsString())
                            .get("paymentId").asText());

            // Act & Assert - Try to refund unauthorized payment
            String refundPayload = "{\"method\":\"CARD_NOT_PRESENT\",\"amount\":{\"amount\":300.00,\"currency\":\"USD\"}}";
            mockMvc.perform(post("/api/v1/payments/" + paymentId + "/refund")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(refundPayload))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("Should reject refund with missing amount")
        void rejectRefundWithMissingAmount() throws Exception {
            // Arrange
            UUID paymentId = authorizeAndCapturePayment("300.00");

            // Act & Assert
            String refundPayload = "{\"method\":\"CARD_NOT_PRESENT\"}";
            mockMvc.perform(post("/api/v1/payments/" + paymentId + "/refund")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(refundPayload))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("Should reject refund for non-existent payment")
        void rejectNonExistentPaymentRefund() throws Exception {
            // Act & Assert
            String refundPayload = "{\"method\":\"CARD_NOT_PRESENT\",\"amount\":{\"amount\":300.00,\"currency\":\"USD\"}}";
            mockMvc.perform(post("/api/v1/payments/" + UUID.randomUUID() + "/refund")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(refundPayload))
                    .andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("Payment Void")
    class PaymentVoid {

        @Test
        @DisplayName("Should void authorized payment successfully")
        void voidPaymentSuccessfully() throws Exception {
            // Arrange - Authorize payment
            String authorizePayload = buildAuthorizePayload("300.00", "tok_void");
            MvcResult authResult = mockMvc.perform(post("/api/v1/invoices/" + invoice.getId() + "/payments/authorize")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(authorizePayload))
                    .andExpect(status().isOk())
                    .andReturn();

            UUID paymentId = UUID.fromString(
                    objectMapper.readTree(authResult.getResponse().getContentAsString())
                            .get("paymentId").asText());

            // Act & Assert - Void the payment
            mockMvc.perform(post("/api/v1/payments/" + paymentId + "/void"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("VOIDED"));

            // Verify payment status
            Payment voided = paymentRepository.findById(paymentId).orElseThrow();
            assertThat(voided.getStatus()).isEqualTo(PaymentStatus.VOIDED);
        }

        @Test
        @DisplayName("Should restore invoice balance when voiding payment")
        void restoreInvoiceBalanceOnVoid() throws Exception {
            // Arrange - Authorize payment
            String authorizePayload = buildAuthorizePayload("150.00", "tok_void_balance");
            MvcResult authResult = mockMvc.perform(post("/api/v1/invoices/" + invoice.getId() + "/payments/authorize")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(authorizePayload))
                    .andExpect(status().isOk())
                    .andReturn();

            UUID paymentId = UUID.fromString(
                    objectMapper.readTree(authResult.getResponse().getContentAsString())
                            .get("paymentId").asText());

            // Verify balance was reduced
            Invoice afterAuth = invoiceRepository.findById(invoice.getId()).orElseThrow();
            assertThat(afterAuth.getBalanceDue().getAmount())
                    .isEqualByComparingTo(new BigDecimal("150.00").setScale(2));

            // Act - Void the payment
            mockMvc.perform(post("/api/v1/payments/" + paymentId + "/void"))
                    .andExpect(status().isOk());

            // Assert - Balance should be restored
            Invoice afterVoid = invoiceRepository.findById(invoice.getId()).orElseThrow();
            assertThat(afterVoid.getBalanceDue().getAmount())
                    .isEqualByComparingTo(new BigDecimal("300.00").setScale(2));
        }

        @Test
        @DisplayName("Should reject void of captured payment")
        void rejectVoidOfCapturedPayment() throws Exception {
            // Arrange - Authorize and capture
            UUID paymentId = authorizeAndCapturePayment("300.00");

            // Act & Assert - Try to void captured payment
            mockMvc.perform(post("/api/v1/payments/" + paymentId + "/void"))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("Should reject void of non-existent payment")
        void rejectNonExistentPaymentVoid() throws Exception {
            // Act & Assert
            mockMvc.perform(post("/api/v1/payments/" + UUID.randomUUID() + "/void"))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("Should reject void of already voided payment")
        void rejectDuplicateVoid() throws Exception {
            // Arrange - Authorize and void
            String authorizePayload = buildAuthorizePayload("300.00", "tok_duplicate_void");
            MvcResult authResult = mockMvc.perform(post("/api/v1/invoices/" + invoice.getId() + "/payments/authorize")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(authorizePayload))
                    .andExpect(status().isOk())
                    .andReturn();

            UUID paymentId = UUID.fromString(
                    objectMapper.readTree(authResult.getResponse().getContentAsString())
                            .get("paymentId").asText());

            mockMvc.perform(post("/api/v1/payments/" + paymentId + "/void"))
                    .andExpect(status().isOk());

            // Act & Assert - Try to void again
            mockMvc.perform(post("/api/v1/payments/" + paymentId + "/void"))
                    .andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("Complete Payment Workflows")
    class CompletePaymentWorkflows {

        @Test
        @DisplayName("Should complete authorize and capture workflow")
        void completeAuthorizeAndCaptureWorkflow() throws Exception {
            // Arrange
            String authorizePayload = buildAuthorizePayload("300.00", "tok_workflow");

            // Act - Authorize
            MvcResult authorizationResult = mockMvc.perform(
                            post("/api/v1/invoices/" + invoice.getId() + "/payments/authorize")
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(authorizePayload))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("AUTHORIZED"))
                    .andReturn();

            UUID paymentId = UUID.fromString(
                    objectMapper.readTree(authorizationResult.getResponse().getContentAsString())
                            .get("paymentId").asText());

            // Act - Capture
            mockMvc.perform(post("/api/v1/payments/" + paymentId + "/capture"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("CAPTURED"));

            // Assert - Verify final state
            Invoice refreshed = invoiceRepository.findById(invoice.getId()).orElseThrow();
            assertThat(refreshed.getStatus()).isEqualTo(InvoiceStatus.PAID);
            assertThat(refreshed.getBalanceDue().getAmount())
                    .isEqualByComparingTo(BigDecimal.ZERO.setScale(2));
        }

        @Test
        @DisplayName("Should complete authorize, capture, and refund workflow")
        void completeFullRefundWorkflow() throws Exception {
            // Arrange & Act - Authorize and capture
            UUID paymentId = authorizeAndCapturePayment("300.00");

            // Act - Refund
            String refundPayload = "{\"method\":\"CARD_NOT_PRESENT\",\"amount\":{\"amount\":300.00,\"currency\":\"USD\"}}";
            mockMvc.perform(post("/api/v1/payments/" + paymentId + "/refund")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(refundPayload))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("REFUNDED"));

            // Assert - Verify payment and invoice state
            Payment refunded = paymentRepository.findById(paymentId).orElseThrow();
            assertThat(refunded.getStatus()).isEqualTo(PaymentStatus.REFUNDED);

            Invoice refreshed = invoiceRepository.findById(invoice.getId()).orElseThrow();
            assertThat(refreshed.getBalanceDue().getAmount())
                    .isEqualByComparingTo(new BigDecimal("300.00").setScale(2));
        }

        @Test
        @DisplayName("Should complete authorize and void workflow")
        void completeAuthorizeAndVoidWorkflow() throws Exception {
            // Arrange
            String authorizePayload = buildAuthorizePayload("300.00", "tok_auth_void");

            // Act - Authorize
            MvcResult authResult = mockMvc.perform(
                            post("/api/v1/invoices/" + invoice.getId() + "/payments/authorize")
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(authorizePayload))
                    .andExpect(status().isOk())
                    .andReturn();

            UUID paymentId = UUID.fromString(
                    objectMapper.readTree(authResult.getResponse().getContentAsString())
                            .get("paymentId").asText());

            // Act - Void
            mockMvc.perform(post("/api/v1/payments/" + paymentId + "/void"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("VOIDED"));

            // Assert - Verify final state
            Payment voided = paymentRepository.findById(paymentId).orElseThrow();
            assertThat(voided.getStatus()).isEqualTo(PaymentStatus.VOIDED);

            Invoice refreshed = invoiceRepository.findById(invoice.getId()).orElseThrow();
            assertThat(refreshed.getBalanceDue().getAmount())
                    .isEqualByComparingTo(new BigDecimal("300.00").setScale(2));
        }

        @Test
        @DisplayName("Should handle split payments workflow")
        void completeSplitPaymentWorkflow() throws Exception {
            // Act - First payment (50%)
            UUID payment1 = authorizeAndCapturePayment("150.00");

            // Assert - Verify partial payment
            Invoice afterFirst = invoiceRepository.findById(invoice.getId()).orElseThrow();
            assertThat(afterFirst.getBalanceDue().getAmount())
                    .isEqualByComparingTo(new BigDecimal("150.00").setScale(2));

            // Act - Second payment (50%)
            UUID payment2 = authorizeAndCapturePayment("150.00");

            // Assert - Verify full payment
            Invoice afterSecond = invoiceRepository.findById(invoice.getId()).orElseThrow();
            assertThat(afterSecond.getStatus()).isEqualTo(InvoiceStatus.PAID);
            assertThat(afterSecond.getBalanceDue().getAmount())
                    .isEqualByComparingTo(BigDecimal.ZERO.setScale(2));

            // Verify both payments exist
            assertThat(paymentRepository.findById(payment1)).isPresent();
            assertThat(paymentRepository.findById(payment2)).isPresent();
        }
    }

    // Helper methods

    private String buildAuthorizePayload(String amount, String token) {
        return String.format(
                "{\"method\":\"CARD_NOT_PRESENT\",\"paymentToken\":\"%s\",\"initiatedBy\":\"TEST\",\"amount\":{\"amount\":%s,\"currency\":\"USD\"}}",
                token, amount);
    }

    private UUID authorizeAndCapturePayment(String amount) throws Exception {
        // Authorize
        String authorizePayload = buildAuthorizePayload(amount, "tok_" + UUID.randomUUID().toString().substring(0, 8));
        MvcResult authResult = mockMvc.perform(post("/api/v1/invoices/" + invoice.getId() + "/payments/authorize")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(authorizePayload))
                .andExpect(status().isOk())
                .andReturn();

        UUID paymentId = UUID.fromString(
                objectMapper.readTree(authResult.getResponse().getContentAsString())
                        .get("paymentId").asText());

        // Capture
        mockMvc.perform(post("/api/v1/payments/" + paymentId + "/capture"))
                .andExpect(status().isOk());

        return paymentId;
    }

    private Invoice createInvoice(Booking booking, BigDecimal amount) {
        return invoiceRepository.save(Invoice.builder()
                .id(UUID.randomUUID())
                .bookingId(booking.getId())
                .propertyId(property.getId())
                .lineItems(List.of(InvoiceLineItem.builder()
                        .description("Room night")
                        .quantity(2)
                        .amount(TestDataBuilder.createMoney(amount))
                        .build()))
                .subTotal(TestDataBuilder.createMoney(amount))
                .taxTotal(TestDataBuilder.createMoney(BigDecimal.ZERO))
                .grandTotal(TestDataBuilder.createMoney(amount))
                .balanceDue(TestDataBuilder.createMoney(amount))
                .status(InvoiceStatus.ISSUED)
                .issuedAt(OffsetDateTime.now())
                .dueAt(OffsetDateTime.now().plusDays(7))
                .build());
    }
}
