CREATE TABLE properties (
    id UUID PRIMARY KEY,
    code VARCHAR(32) NOT NULL UNIQUE,
    name VARCHAR(255) NOT NULL,
    timezone VARCHAR(64) NOT NULL,
    default_currency VARCHAR(3) NOT NULL,
    address_line_1 VARCHAR(255),
    address_line_2 VARCHAR(255),
    address_city VARCHAR(128),
    address_state VARCHAR(128),
    address_postal_code VARCHAR(32),
    address_country VARCHAR(64),
    contact_email VARCHAR(255),
    contact_phone VARCHAR(64)
);

CREATE TABLE room_types (
    id UUID PRIMARY KEY,
    property_id UUID NOT NULL REFERENCES properties(id),
    code VARCHAR(32) NOT NULL,
    name VARCHAR(255) NOT NULL,
    description VARCHAR(500),
    capacity INTEGER NOT NULL,
    bed_configuration VARCHAR(128),
    base_rate_amount NUMERIC(15,2),
    base_rate_currency VARCHAR(3),
    CONSTRAINT uq_room_types_property_code UNIQUE(property_id, code)
);

CREATE TABLE room_type_amenities (
    room_type_id UUID NOT NULL REFERENCES room_types(id) ON DELETE CASCADE,
    amenity VARCHAR(64) NOT NULL,
    PRIMARY KEY (room_type_id, amenity)
);

CREATE TABLE rooms (
    id UUID PRIMARY KEY,
    property_id UUID NOT NULL REFERENCES properties(id),
    room_type_id UUID NOT NULL REFERENCES room_types(id),
    room_number VARCHAR(16) NOT NULL,
    floor VARCHAR(16),
    status VARCHAR(32) NOT NULL,
    housekeeping_status VARCHAR(32) NOT NULL,
    maintenance_notes VARCHAR(512),
    CONSTRAINT uq_rooms_property_number UNIQUE(property_id, room_number)
);

CREATE TABLE guests (
    id UUID PRIMARY KEY,
    customer_number VARCHAR(32),
    contact_email VARCHAR(255),
    contact_phone VARCHAR(64),
    address_line_1 VARCHAR(255),
    address_line_2 VARCHAR(255),
    address_city VARCHAR(128),
    address_state VARCHAR(128),
    address_postal_code VARCHAR(32),
    address_country VARCHAR(64),
    preferences VARCHAR(1024),
    marketing_opt_in BOOLEAN NOT NULL,
    loyalty_profile_id UUID,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ,
    CONSTRAINT uq_guests_customer_number UNIQUE(customer_number)
);

CREATE TABLE rate_plans (
    id UUID PRIMARY KEY,
    property_id UUID NOT NULL REFERENCES properties(id),
    name VARCHAR(255) NOT NULL,
    channel VARCHAR(32) NOT NULL,
    default_rate_amount NUMERIC(15,2),
    default_rate_currency VARCHAR(3),
    pricing_rules VARCHAR(2048),
    cancellation_policy VARCHAR(1024),
    stay_restrictions VARCHAR(512)
);

CREATE TABLE rate_plan_room_types (
    rate_plan_id UUID NOT NULL REFERENCES rate_plans(id) ON DELETE CASCADE,
    room_type_id UUID NOT NULL REFERENCES room_types(id),
    PRIMARY KEY (rate_plan_id, room_type_id)
);

CREATE TABLE bookings (
    id UUID PRIMARY KEY,
    property_id UUID NOT NULL REFERENCES properties(id),
    reference VARCHAR(32) NOT NULL UNIQUE,
    guest_id UUID NOT NULL REFERENCES guests(id),
    status VARCHAR(32) NOT NULL,
    payment_status VARCHAR(32) NOT NULL,
    channel VARCHAR(32) NOT NULL,
    source VARCHAR(64),
    created_by VARCHAR(64),
    check_in DATE NOT NULL,
    check_out DATE NOT NULL,
    adults INTEGER NOT NULL,
    children INTEGER NOT NULL,
    rate_plan_id UUID NOT NULL REFERENCES rate_plans(id),
    total_amount NUMERIC(15,2),
    total_currency VARCHAR(3),
    balance_due_amount NUMERIC(15,2),
    balance_due_currency VARCHAR(3),
    notes VARCHAR(2048),
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ,
    version BIGINT
);

CREATE TABLE booking_rooms (
    booking_id UUID NOT NULL REFERENCES bookings(id) ON DELETE CASCADE,
    room_id UUID NOT NULL REFERENCES rooms(id),
    PRIMARY KEY (booking_id, room_id)
);

CREATE INDEX idx_bookings_property_dates ON bookings(property_id, check_in, check_out);

CREATE TABLE addons (
    id UUID PRIMARY KEY,
    property_id UUID NOT NULL REFERENCES properties(id),
    name VARCHAR(255) NOT NULL,
    description VARCHAR(1024),
    price_amount NUMERIC(15,2),
    price_currency VARCHAR(3)
);

CREATE TABLE invoices (
    id UUID PRIMARY KEY,
    booking_id UUID NOT NULL UNIQUE REFERENCES bookings(id),
    property_id UUID NOT NULL REFERENCES properties(id),
    subtotal_amount NUMERIC(15,2),
    subtotal_currency VARCHAR(3),
    tax_amount NUMERIC(15,2),
    tax_currency VARCHAR(3),
    grand_total_amount NUMERIC(15,2),
    grand_total_currency VARCHAR(3),
    balance_due_amount NUMERIC(15,2),
    balance_due_currency VARCHAR(3),
    status VARCHAR(32) NOT NULL,
    issued_at TIMESTAMPTZ NOT NULL,
    due_at TIMESTAMPTZ
);

CREATE TABLE invoice_line_items (
    invoice_id UUID NOT NULL REFERENCES invoices(id) ON DELETE CASCADE,
    line_description VARCHAR(255) NOT NULL,
    line_quantity INTEGER NOT NULL,
    amount NUMERIC(15,2),
    currency VARCHAR(3)
);

CREATE TABLE payments (
    id UUID PRIMARY KEY,
    invoice_id UUID NOT NULL REFERENCES invoices(id),
    method VARCHAR(32) NOT NULL,
    processor VARCHAR(64) NOT NULL,
    amount NUMERIC(15,2),
    currency VARCHAR(3),
    status VARCHAR(32) NOT NULL,
    auth_code VARCHAR(64),
    failure_reason VARCHAR(512),
    processed_at TIMESTAMPTZ
);

CREATE TABLE loyalty_profiles (
    id UUID PRIMARY KEY,
    guest_id UUID NOT NULL UNIQUE REFERENCES guests(id),
    tier VARCHAR(32) NOT NULL,
    points_balance BIGINT NOT NULL,
    points_expiry VARCHAR(1024),
    updated_at TIMESTAMPTZ
);

CREATE TABLE promotions (
    id UUID PRIMARY KEY,
    property_id UUID NOT NULL REFERENCES properties(id),
    name VARCHAR(255) NOT NULL,
    promo_code VARCHAR(32),
    discount_type VARCHAR(32) NOT NULL,
    value_amount NUMERIC(15,2),
    value_currency VARCHAR(3),
    starts_on DATE NOT NULL,
    ends_on DATE,
    restrictions VARCHAR(1024)
);

CREATE TABLE maintenance_requests (
    id UUID PRIMARY KEY,
    property_id UUID NOT NULL REFERENCES properties(id),
    room_id UUID REFERENCES rooms(id),
    description VARCHAR(1024) NOT NULL,
    status VARCHAR(32) NOT NULL,
    severity VARCHAR(32),
    scheduled_from TIMESTAMPTZ,
    scheduled_to TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ
);

CREATE TABLE audit_entries (
    id UUID PRIMARY KEY,
    entity_type VARCHAR(128) NOT NULL,
    entity_id VARCHAR(64) NOT NULL,
    action VARCHAR(32) NOT NULL,
    performed_by VARCHAR(64),
    details VARCHAR(2048),
    occurred_at TIMESTAMPTZ NOT NULL
);
