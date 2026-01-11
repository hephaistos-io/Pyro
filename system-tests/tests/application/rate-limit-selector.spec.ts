import {expect, test} from '@playwright/test';
import {
    completeInviteRegistration,
    createApplication,
    createCompany,
    createInvite,
    loginUser,
    navigateToUsersTab,
    openApplication,
    registerUser,
    uniqueEmail
} from '../../utils';

test.describe('Rate Limit Selector Visibility', () => {
    test('Admin sees Rate Limit selector in API Attributes, Developer does not', async ({browser}) => {
        const adminEmail = uniqueEmail('admin');
        const devEmail = uniqueEmail('dev');
        const companyName = 'Test Corp';
        const appName = 'Test Application';

        // ============================================
        // Step 1: Admin creates company and application
        // ============================================
        const adminPage = await browser.newPage();
        await registerUser(adminPage, adminEmail, 'Admin', 'User');
        await loginUser(adminPage, adminEmail);
        await createCompany(adminPage, companyName);
        await createApplication(adminPage, appName);

        // Open the application to see the overview
        await openApplication(adminPage, appName);

        // ============================================
        // Step 2: Verify Admin sees Rate Limit section
        // ============================================
        // Wait for environment to be selected and API Attributes to be visible
        await expect(adminPage.getByRole('heading', {name: 'API Attributes'})).toBeVisible();

        // API Attributes is expanded by default, so Rate Limit should be visible

        // Verify Rate Limit section is visible
        await expect(adminPage.getByText('Rate Limit', {exact: true})).toBeVisible();

        // Verify the slider is visible
        await expect(adminPage.getByRole('slider', {name: 'Select rate limit tier'})).toBeVisible();

        // Verify req/sec display is visible
        await expect(adminPage.getByText('req/sec')).toBeVisible();

        // ============================================
        // Step 3: Invite a Developer user (with access to the application)
        // ============================================
        // Navigate back to dashboard first
        await adminPage.getByRole('button', {name: 'Back to Dashboard'}).click();
        await expect(adminPage.locator('.app-cards')).toBeVisible();

        await navigateToUsersTab(adminPage);
        const inviteUrl = await createInvite(adminPage, devEmail, 'Developer', {grantAccessToApps: [appName]});

        // ============================================
        // Step 4: Developer completes registration
        // ============================================
        const devPage = await browser.newPage();
        await completeInviteRegistration(devPage, inviteUrl, 'Dev', 'User');
        await loginUser(devPage, devEmail);

        // ============================================
        // Step 5: Developer opens the application
        // ============================================
        // Wait for dashboard content to load
        await expect(devPage.locator('.app-cards')).toBeVisible();

        // The application should be visible (DEV can see apps)
        await expect(devPage.locator('.app-card').filter({hasText: appName})).toBeVisible();

        // Open the application
        await openApplication(devPage, appName);

        // Wait for API Attributes section to be visible
        await expect(devPage.getByRole('heading', {name: 'API Attributes'})).toBeVisible();

        // ============================================
        // Step 6: Verify Developer sees Rate Limit section but cannot change it
        // ============================================
        // The Rate Limit section IS visible for developers (they can see tier info)
        await expect(devPage.getByText('Rate Limit', {exact: true})).toBeVisible();

        // But the slider should be disabled (developer cannot change tier)
        const slider = devPage.getByRole('slider', {name: 'Select rate limit tier'});
        await expect(slider).toBeVisible();
        await expect(slider).toBeDisabled();

        // They should still see other API Attributes content (like Identifier)
        await expect(devPage.getByText('Identifier')).toBeVisible();

        await adminPage.close();
        await devPage.close();
    });

    test('Admin can interact with Rate Limit slider', async ({browser}) => {
        const adminEmail = uniqueEmail('admin');
        const companyName = 'Test Corp';
        const appName = 'Test Application';

        const adminPage = await browser.newPage();
        await registerUser(adminPage, adminEmail, 'Admin', 'User');
        await loginUser(adminPage, adminEmail);
        await createCompany(adminPage, companyName);
        await createApplication(adminPage, appName);
        await openApplication(adminPage, appName);

        // Wait for API Attributes section to be visible
        await expect(adminPage.getByRole('heading', {name: 'API Attributes'})).toBeVisible();

        // Verify Rate Limit section is visible and shows default Free tier (5 req/sec)
        await expect(adminPage.getByText('Rate Limit', {exact: true})).toBeVisible();
        await expect(adminPage.locator('.tier-display__limit-value')).toContainText('5');
        await expect(adminPage.getByText('$0')).toBeVisible();

        // Use the slider input directly to change the tier
        const slider = adminPage.getByRole('slider', {name: 'Select rate limit tier'});

        // Move slider to last position (Business tier - index 4)
        await slider.fill('4');

        // Verify the display updates to Business tier values
        await expect(adminPage.locator('.tier-display__limit-value')).toContainText('2k');
        await expect(adminPage.getByText('$100')).toBeVisible();
        await expect(adminPage.getByText('100M/month')).toBeVisible();

        // Move slider to middle position (Standard tier - index 2)
        await slider.fill('2');

        // Verify the display updates to Standard tier values
        await expect(adminPage.locator('.tier-display__limit-value')).toContainText('100');
        await expect(adminPage.getByText('$25')).toBeVisible();
        await expect(adminPage.getByText('10M/month')).toBeVisible();

        await adminPage.close();
    });

    test('Viewer does not see Rate Limit selector', async ({browser}) => {
        const adminEmail = uniqueEmail('admin');
        const viewerEmail = uniqueEmail('viewer');
        const companyName = 'Test Corp';
        const appName = 'Test App';

        // Setup admin with company and application
        const adminPage = await browser.newPage();
        await registerUser(adminPage, adminEmail, 'Admin', 'User');
        await loginUser(adminPage, adminEmail);
        await createCompany(adminPage, companyName);
        await createApplication(adminPage, appName);

        // Invite a Viewer with application access
        await navigateToUsersTab(adminPage);
        const inviteUrl = await createInvite(adminPage, viewerEmail, 'Viewer', {grantAccessToApps: [appName]});

        // Viewer completes registration
        const viewerPage = await browser.newPage();
        await completeInviteRegistration(viewerPage, inviteUrl, 'Viewer', 'User');
        await loginUser(viewerPage, viewerEmail);

        // Wait for dashboard content
        await expect(viewerPage.locator('.app-cards')).toBeVisible();

        // Open the application
        await openApplication(viewerPage, appName);

        // Wait for API Attributes section to be visible
        await expect(viewerPage.getByRole('heading', {name: 'API Attributes'})).toBeVisible();

        // Viewer should NOT see Rate Limit section (admin-only)
        await expect(viewerPage.getByText('Rate Limit', {exact: true})).not.toBeVisible();
        await expect(viewerPage.getByRole('slider', {name: 'Select rate limit tier'})).not.toBeVisible();

        // But they should still see Identifier
        await expect(viewerPage.getByText('Identifier')).toBeVisible();

        await adminPage.close();
        await viewerPage.close();
    });
});
