import http from 'k6/http';
import { check, sleep } from 'k6';
import { Rate } from 'k6/metrics';

/**
 * K6 Load Testing Script for West Bethel Motel Booking System
 * Tests normal load conditions with realistic user scenarios
 *
 * Run: k6 run k6-scripts/load-test.js
 */

const errorRate = new Rate('errors');
const BASE_URL = __ENV.BASE_URL || 'http://localhost:8080';

export const options = {
    stages: [
        { duration: '1m', target: 10 },  // Ramp up to 10 users
        { duration: '3m', target: 50 },  // Ramp up to 50 users
        { duration: '5m', target: 50 },  // Stay at 50 users
        { duration: '2m', target: 100 }, // Spike to 100 users
        { duration: '3m', target: 50 },  // Back to 50 users
        { duration: '2m', target: 0 },   // Ramp down
    ],
    thresholds: {
        http_req_duration: ['p(95)<2000'], // 95% of requests under 2s
        http_req_failed: ['rate<0.1'],     // Less than 10% errors
        errors: ['rate<0.1'],
    },
};

// Test data
const users = [];
let authToken = '';

export function setup() {
    // Register a test user
    const registerPayload = JSON.stringify({
        username: `loadtest_${Date.now()}`,
        email: `loadtest${Date.now()}@test.com`,
        password: 'Load123!@#',
        firstName: 'Load',
        lastName: 'Test'
    });

    const registerRes = http.post(`${BASE_URL}/api/auth/register`, registerPayload, {
        headers: { 'Content-Type': 'application/json' },
    });

    if (registerRes.status === 201) {
        return { token: registerRes.json('token') };
    }

    // Fallback - login with existing user
    const loginPayload = JSON.stringify({
        username: 'loadtest',
        password: 'Load123!@#'
    });

    const loginRes = http.post(`${BASE_URL}/api/auth/login`, loginPayload, {
        headers: { 'Content-Type': 'application/json' },
    });

    return { token: loginRes.json('token') || '' };
}

export default function(data) {
    const token = data.token;
    const headers = {
        'Content-Type': 'application/json',
        'Authorization': `Bearer ${token}`
    };

    // Scenario: Browse properties
    let res = http.get(`${BASE_URL}/api/properties`, { headers });
    check(res, {
        'property list status is 200': (r) => r.status === 200,
        'property list response time OK': (r) => r.timings.duration < 1000,
    }) || errorRate.add(1);

    sleep(1);

    // Scenario: Search available rooms
    const checkIn = '2025-12-01';
    const checkOut = '2025-12-05';

    res = http.get(
        `${BASE_URL}/api/rooms/availability?checkIn=${checkIn}&checkOut=${checkOut}&guests=2`,
        { headers }
    );

    check(res, {
        'availability search status is 200': (r) => r.status === 200,
        'availability response time OK': (r) => r.timings.duration < 1500,
    }) || errorRate.add(1);

    sleep(2);

    // Scenario: View room types
    res = http.get(`${BASE_URL}/api/room-types`, { headers });
    check(res, {
        'room types status is 200 or 401': (r) => r.status === 200 || r.status === 401,
    });

    sleep(1);

    // Scenario: Create booking (may fail due to room availability)
    const bookingPayload = JSON.stringify({
        roomId: 1,
        guestEmail: `loadtest${Date.now()}@test.com`,
        checkInDate: checkIn,
        checkOutDate: checkOut,
        numberOfGuests: 2,
        specialRequests: 'K6 load test booking'
    });

    res = http.post(`${BASE_URL}/api/bookings`, bookingPayload, { headers });
    check(res, {
        'booking creation attempted': (r) => r.status === 201 || r.status === 400 || r.status === 422,
    });

    sleep(1);

    // Scenario: View user bookings
    res = http.get(`${BASE_URL}/api/bookings`, { headers });
    check(res, {
        'bookings list status is 200': (r) => r.status === 200,
    }) || errorRate.add(1);

    sleep(2);
}

export function teardown(data) {
    console.log('Load test completed');
}
