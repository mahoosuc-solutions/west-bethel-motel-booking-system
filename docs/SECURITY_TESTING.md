# Security Testing Documentation

## Overview

This document describes the comprehensive security testing framework for the West Bethel Motel Booking System. The security testing suite includes 100+ tests covering all major attack vectors and security requirements.

## Table of Contents

1. [Test Categories](#test-categories)
2. [Running Security Tests](#running-security-tests)
3. [Test Coverage](#test-coverage)
4. [Security Validations](#security-validations)
5. [OWASP Dependency Check](#owasp-dependency-check)
6. [Known Limitations](#known-limitations)
7. [Penetration Testing Guidelines](#penetration-testing-guidelines)
8. [CI/CD Integration](#cicd-integration)

## Test Categories

### 1. Input Validation Tests (40+ tests)
**Location:** `src/test/java/com/westbethel/motel_booking/security/InputValidationTest.java`

Tests comprehensive input validation for all DTOs including:
- Currency code validation (allowlist approach)
- Amount boundary testing
- Email format validation with SQL injection and XSS prevention
- Phone number international format validation
- UUID format validation
- Date range validation (check-in before check-out, min/max nights)
- Special character filtering
- Boundary value testing for all numeric fields
- Null and empty value handling
- Oversized input rejection

**Key Features:**
- Uses custom validators: `@ValidCurrency`, `@ValidDateRange`, `@ValidPhoneNumber`, `@ValidUUID`, `@NoSpecialCharacters`
- Parameterized tests using injection payload providers
- Covers all DTOs: PaymentAmountDto, GuestCreateRequest, BookingCreateRequest

### 2. Injection Security Tests (20+ tests)
**Location:** `src/test/java/com/westbethel/motel_booking/security/integration/InjectionSecurityTest.java`

Integration tests for injection attack prevention:
- SQL injection in all string fields
- XSS (Cross-Site Scripting) attempts
- Path traversal attacks
- Command injection
- LDAP injection
- XML injection
- JSON injection
- Null byte injection
- Mass assignment protection
- Overflow attacks
- Multiple simultaneous injection vectors

**Validation Points:**
- All request endpoints
- Query parameters
- Path variables
- Request body fields
- Error responses (no sensitive data leakage)

### 3. Password Security Tests (15+ tests)
**Location:** `src/test/java/com/westbethel/motel_booking/security/PasswordSecurityTest.java`

Comprehensive password security testing:
- BCrypt hashing (work factor 12)
- Same password produces different hashes (salt verification)
- Password strength requirements (8+ chars, uppercase, lowercase, digit, special char)
- Common password rejection
- Password history (prevent reuse)
- Timing attack prevention (constant-time comparison)
- Password reset token security

**Password Requirements:**
- Minimum 8 characters
- At least 1 uppercase letter
- At least 1 lowercase letter
- At least 1 digit
- At least 1 special character
- Not in common password list

### 4. Security Integration Tests (25+ tests)
**Location:** `src/test/java/com/westbethel/motel_booking/security/integration/SecurityIntegrationTest.java`

End-to-end security flow testing:
- Complete authentication flow
- Authorization with valid/invalid/expired tokens
- Token lifecycle (generation, validation, expiration, refresh)
- Session management and timeout
- CORS preflight and actual requests
- Security headers (X-Content-Type-Options, X-Frame-Options, HSTS, X-XSS-Protection)
- Content-Type validation
- Rate limiting integration
- Error handling (no sensitive data exposure)
- Account lockout after failed attempts
- Authorization matrix (user vs admin access)

## Running Security Tests

### Run All Security Tests
```bash
mvn test -Dtest="*Security*Test"
```

### Run Specific Test Categories
```bash
# Input validation tests
mvn test -Dtest=InputValidationTest

# Injection security tests
mvn test -Dtest=InjectionSecurityTest

# Password security tests
mvn test -Dtest=PasswordSecurityTest

# Integration tests
mvn test -Dtest=SecurityIntegrationTest
```

### Run with Coverage Report
```bash
mvn clean test jacoco:report
```
View report at: `target/site/jacoco/index.html`

### Run All Tests
```bash
mvn clean test
```

## Test Coverage

### Security Test Metrics

| Test Category | Number of Tests | Coverage Area |
|---------------|----------------|---------------|
| Input Validation | 40+ | All DTOs, custom validators |
| Injection Security | 20+ | SQL, XSS, Path Traversal, Command, etc. |
| Password Security | 15+ | Hashing, strength, history, timing |
| Security Integration | 25+ | Auth flows, CORS, headers, sessions |
| **TOTAL** | **100+** | **Comprehensive security coverage** |

### Code Coverage Targets
- Overall line coverage: 60%+
- Security-critical code: 80%+
- Custom validators: 100%
- Input sanitization: 100%

## Security Validations

### 1. Currency Injection Prevention

**Problem:** Currency field was vulnerable to injection attacks.

**Solution:**
- Created `SupportedCurrency` enum with allowlist (USD, EUR, GBP, CAD)
- Added `@ValidCurrency` custom validator
- Enhanced `PaymentAmountDto` with comprehensive validation
- Added boundary testing for amounts

**Tests:** 5+ tests in InputValidationTest

### 2. Input Sanitization

**Service:** `InputSanitizer.java`

Provides methods to:
- Sanitize HTML (remove scripts, dangerous tags, event handlers)
- Sanitize SQL (escape special characters, remove comments)
- Sanitize file paths (prevent directory traversal)
- Validate URLs (allow only http/https)
- Remove dangerous special characters
- Detect injection attempts (SQL, XSS, path traversal, command)

**Usage:**
```java
@Autowired
private InputSanitizer inputSanitizer;

// Check for injection
if (inputSanitizer.containsSqlInjection(input)) {
    throw new ValidationException("Potential SQL injection detected");
}

// Sanitize HTML
String clean = inputSanitizer.sanitizeHtml(userInput);
```

### 3. Request Size Limits

**Configuration in application.yml:**
```yaml
server:
  max-http-header-size: 8KB
  tomcat:
    max-swallow-size: 10MB
    max-http-post-size: 10MB
    connection-timeout: 30s

spring.servlet.multipart:
  max-file-size: 10MB
  max-request-size: 10MB
```

### 4. Custom Validators

#### @ValidCurrency
- Validates currency codes against allowlist
- Prevents injection through currency field
- Automatically trims and validates length

#### @ValidDateRange
- Applied at class level (e.g., BookingCreateRequest)
- Validates check-in is before check-out
- Enforces minimum nights (1)
- Enforces maximum nights (90)

#### @ValidPhoneNumber
- Accepts international format (+country code)
- Validates against injection patterns
- Max length 20 characters
- Required flag available

#### @ValidUUID
- Validates UUID format (8-4-4-4-12)
- Prevents injection through UUID fields

#### @NoSpecialCharacters
- Two modes: STRICT (alphanumeric + spaces), RELAXED (+ basic punctuation)
- Blocks dangerous characters: ' " ; < > & | ` $ \ { } [ ]
- Prevents SQL injection, XSS, command injection

## OWASP Dependency Check

### Running Dependency Check
```bash
mvn dependency-check:check
```

### Configuration
- Fails build on CVSS score ≥ 7 (High/Critical vulnerabilities)
- Generates HTML and JSON reports
- Output: `target/dependency-check/dependency-check-report.html`

### Suppressing False Positives

Edit `owasp-suppressions.xml`:
```xml
<suppress>
    <notes>Reason for suppression</notes>
    <packageUrl regex="true">^pkg:maven/group/artifact@.*$</packageUrl>
    <cve>CVE-2024-XXXXX</cve>
</suppress>
```

### Updating Vulnerability Database
```bash
mvn dependency-check:update-only
```

## Known Limitations

### 1. Authentication/Authorization Infrastructure
- Test framework includes mock JWT token generation
- Actual JWT implementation needs to be integrated
- Tests use placeholder authentication

### 2. Rate Limiting
- Configuration exists in application.yml
- Implementation requires Redis-based rate limiter
- Tests provide framework for validation

### 3. CSRF Protection
- Tests prepared but require CSRF token implementation
- Spring Security CSRF needs configuration

### 4. Audit Logging
- Test framework prepared
- Actual audit logging implementation needed

## Penetration Testing Guidelines

### Recommended Tests

1. **Automated Scanning**
   - OWASP ZAP (Zed Attack Proxy)
   - Burp Suite Community Edition
   - Nikto web scanner

2. **Manual Testing**
   - Test all injection payloads from `InjectionPayloadProvider`
   - Verify rate limiting behavior
   - Test session management
   - Verify CORS policies
   - Check security headers

3. **Authentication Testing**
   - Brute force protection
   - Account lockout
   - Password reset flow
   - Token expiration

4. **Authorization Testing**
   - Vertical privilege escalation
   - Horizontal privilege escalation
   - Forced browsing
   - Missing function level access control

### Penetration Testing Checklist

- [ ] SQL Injection in all input fields
- [ ] XSS in all input and output
- [ ] CSRF protection on state-changing operations
- [ ] Authentication bypass attempts
- [ ] Session fixation/hijacking
- [ ] Insecure direct object references
- [ ] Missing access controls
- [ ] Security misconfiguration
- [ ] Sensitive data exposure
- [ ] Insufficient logging and monitoring
- [ ] Server-side request forgery (SSRF)
- [ ] XML external entities (XXE)
- [ ] Insecure deserialization
- [ ] Using components with known vulnerabilities

## CI/CD Integration

### GitHub Actions Example

```yaml
name: Security Tests

on: [push, pull_request]

jobs:
  security-tests:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v3
      - name: Set up JDK 17
        uses: actions/setup-java@v3
        with:
          java-version: '17'
      - name: Run Security Tests
        run: mvn test -Dtest="*Security*Test"
      - name: OWASP Dependency Check
        run: mvn dependency-check:check
      - name: Upload Dependency Check Report
        uses: actions/upload-artifact@v3
        with:
          name: dependency-check-report
          path: target/dependency-check/
```

### Jenkins Pipeline Example

```groovy
pipeline {
    agent any
    stages {
        stage('Security Tests') {
            steps {
                sh 'mvn test -Dtest="*Security*Test"'
            }
        }
        stage('OWASP Dependency Check') {
            steps {
                sh 'mvn dependency-check:check'
                publishHTML([
                    reportDir: 'target/dependency-check',
                    reportFiles: 'dependency-check-report.html',
                    reportName: 'OWASP Dependency Check'
                ])
            }
        }
        stage('Coverage Report') {
            steps {
                sh 'mvn jacoco:report'
                jacoco()
            }
        }
    }
}
```

## Test Automation Best Practices

1. **Run security tests on every commit**
2. **Fail fast on high-severity vulnerabilities**
3. **Keep dependency database updated weekly**
4. **Review and update suppression rules monthly**
5. **Maintain test coverage above 60%**
6. **Review security test results in code reviews**

## Security Testing Workflow

```
1. Developer writes code
   ↓
2. Run unit tests (including security tests)
   ↓
3. Run OWASP dependency check
   ↓
4. Commit code
   ↓
5. CI runs all security tests
   ↓
6. If tests pass → Deploy to staging
   ↓
7. Run penetration tests on staging
   ↓
8. If pen tests pass → Deploy to production
   ↓
9. Monitor security logs and alerts
```

## Reporting Security Issues

If you discover a security vulnerability:

1. **DO NOT** create a public GitHub issue
2. Email security@westbethelmotel.com with:
   - Description of the vulnerability
   - Steps to reproduce
   - Potential impact
   - Suggested fix (if available)
3. Wait for confirmation before disclosure

## Additional Resources

- [OWASP Top 10](https://owasp.org/www-project-top-ten/)
- [OWASP Testing Guide](https://owasp.org/www-project-web-security-testing-guide/)
- [Spring Security Documentation](https://docs.spring.io/spring-security/reference/)
- [Bean Validation Specification](https://beanvalidation.org/2.0/spec/)

## Version History

- v1.0.0 (2025-10-23): Initial security testing framework
  - 100+ security tests implemented
  - Input validation infrastructure
  - Injection attack prevention
  - Password security
  - OWASP dependency check integration
