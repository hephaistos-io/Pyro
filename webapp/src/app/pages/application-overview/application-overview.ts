import {Component, computed, effect, inject, OnInit, signal} from '@angular/core';
import {Router} from '@angular/router';
import {ApiKeyResponse, ApplicationResponse, EnvironmentResponse} from '../../api/generated/models';
import {Api} from '../../api/generated/api';
import {createEnvironment, deleteEnvironment, getApiKeyByType, regenerateApiKey} from '../../api/generated/functions';

interface RequestTier {
    id: string;
    name: string;
    dailyLimit: number;
    monthlyPrice: number;
}

interface UserTier {
    id: string;
    name: string;
    maxUsers: number;
    monthlyPrice: number;
}

@Component({
    selector: 'app-application-overview',
    imports: [],
    templateUrl: './application-overview.html',
    styleUrl: './application-overview.scss',
})
export class ApplicationOverview implements OnInit {
    // Environments from ApplicationResponse
    environments = computed<EnvironmentResponse[]>(() => this.application()?.environments ?? []);
    selectedEnvironment = signal<EnvironmentResponse | null>(null);

    application = signal<ApplicationResponse | null>(null);
    applicationName = computed(() => this.application()?.name ?? 'Application');

    // ============================================================================
    // MOCK DATA - Replace with API calls when backend endpoints are available
    // ============================================================================

    // Pricing constants (will come from backend pricing service)
    readonly environmentFee = 10; // Additional environments cost $10/mo (first one is free)
    isCreatingEnvironment = signal(false);
    isDeletingEnvironment = signal(false);
    // Check if selected environment can be deleted (only PAID tier can be deleted)
    canDeleteEnvironment = computed(() => {
        const env = this.selectedEnvironment();
        return env?.tier === 'PAID';
    });
    showEnvironmentCreation = signal(false);
    showEnvironmentDropdown = signal(false);
    environmentSearchQuery = signal('');

  // API key visibility and state
    showReadKey = signal(false);
    showWriteKey = signal(false);
  readKey = signal<string | null>(null);
  writeKey = signal<string | null>(null);
  readKeyData = signal<ApiKeyResponse | null>(null);
  writeKeyData = signal<ApiKeyResponse | null>(null);
  isLoadingReadKey = signal(false);
  isLoadingWriteKey = signal(false);

    // Key refresh confirmation
    showKeyRefreshConfirmation = signal(false);
    keyToRefresh = signal<'read' | 'write' | null>(null);
  isRefreshingKey = signal(false);

    // Environment deletion confirmation
    showEnvironmentDeletion = signal(false);
    // Mock total users - Replace with actual stats from backend
    totalUsers = signal(0);
    // Mock total hits this month - Replace with actual stats from backend
    totalHitsThisMonth = signal(0);

    // Mock request tiers - Replace with: this.api.invoke(getRequestTiers)
    requestTiers = signal<RequestTier[]>([
        {id: 'tier1', name: '1k', dailyLimit: 1000, monthlyPrice: 0},
        {id: 'tier2', name: '10k', dailyLimit: 10000, monthlyPrice: 19},
        {id: 'tier3', name: '50k', dailyLimit: 50000, monthlyPrice: 49},
        {id: 'tier4', name: '500k', dailyLimit: 500000, monthlyPrice: 149},
    ]);
    selectedRequestTierIndex = signal(2); // Default to 50k (index 2)
    currentRequestTier = computed(() => this.requestTiers()[this.selectedRequestTierIndex()]);

    // Mock user tiers - Replace with: this.api.invoke(getUserTiers)
    userTiers = signal<UserTier[]>([
        {id: 'tier1', name: '100', maxUsers: 100, monthlyPrice: 0},
        {id: 'tier2', name: '1k', maxUsers: 1000, monthlyPrice: 29},
        {id: 'tier3', name: '10k', maxUsers: 10000, monthlyPrice: 79},
        {id: 'tier4', name: '100k', maxUsers: 100000, monthlyPrice: 199},
    ]);
    selectedUserTierIndex = signal(2); // Default to 10k (index 2)
    currentUserTier = computed(() => this.userTiers()[this.selectedUserTierIndex()]);

    // Combined monthly price for selected environment
    totalMonthlyPrice = computed(() =>
        this.currentRequestTier().monthlyPrice + this.currentUserTier().monthlyPrice
    );
    // Pricing breakdown per environment (simplified since we don't have stats from API yet)
    pricingBreakdown = computed(() => {
        const envs = this.environments();
        return envs.map(env => ({
            id: env.id,
            name: env.name,
            tier: env.tier,
            description: env.description,
            requestTierPrice: 0,
            userTierPrice: 0,
            total: 0
        }));
    });
    // Computed identifier (lowercase environment name)
    environmentIdentifier = computed(() => {
        const env = this.selectedEnvironment();
        return env ? env.name?.toLowerCase().replace(/\s+/g, '-') ?? '' : '';
    });

    // Environment count
    environmentCount = computed(() => this.environments().length);
    // Computed filtered environments
    filteredEnvironments = computed(() => {
        const query = this.environmentSearchQuery().toLowerCase();
        const envs = this.environments();

        if (!query) {
            return envs;
        }

        return envs.filter(env =>
          env.name?.toLowerCase().includes(query) ||
            env.description?.toLowerCase().includes(query)
        );
    });

    // Additional environment fees (first one is free)
    additionalEnvironmentFees = computed(() => {
        const count = this.environments().length;
        return count > 1 ? (count - 1) * this.environmentFee : 0;
    });

    // Total monthly price for entire application
    applicationTotalMonthlyPrice = computed(() => {
        const envPrices = this.pricingBreakdown().reduce((sum, env) => sum + env.total, 0);
        return envPrices + this.additionalEnvironmentFees();
    });

    // Maximum price for chart scaling
    maxEnvironmentPrice = computed(() => {
        const breakdown = this.pricingBreakdown();
        return Math.max(...breakdown.map(env => env.total), 1);
    });
    private router = inject(Router);

    // Mock usage statistics - Replace with: this.api.invoke(getUsageStats, {envId})
    usageStats = signal({
        fetchesToday: 12847,
        dailyLimit: 50000,
        users: 7954,
        totalThisMonth: 287432,
        avgResponseTime: 42, // ms
    });

    // Mock leniency tracking - Replace with: this.api.invoke(getLeniencyStats, {envId})
    leniencyStats = signal({
        used: 2,
        allowed: 2,
    });

    // Mock weekly fetch data - Replace with: this.api.invoke(getWeeklyFetches, {envId})
    weeklyFetches = signal([
        {day: 'Mon', fetches: 38420, limit: 50000},
        {day: 'Tue', fetches: 50000, limit: 50000},  // Hit limit
        {day: 'Wed', fetches: 35890, limit: 50000},
        {day: 'Thu', fetches: 51200, limit: 50000},  // Exceeded limit
        {day: 'Fri', fetches: 31560, limit: 50000},
        {day: 'Sat', fetches: 18940, limit: 50000},
        {day: 'Sun', fetches: 12847, limit: 50000},
    ]);

    // Computed high risk alert based on leniency usage
    highRiskAlert = computed(() => {
        const leniency = this.leniencyStats();
        if (leniency.used >= leniency.allowed) {
            return {
                message: `We offer leniency for exceeded limits ${leniency.allowed} times per month to prevent application downtime. This allowance has been fully used. Your application is at high risk of service interruption.`
            };
        }
        return null;
    });

    // Computed usage percentage
    usagePercentage = computed(() => {
        const stats = this.usageStats();
        return Math.round((stats.fetchesToday / stats.dailyLimit) * 100);
    });

    // Computed recommendation based on usage patterns
    usageRecommendation = computed(() => {
        const fetches = this.weeklyFetches();
        const daysAtLimit = fetches.filter(day => day.fetches >= day.limit).length;

        if (daysAtLimit >= 2) {
            return {
                type: 'warning' as const,
                message: `You've hit the limit ${daysAtLimit} times this week. Consider upgrading to a higher tier.`
            };
        } else if (daysAtLimit === 1) {
            return {
                type: 'info' as const,
                message: 'You hit your daily limit once this week. Monitor your usage to avoid interruptions.'
            };
        } else if (this.usagePercentage() > 80) {
            return {
                type: 'info' as const,
                message: 'You\'re approaching your daily limit. Consider your usage patterns.'
            };
        }
        return null;
    });
    private api = inject(Api);

    constructor() {
        // Auto-select first environment when environments change
        effect(() => {
            const envs = this.environments();
            const selected = this.selectedEnvironment();
            if (envs.length > 0 && !selected) {
                this.selectedEnvironment.set(envs[0]);
            }
        });

      // Clear API key state when environment changes
      effect(() => {
        // Just reading selectedEnvironment triggers the effect when it changes
        this.selectedEnvironment();
        // Clear all key state
        this.showReadKey.set(false);
        this.showWriteKey.set(false);
        this.readKey.set(null);
        this.writeKey.set(null);
        this.readKeyData.set(null);
        this.writeKeyData.set(null);
      }, {allowSignalWrites: true});
    }

    // Format number with commas
    formatNumber(num: number): string {
        return num.toLocaleString();
    }

    ngOnInit(): void {
        // Get application from router state
        const state = this.router.getCurrentNavigation()?.extras.state;
        if (state && state['application']) {
            this.application.set(state['application']);
        } else {
            // If no state (e.g., direct navigation), try to get from history state
            const historyState = history.state;
            if (historyState && historyState['application']) {
                this.application.set(historyState['application']);
            } else {
                // No application data, redirect back to dashboard
                this.router.navigate(['/dashboard']);
            }
        }
    }

    goBack(): void {
        this.router.navigate(['/dashboard']);
    }

    toggleEnvironmentDropdown(): void {
        this.showEnvironmentDropdown.update(show => !show);
        if (this.showEnvironmentDropdown()) {
            this.environmentSearchQuery.set('');
        }
    }

    closeEnvironmentDropdown(): void {
        this.showEnvironmentDropdown.set(false);
        this.environmentSearchQuery.set('');
    }

    selectEnvironment(environment: EnvironmentResponse): void {
        this.selectedEnvironment.set(environment);
        this.closeEnvironmentDropdown();
    }

    onSearchQueryChange(query: string): void {
        this.environmentSearchQuery.set(query);
    }

    onAddEnvironmentClick(): void {
        this.closeEnvironmentDropdown();
        this.showEnvironmentCreation.set(true);
    }

    onCloseEnvironmentCreation(): void {
        this.showEnvironmentCreation.set(false);
    }

    async onEnvironmentCreated(name: string, description?: string): Promise<void> {
        const app = this.application();
        if (!app?.id) {
            return;
        }

        this.isCreatingEnvironment.set(true);
        try {
            const newEnv = await this.api.invoke(createEnvironment, {
                applicationId: app.id,
                body: {
                    name: name,
                    description: description ?? ''
                }
            });

            // Update the application with the new environment
            this.application.update(current => {
                if (!current) return current;
                return {
                    ...current,
                    environments: [...(current.environments ?? []), newEnv]
                };
            });

            this.selectedEnvironment.set(newEnv);
            this.showEnvironmentCreation.set(false);
        } catch (error) {
            console.error('Failed to create environment:', error);
        } finally {
            this.isCreatingEnvironment.set(false);
        }
    }

    // API Key methods
  async toggleReadKeyVisibility(): Promise<void> {
    if (this.showReadKey()) {
      // Hide the key - clear the state
      this.showReadKey.set(false);
      this.readKey.set(null);
    } else {
      // Show the key - fetch from backend
      await this.fetchReadKey();
      this.showReadKey.set(true);
    }
  }

  async toggleWriteKeyVisibility(): Promise<void> {
    if (this.showWriteKey()) {
      // Hide the key - clear the state
      this.showWriteKey.set(false);
      this.writeKey.set(null);
    } else {
      // Show the key - fetch from backend
      await this.fetchWriteKey();
      this.showWriteKey.set(true);
    }
  }

  async confirmKeyRefresh(): Promise<void> {
    const keyType = this.keyToRefresh();
    const app = this.application();
    const env = this.selectedEnvironment();

    if (!keyType || !app?.id || !env?.id) {
      this.showKeyRefreshConfirmation.set(false);
      this.keyToRefresh.set(null);
      return;
    }

    this.isRefreshingKey.set(true);
    try {
      const response = await this.api.invoke(regenerateApiKey, {
        applicationId: app.id,
        environmentId: env.id,
        keyType: keyType === 'read' ? 'READ' : 'WRITE'
      });

      // Update the key and key data with the new secret
      if (keyType === 'read') {
        this.readKey.set(response.secretKey ?? null);
        this.readKeyData.set(response);
      } else {
        this.writeKey.set(response.secretKey ?? null);
        this.writeKeyData.set(response);
      }
    } catch (error) {
      console.error('Failed to regenerate key:', error);
    } finally {
      this.isRefreshingKey.set(false);
      this.showKeyRefreshConfirmation.set(false);
      this.keyToRefresh.set(null);
    }
  }

  getKeyPlaceholder(): string {
    return '••••••••••••••••••••••••';
    }

    requestKeyRefresh(keyType: 'read' | 'write'): void {
        this.keyToRefresh.set(keyType);
        this.showKeyRefreshConfirmation.set(true);
    }

    cancelKeyRefresh(): void {
        this.showKeyRefreshConfirmation.set(false);
        this.keyToRefresh.set(null);
    }

  private async fetchReadKey(): Promise<void> {
    const app = this.application();
    const env = this.selectedEnvironment();
    if (!app?.id || !env?.id) return;

    this.isLoadingReadKey.set(true);
    try {
      const response = await this.api.invoke(getApiKeyByType, {
        applicationId: app.id,
        environmentId: env.id,
        keyType: 'READ'
      });
      this.readKeyData.set(response);
      this.readKey.set(response.secretKey ?? null);
    } catch (error) {
      console.error('Failed to fetch read key:', error);
      this.readKey.set(null);
    } finally {
      this.isLoadingReadKey.set(false);
    }
    }

  private async fetchWriteKey(): Promise<void> {
    const app = this.application();
    const env = this.selectedEnvironment();
    if (!app?.id || !env?.id) return;

    this.isLoadingWriteKey.set(true);
    try {
      const response = await this.api.invoke(getApiKeyByType, {
        applicationId: app.id,
        environmentId: env.id,
        keyType: 'WRITE'
      });
      this.writeKeyData.set(response);
      this.writeKey.set(response.secretKey ?? null);
    } catch (error) {
      console.error('Failed to fetch write key:', error);
      this.writeKey.set(null);
    } finally {
      this.isLoadingWriteKey.set(false);
    }
    }

    // Environment deletion methods
    requestEnvironmentDeletion(): void {
        // Only allow deletion for PAID tier environments
        if (this.canDeleteEnvironment()) {
            this.showEnvironmentDeletion.set(true);
        }
    }

    cancelEnvironmentDeletion(): void {
        this.showEnvironmentDeletion.set(false);
    }

    async confirmEnvironmentDeletion(): Promise<void> {
        const app = this.application();
        const currentEnv = this.selectedEnvironment();

        if (!app?.id || !currentEnv?.id) {
            return;
        }

        // Double-check tier before deletion
        if (currentEnv.tier === 'FREE') {
            console.error('Cannot delete FREE tier environment');
            this.showEnvironmentDeletion.set(false);
            return;
        }

        this.isDeletingEnvironment.set(true);
        try {
            await this.api.invoke(deleteEnvironment, {
                applicationId: app.id,
                environmentId: currentEnv.id
            });

            // Update application to remove the environment
            this.application.update(current => {
                if (!current) return current;
                return {
                    ...current,
                    environments: (current.environments ?? []).filter(env => env.id !== currentEnv.id)
                };
            });

            // Select another environment if available
            const remaining: EnvironmentResponse[] = this.environments();
            if (remaining.length > 0) {
                this.selectedEnvironment.set(remaining[0] as EnvironmentResponse);
            } else {
                this.selectedEnvironment.set(null);
            }

            this.showEnvironmentDeletion.set(false);
        } catch (error) {
            console.error('Failed to delete environment:', error);
        } finally {
            this.isDeletingEnvironment.set(false);
        }
    }

    // Request Tier methods
    onRequestTierSliderChange(event: Event): void {
        const input = event.target as HTMLInputElement;
        this.selectedRequestTierIndex.set(parseInt(input.value, 10));
    }

    // User Tier methods
    onUserTierSliderChange(event: Event): void {
        const input = event.target as HTMLInputElement;
        this.selectedUserTierIndex.set(parseInt(input.value, 10));
    }

    formatLimit(limit: number): string {
        if (limit >= 1000000) {
            return (limit / 1000000) + 'M';
        } else if (limit >= 1000) {
            return (limit / 1000) + 'k';
        }
        return limit.toString();
    }
}
