# West Bethel Motel Booking System

Spring Boot rewrite of the West Bethel Motel booking platform. The project is structured as a modular monolith with domain-driven packages for inventory, reservations, pricing, billing, loyalty, reporting, and integrations.

## Getting Started

1. Ensure Java 17+ and Maven 3.9+ are installed.
2. Build the project (dependencies will be downloaded on the first run):
   ```bash
   mvn clean verify
   ```
3. Run the application locally:
   ```bash
   mvn spring-boot:run
   ```
4. The API will be available at `http://localhost:8080`.

> **Note:** The default configuration expects PostgreSQL and Redis instances; adjust `src/main/resources/application.yml` or provide overrides via environment variables before connecting to real services.

## Running Tests

Integration tests cover availability, reservation, and billing flows via MockMvc. Execute:
```bash
mvn test
```

## Container Image

Build and run the Docker image:
```bash
docker build -t west-bethel/motel-booking .
docker run --rm -p 8080:8080 west-bethel/motel-booking
```

## Continuous Integration

A sample GitHub Actions workflow (`.github/workflows/build.yml`) is provided to build and test the application on every push. Adjust Java or caching strategies as needed for your environment.

## Next Steps

- Connect to real payment, notification, and loyalty providers by replacing the simulated adapters.
- Harden security (JWT/OAuth2), add audit logging hooks, and enforce multitenant data filtering.
- Expand reporting catalogue and build UI dashboards for front-desk and management teams.
