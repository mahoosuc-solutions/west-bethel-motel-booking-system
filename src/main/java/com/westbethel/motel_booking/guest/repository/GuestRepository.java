package com.westbethel.motel_booking.guest.repository;

import com.westbethel.motel_booking.guest.domain.Guest;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface GuestRepository extends JpaRepository<Guest, UUID> {
}
