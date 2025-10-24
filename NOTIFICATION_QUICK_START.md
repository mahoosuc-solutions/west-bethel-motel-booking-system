# Email Notification System - Quick Start Guide

## Setup (5 Minutes)

### 1. Configure Environment Variables

Add to your `.env` file:

```bash
# Email Configuration (Required)
MAIL_HOST=smtp.gmail.com
MAIL_PORT=587
MAIL_USERNAME=your-email@gmail.com
MAIL_PASSWORD=your-app-password

# Notification Settings
NOTIFICATION_FROM_ADDRESS=noreply@westbethelmotel.com
NOTIFICATION_FROM_NAME=West Bethel Motel

# Frontend URL (for email links)
FRONTEND_URL=http://localhost:3000
```

### 2. Gmail Setup (Development)

1. Enable 2FA on Google account
2. Generate App Password: Google Account → Security → App Passwords
3. Use 16-character password in `MAIL_PASSWORD`

### 3. Run Database Migration

```bash
./mvnw flyway:migrate
```

This creates the `notification_preferences` table.

---

## Using the System

### Send Welcome Email (from any service)

```java
@Service
public class UserService {
    private final ApplicationEventPublisher eventPublisher;

    public void registerUser(User user, String verificationToken) {
        // ... create user ...

        eventPublisher.publishEvent(UserRegisteredEvent.builder()
            .email(user.getEmail())
            .firstName(user.getFirstName())
            .lastName(user.getLastName())
            .verificationLink("http://localhost:3000/verify?token=" + verificationToken)
            .build());
    }
}
```

### Send Booking Confirmation

```java
eventPublisher.publishEvent(BookingCreatedEvent.builder()
    .email(customer.getEmail())
    .firstName(customer.getFirstName())
    .confirmationNumber(booking.getConfirmationNumber())
    .roomType(booking.getRoomType())
    .checkInDate(booking.getCheckInDate())
    .checkOutDate(booking.getCheckOutDate())
    .numberOfNights(booking.getNumberOfNights())
    .numberOfGuests(booking.getNumberOfGuests())
    .totalAmount(booking.getTotalAmount())
    .build());
```

### Send Security Alert (Agent 1 Integration)

```java
@Service
public class PasswordResetService {
    private final SecurityEmailService securityEmailService;

    public void resetPassword(String email, String token) {
        securityEmailService.sendPasswordResetEmail(email, token);
    }
}
```

---

## Available Events

1. **UserRegisteredEvent** - Welcome email with verification
2. **EmailVerificationRequestedEvent** - Email verification
3. **PasswordResetRequestedEvent** - Password reset link
4. **PasswordChangedEvent** - Password change confirmation
5. **BookingCreatedEvent** - Booking confirmation
6. **BookingCancelledEvent** - Cancellation notice
7. **PaymentReceivedEvent** - Payment receipt
8. **PaymentFailedEvent** - Payment failure alert
9. **LoyaltyPointsEarnedEvent** - Loyalty points update
10. **SecurityAlertEvent** - Security notifications

---

## Admin Panel

Access admin endpoints (requires ADMIN role):

```bash
# View queue status
GET /api/v1/admin/emails/queue/status

# View failed emails
GET /api/v1/admin/emails/failed

# Retry failed email
POST /api/v1/admin/emails/retry/{id}

# Send test email
POST /api/v1/admin/emails/test
{
  "to": "test@example.com",
  "subject": "Test",
  "templateName": "booking-confirmation",
  "templateVariables": {
    "firstName": "John",
    "confirmationNumber": "TEST-001",
    ...
  }
}
```

---

## User Preferences

Users can manage their notification preferences:

```bash
# Get preferences
GET /api/v1/users/me/notification-preferences

# Update preferences
PUT /api/v1/users/me/notification-preferences
{
  "emailEnabled": true,
  "bookingConfirmations": true,
  "paymentReceipts": true,
  "loyaltyUpdates": false,
  "promotionalEmails": false
}

# Reset to defaults
POST /api/v1/users/me/notification-preferences/reset
```

**Note:** Security alerts cannot be disabled by users.

---

## Email Templates

10 professionally designed templates available:

1. `welcome-email` - User registration
2. `email-verification` - Email verification
3. `password-reset` - Password reset
4. `password-changed` - Password change alert
5. `booking-confirmation` - Booking details
6. `booking-cancelled` - Cancellation
7. `payment-receipt` - Payment confirmation
8. `payment-failed` - Payment failure
9. `loyalty-points-earned` - Loyalty update
10. `security-alert` - Security notifications

All templates are:
- Responsive (mobile-friendly)
- Branded (West Bethel Motel colors)
- Accessible (high contrast, alt text)
- Professional (modern design)

---

## Troubleshooting

### Emails not sending?

1. Check environment variables are set
2. Test SMTP connection:
   ```bash
   curl -X POST localhost:8080/api/v1/admin/emails/test \
     -H "Content-Type: application/json" \
     -d '{"to":"your-email@test.com","subject":"Test","body":"Test"}'
   ```
3. Check failed emails:
   ```bash
   curl localhost:8080/api/v1/admin/emails/failed
   ```

### Queue growing too large?

1. Check queue status:
   ```bash
   curl localhost:8080/api/v1/admin/emails/queue/status
   ```
2. Retry failed emails:
   ```bash
   curl -X POST localhost:8080/api/v1/admin/emails/retry-all
   ```

### Template not found?

1. Verify template exists in `src/main/resources/templates/email/`
2. Check template name matches exactly (case-sensitive)
3. Restart application

---

## Production Deployment

### Recommended SMTP Provider: SendGrid

```bash
MAIL_HOST=smtp.sendgrid.net
MAIL_PORT=587
MAIL_USERNAME=apikey
MAIL_PASSWORD=SG.your-api-key-here
```

Free tier: 100 emails/day

### Alternative: AWS SES

```bash
MAIL_HOST=email-smtp.us-east-1.amazonaws.com
MAIL_PORT=587
MAIL_USERNAME=your-aws-smtp-username
MAIL_PASSWORD=your-aws-smtp-password
```

---

## Key Features

✅ Async email sending (non-blocking)
✅ Redis-backed queue with retry logic
✅ 10 professional HTML templates
✅ Event-driven architecture
✅ User preference management
✅ Admin monitoring panel
✅ Security integration ready
✅ Mobile-responsive emails

---

## Next Steps

1. Set up SMTP provider (Gmail for dev, SendGrid/SES for prod)
2. Configure environment variables
3. Test with admin panel test email endpoint
4. Integrate events into your services
5. Monitor queue in production

---

## Support

See full documentation: `EMAIL_NOTIFICATION_SYSTEM_REPORT.md`

For issues, check troubleshooting section or contact the development team.
