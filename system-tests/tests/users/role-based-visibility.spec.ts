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

test.describe('Role-Based UI Visibility', () => {
    test('Admin sees Monthly Cost Breakdown, Developer does not', async ({browser}) => {
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

        // Create an application so the cost overview has content
        await createApplication(adminPage, appName);

        // ============================================
        // Step 2: Verify Admin sees Monthly Cost Breakdown
        // ============================================
        await expect(adminPage.locator('.costs-overview')).toBeVisible();
        await expect(adminPage.getByText('Monthly Cost Breakdown')).toBeVisible();

        // ============================================
        // Step 3: Invite a Developer user (with access to the application)
        // ============================================
        await navigateToUsersTab(adminPage);
        const inviteUrl = await createInvite(adminPage, devEmail, 'Developer', {grantAccessToApps: [appName]});

        // ============================================
        // Step 4: Developer completes registration
        // ============================================
        const devPage = await browser.newPage();
        await completeInviteRegistration(devPage, inviteUrl, 'Dev', 'User');
        await loginUser(devPage, devEmail);

        // ============================================
        // Step 5: Verify Developer does NOT see Monthly Cost Breakdown
        // ============================================
        // Wait for dashboard content to load
        await expect(devPage.locator('.app-cards')).toBeVisible();

        // The application should be visible (DEV can see apps)
        await expect(devPage.locator('.app-card').filter({hasText: appName})).toBeVisible();

        // But the cost breakdown should NOT be visible
        await expect(devPage.locator('.costs-overview')).not.toBeVisible();
        await expect(devPage.getByText('Monthly Cost Breakdown')).not.toBeVisible();

        await adminPage.close();
        await devPage.close();
    });

    test('Viewer does not see Monthly Cost Breakdown', async ({browser}) => {
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

        // Verify admin sees cost breakdown
        await expect(adminPage.locator('.costs-overview')).toBeVisible();

        // Invite a Viewer (with access to the application so they can see something on dashboard)
        await navigateToUsersTab(adminPage);
        const inviteUrl = await createInvite(adminPage, viewerEmail, 'Viewer', {grantAccessToApps: [appName]});

        // Viewer completes registration
        const viewerPage = await browser.newPage();
        await completeInviteRegistration(viewerPage, inviteUrl, 'Viewer', 'User');
        await loginUser(viewerPage, viewerEmail);

        // Wait for dashboard to fully load
        await viewerPage.waitForLoadState('networkidle');

        // Viewer should see the application (since they have access)
        await expect(viewerPage.locator('.app-card').filter({hasText: appName})).toBeVisible();

        // Viewer should NOT see cost breakdown (Admin only)
        await expect(viewerPage.locator('.costs-overview')).not.toBeVisible();
        await expect(viewerPage.getByText('Monthly Cost Breakdown')).not.toBeVisible();

        await adminPage.close();
        await viewerPage.close();
    });

    test('Multiple Admins can all see Monthly Cost Breakdown', async ({browser}) => {
        const admin1Email = uniqueEmail('admin1');
        const admin2Email = uniqueEmail('admin2');
        const companyName = 'Test Corp';
        const appName = 'Test App';

        // Setup first admin with company and application
        const admin1Page = await browser.newPage();
        await registerUser(admin1Page, admin1Email, 'Admin', 'One');
        await loginUser(admin1Page, admin1Email);
        await createCompany(admin1Page, companyName);
        await createApplication(admin1Page, appName);

        // Verify admin1 sees cost breakdown
        await expect(admin1Page.locator('.costs-overview')).toBeVisible();

        // Invite a second Admin (with access to the application)
        await navigateToUsersTab(admin1Page);
        const inviteUrl = await createInvite(admin1Page, admin2Email, 'Admin', {grantAccessToApps: [appName]});

        // Admin2 completes registration
        const admin2Page = await browser.newPage();
        await completeInviteRegistration(admin2Page, inviteUrl, 'Admin', 'Two');
        await loginUser(admin2Page, admin2Email);

        // Wait for dashboard content
        await expect(admin2Page.locator('.app-cards')).toBeVisible();

        // Admin2 should ALSO see cost breakdown
        await expect(admin2Page.locator('.costs-overview')).toBeVisible();
        await expect(admin2Page.getByText('Monthly Cost Breakdown')).toBeVisible();

        await admin1Page.close();
        await admin2Page.close();
    });

    test('Admin sees environment management buttons, Developer does not', async ({browser}) => {
        const adminEmail = uniqueEmail('admin');
        const devEmail = uniqueEmail('dev');
        const companyName = 'Test Corp';
        const appName = 'Test Application';

        // Setup admin with company and application
        const adminPage = await browser.newPage();
        await registerUser(adminPage, adminEmail, 'Admin', 'User');
        await loginUser(adminPage, adminEmail);
        await createCompany(adminPage, companyName);
        await createApplication(adminPage, appName);

        // Navigate to application page
        await openApplication(adminPage, appName);

        // Admin should see environment management buttons
        await expect(adminPage.locator('.add-env-btn')).toBeVisible();
        await expect(adminPage.locator('.edit-env-btn')).toBeVisible();
        await expect(adminPage.locator('.delete-env-btn')).toBeVisible();

        // Go back to dashboard to invite a Developer user
        await adminPage.goto('/dashboard');
        await navigateToUsersTab(adminPage);
        const inviteUrl = await createInvite(adminPage, devEmail, 'Developer', {grantAccessToApps: [appName]});

        // Developer completes registration
        const devPage = await browser.newPage();
        await completeInviteRegistration(devPage, inviteUrl, 'Dev', 'User');
        await loginUser(devPage, devEmail);

        // Navigate to application page
        await openApplication(devPage, appName);

        // Developer should see the environment selector but NOT management buttons
        await expect(devPage.locator('.selector-button')).toBeVisible();
        await expect(devPage.locator('.add-env-btn')).not.toBeVisible();
        await expect(devPage.locator('.edit-env-btn')).not.toBeVisible();
        await expect(devPage.locator('.delete-env-btn')).not.toBeVisible();

        await adminPage.close();
        await devPage.close();
    });

    test('Viewer does not see environment management buttons', async ({browser}) => {
        const adminEmail = uniqueEmail('admin');
        const viewerEmail = uniqueEmail('viewer');
        const companyName = 'Test Corp';
        const appName = 'Test Application';

        // Setup admin with company and application
        const adminPage = await browser.newPage();
        await registerUser(adminPage, adminEmail, 'Admin', 'User');
        await loginUser(adminPage, adminEmail);
        await createCompany(adminPage, companyName);
        await createApplication(adminPage, appName);

        // Invite a Viewer user
        await navigateToUsersTab(adminPage);
        const inviteUrl = await createInvite(adminPage, viewerEmail, 'Viewer', {grantAccessToApps: [appName]});

        // Viewer completes registration
        const viewerPage = await browser.newPage();
        await completeInviteRegistration(viewerPage, inviteUrl, 'Viewer', 'User');
        await loginUser(viewerPage, viewerEmail);

        // Navigate to application page
        await openApplication(viewerPage, appName);

        // Viewer should see the environment selector but NOT management buttons
        await expect(viewerPage.locator('.selector-button')).toBeVisible();
        await expect(viewerPage.locator('.add-env-btn')).not.toBeVisible();
        await expect(viewerPage.locator('.edit-env-btn')).not.toBeVisible();
        await expect(viewerPage.locator('.delete-env-btn')).not.toBeVisible();

        await adminPage.close();
        await viewerPage.close();
    });
});
