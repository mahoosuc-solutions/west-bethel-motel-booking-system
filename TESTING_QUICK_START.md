# Testing Quick Start Guide
**West Bethel Motel Booking System**

## Running Tests

### Run All Tests
```bash
mvn clean test
```

### Run Specific Test Class
```bash
mvn test -Dtest=AvailabilityControllerTest
mvn test -Dtest=BookingControllerTest
mvn test -Dtest=PaymentControllerTest
```

### Run All Controller Tests
```bash
mvn test -Dtest=*ControllerTest
```

### Run with Coverage Report
```bash
mvn clean test jacoco:report
```
Coverage report location: `target/site/jacoco/index.html`

---

## Test Structure

```
src/test/java/
├── com/westbethel/motel_booking/
│   ├── testutil/
│   │   ├── BaseIntegrationTest.java      # Base class for all tests
│   │   └── TestDataBuilder.java          # Test data builders
│   ├── availability/api/
│   │   └── AvailabilityControllerTest.java  (27 tests)
│   ├── reservation/api/
│   │   └── BookingControllerTest.java       (29 tests)
│   └── billing/api/
│       └── PaymentControllerTest.java       (31 tests)
```

---

## Quick Test Examples

### Creating Test Data
```java
// Use TestDataBuilder for consistent test data
Property property = TestDataBuilder.createProperty();
RoomType roomType = TestDataBuilder.createRoomType(property.getId());
Guest guest = TestDataBuilder.createGuest();
DateRange dates = DateRange.futureRange(7, 2); // 7 days from now, 2 nights
```

### Writing a New Test
```java
@Test
@DisplayName("Should do something useful")
void testSomething() throws Exception {
    // Arrange - Create test data
    Property property = propertyRepository.save(TestDataBuilder.createProperty());

    // Act - Execute API call
    mockMvc.perform(get("/api/v1/endpoint")
            .param("propertyId", property.getId().toString()))

    // Assert - Verify response
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.field").value("expected"));
}
```

---

## Test Categories

### 1. AvailabilityControllerTest (27 tests)
- **Endpoint**: `GET /api/v1/availability`
- **Tests**: Room availability searches, filtering, validation
- **Key Scenarios**:
  - Full availability
  - Partial availability
  - No availability
  - Date validation
  - Room type filtering

### 2. BookingControllerTest (29 tests)
- **Endpoints**:
  - `POST /api/v1/reservations`
  - `POST /api/v1/reservations/{confirmationNumber}/cancel`
- **Tests**: Booking creation, cancellation, validation
- **Key Scenarios**:
  - Successful booking creation
  - Booking cancellation
  - Validation errors
  - Business rule violations

### 3. PaymentControllerTest (31 tests)
- **Endpoints**:
  - `POST /api/v1/invoices/{invoiceId}/payments/authorize`
  - `POST /api/v1/payments/{paymentId}/capture`
  - `POST /api/v1/payments/{paymentId}/refund`
  - `POST /api/v1/payments/{paymentId}/void`
- **Tests**: Payment flows, state transitions, validation
- **Key Scenarios**:
  - Authorize payment
  - Capture payment
  - Refund payment
  - Void payment
  - Split payments

---

## Common Test Patterns

### Testing HTTP GET
```java
mockMvc.perform(get("/api/v1/endpoint")
        .param("key", "value"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.field").value("expected"));
```

### Testing HTTP POST
```java
mockMvc.perform(post("/api/v1/endpoint")
        .contentType(MediaType.APPLICATION_JSON)
        .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").exists());
```

### Testing Validation Errors
```java
mockMvc.perform(post("/api/v1/endpoint")
        .contentType(MediaType.APPLICATION_JSON)
        .content("{invalid json}"))
        .andExpect(status().isBadRequest());
```

### Verifying Database State
```java
// Create entity
mockMvc.perform(post("/api/v1/reservations")...);

// Verify it was saved
Booking saved = bookingRepository.findByReference(confirmationNumber).orElseThrow();
assertThat(saved.getStatus()).isEqualTo(BookingStatus.CONFIRMED);
```

---

## Troubleshooting

### Tests Fail with "JAVA_HOME not defined"
**Solution**: Ensure Java 17+ is installed and JAVA_HOME is set
```bash
export JAVA_HOME=/path/to/java17
export PATH=$JAVA_HOME/bin:$PATH
```

### Tests Fail with Database Errors
**Solution**: Ensure Flyway migrations are present in `src/main/resources/db/migration/`

### Tests Fail with "Bean not found"
**Solution**: Check that all required components are annotated with Spring stereotypes (@Service, @Repository, etc.)

### Tests Are Slow
**Solution**: Tests use H2 in-memory database and should be fast. If slow:
- Check for missing database indexes
- Review n+1 query issues
- Disable logging in test configuration

---

## Test Data Management

### Using TestDataBuilder
The TestDataBuilder provides these methods:

```java
// Entities
Property createProperty()
RoomType createRoomType(UUID propertyId, String code, String name, int capacity, BigDecimal rate)
Room createRoom(UUID propertyId, UUID roomTypeId, String roomNumber)
Guest createGuest()
RatePlan createRatePlan(UUID propertyId, UUID roomTypeId, BigDecimal rate)
Booking createBooking(UUID propertyId, UUID guestId, UUID ratePlanId, LocalDate checkIn, LocalDate checkOut, BigDecimal amount)

// Utilities
Money createMoney(BigDecimal amount)
DateRange.futureRange(int daysFromNow, int nights)
DateRange.pastRange(int daysAgo, int nights)
```

### Test Isolation
All tests extend `BaseIntegrationTest` which:
- Cleans database before each test
- Provides autowired MockMvc
- Provides autowired repositories
- Ensures test independence

---

## Best Practices

### DO
- ✅ Use TestDataBuilder for creating test data
- ✅ Follow Arrange-Act-Assert pattern
- ✅ Use @DisplayName for readable test names
- ✅ Test both happy paths and error cases
- ✅ Verify database state after operations
- ✅ Keep tests independent
- ✅ Use @Nested for logical grouping

### DON'T
- ❌ Share mutable state between tests
- ❌ Rely on test execution order
- ❌ Hard-code UUIDs or dates
- ❌ Skip error case testing
- ❌ Test multiple scenarios in one test
- ❌ Mock repositories in integration tests
- ❌ Ignore test failures

---

## Adding New Tests

### 1. Extend BaseIntegrationTest
```java
@DisplayName("My Controller Integration Tests")
class MyControllerTest extends BaseIntegrationTest {

    @Autowired
    private ObjectMapper objectMapper;

    // Your tests here
}
```

### 2. Add Test Data Builders (if needed)
Add methods to TestDataBuilder.java for new entities

### 3. Write Tests Using Nested Classes
```java
@Nested
@DisplayName("Feature Category")
class FeatureTests {

    @Test
    @DisplayName("Should do something specific")
    void testSomething() {
        // Test implementation
    }
}
```

### 4. Verify Coverage
```bash
mvn clean test jacoco:report
open target/site/jacoco/index.html
```

---

## CI/CD Integration

### GitHub Actions Example
```yaml
name: Tests

on: [push, pull_request]

jobs:
  test:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v2
      - name: Set up JDK 17
        uses: actions/setup-java@v2
        with:
          java-version: '17'
      - name: Run tests
        run: mvn clean test
      - name: Generate coverage report
        run: mvn jacoco:report
      - name: Upload coverage
        uses: codecov/codecov-action@v2
```

---

## Coverage Goals

- **Controller Layer**: 95%+
- **Service Layer**: 85%+
- **Repository Layer**: 90%+
- **Overall**: 85%+

---

## Resources

- **Full Documentation**: See `src/test/java/README.md`
- **Detailed Report**: See `TEST_ENHANCEMENT_REPORT.md`
- **Spring Boot Testing Guide**: https://spring.io/guides/gs/testing-web/
- **AssertJ Documentation**: https://assertj.github.io/doc/

---

## Quick Commands Reference

```bash
# Build project
mvn clean install

# Run all tests
mvn test

# Run single test class
mvn test -Dtest=ClassName

# Run single test method
mvn test -Dtest=ClassName#methodName

# Run with coverage
mvn clean test jacoco:report

# Skip tests
mvn install -DskipTests

# Run tests in parallel
mvn test -T 4

# Run only failed tests
mvn test -Dsurefire.rerunFailingTestsCount=2

# Debug tests
mvn test -Dmaven.surefire.debug

# Generate test report
mvn surefire-report:report
```

---

**Need Help?** Check the full documentation in `src/test/java/README.md` or review existing tests for examples.
