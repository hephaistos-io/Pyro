import {expect, Page, test} from '@playwright/test';
import {setupUserWithApplication} from '../../utils';

test.describe('Usage Statistics', () => {
    test.describe.configure({mode: 'serial'});

    let sharedPage: Page;
    let sharedAppName: string;

    test.beforeAll(async ({browser}) => {
        const context = await browser.newContext();
        sharedPage = await context.newPage();
        const setup = await setupUserWithApplication(sharedPage);
        sharedAppName = setup.appName;
    });

    test.afterAll(async () => {
        await sharedPage.close();
    });

    test('displays usage overview card on application overview', async () => {
        // Navigate to the application overview page
        await sharedPage.goto('/dashboard');
        await sharedPage.getByRole('button', {name: sharedAppName}).click();

        // Wait for the overview tab to be visible
        await expect(sharedPage.getByRole('button', {name: 'Overview', exact: true})).toBeVisible();

        // Verify the Usage Overview card is visible
        await expect(sharedPage.getByRole('heading', {name: 'Usage Overview'})).toBeVisible();
    });

    test('displays period selector with 7d, 15d, 30d options', async () => {
        // Verify the period selector buttons are visible
        const periodSelector = sharedPage.locator('.usage-stats-card__period-selector');
        await expect(periodSelector).toBeVisible();

        await expect(periodSelector.getByRole('button', {name: '7d'})).toBeVisible();
        await expect(periodSelector.getByRole('button', {name: '15d'})).toBeVisible();
        await expect(periodSelector.getByRole('button', {name: '30d'})).toBeVisible();
    });

    test('7d button can be selected and shows active state', async () => {
        const periodSelector = sharedPage.locator('.usage-stats-card__period-selector');
        const btn7d = periodSelector.getByRole('button', {name: '7d'});

        // Click 7d button to select it
        await btn7d.click();

        // Verify the 7d button has the active class
        await expect(btn7d).toHaveClass(/active/);
    });

    test('displays stats grid with correct labels when data exists', async () => {
        const statsGrid = sharedPage.locator('.usage-stats');
        const emptyState = sharedPage.locator('.usage-chart__empty');

        // For a new app, there may be no data yet - skip label check if empty state is shown
        const hasEmptyState = await emptyState.isVisible();
        if (hasEmptyState) {
            // Empty state is expected for new applications
            return;
        }

        // Verify the stats labels are present when data exists
        await expect(statsGrid.getByText('Requests today')).toBeVisible();
        await expect(statsGrid.getByText('Peak req/sec today')).toBeVisible();
        await expect(statsGrid.getByText('Avg req/sec today')).toBeVisible();
        await expect(statsGrid.getByText('Rejected today')).toBeVisible();
        await expect(statsGrid.getByText(/Total \(\d+ days\)/)).toBeVisible();
    });

    test('clicking 15d button updates selection', async () => {
        const periodSelector = sharedPage.locator('.usage-stats-card__period-selector');
        const btn15d = periodSelector.getByRole('button', {name: '15d'});

        await btn15d.click();

        // Verify 15d button now has active class
        await expect(btn15d).toHaveClass(/active/);

        // Wait for loading to complete (if any)
        await expect(sharedPage.getByText('Loading statistics...')).not.toBeVisible();

        // Verify the total label updates to show 15 days (only if stats grid is visible)
        const hasEmptyState = await sharedPage.getByText('No usage data available yet').isVisible();
        if (!hasEmptyState) {
            await expect(sharedPage.getByText('Total (15 days)')).toBeVisible();
        }
    });

    test('clicking 30d button updates selection', async () => {
        const periodSelector = sharedPage.locator('.usage-stats-card__period-selector');
        const btn30d = periodSelector.getByRole('button', {name: '30d'});

        await btn30d.click();

        // Verify 30d button now has active class
        await expect(btn30d).toHaveClass(/active/);

        // Wait for loading to complete (if any)
        await expect(sharedPage.getByText('Loading statistics...')).not.toBeVisible();

        // Verify the total label updates to show 30 days (only if stats grid is visible)
        const hasEmptyState = await sharedPage.getByText('No usage data available yet').isVisible();
        if (!hasEmptyState) {
            await expect(sharedPage.getByText('Total (30 days)')).toBeVisible();
        }
    });

    test('usage chart or empty state is visible', async () => {
        // Reset to 7 days
        const periodSelector = sharedPage.locator('.usage-stats-card__period-selector');
        await periodSelector.getByRole('button', {name: '7d'}).click();

        // Wait for either the chart OR empty state to be visible
        // Using locator.or() to match either element
        const chartOrEmpty = sharedPage.locator('.usage-chart').or(sharedPage.locator('.usage-chart__empty'));
        await expect(chartOrEmpty).toBeVisible();
    });

    test('card can be collapsed and expanded', async () => {
        // Find and click the header to collapse
        const header = sharedPage.locator('.usage-stats-card__header');
        await header.click();

        // Content should be hidden
        const content = sharedPage.locator('.usage-stats-card__content');
        await expect(content).toBeHidden();

        // Click again to expand
        await header.click();

        // Content should be visible again
        await expect(content).toBeVisible();
    });

    test('shows empty state message when no usage data', async () => {
        // For a new application without any API calls, it should show empty or loading state
        // Check that either the chart with bars OR empty state is shown
        const emptyState = sharedPage.locator('.usage-chart__empty');
        const chartBars = sharedPage.locator('.usage-chart__bar-container');

        // One of these should be present - either empty state or chart with data
        const hasEmptyState = await emptyState.isVisible();
        const hasChartBars = await chartBars.first().isVisible().catch(() => false);

        // At least one should be true (new app has no data so likely empty state)
        expect(hasEmptyState || hasChartBars).toBeTruthy();
    });

    test('rejected stat shows 0 and no warning style for new application', async () => {
        const emptyState = sharedPage.locator('.usage-chart__empty');

        // For a new app, there may be no data yet - skip if empty state is shown
        const hasEmptyState = await emptyState.isVisible();
        if (hasEmptyState) {
            // Empty state is expected for new applications - stats grid won't be visible
            return;
        }

        const statsGrid = sharedPage.locator('.usage-stats');
        const rejectedStat = statsGrid.locator('.usage-stat').filter({hasText: 'Rejected today'});

        // Verify the stat exists and shows 0
        await expect(rejectedStat).toBeVisible();
        await expect(rejectedStat.locator('.usage-stat__value')).toContainText('0');

        // Verify no warning class is applied (no rejections = no warning)
        await expect(rejectedStat).not.toHaveClass(/usage-stat--warning/);
    });
});
