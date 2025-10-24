package com.westbethel.motel_booking.common.audit.repository;

import com.westbethel.motel_booking.common.audit.AuditEntry;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AuditEntryRepository extends JpaRepository<AuditEntry, UUID> {

    List<AuditEntry> findByEntityTypeAndEntityId(String entityType, String entityId);

    @Query("""
            select a from AuditEntry a
            where a.entityType = :entityType
              and a.entityId = :entityId
              and a.occurredAt >= :startDate
              and a.occurredAt <= :endDate
            order by a.occurredAt desc
            """)
    List<AuditEntry> findByEntityTypeAndEntityIdAndDateRange(
            @Param("entityType") String entityType,
            @Param("entityId") String entityId,
            @Param("startDate") OffsetDateTime startDate,
            @Param("endDate") OffsetDateTime endDate);

    List<AuditEntry> findByPerformedBy(String performedBy);
}
