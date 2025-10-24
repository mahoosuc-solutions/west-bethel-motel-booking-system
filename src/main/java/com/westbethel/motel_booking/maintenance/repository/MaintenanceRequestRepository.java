package com.westbethel.motel_booking.maintenance.repository;

import com.westbethel.motel_booking.common.model.MaintenanceStatus;
import com.westbethel.motel_booking.maintenance.domain.MaintenanceRequest;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MaintenanceRequestRepository extends JpaRepository<MaintenanceRequest, UUID> {

    List<MaintenanceRequest> findByPropertyId(UUID propertyId);

    List<MaintenanceRequest> findByPropertyIdAndStatus(UUID propertyId, MaintenanceStatus status);

    List<MaintenanceRequest> findByRoomId(UUID roomId);

    List<MaintenanceRequest> findByRoomIdAndStatus(UUID roomId, MaintenanceStatus status);
}
