# Test Data Quick Reference

## Quick Start
```bash
# Enable seeding and run
mvn spring-boot:run -Dspring-boot.run.profiles=dev
```

## Fixed Test UUIDs

### Property
```
WBM Property: 00000000-0000-0000-0000-000000000001
```

### Room Types
```
STANDARD: 00000000-0000-0000-0000-000000000010  ($89/night, 2 guests, Queen)
DELUXE:   00000000-0000-0000-0000-000000000011  ($129/night, 3 guests, King)
SUITE:    00000000-0000-0000-0000-000000000012  ($199/night, 4 guests, King+Sofa)
```

### Test Guests
```
John Doe:    00000000-0000-0000-0000-000000000020  (john.doe@example.com)
Jane Smith:  00000000-0000-0000-0000-000000000021  (jane.smith@example.com, GOLD loyalty)
Bob Jones:   00000000-0000-0000-0000-000000000022  (bob.jones@example.com)
```

### Rate Plans
```
Standard Rate:    00000000-0000-0000-0000-000000000030  (DIRECT, all rooms)
Weekend Special:  00000000-0000-0000-0000-000000000031  (DIRECT, STANDARD/DELUXE)
```

### Loyalty Profiles
```
Jane's Loyalty:   00000000-0000-0000-0000-000000000040  (GOLD, 2500 points)
```

## Available Rooms
```
Floor 1: 101, 102, 103, 104, 105 (STANDARD)
Floor 2: 201, 202, 203 (DELUXE)
Floor 3: 301, 302 (SUITE)
```

## Common Test Scenarios

### 1. Book a Standard Room for John
```json
{
  "guestId": "00000000-0000-0000-0000-000000000020",
  "propertyId": "00000000-0000-0000-0000-000000000001",
  "roomTypeId": "00000000-0000-0000-0000-000000000010",
  "checkIn": "2024-12-01",
  "checkOut": "2024-12-03",
  "adults": 2
}
```

### 2. Book a Suite for Jane (Loyalty Member)
```json
{
  "guestId": "00000000-0000-0000-0000-000000000021",
  "propertyId": "00000000-0000-0000-0000-000000000001",
  "roomTypeId": "00000000-0000-0000-0000-000000000012",
  "checkIn": "2024-12-15",
  "checkOut": "2024-12-18",
  "adults": 3
}
```

### 3. Weekend Special Booking for Bob
```json
{
  "guestId": "00000000-0000-0000-0000-000000000022",
  "propertyId": "00000000-0000-0000-0000-000000000001",
  "roomTypeId": "00000000-0000-0000-0000-000000000011",
  "ratePlanId": "00000000-0000-0000-0000-000000000031",
  "checkIn": "2024-12-06",
  "checkOut": "2024-12-08",
  "adults": 2
}
```

## Guest Contact Info

| Guest      | Email                    | Phone            | Customer #  |
|------------|--------------------------|------------------|-------------|
| John Doe   | john.doe@example.com     | +1-555-100-0001  | CUST-001    |
| Jane Smith | jane.smith@example.com   | +1-555-200-0002  | CUST-002    |
| Bob Jones  | bob.jones@example.com    | +1-555-300-0003  | CUST-003    |

## Property Details
- **Code**: WBM
- **Name**: West Bethel Motel
- **Timezone**: America/New_York
- **Currency**: USD
- **Email**: info@westbethelmotel.com
- **Phone**: +1-207-555-0100

## Pricing Summary
- Standard Room: $89/night
- Deluxe Room: $129/night
- Executive Suite: $199/night

## Rate Plan Features
- **Standard Rate**: Base pricing, 24-hour cancellation
- **Weekend Special**: 10-15% discount, 48-hour cancellation, 2-night minimum
