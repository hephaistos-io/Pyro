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

        // Reload to get fresh data and wait for dashboard to load
        await page.reload();
        await expect(page.locator('.app-cards, .users-table')).toBeVisible();
        await navigateToUsersTab(page);

        // Check dev1's applications
        const dev1Row = getUserRow(page, dev1Email);
        await expect(dev1Row).toBeVisible();
        await expect(dev1Row.getByText(app1Name)).toBeVisible();
        await expect(dev1Row.getByText(app2Name)).not.toBeVisible();

        // Check dev2's applications
        const dev2Row = getUserRow(page, dev2Email);
        await expect(dev2Row).toBeVisible();
        await expect(dev2Row.getByText(app2Name)).toBeVisible();
        await expect(dev2Row.getByText(app1Name)).not.toBeVisible();

        // Admin should have access to all applications (displayed as "All (Admin Role)")
        const adminRow = getUserRow(page, adminEmail);
        await expect(adminRow).toBeVisible();
        await expect(adminRow.getByText('All (Admin Role)')).toBeVisible();
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

        // Setup both companies in parallel
        const [pageA, pageB] = await Promise.all([
            browser.newPage(),
            browser.newPage()
        ]);

        // Register both admins in parallel
        await Promise.all([
            registerUser(pageA, adminAEmail, 'Admin', 'CompanyA'),
            registerUser(pageB, adminBEmail, 'Admin', 'CompanyB')
        ]);

        // Login both admins in parallel
        await Promise.all([
            loginUser(pageA, adminAEmail),
            loginUser(pageB, adminBEmail)
        ]);

        // Create companies in parallel
        await Promise.all([
            createCompany(pageA, 'Company Alpha'),
            createCompany(pageB, 'Company Beta')
        ]);

        // Create applications in parallel
        await Promise.all([
            createApplication(pageA, appAName),
            createApplication(pageB, appBName)
        ]);

        // Navigate to Users and create invites in parallel
        await Promise.all([
            navigateToUsersTab(pageA),
            navigateToUsersTab(pageB)
        ]);

        await Promise.all([
            createInvite(pageA, devAEmail, 'Developer', {grantAccessToApps: [appAName]}),
            createInvite(pageB, devBEmail, 'Developer', {grantAccessToApps: [appBName]})
        ]);

        // Reload both pages in parallel and verify isolation
        await Promise.all([
            pageA.reload(),
            pageB.reload()
        ]);

        await Promise.all([
            navigateToUsersTab(pageA),
            navigateToUsersTab(pageB)
        ]);

        // Verify Company A only sees their users
        const emailsA = await pageA.locator('.users-table__row .user-identity__email').allTextContents();
        expect(emailsA).toContain(adminAEmail);
        expect(emailsA).toContain(devAEmail);
        expect(emailsA).not.toContain(adminBEmail);
        expect(emailsA).not.toContain(devBEmail);

        // Verify Company B only sees their users
        const emailsB = await pageB.locator('.users-table__row .user-identity__email').allTextContents();
        expect(emailsB).toContain(adminBEmail);
        expect(emailsB).toContain(devBEmail);
        expect(emailsB).not.toContain(adminAEmail);
        expect(emailsB).not.toContain(devAEmail);

        await Promise.all([pageA.close(), pageB.close()]);
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

        // Wait for the dashboard to fully load
        await viewerPage.waitForLoadState('networkidle');

        // Verify viewer sees no applications (the app-cards container might be empty or the specific app not visible)
        // ReadOnly users (Viewers) don't see the Add Application button
        await expect(viewerPage.locator('.app-card').filter({hasText: appName})).not.toBeVisible({timeout: 5000});

        // Viewer should not see any application cards
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
        await expect(page.locator('.app-cards')).toBeVisible();

        // Create a second application
        await createApplication(page, app2Name);

        // Reload and navigate to Users tab
        await page.reload();
        await expect(page.locator('.app-cards, .users-table')).toBeVisible();
        await navigateToUsersTab(page);

        // Verify dev only has access to app1, not app2
        const devRow = getUserRow(page, devEmail);
        await expect(devRow).toBeVisible();
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

        // Reload and navigate to Users tab
        await page.reload();
        await expect(page.locator('.app-cards, .users-table')).toBeVisible();
        await navigateToUsersTab(page);

        // Verify admin has access to all applications (displayed as "All (Admin Role)")
        const adminRow = getUserRow(page, adminEmail);
        await expect(adminRow).toBeVisible();
        await expect(adminRow.getByText('All (Admin Role)')).toBeVisible();
    });
});
