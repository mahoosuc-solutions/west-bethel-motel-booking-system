import http from 'k6/http';
import { check, sleep } from 'k6';
import { Rate } from 'k6/metrics';

/**
 * K6 Stress Testing Script for West Bethel Motel Booking System
 * Tests system limits and breaking points
 *
 * Run: k6 run k6-scripts/stress-test.js
 */

const errorRate = new Rate('errors');
const BASE_URL = __ENV.BASE_URL || 'http://localhost:8080';

export const options = {
    stages: [
        { duration: '2m', target: 50 },   // Warm up
        { duration: '2m', target: 100 },  // Approaching limits
        { duration: '3m', target: 200 },  // Beyond normal capacity
        { duration: '2m', target: 300 },  // Push to breaking point
        { duration: '3m', target: 400 },  // Maximum stress
        { duration: '5m', target: 0 },    // Recovery
    ],
    thresholds: {
        http_req_duration: ['p(99)<5000'], // 99% under 5s (relaxed for stress test)
        http_req_failed: ['rate<0.5'],     // Up to 50% errors acceptable in stress test
    },
};

export function setup() {
    const registerPayload = JSON.stringify({
        username: `stresstest_${Date.now()}`,
        email: `stress${Date.now()}@test.com`,
        password: 'Stress123!@#',
        firstName: 'Stress',
        lastName: 'Test'
    });

    const res = http.post(`${BASE_URL}/api/auth/register`, registerPayload, {
        headers: { 'Content-Type': 'application/json' },
    });

    return { token: res.json('token') || '' };
}

export default function(data) {
    const token = data.token;
    const headers = {
        'Content-Type': 'application/json',
        'Authorization': `Bearer ${token}`
    };

    // Rapid-fire requests with minimal delays
    const scenarios = [
        () => http.get(`${BASE_URL}/api/properties`, { headers }),
        () => http.get(`${BASE_URL}/api/rooms`, { headers }),
        () => http.get(`${BASE_URL}/api/bookings`, { headers }),
        () => http.get(`${BASE_URL}/api/room-types`, { headers }),
        () => http.get(`${BASE_URL}/api/rooms/availability?checkIn=2025-12-01&checkOut=2025-12-05&guests=2`, { headers }),
    ];

    // Execute random scenario
    const scenario = scenarios[Math.floor(Math.random() * scenarios.length)];
    const res = scenario();

    check(res, {
        'request completed': (r) => r.status > 0,
        'not server error': (r) => r.status < 500,
    }) || errorRate.add(1);

    sleep(Math.random() * 0.5); // Very short random delay
}

export function teardown(data) {
    console.log('Stress test completed - Check for system recovery');
}
