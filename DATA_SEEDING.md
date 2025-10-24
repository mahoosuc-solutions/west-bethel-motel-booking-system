# Database Seeding Guide

This document describes the database seeding functionality for the West Bethel Motel Booking System.

## Overview

The `DataSeeder` component provides comprehensive test data for development and testing environments. It automatically populates the database with realistic data when the application starts with the `dev` profile enabled.

## Location

- **Seeder Class**: `/src/main/java/com/westbethel/motel_booking/config/DataSeeder.java`
- **Dev Profile Config**: `/src/main/resources/application-dev.yml`

## How to Enable

### Option 1: Maven Command
```bash
mvn spring-boot:run -Dspring-boot.run.profiles=dev
```

### Option 2: Environment Variable
```bash
export SPRING_PROFILES_ACTIVE=dev
mvn spring-boot:run
```

### Option 3: IDE Configuration
Set the active profile in your IDE's run configuration:
- **IntelliJ IDEA**: Run > Edit Configurations > Active profiles: `dev`
- **Eclipse**: Run > Run Configurations > Arguments > Program arguments: `--spring.profiles.active=dev`

### Option 4: Application Properties
Add to `application.yml`:
```yaml
spring:
  profiles:
    active: dev
```

## Seeded Data

### Property
- **Name**: West Bethel Motel
- **Code**: WBM
- **Timezone**: America/New_York
- **Currency**: USD
- **Address**: 123 Mountain View Road, West Bethel, ME 04286
- **Contact**: info@westbethelmotel.com, +1-207-555-0100

### Room Types

| Code     | Name            | Capacity | Bed Configuration      | Base Rate | Count |
|----------|-----------------|----------|------------------------|-----------|-------|
| STANDARD | Standard Room   | 2        | 1 Queen Bed            | $89/night | 5     |
| DELUXE   | Deluxe Room     | 3        | 1 King Bed             | $129/night| 3     |
| SUITE    | Executive Suite | 4        | 1 King Bed + Sofa Bed  | $199/night| 2     |

#### Amenities
- **STANDARD**: WiFi, TV, Coffee Maker, Mini Fridge
- **DELUXE**: WiFi, Smart TV, Coffee Maker, Mini Fridge, Microwave, Work Desk
- **SUITE**: WiFi, Smart TV, Coffee Maker, Full Refrigerator, Microwave, Executive Work Desk, Separate Living Area, Whirlpool Tub

### Rooms

| Room Number | Floor | Type     | Status    | Housekeeping |
|-------------|-------|----------|-----------|--------------|
| 101-105     | 1     | STANDARD | AVAILABLE | CLEAN        |
| 201-203     | 2     | DELUXE   | AVAILABLE | CLEAN        |
| 301-302     | 3     | SUITE    | AVAILABLE | CLEAN        |

**Total Rooms**: 10 (5 Standard, 3 Deluxe, 2 Suite)

### Test Guests

| Email                     | Customer Number | Phone          | Loyalty Status        | Address              |
|---------------------------|-----------------|----------------|-----------------------|----------------------|
| john.doe@example.com      | CUST-001        | +1-555-100-0001| None                  | Portland, ME         |
| jane.smith@example.com    | CUST-002        | +1-555-200-0002| GOLD (2500 points)    | Bethel, ME           |
| bob.jones@example.com     | CUST-003        | +1-555-300-0003| None                  | Augusta, ME          |

#### Guest Preferences
- **John Doe**: Non-smoking, Ground floor preferred
- **Jane Smith**: Quiet room, High floor, Extra pillows
- **Bob Jones**: King bed, Late checkout if available

### Rate Plans

#### 1. Standard Rate
- **Channel**: DIRECT
- **Eligible Room Types**: All (STANDARD, DELUXE, SUITE)
- **Pricing**: Uses room type base rates
- **Cancellation Policy**: Free cancellation up to 24 hours before check-in
- **Stay Restrictions**: Min 1 night, Max 30 nights

#### 2. Weekend Special
- **Channel**: DIRECT
- **Eligible Room Types**: STANDARD, DELUXE
- **Pricing**: 10% discount on Friday/Saturday, 15% for 2+ weekend nights
- **Cancellation Policy**: Free cancellation up to 48 hours before check-in
- **Stay Restrictions**: Min 2 nights (must include Friday or Saturday), Max 7 nights

## Seeded Entity Counts

After seeding completes, the database will contain:
- **1** Property
- **3** Room Types
- **10** Rooms
- **3** Guests
- **1** Loyalty Profile
- **2** Rate Plans

## Fixed UUIDs

The seeder uses fixed UUIDs for consistent testing:

```java
// Property
PROPERTY_ID = 00000000-0000-0000-0000-000000000001

// Room Types
STANDARD_ROOM_TYPE_ID = 00000000-0000-0000-0000-000000000010
DELUXE_ROOM_TYPE_ID   = 00000000-0000-0000-0000-000000000011
SUITE_ROOM_TYPE_ID    = 00000000-0000-0000-0000-000000000012

// Guests
GUEST_JOHN_ID = 00000000-0000-0000-0000-000000000020
GUEST_JANE_ID = 00000000-0000-0000-0000-000000000021
GUEST_BOB_ID  = 00000000-0000-0000-0000-000000000022

// Rate Plans
RATE_PLAN_STANDARD_ID = 00000000-0000-0000-0000-000000000030
RATE_PLAN_WEEKEND_ID  = 00000000-0000-0000-0000-000000000031

// Loyalty Profiles
LOYALTY_JANE_ID = 00000000-0000-0000-0000-000000000040
```

These fixed UUIDs make it easy to reference test data in integration tests and API testing tools like Postman.

## Idempotency

The seeder checks if data already exists before seeding:
- It looks for a property with code "WBM"
- If found, seeding is skipped to prevent duplicates
- To re-seed, drop and recreate the database

## Logging

When seeding runs, you'll see detailed logs:
```
========================================
Starting Database Seeding for Development
========================================
Seeding Property...
  - Seeded property: West Bethel Motel (WBM)
Seeding Room Types...
  - Seeded 3 room types: STANDARD, DELUXE, SUITE
Seeding Rooms...
  - Seeded 10 rooms: 5 STANDARD, 3 DELUXE, 2 SUITE
Seeding Guests...
  - Seeded 3 guests: john.doe@example.com, jane.smith@example.com, bob.jones@example.com
Seeding Loyalty Profiles...
  - Seeded 1 loyalty profile: Jane Smith (GOLD tier, 2500 points)
Seeding Rate Plans...
  - Seeded 2 rate plans: Standard Rate, Weekend Special
========================================
Database Seeding Completed Successfully
========================================
```

## Dependencies

The seeder uses the following repositories:
- `PropertyRepository`
- `RoomTypeRepository`
- `RoomRepository`
- `GuestRepository`
- `LoyaltyProfileRepository`
- `RatePlanRepository`

All dependencies are automatically injected via constructor injection.

## Disabling Seeding

To run the application without seeding:
1. Don't activate the `dev` profile
2. Use default profile or other profiles (e.g., `prod`, `test`)

```bash
mvn spring-boot:run
# OR
mvn spring-boot:run -Dspring-boot.run.profiles=prod
```

## Integration Testing

The seeded data is ideal for:
- **Manual API Testing**: Use the fixed UUIDs in Postman/cURL requests
- **Integration Tests**: Reference known entities by their fixed UUIDs
- **UI Development**: Consistent data for frontend development
- **Demo Environments**: Realistic data for demonstrations

## Example API Requests

### Search Available Rooms
```bash
curl -X GET "http://localhost:8080/api/availability/search?propertyId=00000000-0000-0000-0000-000000000001&checkIn=2024-12-01&checkOut=2024-12-03"
```

### Create a Booking for John Doe
```bash
curl -X POST "http://localhost:8080/api/bookings" \
  -H "Content-Type: application/json" \
  -d '{
    "guestId": "00000000-0000-0000-0000-000000000020",
    "propertyId": "00000000-0000-0000-0000-000000000001",
    "roomTypeId": "00000000-0000-0000-0000-000000000010",
    "checkIn": "2024-12-01",
    "checkOut": "2024-12-03",
    "adults": 2
  }'
```

## Troubleshooting

### Seeding Doesn't Run
- **Check Profile**: Ensure `dev` profile is active
- **Check Logs**: Look for "Starting Database Seeding" message
- **Database State**: If data exists, seeding is skipped

### Database Errors
- **Foreign Key Violations**: Ensure Flyway migrations are up to date
- **Constraint Violations**: Check that the database schema matches entity definitions

### To Reset Data
```bash
# Stop the application
# Drop and recreate the database
dropdb motel_booking
createdb motel_booking

# Restart with dev profile
mvn spring-boot:run -Dspring-boot.run.profiles=dev
```

## Future Enhancements

Potential improvements to the seeding functionality:
- Add more diverse guest profiles
- Seed sample bookings and reservations
- Add historical data for reporting tests
- Create different seeding profiles (minimal, full, performance-test)
- Add command-line arguments to customize seeding
