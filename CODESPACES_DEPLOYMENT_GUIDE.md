# GitHub Codespaces Deployment Guide
## West Bethel Motel Booking System

**Last Updated:** 2025-10-24
**Estimated Setup Time:** 5-10 minutes (automatic)

---

## 📋 Pre-Deployment Checklist

Before deploying to Codespaces, run the validation script:

```bash
./validate-codespaces-setup.sh
```

**Expected Output:**
- ✅ All configuration files valid
- ✅ Feature flags implemented
- ✅ Documentation complete

---

## 🚀 Method 1: Deploy via GitHub Web UI (Recommended)

### Step 1: Navigate to Repository
1. Go to your GitHub repository:
   ```
   https://github.com/YOUR_USERNAME/west-bethel-motel-booking-system
   ```

### Step 2: Create Codespace
1. Click the green **"Code"** button (top right)
2. Select the **"Codespaces"** tab
3. Click **"Create codespace on main"**

**What happens next:**
- GitHub creates a container with all services
- Automatically installs Java 17, Maven, and tools
- Starts PostgreSQL, Redis, Mailhog, Prometheus, Grafana
- Installs all Maven dependencies
- Creates application properties for Codespaces
- Sets up quick reference scripts

**Wait Time:** 5-10 minutes for first creation
**Subsequent starts:** 30-60 seconds

### Step 3: Verify Automatic Setup
Once the Codespace opens, you'll see:
```
=========================================
Codespaces Post-Create Setup Starting...
=========================================
✓ Maven dependencies installed
✓ PostgreSQL is ready
✓ Redis is ready
✓ Database connection verified
✓ Codespaces application properties created
✓ Git configured
✓ Quick reference scripts created
=========================================
Codespaces Setup Complete!
=========================================
```

---

## 🚀 Method 2: Deploy via GitHub CLI

### Prerequisites
Install GitHub CLI:
```bash
# macOS
brew install gh

# Linux (Debian/Ubuntu)
curl -fsSL https://cli.github.com/packages/githubcli-archive-keyring.gpg | \
  sudo dd of=/usr/share/keyrings/githubcli-archive-keyring.gpg
echo "deb [arch=$(dpkg --print-architecture) signed-by=/usr/share/keyrings/githubcli-archive-keyring.gpg] \
  https://cli.github.com/packages stable main" | sudo tee /etc/apt/sources.list.d/github-cli.list > /dev/null
sudo apt update
sudo apt install gh

# Windows
winget install GitHub.cli
```

### Step 1: Authenticate
```bash
gh auth login
```

### Step 2: Create Codespace
```bash
# Create new codespace
gh codespace create --repo YOUR_USERNAME/west-bethel-motel-booking-system

# List codespaces
gh codespace list

# Open in browser
gh codespace code -c CODESPACE_NAME

# Or SSH into codespace
gh codespace ssh -c CODESPACE_NAME
```

---

## 🚀 Method 3: Deploy via VS Code

### Step 1: Install Extension
1. Open VS Code
2. Install "GitHub Codespaces" extension
3. Sign in to GitHub

### Step 2: Create Codespace
1. Open Command Palette (Cmd+Shift+P / Ctrl+Shift+P)
2. Select **"Codespaces: Create New Codespace"**
3. Choose your repository
4. Select branch (main)

---

## ✅ Post-Deployment Verification

### 1. Check Services Are Running

```bash
# Check PostgreSQL
pg_isready -h localhost -p 5432 -U postgres
# Expected: localhost:5432 - accepting connections

# Check Redis
redis-cli -h localhost -p 6379 -a devredispass ping
# Expected: PONG

# Check Mailhog
curl -s http://localhost:8025/ | grep -q "Mailhog" && echo "Mailhog OK"
# Expected: Mailhog OK
```

### 2. Start the Application

```bash
# Use the quick start script
./run-app.sh

# Or manually
mvn spring-boot:run -Dspring-boot.run.profiles=codespaces
```

**Expected Output:**
```
  .   ____          _            __ _ _
 /\\ / ___'_ __ _ _(_)_ __  __ _ \ \ \ \
( ( )\___ | '_ | '_| | '_ \/ _` | \ \ \ \
 \\/  ___)| |_)| | | | | || (_| |  ) ) ) )
  '  |____| .__|_| |_|_| |_\__, | / / / /
 =========|_|==============|___/=/_/_/_/
 :: Spring Boot ::                (v3.x.x)

Started MotelBookingApplication in X.XXX seconds
```

### 3. Verify Health

```bash
# Use the health check script
./check-health.sh

# Or manually
curl http://localhost:8080/actuator/health | jq .
```

**Expected Response:**
```json
{
  "status": "UP",
  "components": {
    "db": { "status": "UP" },
    "redis": { "status": "UP" },
    "diskSpace": { "status": "UP" },
    "ping": { "status": "UP" }
  }
}
```

### 4. Access Services

**Application:**
- URL: http://localhost:8080
- API Docs: http://localhost:8080/swagger-ui.html
- Health: http://localhost:8080/actuator/health

**Mailhog (Email Testing):**
- URL: http://localhost:8025
- SMTP: localhost:1025 (no authentication)

**Prometheus (Metrics):**
- URL: http://localhost:9090
- Targets: http://localhost:9090/targets

**Grafana (Dashboards):**
- URL: http://localhost:3000
- Login: admin / admin
- Datasource: Prometheus (pre-configured)

**PostgreSQL:**
```bash
psql -h localhost -U postgres -d motel_booking_dev
# Password: devpassword
```

**Redis:**
```bash
redis-cli -h localhost -p 6379 -a devredispass
```

---

## 🧪 Running Tests

### Run All Tests (792+ tests)
```bash
# Use the quick script
./run-tests.sh

# Or manually
mvn clean test
```

**Expected:** All 792+ tests pass with 90%+ coverage

### Run Specific Test Categories
```bash
# Unit tests
mvn test -Dtest=*Test

# Integration tests
mvn test -Dtest=*IT

# E2E tests
mvn test -Dtest=*E2ETest

# Security tests
mvn test -Dtest=*SecurityTest

# Load tests (K6)
k6 run k6-scripts/load-test.js
```

### Run Tests with Coverage
```bash
mvn clean test jacoco:report

# View report
open target/site/jacoco/index.html
```

---

## 📊 Testing Checklist

Complete this checklist to validate the Codespaces deployment:

### Infrastructure Tests
- [ ] PostgreSQL is running and accessible
- [ ] Redis is running and accessible
- [ ] Mailhog is running and accessible
- [ ] Prometheus is scraping metrics
- [ ] Grafana can connect to Prometheus

### Application Tests
- [ ] Application starts without errors
- [ ] Health check returns UP status
- [ ] All 792+ tests pass
- [ ] No critical compilation warnings
- [ ] Swagger UI is accessible

### Feature Tests
- [ ] User registration works
- [ ] User login works
- [ ] MFA setup and verification works
- [ ] Email notifications are sent (check Mailhog)
- [ ] Booking creation works
- [ ] Payment processing works (test mode)
- [ ] Admin features are accessible

### Performance Tests
- [ ] Application responds in < 1 second
- [ ] Cache hit ratio > 70%
- [ ] Database queries are optimized
- [ ] No memory leaks detected

### Security Tests
- [ ] JWT authentication works
- [ ] Token blacklist prevents reuse
- [ ] MFA is optional (not required by default)
- [ ] Session management works
- [ ] Password reset flow works
- [ ] Email verification works

---

## 🐛 Troubleshooting

### Problem: Codespace won't start
**Solution:**
1. Check GitHub status: https://www.githubstatus.com
2. Try creating a new Codespace
3. Check repository size (< 10GB recommended)

### Problem: Services not starting
**Solution:**
```bash
# Check Docker containers
docker ps

# Restart services
docker-compose -f .devcontainer/docker-compose.yml restart

# Check logs
docker-compose -f .devcontainer/docker-compose.yml logs
```

### Problem: Application won't start
**Solution:**
```bash
# Check Java version
java -version
# Should be 17+

# Check Maven
mvn -version

# Clean and rebuild
mvn clean install -DskipTests

# Check logs
tail -f logs/application.log
```

### Problem: Tests failing
**Solution:**
```bash
# Ensure database is accessible
psql -h localhost -U postgres -d motel_booking_dev

# Reset database
./scripts/reset-database.sh

# Clear Maven cache
mvn clean

# Run tests with debug logging
mvn test -X
```

### Problem: Cannot access services (ports)
**Solution:**
1. VS Code will show port forwarding notifications
2. Click "Open in Browser" when prompted
3. Or manually forward ports:
   - View → Command Palette → "Forward a Port"
   - Enter port number (8080, 8025, 3000, etc.)

### Problem: Out of memory
**Solution:**
```bash
# Check available memory
free -h

# Increase JVM memory (if needed)
export MAVEN_OPTS="-Xmx2g"
mvn spring-boot:run
```

---

## 📈 Next Steps After Successful Deployment

### 1. Explore the Application
- Review API documentation at `/swagger-ui.html`
- Test all major workflows (registration, login, booking)
- Check email notifications in Mailhog
- Review metrics in Grafana

### 2. Run Load Tests
```bash
# Install K6 if not already installed
brew install k6  # macOS
# or download from https://k6.io/

# Run load test
k6 run k6-scripts/load-test.js

# Run stress test
k6 run k6-scripts/stress-test.js
```

### 3. Complete Production Readiness Review
- Open `docs/PRODUCTION_READINESS_REVIEW.md`
- Complete all checklist items
- Address any issues found

### 4. Plan Staging Deployment
- Review `docs/deployment/DEPLOYMENT_GUIDE.md`
- Prepare GCP staging environment
- Schedule Launch Readiness Review

---

## 💡 Tips & Best Practices

### Performance
- Keep Codespace running to avoid cold starts
- Use `mvn spring-boot:run` for faster restarts
- Enable hot reload for development

### Testing
- Run tests frequently during development
- Focus on failing tests first
- Use test coverage reports to find gaps

### Monitoring
- Keep Grafana dashboards open while testing
- Watch for errors in Prometheus alerts
- Check application logs regularly

### Email Testing
- All emails go to Mailhog (port 8025)
- No real emails are sent in Codespaces
- Perfect for testing notification flows

---

## 📞 Support & Resources

### Quick Reference Scripts
```bash
./run-app.sh              # Start application
./run-tests.sh            # Run all tests
./check-health.sh         # Check application health
./validate-codespaces-setup.sh  # Validate configuration
```

### Documentation
- **Main Guide:** FAANG_RELEASE_PLAN_SUMMARY.md
- **API Docs:** API_DOCUMENTATION.md
- **Testing:** TESTING_QUICK_START.md
- **Deployment:** docs/deployment/DEPLOYMENT_GUIDE.md

### Monitoring URLs
- Application: http://localhost:8080
- Mailhog: http://localhost:8025
- Grafana: http://localhost:3000
- Prometheus: http://localhost:9090

### Database Access
```bash
# PostgreSQL
psql -h localhost -U postgres -d motel_booking_dev
# Password: devpassword

# Redis
redis-cli -h localhost -p 6379 -a devredispass
```

---

## ✅ Deployment Success Criteria

Deployment is successful when:
- ✅ All services are running (PostgreSQL, Redis, Mailhog)
- ✅ Application starts without errors
- ✅ Health check returns UP status
- ✅ All 792+ tests pass
- ✅ API endpoints are accessible
- ✅ Email notifications work (Mailhog)
- ✅ Monitoring dashboards show data (Grafana)
- ✅ Feature flags are configurable
- ✅ Performance meets targets (p95 < 1s)

---

## 🎉 You're Ready!

Once all checks pass, you have successfully deployed to GitHub Codespaces!

**Next Steps:**
1. Complete testing in Codespaces environment
2. Review Production Readiness Review (PRR)
3. Begin Week 2: GCP staging deployment
4. Proceed with Launch Readiness Review (LRR)

---

**Document Version:** 1.0
**Last Updated:** 2025-10-24
**Status:** Ready for deployment
