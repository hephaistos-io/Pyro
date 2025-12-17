import {expect, test} from '@playwright/test';
import {
    createCompany,
    createInviteSimple,
    DEFAULT_PASSWORD,
    getDisplayedUserEmails,
    getInviteUrl,
    getUserRow,
    getUserStatuses,
    loginUser,
    navigateToUsersTab,
    registerUser,
    uniqueEmail
} from '../../utils';

test.describe('Invite Display in Users Table', () => {
    test('pending invite appears in users table with invited status', async ({page}) => {
        const adminEmail = uniqueEmail('admin');
        const invitedEmail = uniqueEmail('invited');

        // Register and setup admin user with company
        await registerUser(page, adminEmail, 'Admin', 'User');
        await loginUser(page, adminEmail);
        await createCompany(page, 'Test Company');

        // Navigate to Users tab
        await navigateToUsersTab(page);

        // Verify only admin is shown initially
        let emails = await getDisplayedUserEmails(page);
        expect(emails).toHaveLength(1);
        expect(emails).toContain(adminEmail);

        // Create an invite
        await createInviteSimple(page, invitedEmail);

        // Wait for the table to update
        await page.waitForTimeout(1000);

        // Verify the invited user appears in the table
        emails = await getDisplayedUserEmails(page);
        expect(emails).toHaveLength(2);
        expect(emails).toContain(adminEmail);
        expect(emails).toContain(invitedEmail);

        // Verify the status badges
        const statuses = await getUserStatuses(page);
        const adminStatus = statuses.find(s => s.email === adminEmail);
        const invitedStatus = statuses.find(s => s.email === invitedEmail);

        expect(adminStatus?.status).toBe('active');
        expect(invitedStatus?.status).toBe('invited');
    });

    test('invited user disappears from pending after registration', async ({browser}) => {
        const adminEmail = uniqueEmail('admin');
        const invitedEmail = uniqueEmail('invited');

        // ============================================
        // Step 1: Admin creates an invite
        // ============================================
        const adminPage = await browser.newPage();
        await registerUser(adminPage, adminEmail, 'Admin', 'User');
        await loginUser(adminPage, adminEmail);
        await createCompany(adminPage, 'Test Company');

        await navigateToUsersTab(adminPage);
        await createInviteSimple(adminPage, invitedEmail);

        // Verify invite appears
        await adminPage.waitForTimeout(1000);
        let emails = await getDisplayedUserEmails(adminPage);
        expect(emails).toContain(invitedEmail);

        let statuses = await getUserStatuses(adminPage);
        expect(statuses.find(s => s.email === invitedEmail)?.status).toBe('invited');

        // ============================================
        // Step 2: Get the invite URL by regenerating
        // ============================================
        const inviteUrl = await getInviteUrl(adminPage, invitedEmail);
        expect(inviteUrl).toContain('/register?invite=');

        // ============================================
        // Step 3: Invited user registers using the invite link
        // ============================================
        const invitedPage = await browser.newPage();

        // Navigate to the invite URL
        await invitedPage.goto(inviteUrl);

        // Wait for the invite banner to appear (validates invite is valid)
        await expect(invitedPage.locator('.invite-banner')).toBeVisible();
        await expect(invitedPage.locator('.invite-banner')).toContainText('Test Company');

        // Email should be pre-filled and read-only
        const emailInput = invitedPage.locator('input#email');
        await expect(emailInput).toBeDisabled();
        await expect(emailInput).toHaveValue(invitedEmail);

        // Fill in the registration form (only first name, last name, password needed)
        await invitedPage.getByLabel('First Name').fill('Invited');
        await invitedPage.getByLabel('Last Name').fill('User');
        await invitedPage.getByLabel('Password', {exact: true}).fill(DEFAULT_PASSWORD);
        await invitedPage.getByLabel('Confirm Password').fill(DEFAULT_PASSWORD);

        // Submit the registration
        await invitedPage.getByRole('button', {name: /Join Test Company/i}).click();

        // Wait for redirect to login page
        await expect(invitedPage).toHaveURL('/login');

        await invitedPage.close();

        // ============================================
        // Step 4: Verify the user now shows as active (not invited)
        // ============================================
        await adminPage.reload();
        await navigateToUsersTab(adminPage);

        // The user should now be active since they registered
        statuses = await getUserStatuses(adminPage);
        const invitedUserStatus = statuses.find(s => s.email === invitedEmail);
        expect(invitedUserStatus?.status).toBe('active');

        await adminPage.close();
    });

    test('multiple pending invites are displayed correctly', async ({page}) => {
        const adminEmail = uniqueEmail('admin');
        const invite1Email = uniqueEmail('invite1');
        const invite2Email = uniqueEmail('invite2');
        const invite3Email = uniqueEmail('invite3');

        // Setup admin with company
        await registerUser(page, adminEmail, 'Admin', 'User');
        await loginUser(page, adminEmail);
        await createCompany(page, 'Test Company');

        await navigateToUsersTab(page);

        // Create multiple invites
        await createInviteSimple(page, invite1Email);
        await page.waitForTimeout(500);
        await createInviteSimple(page, invite2Email);
        await page.waitForTimeout(500);
        await createInviteSimple(page, invite3Email);

        // Wait for table to update
        await page.waitForTimeout(1000);

        // Verify all invites appear
        const emails = await getDisplayedUserEmails(page);
        expect(emails).toHaveLength(4); // admin + 3 invites
        expect(emails).toContain(adminEmail);
        expect(emails).toContain(invite1Email);
        expect(emails).toContain(invite2Email);
        expect(emails).toContain(invite3Email);

        // Verify statuses
        const statuses = await getUserStatuses(page);
        expect(statuses.find(s => s.email === adminEmail)?.status).toBe('active');
        expect(statuses.find(s => s.email === invite1Email)?.status).toBe('invited');
        expect(statuses.find(s => s.email === invite2Email)?.status).toBe('invited');
        expect(statuses.find(s => s.email === invite3Email)?.status).toBe('invited');
    });

    test('pending invites are filtered by company (multi-tenancy)', async ({browser}) => {
        const adminAEmail = uniqueEmail('admin-a');
        const adminBEmail = uniqueEmail('admin-b');
        const inviteAEmail = uniqueEmail('invite-a');
        const inviteBEmail = uniqueEmail('invite-b');

        // ============================================
        // Setup Company A with an invite
        // ============================================
        const pageA = await browser.newPage();
        await registerUser(pageA, adminAEmail, 'Admin', 'CompanyA');
        await loginUser(pageA, adminAEmail);
        await createCompany(pageA, 'Company Alpha');

        await navigateToUsersTab(pageA);
        await createInviteSimple(pageA, inviteAEmail);

        // ============================================
        // Setup Company B with an invite
        // ============================================
        const pageB = await browser.newPage();
        await registerUser(pageB, adminBEmail, 'Admin', 'CompanyB');
        await loginUser(pageB, adminBEmail);
        await createCompany(pageB, 'Company Beta');

        await navigateToUsersTab(pageB);
        await createInviteSimple(pageB, inviteBEmail);

        // ============================================
        // Verify Company A only sees their invite
        // ============================================
        await pageA.reload();
        await navigateToUsersTab(pageA);

        let emailsA = await getDisplayedUserEmails(pageA);
        expect(emailsA).toHaveLength(2); // admin + invite
        expect(emailsA).toContain(adminAEmail);
        expect(emailsA).toContain(inviteAEmail);
        expect(emailsA).not.toContain(adminBEmail);
        expect(emailsA).not.toContain(inviteBEmail);

        // ============================================
        // Verify Company B only sees their invite
        // ============================================
        await pageB.reload();
        await navigateToUsersTab(pageB);

        let emailsB = await getDisplayedUserEmails(pageB);
        expect(emailsB).toHaveLength(2); // admin + invite
        expect(emailsB).toContain(adminBEmail);
        expect(emailsB).toContain(inviteBEmail);
        expect(emailsB).not.toContain(adminAEmail);
        expect(emailsB).not.toContain(inviteAEmail);

        await pageA.close();
        await pageB.close();
    });
});

test.describe('Invite Regenerate and Delete', () => {
    test('regenerate invite shows new link with 7-day expiration', async ({page}) => {
        const adminEmail = uniqueEmail('admin');
        const invitedEmail = uniqueEmail('invited');

        // Setup admin with company and create invite
        await registerUser(page, adminEmail, 'Admin', 'User');
        await loginUser(page, adminEmail);
        await createCompany(page, 'Test Company');
        await navigateToUsersTab(page);
        await createInviteSimple(page, invitedEmail);

        // Wait for table to update
        await page.waitForTimeout(1000);

        // Find the row with the invited user
        const inviteRow = getUserRow(page, invitedEmail);
        await expect(inviteRow).toBeVisible();

        // Click the regenerate button
        const regenerateButton = inviteRow.locator('.btn-regenerate-user');
        await expect(regenerateButton).toBeVisible();
        await regenerateButton.click();

        // Verify the regenerate success overlay appears
        await expect(page.getByRole('heading', {name: 'Invite Regenerated'})).toBeVisible();
        await expect(page.getByText(`A new invitation link has been created for`)).toBeVisible();
        await expect(page.getByRole('strong')).toContainText(invitedEmail);

        // Verify the invite URL input is present
        const urlInput = page.locator('input.invite-url-input');
        await expect(urlInput).toBeVisible();
        const inviteUrl = await urlInput.inputValue();
        expect(inviteUrl).toContain('/register?invite=');

        // Verify expiration notice
        await expect(page.getByText('This link expires in 7 days')).toBeVisible();

        // Close the overlay
        await page.getByRole('button', {name: 'Done'}).click();
        await expect(page.getByText('Invite Regenerated')).not.toBeVisible();
    });

    test('regenerate invite copy button works', async ({page, context, browserName}) => {
        const adminEmail = uniqueEmail('admin');
        const invitedEmail = uniqueEmail('invited');

        // Grant clipboard permissions (only supported in Chromium via Playwright API)
        if (browserName === 'chromium') {
            await context.grantPermissions(['clipboard-read', 'clipboard-write']);
        }

        // Setup
        await registerUser(page, adminEmail, 'Admin', 'User');
        await loginUser(page, adminEmail);
        await createCompany(page, 'Test Company');
        await navigateToUsersTab(page);
        await createInviteSimple(page, invitedEmail);
        await page.waitForTimeout(1000);

        // Regenerate the invite
        const inviteRow = getUserRow(page, invitedEmail);
        await inviteRow.locator('.btn-regenerate-user').click();
        await expect(page.getByText('Invite Regenerated')).toBeVisible();

        // Get the URL before copying
        const urlInput = page.locator('input.invite-url-input');
        const inviteUrl = await urlInput.inputValue();

        // Click copy button
        await page.getByRole('button', {name: 'Copy'}).click();

        // Verify "Copied!" feedback appears
        await expect(page.getByRole('button', {name: 'Copied!'})).toBeVisible();

        // Verify clipboard contains the URL (only in Chromium where we can read clipboard)
        if (browserName === 'chromium') {
            const clipboardText = await page.evaluate(() => navigator.clipboard.readText());
            expect(clipboardText).toBe(inviteUrl);
        }

        // Close the overlay
        await page.getByRole('button', {name: 'Done'}).click();
    });

    test('delete invite removes it from the table', async ({page}) => {
        const adminEmail = uniqueEmail('admin');
        const invitedEmail = uniqueEmail('invited');

        // Setup
        await registerUser(page, adminEmail, 'Admin', 'User');
        await loginUser(page, adminEmail);
        await createCompany(page, 'Test Company');
        await navigateToUsersTab(page);
        await createInviteSimple(page, invitedEmail);
        await page.waitForTimeout(1000);

        // Verify invite is in the table
        let emails = await getDisplayedUserEmails(page);
        expect(emails).toContain(invitedEmail);

        // Find the invite row and click delete
        const inviteRow = getUserRow(page, invitedEmail);
        await inviteRow.locator('.btn-delete-user').click();

        // Verify confirmation dialog appears
        await expect(page.getByRole('heading', {name: 'Delete Invite'})).toBeVisible();
        await expect(page.getByText(`delete the invite for`)).toBeVisible();

        // Confirm deletion
        await page.getByRole('button', {name: 'Delete Invite', exact: true}).click();

        // Wait for deletion to complete
        await page.waitForTimeout(1000);

        // Verify invite is removed from table
        emails = await getDisplayedUserEmails(page);
        expect(emails).not.toContain(invitedEmail);
        expect(emails).toHaveLength(1); // Only admin remains
    });

    test('cancel delete invite keeps invite in table', async ({page}) => {
        const adminEmail = uniqueEmail('admin');
        const invitedEmail = uniqueEmail('invited');

        // Setup
        await registerUser(page, adminEmail, 'Admin', 'User');
        await loginUser(page, adminEmail);
        await createCompany(page, 'Test Company');
        await navigateToUsersTab(page);
        await createInviteSimple(page, invitedEmail);
        await page.waitForTimeout(1000);

        // Find the invite row and click delete
        const inviteRow = getUserRow(page, invitedEmail);
        await inviteRow.locator('.btn-delete-user').click();

        // Verify confirmation dialog appears
        await expect(page.getByRole('heading', {name: 'Delete Invite'})).toBeVisible();

        // Cancel deletion
        await page.getByRole('button', {name: 'Cancel'}).click();

        // Verify dialog closes
        await expect(page.getByRole('heading', {name: 'Delete Invite'})).not.toBeVisible();

        // Verify invite is still in table
        const emails = await getDisplayedUserEmails(page);
        expect(emails).toContain(invitedEmail);
    });

    test('regenerate button only shows for invited/expired users, not active', async ({page}) => {
        const adminEmail = uniqueEmail('admin');
        const invitedEmail = uniqueEmail('invited');

        // Setup
        await registerUser(page, adminEmail, 'Admin', 'User');
        await loginUser(page, adminEmail);
        await createCompany(page, 'Test Company');
        await navigateToUsersTab(page);
        await createInviteSimple(page, invitedEmail);
        await page.waitForTimeout(1000);

        // Admin row should have edit button but NOT regenerate button
        const adminRow = getUserRow(page, adminEmail);
        await expect(adminRow.locator('.btn-edit-user')).toBeVisible();
        await expect(adminRow.locator('.btn-regenerate-user')).not.toBeVisible();

        // Invited row should have regenerate button but NOT edit button
        const inviteRow = getUserRow(page, invitedEmail);
        await expect(inviteRow.locator('.btn-regenerate-user')).toBeVisible();
        await expect(inviteRow.locator('.btn-edit-user')).not.toBeVisible();
    });

    test('invite cannot be regenerated from another company', async ({browser}) => {
        const adminAEmail = uniqueEmail('admin-a');
        const adminBEmail = uniqueEmail('admin-b');
        const inviteAEmail = uniqueEmail('invite-a');

        // Setup Company A with an invite
        const pageA = await browser.newPage();
        await registerUser(pageA, adminAEmail, 'Admin', 'CompanyA');
        await loginUser(pageA, adminAEmail);
        await createCompany(pageA, 'Company Alpha');
        await navigateToUsersTab(pageA);
        await createInviteSimple(pageA, inviteAEmail);
        await pageA.waitForTimeout(1000);

        // Verify invite exists in Company A
        let emailsA = await getDisplayedUserEmails(pageA);
        expect(emailsA).toContain(inviteAEmail);

        // Setup Company B
        const pageB = await browser.newPage();
        await registerUser(pageB, adminBEmail, 'Admin', 'CompanyB');
        await loginUser(pageB, adminBEmail);
        await createCompany(pageB, 'Company Beta');
        await navigateToUsersTab(pageB);

        // Company B should not see Company A's invite
        const emailsB = await getDisplayedUserEmails(pageB);
        expect(emailsB).not.toContain(inviteAEmail);
        expect(emailsB).toHaveLength(1); // Only admin B

        await pageA.close();
        await pageB.close();
    });
});
