package com.westbethel.motel_booking.maintenance.domain;

import com.westbethel.motel_booking.common.model.MaintenanceStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "maintenance_requests")
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class MaintenanceRequest {

    @Id
    private UUID id;

    @Column(name = "property_id", nullable = false)
    private UUID propertyId;

    @Column(name = "room_id")
    private UUID roomId;

    @Column(nullable = false, length = 1024)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private MaintenanceStatus status;

    @Column(name = "severity", length = 32)
    private String severity;

    @Column(name = "scheduled_from")
    private OffsetDateTime scheduledFrom;

    @Column(name = "scheduled_to")
    private OffsetDateTime scheduledTo;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at")
    private OffsetDateTime updatedAt;
}
