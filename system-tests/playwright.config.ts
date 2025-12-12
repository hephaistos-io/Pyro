import {defineConfig, devices} from '@playwright/test';

/**
 * Playwright configuration for FlagForge E2E tests.
 * See https://playwright.dev/docs/test-configuration
 */
export default defineConfig({
    testDir: './tests',

    /* Run tests in files in parallel */
    fullyParallel: true,

    /* Fail the build on CI if you accidentally left test.only in the source code */
    forbidOnly: !!process.env.CI,

    /* Retry on CI only */
    retries: process.env.CI ? 2 : 0,

    /* Parallel workers */
    workers: process.env.CI ? 1 : undefined,

    /* Reporter configuration */
    reporter: [
        ['html', {open: 'never'}],
        ['list']
    ],

    /* Shared settings for all projects */
    use: {
        /* Base URL - defaults to local Docker setup */
        baseURL: process.env.BASE_URL || 'http://localhost',

        /* Collect trace when retrying the failed test */
        trace: 'on-first-retry',

        /* Screenshot on failure */
        screenshot: 'only-on-failure',

        /* Video on failure */
        video: 'on-first-retry',
    },

    /* Configure projects for major browsers */
    projects: [
        {
            name: 'chromium',
            use: {...devices['Desktop Chrome']},
        },
        {
            name: 'firefox',
            use: {...devices['Desktop Firefox']},
        },
        {
            name: 'webkit',
            use: {...devices['Desktop Safari']},
        },
    ],

    /* Global timeout for each test */
    timeout: 30000,

    /* Expect timeout */
    expect: {
        timeout: 5000,
    },
});
