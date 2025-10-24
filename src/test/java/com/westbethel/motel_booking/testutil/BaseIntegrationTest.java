package com.westbethel.motel_booking.testutil;

import com.westbethel.motel_booking.billing.repository.InvoiceRepository;
import com.westbethel.motel_booking.billing.repository.PaymentRepository;
import com.westbethel.motel_booking.guest.repository.GuestRepository;
import com.westbethel.motel_booking.inventory.repository.RoomRepository;
import com.westbethel.motel_booking.inventory.repository.RoomTypeRepository;
import com.westbethel.motel_booking.pricing.repository.RatePlanRepository;
import com.westbethel.motel_booking.property.repository.PropertyRepository;
import com.westbethel.motel_booking.reservation.repository.BookingRepository;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Base class for integration tests providing common setup and repository cleanup.
 * All controller integration tests should extend this class to ensure proper test isolation
 * and consistent H2 database state between tests.
 */
@SpringBootTest
@AutoConfigureMockMvc
public abstract class BaseIntegrationTest {

    @Autowired
    protected MockMvc mockMvc;

    @Autowired
    protected PropertyRepository propertyRepository;

    @Autowired
    protected RoomTypeRepository roomTypeRepository;

    @Autowired
    protected RoomRepository roomRepository;

    @Autowired
    protected GuestRepository guestRepository;

    @Autowired
    protected BookingRepository bookingRepository;

    @Autowired
    protected RatePlanRepository ratePlanRepository;

    @Autowired
    protected InvoiceRepository invoiceRepository;

    @Autowired(required = false)
    protected PaymentRepository paymentRepository;

    /**
     * Cleans up all test data before each test to ensure test isolation.
     * Order matters due to foreign key constraints.
     */
    @BeforeEach
    void cleanupDatabase() {
        if (paymentRepository != null) {
            paymentRepository.deleteAll();
        }
        if (invoiceRepository != null) {
            invoiceRepository.deleteAll();
        }
        if (bookingRepository != null) {
            bookingRepository.deleteAll();
        }
        if (roomRepository != null) {
            roomRepository.deleteAll();
        }
        if (ratePlanRepository != null) {
            ratePlanRepository.deleteAll();
        }
        if (roomTypeRepository != null) {
            roomTypeRepository.deleteAll();
        }
        if (guestRepository != null) {
            guestRepository.deleteAll();
        }
        if (propertyRepository != null) {
            propertyRepository.deleteAll();
        }
    }
}
