package com.westbethel.motel_booking.common.audit;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "audit_entries")
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class AuditEntry {

    @Id
    private UUID id;

    @Column(name = "entity_type", nullable = false, length = 128)
    private String entityType;

    @Column(name = "entity_id", nullable = false, length = 36)
    private String entityId;

    @Column(name = "action", nullable = false, length = 32)
    private String action;

    @Column(name = "performed_by", length = 64)
    private String performedBy;

    @Column(name = "details", length = 2048)
    private String details;

    @Column(name = "occurred_at", nullable = false)
    private OffsetDateTime occurredAt;
}
