import {expect, test} from '@playwright/test';
import {
    completeInviteRegistration,
    createApplication,
    createCompany,
    createInvite,
    getUserRow,
    loginUser,
    navigateToUsersTab,
    registerUser,
    uniqueEmail
} from '../../utils';

test.describe('Application Access Management', () => {
    test('admin sees applications in user table after granting access', async ({page}) => {
        const adminEmail = uniqueEmail('admin');
        const devEmail = uniqueEmail('dev');
        const app1Name = 'Mobile App';
        const app2Name = 'Web Portal';

        // Setup admin with company and applications
        await registerUser(page, adminEmail, 'Admin', 'User');
        await loginUser(page, adminEmail);
        await createCompany(page, 'Test Company');

        // Create two applications
        await createApplication(page, app1Name);
        await createApplication(page, app2Name);

        // Navigate to Users tab and invite a developer with access to both apps
        await navigateToUsersTab(page);
        await createInvite(page, devEmail, 'Developer', {grantAccessToApps: [app1Name, app2Name]});

        // Reload to fetch updated user data
        await page.reload();
        await navigateToUsersTab(page);

        // Find the invited user's row
        const devRow = getUserRow(page, devEmail);
        await expect(devRow).toBeVisible();

        // Verify the applications are displayed in the user's row
        // The applications should be visible as tags or in a column
        await expect(devRow.getByText(app1Name)).toBeVisible();
        await expect(devRow.getByText(app2Name)).toBeVisible();
    });

    test('invited user with app access can see granted applications after registration', async ({browser}) => {
        const adminEmail = uniqueEmail('admin');
        const devEmail = uniqueEmail('dev');
        const app1Name = 'Mobile App';
        const app2Name = 'Web Portal';
        const app3Name = 'Admin Dashboard';

        // Setup admin with company and applications
        const adminPage = await browser.newPage();
        await registerUser(adminPage, adminEmail, 'Admin', 'User');
        await loginUser(adminPage, adminEmail);
        await createCompany(adminPage, 'Test Company');

        await createApplication(adminPage, app1Name);
        await createApplication(adminPage, app2Name);
        await createApplication(adminPage, app3Name);

        // Invite developer with access to only app1 and app2
        await navigateToUsersTab(adminPage);
        const inviteUrl = await createInvite(adminPage, devEmail, 'Developer', {
            grantAccessToApps: [app1Name, app2Name]
        });

        // Developer completes registration
        const devPage = await browser.newPage();
        await completeInviteRegistration(devPage, inviteUrl, 'Dev', 'User');
        await loginUser(devPage, devEmail);

        // Verify developer sees only the apps they have access to
        await expect(devPage.locator('.app-cards')).toBeVisible();
        await expect(devPage.locator('.app-card').filter({hasText: app1Name})).toBeVisible();
        await expect(devPage.locator('.app-card').filter({hasText: app2Name})).toBeVisible();
        await expect(devPage.locator('.app-card').filter({hasText: app3Name})).not.toBeVisible();

        await adminPage.close();
        await devPage.close();
    });

    test('admin can view applications assigned to team members', async ({page}) => {
        const adminEmail = uniqueEmail('admin');
        const dev1Email = uniqueEmail('dev1');
        const dev2Email = uniqueEmail('dev2');
        const app1Name = 'Mobile App';
        const app2Name = 'Web Portal';

        // Setup
        await registerUser(page, adminEmail, 'Admin', 'User');
        await loginUser(page, adminEmail);
        await createCompany(page, 'Test Company');

        await createApplication(page, app1Name);
        await createApplication(page, app2Name);

        // Invite dev1 with access to app1
        await navigateToUsersTab(page);
        await createInvite(page, dev1Email, 'Developer', {grantAccessToApps: [app1Name]});

        // Invite dev2 with access to app2
        await createInvite(page, dev2Email, 'Developer', {grantAccessToApps: [app2Name]});

        // Reload to get fresh data
        await page.reload();
        await navigateToUsersTab(page);

        // Check dev1's applications
        const dev1Row = getUserRow(page, dev1Email);
        await expect(dev1Row.getByText(app1Name)).toBeVisible();
        await expect(dev1Row.getByText(app2Name)).not.toBeVisible();

        // Check dev2's applications
        const dev2Row = getUserRow(page, dev2Email);
        await expect(dev2Row.getByText(app2Name)).toBeVisible();
        await expect(dev2Row.getByText(app1Name)).not.toBeVisible();

        // Admin should have access to all applications
        const adminRow = getUserRow(page, adminEmail);
        await expect(adminRow.getByText(app1Name)).toBeVisible();
        await expect(adminRow.getByText(app2Name)).toBeVisible();
    });

    test('admin can edit user application access via edit form', async ({page}) => {
        const adminEmail = uniqueEmail('admin');
        const devEmail = uniqueEmail('dev');
        const app1Name = 'Mobile App';
        const app2Name = 'Web Portal';
        const app3Name = 'Admin Dashboard';

        // Setup
        await registerUser(page, adminEmail, 'Admin', 'User');
        await loginUser(page, adminEmail);
        await createCompany(page, 'Test Company');

        await createApplication(page, app1Name);
        await createApplication(page, app2Name);
        await createApplication(page, app3Name);

        // Invite developer with access to app1 only
        await navigateToUsersTab(page);
        const inviteUrl = await createInvite(page, devEmail, 'Developer', {
            grantAccessToApps: [app1Name]
        });

        // Complete registration using the invite URL
        await completeInviteRegistration(page, inviteUrl, 'Dev', 'User');

        // Login as admin again
        await loginUser(page, adminEmail);
        await navigateToUsersTab(page);

        // Find dev user and click edit
        const devRow = getUserRow(page, devEmail);
        await devRow.locator('.btn-edit-user').click();

        // Wait for edit form
        await expect(page.getByRole('heading', {name: 'Edit User'})).toBeVisible();

        // Verify app1 is checked, app2 and app3 are not
        const app1Checkbox = page.getByRole('checkbox', {name: app1Name});
        const app2Checkbox = page.getByRole('checkbox', {name: app2Name});
        const app3Checkbox = page.getByRole('checkbox', {name: app3Name});

        await expect(app1Checkbox).toBeChecked();
        await expect(app2Checkbox).not.toBeChecked();
        await expect(app3Checkbox).not.toBeChecked();

        // Grant access to app2, revoke app1
        await app1Checkbox.uncheck();
        await app2Checkbox.check();

        // Save changes
        await page.getByRole('button', {name: 'Save Changes'}).click();

        // Wait for form to close
        await expect(page.getByRole('heading', {name: 'Edit User'})).not.toBeVisible();

        // Reload to verify changes persisted
        await page.reload();
        await navigateToUsersTab(page);

        // Verify dev now has access to app2 but not app1 or app3
        const updatedDevRow = getUserRow(page, devEmail);
        await expect(updatedDevRow.getByText(app2Name)).toBeVisible();
        await expect(updatedDevRow.getByText(app1Name)).not.toBeVisible();
        await expect(updatedDevRow.getByText(app3Name)).not.toBeVisible();
    });

    test('application access is isolated between companies (multi-tenancy)', async ({browser}) => {
        const adminAEmail = uniqueEmail('admin-a');
        const adminBEmail = uniqueEmail('admin-b');
        const devAEmail = uniqueEmail('dev-a');
        const devBEmail = uniqueEmail('dev-b');
        const appAName = 'Company A App';
        const appBName = 'Company B App';

        // Setup Company A
        const pageA = await browser.newPage();
        await registerUser(pageA, adminAEmail, 'Admin', 'CompanyA');
        await loginUser(pageA, adminAEmail);
        await createCompany(pageA, 'Company Alpha');
        await createApplication(pageA, appAName);

        // Invite dev to Company A with access to Company A's app
        await navigateToUsersTab(pageA);
        await createInvite(pageA, devAEmail, 'Developer', {grantAccessToApps: [appAName]});

        // Reload and verify
        await pageA.reload();
        await navigateToUsersTab(pageA);
        const devARow = getUserRow(pageA, devAEmail);
        await expect(devARow.getByText(appAName)).toBeVisible();
        await expect(devARow.getByText(appBName)).not.toBeVisible();

        // Setup Company B
        const pageB = await browser.newPage();
        await registerUser(pageB, adminBEmail, 'Admin', 'CompanyB');
        await loginUser(pageB, adminBEmail);
        await createCompany(pageB, 'Company Beta');
        await createApplication(pageB, appBName);

        // Invite dev to Company B with access to Company B's app
        await navigateToUsersTab(pageB);
        await createInvite(pageB, devBEmail, 'Developer', {grantAccessToApps: [appBName]});

        // Reload and verify
        await pageB.reload();
        await navigateToUsersTab(pageB);
        const devBRow = getUserRow(pageB, devBEmail);
        await expect(devBRow.getByText(appBName)).toBeVisible();
        await expect(devBRow.getByText(appAName)).not.toBeVisible();

        // Verify Company A still only sees their data
        await pageA.reload();
        await navigateToUsersTab(pageA);
        const emails = await pageA.locator('.users-table__row .user-identity__email').allTextContents();
        expect(emails).toContain(adminAEmail);
        expect(emails).toContain(devAEmail);
        expect(emails).not.toContain(adminBEmail);
        expect(emails).not.toContain(devBEmail);

        await pageA.close();
        await pageB.close();
    });

    test('user with no application access sees empty dashboard', async ({browser}) => {
        const adminEmail = uniqueEmail('admin');
        const viewerEmail = uniqueEmail('viewer');
        const appName = 'Restricted App';

        // Setup admin with company and application
        const adminPage = await browser.newPage();
        await registerUser(adminPage, adminEmail, 'Admin', 'User');
        await loginUser(adminPage, adminEmail);
        await createCompany(adminPage, 'Test Company');
        await createApplication(adminPage, appName);

        // Invite viewer WITHOUT granting access to any applications
        await navigateToUsersTab(adminPage);
        const inviteUrl = await createInvite(adminPage, viewerEmail, 'Viewer', {
            grantAccessToApps: [] // No applications
        });

        // Viewer completes registration
        const viewerPage = await browser.newPage();
        await completeInviteRegistration(viewerPage, inviteUrl, 'Viewer', 'User');
        await loginUser(viewerPage, viewerEmail);

        // Verify viewer sees no applications
        await expect(viewerPage.locator('.app-cards')).toBeVisible();
        await expect(viewerPage.locator('.app-card').filter({hasText: appName})).not.toBeVisible();

        // Viewer should not see any application cards (only actual apps, not the add button)
        const actualAppCards = await viewerPage.locator('.app-card:not(.app-card--add)').count();
        expect(actualAppCards).toBe(0);

        await adminPage.close();
        await viewerPage.close();
    });

    test('newly created application is not automatically assigned to existing users', async ({page}) => {
        const adminEmail = uniqueEmail('admin');
        const devEmail = uniqueEmail('dev');
        const app1Name = 'First App';
        const app2Name = 'Second App';

        // Setup
        await registerUser(page, adminEmail, 'Admin', 'User');
        await loginUser(page, adminEmail);
        await createCompany(page, 'Test Company');
        await createApplication(page, app1Name);

        // Invite developer with access to app1
        await navigateToUsersTab(page);
        await createInvite(page, devEmail, 'Developer', {grantAccessToApps: [app1Name]});

        // Navigate back to Applications tab
        await page.getByRole('button', {name: 'Applications'}).click();

        // Create a second application
        await createApplication(page, app2Name);

        // Navigate back to Users tab
        await navigateToUsersTab(page);
        await page.reload();
        await navigateToUsersTab(page);

        // Verify dev only has access to app1, not app2
        const devRow = getUserRow(page, devEmail);
        await expect(devRow.getByText(app1Name)).toBeVisible();
        await expect(devRow.getByText(app2Name)).not.toBeVisible();
    });

    test('admin creating application automatically gets access to it', async ({page}) => {
        const adminEmail = uniqueEmail('admin');
        const appName = 'New Application';

        // Setup
        await registerUser(page, adminEmail, 'Admin', 'User');
        await loginUser(page, adminEmail);
        await createCompany(page, 'Test Company');

        // Create application
        await createApplication(page, appName);

        // Navigate to Users tab
        await navigateToUsersTab(page);
        await page.reload();
        await navigateToUsersTab(page);

        // Verify admin has access to the application they created
        const adminRow = getUserRow(page, adminEmail);
        await expect(adminRow.getByText(appName)).toBeVisible();
    });
});
