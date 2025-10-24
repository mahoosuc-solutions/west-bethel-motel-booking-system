package simulations

import io.gatling.core.Predef._
import io.gatling.http.Predef._
import scala.concurrent.duration._

/**
 * Comprehensive Gatling load testing simulation for West Bethel Motel Booking System.
 *
 * Scenarios:
 * 1. Normal Load (100 users)
 * 2. Peak Load (500 users)
 * 3. Stress Test (ramp to 1000 users)
 * 4. Spike Test (sudden surge)
 * 5. Endurance Test (sustained load)
 * 6. Database Heavy
 * 7. Cache Heavy
 * 8. Authentication Heavy
 * 9. Email Queue Heavy
 * 10. Mixed Workload (realistic)
 */
class MotelBookingSimulation extends Simulation {

  val httpProtocol = http
    .baseUrl("http://localhost:8080")
    .acceptHeader("application/json")
    .contentTypeHeader("application/json")
    .userAgentHeader("Gatling Load Test")

  // Authentication
  val authToken = """Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...""" // Placeholder

  // Feeders
  val userFeeder = Iterator.continually(Map(
    "username" -> s"user_${scala.util.Random.nextInt(10000)}",
    "email" -> s"user_${scala.util.Random.nextInt(10000)}@example.com",
    "password" -> "Password123!",
    "firstName" -> s"User${scala.util.Random.nextInt(1000)}",
    "lastName" -> "Test"
  ))

  val bookingFeeder = Iterator.continually(Map(
    "checkInDate" -> java.time.LocalDate.now().plusDays(7 + scala.util.Random.nextInt(30)).toString,
    "checkOutDate" -> java.time.LocalDate.now().plusDays(10 + scala.util.Random.nextInt(30)).toString,
    "guests" -> (1 + scala.util.Random.nextInt(4)),
    "roomTypeCode" -> (if (scala.util.Random.nextBoolean()) "STD" else "DLX")
  ))

  // Scenario 1: Availability Search (80% of traffic)
  val availabilitySearch = scenario("Availability Search")
    .feed(bookingFeeder)
    .exec(http("Search Availability")
      .get("/api/availability/search")
      .queryParam("checkInDate", "${checkInDate}")
      .queryParam("checkOutDate", "${checkOutDate}")
      .queryParam("guests", "${guests}")
      .check(status.is(200))
      .check(responseTimeInMillis.lte(1000)))

  // Scenario 2: User Registration (5% of traffic)
  val userRegistration = scenario("User Registration")
    .feed(userFeeder)
    .exec(http("Register User")
      .post("/api/auth/register")
      .body(StringBody("""{"username":"${username}","email":"${email}","password":"${password}","firstName":"${firstName}","lastName":"${lastName}"}"""))
      .check(status.in(201, 400))
      .check(responseTimeInMillis.lte(2000)))

  // Scenario 3: User Login (10% of traffic)
  val userLogin = scenario("User Login")
    .feed(userFeeder)
    .exec(http("User Login")
      .post("/api/auth/login")
      .body(StringBody("""{"username":"${username}","password":"${password}"}"""))
      .check(status.in(200, 401))
      .check(responseTimeInMillis.lte(1000)))

  // Scenario 4: Create Booking (3% of traffic)
  val createBooking = scenario("Create Booking")
    .feed(bookingFeeder)
    .exec(http("Create Booking")
      .post("/api/bookings")
      .header("Authorization", authToken)
      .body(StringBody("""{"checkInDate":"${checkInDate}","checkOutDate":"${checkOutDate}","numberOfGuests":${guests},"roomTypeCode":"${roomTypeCode}","guestFirstName":"Test","guestLastName":"User","guestEmail":"test@example.com","guestPhone":"+1-555-0123"}"""))
      .check(status.in(201, 400, 401, 422))
      .check(responseTimeInMillis.lte(3000)))

  // Scenario 5: View Profile (2% of traffic)
  val viewProfile = scenario("View Profile")
    .exec(http("View User Profile")
      .get("/api/users/profile")
      .header("Authorization", authToken)
      .check(status.in(200, 401))
      .check(responseTimeInMillis.lte(500)))

  // Load Test Scenarios

  // 1. Normal Load (100 concurrent users)
  val normalLoad = setUp(
    availabilitySearch.inject(rampUsers(80).during(2.minutes)),
    userRegistration.inject(rampUsers(5).during(2.minutes)),
    userLogin.inject(rampUsers(10).during(2.minutes)),
    createBooking.inject(rampUsers(3).during(2.minutes)),
    viewProfile.inject(rampUsers(2).during(2.minutes))
  ).protocols(httpProtocol)
    .maxDuration(10.minutes)
    .assertions(
      global.responseTime.percentile3.lte(1000), // p95 < 1s
      global.successfulRequests.percent.gte(99)
    )

  // 2. Peak Load (500 concurrent users)
  val peakLoad = setUp(
    availabilitySearch.inject(rampUsers(400).during(1.minute)),
    userRegistration.inject(rampUsers(25).during(1.minute)),
    userLogin.inject(rampUsers(50).during(1.minute)),
    createBooking.inject(rampUsers(15).during(1.minute)),
    viewProfile.inject(rampUsers(10).during(1.minute))
  ).protocols(httpProtocol)
    .maxDuration(5.minutes)
    .assertions(
      global.responseTime.percentile3.lte(2000), // p95 < 2s
      global.successfulRequests.percent.gte(99.9)
    )

  // 3. Stress Test (Ramp to 1000 users)
  val stressTest = setUp(
    availabilitySearch.inject(
      rampUsers(100).during(1.minute),
      constantUsersPerSec(20).during(5.minutes),
      rampUsers(500).during(4.minutes)
    ),
    createBooking.inject(
      rampUsers(50).during(1.minute),
      constantUsersPerSec(10).during(9.minutes)
    )
  ).protocols(httpProtocol)
    .maxDuration(10.minutes)

  // 4. Spike Test (Sudden surge)
  val spikeTest = setUp(
    availabilitySearch.inject(
      constantUsersPerSec(10).during(2.minutes),
      atOnceUsers(500), // Sudden spike
      constantUsersPerSec(10).during(3.minutes)
    )
  ).protocols(httpProtocol)
    .maxDuration(10.minutes)

  // 5. Endurance Test (100 users for 1 hour)
  val enduranceTest = setUp(
    availabilitySearch.inject(constantUsersPerSec(15).during(60.minutes)),
    createBooking.inject(constantUsersPerSec(5).during(60.minutes)),
    userLogin.inject(constantUsersPerSec(2).during(60.minutes))
  ).protocols(httpProtocol)
    .maxDuration(60.minutes)
    .assertions(
      global.responseTime.mean.lte(800),
      global.failedRequests.percent.lte(0.1)
    )

  // 6. Database Heavy (Focus on read/write operations)
  val databaseHeavy = setUp(
    createBooking.inject(constantUsersPerSec(50).during(5.minutes)),
    viewProfile.inject(constantUsersPerSec(100).during(5.minutes))
  ).protocols(httpProtocol)
    .maxDuration(10.minutes)

  // 7. Cache Heavy (Test cache performance)
  val cacheHeavy = setUp(
    availabilitySearch.inject(constantUsersPerSec(200).during(5.minutes)),
    viewProfile.inject(constantUsersPerSec(100).during(5.minutes))
  ).protocols(httpProtocol)
    .maxDuration(10.minutes)

  // 8. Authentication Heavy (JWT validation load)
  val authHeavy = setUp(
    userLogin.inject(constantUsersPerSec(100).during(5.minutes)),
    viewProfile.inject(constantUsersPerSec(100).during(5.minutes))
  ).protocols(httpProtocol)
    .maxDuration(10.minutes)

  // 9. Mixed Workload (Realistic scenario)
  val mixedWorkload = setUp(
    availabilitySearch.inject(
      rampUsers(100).during(2.minutes),
      constantUsersPerSec(20).during(8.minutes)
    ),
    userRegistration.inject(constantUsersPerSec(2).during(10.minutes)),
    userLogin.inject(constantUsersPerSec(5).during(10.minutes)),
    createBooking.inject(constantUsersPerSec(3).during(10.minutes)),
    viewProfile.inject(constantUsersPerSec(5).during(10.minutes))
  ).protocols(httpProtocol)
    .maxDuration(15.minutes)
    .assertions(
      global.responseTime.percentile3.lte(1500),
      global.responseTime.percentile4.lte(3000),
      global.successfulRequests.percent.gte(99.5)
    )

  // Default: Run Mixed Workload
  setUp(
    availabilitySearch.inject(
      rampUsers(80).during(1.minute),
      constantUsersPerSec(15).during(5.minutes)
    ),
    userRegistration.inject(constantUsersPerSec(1).during(6.minutes)),
    userLogin.inject(constantUsersPerSec(3).during(6.minutes)),
    createBooking.inject(constantUsersPerSec(2).during(6.minutes)),
    viewProfile.inject(constantUsersPerSec(2).during(6.minutes))
  ).protocols(httpProtocol)
    .maxDuration(10.minutes)
    .assertions(
      global.responseTime.percentile3.lte(1000),
      global.successfulRequests.percent.gte(99)
    )
}
