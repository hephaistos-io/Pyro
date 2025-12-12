import {Component, computed, inject, OnInit, signal} from '@angular/core';
import {Router} from '@angular/router';
import {ApplicationResponse} from '../../api/generated/models';

interface EnvironmentStats {
    users: number;
    hitsThisMonth: number;
    requestTierIndex: number;
    userTierIndex: number;
}

interface Environment {
    id: string;
    name: string;
    type: 'production' | 'staging' | 'development' | 'regional';
    description?: string;
    stats: EnvironmentStats;
}

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
    application = signal<ApplicationResponse | null>(null);
    applicationName = computed(() => this.application()?.name ?? 'Application');
    // Pricing constants
    readonly environmentFee = 10; // Additional environments cost $10/mo (first one is free)
    // Environment management (mocked for now)
    environments = signal<Environment[]>([
        {
            id: '1',
            name: 'Production',
            type: 'production',
            description: 'Live production environment',
            stats: {users: 7954, hitsThisMonth: 287432, requestTierIndex: 2, userTierIndex: 2}
        },
        {
            id: '2',
            name: 'Staging',
            type: 'staging',
            description: 'Pre-production testing',
            stats: {users: 234, hitsThisMonth: 15420, requestTierIndex: 1, userTierIndex: 0}
        },
        {
            id: '3',
            name: 'Germany',
            type: 'regional',
            description: 'Regional deployment for Germany',
            stats: {users: 1250, hitsThisMonth: 45000, requestTierIndex: 1, userTierIndex: 1}
        },
        {
            id: '4',
            name: 'Development',
            type: 'development',
            description: 'Local development environment',
            stats: {users: 12, hitsThisMonth: 890, requestTierIndex: 0, userTierIndex: 0}
        },
        {
            id: '5',
            name: 'France',
            type: 'regional',
            description: 'Regional deployment for France',
            stats: {users: 890, hitsThisMonth: 32100, requestTierIndex: 1, userTierIndex: 1}
        },
        {
            id: '6',
            name: 'UK',
            type: 'regional',
            description: 'Regional deployment for United Kingdom',
            stats: {users: 2100, hitsThisMonth: 78500, requestTierIndex: 2, userTierIndex: 1}
        }
    ]);
    selectedEnvironment = signal<Environment | null>(null);
    showEnvironmentCreation = signal(false);
    showEnvironmentDropdown = signal(false);
    environmentSearchQuery = signal('');
    // API key visibility
    showReadKey = signal(false);
    showWriteKey = signal(false);
    // Mock API keys (in real app would come from API)
    readKey = signal('rk_live_a1b2c3d4e5f6g7h8i9j0');
    writeKey = signal('wk_live_x9y8z7w6v5u4t3s2r1q0');
    // Key refresh confirmation
    showKeyRefreshConfirmation = signal(false);
    keyToRefresh = signal<'read' | 'write' | null>(null);
    // Environment deletion confirmation
    showEnvironmentDeletion = signal(false);
    // Request Tier management
    requestTiers = signal<RequestTier[]>([
        {id: 'tier1', name: '1k', dailyLimit: 1000, monthlyPrice: 0},
        {id: 'tier2', name: '10k', dailyLimit: 10000, monthlyPrice: 19},
        {id: 'tier3', name: '50k', dailyLimit: 50000, monthlyPrice: 49},
        {id: 'tier4', name: '500k', dailyLimit: 500000, monthlyPrice: 149},
    ]);
    selectedRequestTierIndex = signal(2); // Default to 50k (index 2)
    currentRequestTier = computed(() => this.requestTiers()[this.selectedRequestTierIndex()]);
    // User Tier management
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
    // Total users across all environments
    totalUsers = computed(() =>
        this.environments().reduce((sum, env) => sum + env.stats.users, 0)
    );

    // ===== Application-level aggregated stats =====
    // Total hits this month across all environments
    totalHitsThisMonth = computed(() =>
        this.environments().reduce((sum, env) => sum + env.stats.hitsThisMonth, 0)
    );
    // Environment count
    environmentCount = computed(() => this.environments().length);
    // Pricing breakdown per environment
    pricingBreakdown = computed(() => {
        const envs = this.environments();
        const requestTiers = this.requestTiers();
        const userTiers = this.userTiers();

        return envs.map(env => ({
            id: env.id,
            name: env.name,
            requestTierPrice: requestTiers[env.stats.requestTierIndex]?.monthlyPrice ?? 0,
            userTierPrice: userTiers[env.stats.userTierIndex]?.monthlyPrice ?? 0,
            total: (requestTiers[env.stats.requestTierIndex]?.monthlyPrice ?? 0) +
                (userTiers[env.stats.userTierIndex]?.monthlyPrice ?? 0)
        }));
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
    // Computed identifier (lowercase environment name)
    environmentIdentifier = computed(() => {
        const env = this.selectedEnvironment();
        return env ? env.name.toLowerCase().replace(/\s+/g, '-') : '';
    });
    // Usage statistics (mocked)
    usageStats = signal({
        fetchesToday: 12847,
        dailyLimit: 50000,
        users: 7954,
        totalThisMonth: 287432,
        avgResponseTime: 42, // ms
    });
    // Leniency tracking (mocked) - 2 allowed per month
    leniencyStats = signal({
        used: 2,
        allowed: 2,
    });
    // Last 7 days fetch data (mocked)
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
    // Computed filtered environments
    filteredEnvironments = computed(() => {
        const query = this.environmentSearchQuery().toLowerCase();
        const envs = this.environments();

        if (!query) {
            return envs;
        }

        return envs.filter(env =>
            env.name.toLowerCase().includes(query) ||
            env.description?.toLowerCase().includes(query)
        );
    });
    private router = inject(Router);

    // Calculate monthly price for a specific environment
    getEnvironmentMonthlyPrice(env: Environment): number {
        const requestTiers = this.requestTiers();
        const userTiers = this.userTiers();
        const requestPrice = requestTiers[env.stats.requestTierIndex]?.monthlyPrice ?? 0;
        const userPrice = userTiers[env.stats.userTierIndex]?.monthlyPrice ?? 0;
        return requestPrice + userPrice;
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

        // Auto-select first environment if available
        const envs = this.environments();
        if (envs.length > 0) {
            this.selectedEnvironment.set(envs[0]);
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

    selectEnvironment(environment: Environment): void {
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

    onEnvironmentCreated(name: string, description?: string): void {
        // Mock creation - add new environment to the list
        const newEnv: Environment = {
            id: Date.now().toString(),
            name: name,
            type: 'development',
            description: description || undefined,
            stats: {users: 0, hitsThisMonth: 0, requestTierIndex: 0, userTierIndex: 0}
        };
        this.environments.update(envs => [...envs, newEnv]);
        this.selectedEnvironment.set(newEnv);
        this.showEnvironmentCreation.set(false);
    }

    // API Key methods
    toggleReadKeyVisibility(): void {
        this.showReadKey.update(show => !show);
    }

    toggleWriteKeyVisibility(): void {
        this.showWriteKey.update(show => !show);
    }

    requestKeyRefresh(keyType: 'read' | 'write'): void {
        this.keyToRefresh.set(keyType);
        this.showKeyRefreshConfirmation.set(true);
    }

    cancelKeyRefresh(): void {
        this.showKeyRefreshConfirmation.set(false);
        this.keyToRefresh.set(null);
    }

    confirmKeyRefresh(): void {
        const keyType = this.keyToRefresh();
        if (keyType === 'read') {
            // Mock new key generation
            this.readKey.set('rk_live_' + Math.random().toString(36).substring(2, 22));
        } else if (keyType === 'write') {
            this.writeKey.set('wk_live_' + Math.random().toString(36).substring(2, 22));
        }
        this.showKeyRefreshConfirmation.set(false);
        this.keyToRefresh.set(null);
    }

    maskApiKey(key: string): string {
        if (key.length <= 8) return '•'.repeat(key.length);
        return key.substring(0, 8) + '•'.repeat(key.length - 8);
    }

    // Environment deletion methods
    requestEnvironmentDeletion(): void {
        this.showEnvironmentDeletion.set(true);
    }

    cancelEnvironmentDeletion(): void {
        this.showEnvironmentDeletion.set(false);
    }

    confirmEnvironmentDeletion(): void {
        const currentEnv = this.selectedEnvironment();
        if (currentEnv) {
            // Remove the environment from the list
            this.environments.update(envs => envs.filter(env => env.id !== currentEnv.id));

            // Select another environment if available
            const remaining = this.environments();
            if (remaining.length > 0) {
                this.selectedEnvironment.set(remaining[0]);
            } else {
                this.selectedEnvironment.set(null);
            }
        }
        this.showEnvironmentDeletion.set(false);
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
