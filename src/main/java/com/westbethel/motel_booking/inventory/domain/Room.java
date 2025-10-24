package com.westbethel.motel_booking.inventory.domain;

import com.westbethel.motel_booking.common.model.HousekeepingStatus;
import com.westbethel.motel_booking.common.model.RoomStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "rooms")
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class Room {

    @Id
    private UUID id;

    @Column(name = "property_id", nullable = false)
    private UUID propertyId;

    @Column(name = "room_type_id", nullable = false)
    private UUID roomTypeId;

    @Column(name = "room_number", nullable = false, length = 16)
    private String roomNumber;

    @Column(length = 16)
    private String floor;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private RoomStatus status;

    @Enumerated(EnumType.STRING)
    @Column(name = "housekeeping_status", nullable = false, length = 32)
    private HousekeepingStatus housekeepingStatus;

    @Column(name = "maintenance_notes", length = 512)
    private String maintenanceNotes;
}
