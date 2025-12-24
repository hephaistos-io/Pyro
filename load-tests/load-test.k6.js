import http from 'k6/http';
import {check, sleep} from 'k6';
import {Counter, Rate, Trend} from 'k6/metrics';
import {textSummary} from 'https://jslib.k6.io/k6-summary/0.0.1/index.js';

// Environment configuration
const API_KEY = __ENV.API_KEY;
const BASE_URL = __ENV.BASE_URL || 'http://localhost';
const IDENTIFIER = __ENV.IDENTIFIER || '';
const PATTERN = __ENV.PATTERN || 'constant';
const USER_COUNT = parseInt(__ENV.USER_COUNT) || 0;
const PERIOD = parseInt(__ENV.PERIOD) || 60;
const MIN_RPS = parseInt(__ENV.MIN_RPS) || 5;
const MAX_RPS = parseInt(__ENV.MAX_RPS) || 50;

// Validate required configuration
if (!API_KEY) {
    console.error('ERROR: API_KEY environment variable is required');
    console.error('Usage: k6 run -e API_KEY="your-api-key" load-tests/load-test.k6.js');
}

// Custom metrics
const errorRate = new Rate('errors');
const requestsTotal = new Counter('requests_total');
const responseTime = new Trend('response_time');

// Track start time for pattern calculations
const startTime = Date.now();

/**
 * Calculate sleep duration based on the selected traffic pattern.
 *
 * Patterns:
 * - constant: Fixed rate at MAX_RPS
 * - sine: Oscillates between MIN_RPS and MAX_RPS with given PERIOD
 * - ramp: Linear increase from MIN_RPS to MAX_RPS over test duration
 */
function calculateSleep() {
    const elapsed = (Date.now() - startTime) / 1000;
    let targetRps;

    switch (PATTERN) {
        case 'sine':
            // Sine wave: oscillates between MIN_RPS and MAX_RPS
            const amplitude = (MAX_RPS - MIN_RPS) / 2;
            const midpoint = (MAX_RPS + MIN_RPS) / 2;
            targetRps = midpoint + amplitude * Math.sin(2 * Math.PI * elapsed / PERIOD);
            break;

        case 'ramp':
            // Linear ramp: gradually increases from MIN_RPS to MAX_RPS
            // Uses test duration from options or defaults to 60s
            const duration = __ENV.DURATION_SEC ? parseInt(__ENV.DURATION_SEC) : 60;
            const progress = Math.min(elapsed / duration, 1);
            targetRps = MIN_RPS + (MAX_RPS - MIN_RPS) * progress;
            break;

        default: // 'constant'
            targetRps = MAX_RPS;
    }

    // Return sleep time (inverse of target RPS), with minimum bound
    return 1 / Math.max(targetRps, 0.1);
}

/**
 * Build the request URL based on configuration.
 *
 * If USER_COUNT > 0, cycles through user endpoints.
 * Otherwise, uses the system endpoint with optional identifier.
 */
function getRequestUrl() {
    if (USER_COUNT > 0) {
        // Cycle through user endpoints
        const userId = `user-${Math.floor(Math.random() * USER_COUNT)}`;
        return `${BASE_URL}/v1/api/templates/user/${userId}`;
    }

    // Use system endpoint
    let url = `${BASE_URL}/v1/api/templates/system`;
    if (IDENTIFIER) {
        url += `?identifier=${encodeURIComponent(IDENTIFIER)}`;
    }
    return url;
}

/**
 * Main test function - executed by each virtual user (VU).
 */
export default function () {
    const url = getRequestUrl();
    const params = {
        headers: {
            'X-API-Key': API_KEY,
            'Accept': 'application/json',
        },
    };

    const res = http.get(url, params);

    // Record custom metrics
    requestsTotal.add(1);
    responseTime.add(res.timings.duration);

    // Validate response
    const success = check(res, {
        'status is 200': (r) => r.status === 200,
        'response time < 500ms': (r) => r.timings.duration < 500,
        'has valid JSON body': (r) => {
            try {
                JSON.parse(r.body);
                return true;
            } catch {
                return false;
            }
        },
    });

    errorRate.add(!success);

    // Log errors for debugging
    if (!success && res.status !== 200) {
        console.log(`Request failed: ${res.status} - ${res.body}`);
    }

    // Sleep based on traffic pattern
    sleep(calculateSleep());
}

/**
 * Generate summary report at the end of the test.
 */
export function handleSummary(data) {
    // Print configuration used
    console.log('\n=== Load Test Configuration ===');
    console.log(`Base URL: ${BASE_URL}`);
    console.log(`Pattern: ${PATTERN}`);
    if (PATTERN === 'sine') {
        console.log(`  Period: ${PERIOD}s`);
    }
    console.log(`RPS Range: ${MIN_RPS} - ${MAX_RPS}`);
    if (USER_COUNT > 0) {
        console.log(`User endpoints: ${USER_COUNT} users`);
    } else if (IDENTIFIER) {
        console.log(`Identifier: ${IDENTIFIER}`);
    }
    console.log('===============================\n');

    return {
        stdout: textSummary(data, {indent: '  ', enableColors: true}),
    };
}
