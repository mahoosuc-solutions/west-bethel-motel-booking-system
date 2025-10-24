-- V10__Add_Performance_Indexes.sql
-- Performance Optimization: Add Critical Indexes
--
-- This migration adds strategic indexes to improve query performance
-- Target: 50% reduction in query execution time
--
-- Index Strategy:
-- 1. Composite indexes for frequently joined columns
-- 2. Covering indexes (INCLUDE) for read-heavy queries
-- 3. Partial indexes for filtered queries
-- 4. Indexes on foreign keys and date ranges

-- =====================================================
-- BOOKING QUERIES OPTIMIZATION
-- =====================================================

-- Primary booking search: property + date range
-- Covers: Availability checks, booking searches
CREATE INDEX IF NOT EXISTS idx_bookings_property_dates
    ON bookings(property_id, check_in, check_out)
    WHERE status IN ('CONFIRMED', 'CHECKED_IN');

-- Guest booking history with status
-- Covers: Guest dashboard, booking history
CREATE INDEX IF NOT EXISTS idx_bookings_guest_status
    ON bookings(guest_id, status, created_at DESC);

-- Active bookings by status and dates
-- Covers: Operational reports, housekeeping schedules
CREATE INDEX IF NOT EXISTS idx_bookings_status_dates
    ON bookings(status, check_in, check_out)
    WHERE status IN ('CONFIRMED', 'CHECKED_IN');

-- Booking reference lookup (unique constraint may already cover this)
CREATE INDEX IF NOT EXISTS idx_bookings_reference
    ON bookings(reference)
    WHERE reference IS NOT NULL;

-- Composite index with INCLUDE for booking list queries
-- Covers: Most booking queries without additional lookups
CREATE INDEX IF NOT EXISTS idx_bookings_composite
    ON bookings(property_id, status, check_in)
    INCLUDE (check_out, guest_id, total_amount, reference);

-- Check-in/Check-out date range queries
CREATE INDEX IF NOT EXISTS idx_bookings_checkin
    ON bookings(check_in)
    WHERE status IN ('CONFIRMED', 'CHECKED_IN');

CREATE INDEX IF NOT EXISTS idx_bookings_checkout
    ON bookings(check_out)
    WHERE status IN ('CONFIRMED', 'CHECKED_IN');

-- =====================================================
-- ROOM QUERIES OPTIMIZATION
-- =====================================================

-- Available rooms by property and type
-- Covers: Availability searches, room assignment
CREATE INDEX IF NOT EXISTS idx_rooms_property_type_status
    ON rooms(property_id, room_type_id, status);

-- Fast lookup for available rooms
-- Partial index: only AVAILABLE rooms
CREATE INDEX IF NOT EXISTS idx_rooms_available
    ON rooms(status, property_id)
    WHERE status = 'AVAILABLE';

-- Room maintenance queries
CREATE INDEX IF NOT EXISTS idx_rooms_status_property
    ON rooms(status, property_id, room_number);

-- =====================================================
-- PAYMENT QUERIES OPTIMIZATION
-- =====================================================

-- Payment lookup by booking and status
-- Covers: Payment verification, refund processing
CREATE INDEX IF NOT EXISTS idx_payments_booking_status
    ON payments(booking_id, status, created_at DESC);

-- Payment reporting by date and status
-- Covers: Financial reports, reconciliation
CREATE INDEX IF NOT EXISTS idx_payments_created_status
    ON payments(created_at DESC, status)
    INCLUDE (amount, payment_method);

-- Pending/Failed payment monitoring
CREATE INDEX IF NOT EXISTS idx_payments_pending
    ON payments(status, created_at DESC)
    WHERE status IN ('PENDING', 'FAILED');

-- Payment method analytics
CREATE INDEX IF NOT EXISTS idx_payments_method
    ON payments(payment_method, created_at DESC)
    WHERE status = 'COMPLETED';

-- =====================================================
-- USER & AUTHENTICATION OPTIMIZATION
-- =====================================================

-- Email lookup for login (likely has unique constraint)
CREATE INDEX IF NOT EXISTS idx_users_email_enabled
    ON users(email, enabled)
    WHERE enabled = true;

-- Username lookup for login
CREATE INDEX IF NOT EXISTS idx_users_username_enabled
    ON users(username, enabled)
    WHERE enabled = true;

-- Email verification lookups
CREATE INDEX IF NOT EXISTS idx_users_email_verified
    ON users(email, email_verified)
    WHERE email_verified = false;

-- User role queries
CREATE INDEX IF NOT EXISTS idx_users_role
    ON users(created_at DESC)
    INCLUDE (email, username, enabled);

-- =====================================================
-- AUDIT QUERIES OPTIMIZATION
-- =====================================================

-- Audit trail by entity
-- Covers: Audit history for specific entities
CREATE INDEX IF NOT EXISTS idx_audit_entries_entity_date
    ON audit_entries(entity_type, entity_id, occurred_at DESC);

-- Audit trail by user
-- Covers: User activity tracking
CREATE INDEX IF NOT EXISTS idx_audit_entries_performed_by
    ON audit_entries(performed_by, occurred_at DESC)
    WHERE performed_by IS NOT NULL;

-- Recent audit entries
CREATE INDEX IF NOT EXISTS idx_audit_entries_recent
    ON audit_entries(occurred_at DESC)
    INCLUDE (entity_type, entity_id, action, performed_by);

-- =====================================================
-- LOYALTY PROGRAM OPTIMIZATION
-- =====================================================

-- Loyalty profile lookup by guest and tier
-- Covers: Loyalty status checks, tier upgrades
CREATE INDEX IF NOT EXISTS idx_loyalty_profiles_guest_tier
    ON loyalty_profiles(guest_id, current_tier, points_balance DESC);

-- Active loyalty members by tier
CREATE INDEX IF NOT EXISTS idx_loyalty_profiles_tier
    ON loyalty_profiles(current_tier, points_balance DESC)
    WHERE status = 'ACTIVE';

-- =====================================================
-- GUEST QUERIES OPTIMIZATION
-- =====================================================

-- Guest lookup by email
CREATE INDEX IF NOT EXISTS idx_guests_email
    ON guests(email)
    WHERE email IS NOT NULL;

-- Guest lookup by phone
CREATE INDEX IF NOT EXISTS idx_guests_phone
    ON guests(phone)
    WHERE phone IS NOT NULL;

-- =====================================================
-- RATE PLAN & PRICING OPTIMIZATION
-- =====================================================

-- Active rate plans by property
CREATE INDEX IF NOT EXISTS idx_rate_plans_property_active
    ON rate_plans(property_id, effective_from, effective_to)
    WHERE is_active = true;

-- Rate plan validity check
CREATE INDEX IF NOT EXISTS idx_rate_plans_dates
    ON rate_plans(effective_from, effective_to, is_active);

-- =====================================================
-- PROMOTION QUERIES OPTIMIZATION
-- =====================================================

-- Active promotions lookup
CREATE INDEX IF NOT EXISTS idx_promotions_active_dates
    ON promotions(is_active, start_date, end_date)
    WHERE is_active = true;

-- Promotion code lookup
CREATE INDEX IF NOT EXISTS idx_promotions_code
    ON promotions(code)
    WHERE is_active = true;

-- =====================================================
-- MAINTENANCE QUERIES OPTIMIZATION
-- =====================================================

-- Maintenance requests by room and status
CREATE INDEX IF NOT EXISTS idx_maintenance_room_status
    ON maintenance_requests(room_id, status, created_at DESC);

-- Pending maintenance by priority
CREATE INDEX IF NOT EXISTS idx_maintenance_pending
    ON maintenance_requests(status, priority, created_at)
    WHERE status IN ('PENDING', 'IN_PROGRESS');

-- =====================================================
-- INVOICE QUERIES OPTIMIZATION
-- =====================================================

-- Invoice lookup by booking
CREATE INDEX IF NOT EXISTS idx_invoices_booking
    ON invoices(booking_id, created_at DESC);

-- Invoice status queries
CREATE INDEX IF NOT EXISTS idx_invoices_status_date
    ON invoices(status, created_at DESC)
    INCLUDE (booking_id, total_amount);

-- =====================================================
-- PROPERTY & ROOM TYPE OPTIMIZATION
-- =====================================================

-- Property status lookup
CREATE INDEX IF NOT EXISTS idx_properties_status
    ON properties(status, name);

-- Room types by property
CREATE INDEX IF NOT EXISTS idx_room_types_property
    ON room_types(property_id, is_active)
    WHERE is_active = true;

-- =====================================================
-- ADD-ON QUERIES OPTIMIZATION
-- =====================================================

-- Active add-ons by property
CREATE INDEX IF NOT EXISTS idx_addons_property_active
    ON add_ons(property_id, is_active)
    WHERE is_active = true;

-- =====================================================
-- NOTIFICATION PREFERENCES OPTIMIZATION
-- =====================================================

-- User notification preferences lookup
CREATE INDEX IF NOT EXISTS idx_notification_prefs_user
    ON notification_preferences(user_id);

-- =====================================================
-- ANALYZE TABLES
-- =====================================================
-- Update PostgreSQL statistics for query planner optimization

ANALYZE bookings;
ANALYZE rooms;
ANALYZE payments;
ANALYZE users;
ANALYZE audit_entries;
ANALYZE loyalty_profiles;
ANALYZE guests;
ANALYZE rate_plans;
ANALYZE promotions;
ANALYZE maintenance_requests;
ANALYZE invoices;
ANALYZE properties;
ANALYZE room_types;
ANALYZE add_ons;
