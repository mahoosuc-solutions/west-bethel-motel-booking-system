package com.westbethel.motel_booking.common.service;

import com.westbethel.motel_booking.common.audit.AuditEntry;

public interface AuditService {

    void record(AuditEntry entry);
}
