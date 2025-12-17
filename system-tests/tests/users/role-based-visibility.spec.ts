import {expect, test} from '@playwright/test';
import {
    completeInviteRegistration,
    createApplication,
    createCompany,
    createInvite,
    loginUser,
    navigateToUsersTab,
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
        await expect(adminPage.locator('.costs-overview')).toBeVisible({timeout: 5000});
        await expect(adminPage.getByText('Monthly Cost Breakdown')).toBeVisible();

        // ============================================
        // Step 3: Invite a Developer user
        // ============================================
        await navigateToUsersTab(adminPage);
        const inviteUrl = await createInvite(adminPage, devEmail, 'Developer');

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
        await expect(devPage.locator('.app-cards')).toBeVisible({timeout: 10000});

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
        await expect(adminPage.locator('.costs-overview')).toBeVisible({timeout: 5000});

        // Invite a Viewer
        await navigateToUsersTab(adminPage);
        const inviteUrl = await createInvite(adminPage, viewerEmail, 'Viewer');

        // Viewer completes registration
        const viewerPage = await browser.newPage();
        await completeInviteRegistration(viewerPage, inviteUrl, 'Viewer', 'User');
        await loginUser(viewerPage, viewerEmail);

        // Wait for dashboard content
        await expect(viewerPage.locator('.app-cards')).toBeVisible({timeout: 10000});

        // Viewer should NOT see cost breakdown
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
        await expect(admin1Page.locator('.costs-overview')).toBeVisible({timeout: 5000});

        // Invite a second Admin
        await navigateToUsersTab(admin1Page);
        const inviteUrl = await createInvite(admin1Page, admin2Email, 'Admin');

        // Admin2 completes registration
        const admin2Page = await browser.newPage();
        await completeInviteRegistration(admin2Page, inviteUrl, 'Admin', 'Two');
        await loginUser(admin2Page, admin2Email);

        // Wait for dashboard content
        await expect(admin2Page.locator('.app-cards')).toBeVisible({timeout: 10000});

        // Admin2 should ALSO see cost breakdown
        await expect(admin2Page.locator('.costs-overview')).toBeVisible({timeout: 5000});
        await expect(admin2Page.getByText('Monthly Cost Breakdown')).toBeVisible();

        await admin1Page.close();
        await admin2Page.close();
    });
});
