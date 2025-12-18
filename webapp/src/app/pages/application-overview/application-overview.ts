import {Component, computed, effect, inject, OnInit, signal} from '@angular/core';
import {Router} from '@angular/router';
import {ApiKeyResponse, ApplicationResponse, EnvironmentResponse} from '../../api/generated/models';
import {Api} from '../../api/generated/api';
import {createEnvironment} from '../../api/generated/fn/environments/create-environment';
import {deleteEnvironment} from '../../api/generated/fn/environments/delete-environment';
import {getApiKeyByType} from '../../api/generated/fn/api-keys/get-api-key-by-type';
import {regenerateApiKey} from '../../api/generated/fn/api-keys/regenerate-api-key';
import {FormsModule} from '@angular/forms';

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

// ============================================================================
// TEMPLATE TYPES
// ============================================================================

// System Template: simple key-value pairs
interface SystemTemplateField {
  key: string;
  value: string;
  description?: string;
}

// User Template: field schema (app-level) + default values (env-level)
type FieldType = 'string' | 'number' | 'boolean' | 'enum';

interface UserFieldSchema {
  key: string;
  type: FieldType;
  description?: string;
  constraints?: {
    min?: number;        // For number type
    max?: number;        // For number type
    maxLength?: number;  // For string type
    options?: string[];  // For enum type
  };
}

// Combined view: schema + environment-specific default value
interface UserTemplateField extends UserFieldSchema {
  defaultValue: string;  // Environment-specific default
}

interface IdentifierOverride {
  identifier: string;
  systemOverrides: SystemTemplateField[];
  userOverrides: { key: string; value: string }[];  // Override just the default value
}

// Environment-specific template data
interface EnvironmentTemplateData {
  systemTemplate: { fields: SystemTemplateField[] };
  userTemplateDefaults: { key: string; value: string }[];  // Env-specific defaults
  identifierOverrides: IdentifierOverride[];
}

@Component({
    selector: 'app-application-overview',
  imports: [FormsModule],
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

  // ============================================================================
  // TEMPLATE MOCK DATA - Replace with API calls when backend endpoints are available
  // ============================================================================

  // Card collapse state (expanded by default)
  apiAttributesExpanded = signal(true);
  usageOverviewExpanded = signal(true);

  // Tab navigation
  activeTab = signal<'overview' | 'configuration' | 'overrides'>('overview');

  // Overrides tab state
  overridesTemplateType = signal<'system' | 'user'>('system');
  overridesSearchQuery = signal('');
  showOnlyOverrides = signal(false);
  editingMatrixCell = signal<{ identifier: string; attribute: string } | null>(null);

  // Template (Configuration) tab state
  templateTemplateType = signal<'system' | 'user'>('system');
  templateSearchQuery = signal('');
  showFieldsWithDefaults = signal(false);
  editingTemplateCell = signal<{ fieldKey: string; property: string } | null>(null);

  // Override sections expand state (legacy, kept for template cards)
  showSystemOverrides = signal(false);
  showUserOverrides = signal(false);

  // Inline editing state
  editingSystemField = signal<{ key: string; field: 'key' | 'value' } | null>(null);
  editingUserField = signal<{ key: string; field: 'key' | 'type' | 'defaultValue' } | null>(null);
  editingIdentifier = signal<{ identifier: string; template: 'system' | 'user'; field: string } | null>(null);
  editValue = signal('');

  // New field forms
  showAddSystemField = signal(false);
  showAddUserField = signal(false);
  showAddIdentifier = signal(false);
  newSystemFieldKey = signal('');
  newSystemFieldValue = signal('');
  newUserFieldKey = signal('');
  newUserFieldType = signal<FieldType>('string');
  newIdentifierName = signal('');

  // APPLICATION-LEVEL: User template schema (same across all environments)
  userTemplateSchema = signal<UserFieldSchema[]>([
    {key: 'city', type: 'string', description: 'User\'s city of residence', constraints: {maxLength: 100}},
    {
      key: 'language',
      type: 'enum',
      description: 'Preferred language for UI',
      constraints: {options: ['french', 'german', 'english', 'spanish']}
    },
    {key: 'age', type: 'number', description: 'User age for age-gated features', constraints: {min: 0, max: 120}},
    {key: 'notifications_enabled', type: 'boolean', description: 'Enable push notifications'},
    {key: 'max_items', type: 'number', description: 'Maximum items per page', constraints: {min: 1, max: 100}}
  ]);

  // ENVIRONMENT-LEVEL: Template data by environment name
  templateData = signal<Record<string, EnvironmentTemplateData>>({
    'Development': {
      systemTemplate: {
        fields: [
          {key: 'name', value: 'dev', description: 'Environment name identifier'},
          {key: 'country', value: 'france', description: 'Default country code'},
          {key: 'region', value: 'eu-west', description: 'Cloud region for deployment'},
          {key: 'api_version', value: 'v2', description: 'API version to use'}
        ]
      },
      userTemplateDefaults: [
        {key: 'city', value: 'paris'},
        {key: 'language', value: 'french'},
        {key: 'age', value: '18'},
        {key: 'notifications_enabled', value: 'true'},
        {key: 'max_items', value: '10'}
      ],
      identifierOverrides: [
        {identifier: 'FR', systemOverrides: [], userOverrides: []},
        {
          identifier: 'DE',
          systemOverrides: [{key: 'country', value: 'germany'}, {key: 'region', value: 'eu-central'}],
          userOverrides: [{key: 'city', value: 'berlin'}, {key: 'language', value: 'german'}]
        },
        {
          identifier: 'ES',
          systemOverrides: [{key: 'country', value: 'spain'}],
          userOverrides: [{key: 'city', value: 'madrid'}, {key: 'language', value: 'spanish'}]
        }
      ]
    },
    'Production': {
      systemTemplate: {
        fields: [
          {key: 'name', value: 'prod', description: 'Environment name identifier'},
          {key: 'country', value: 'usa', description: 'Default country code'},
          {key: 'region', value: 'us-east', description: 'Cloud region for deployment'},
          {key: 'api_version', value: 'v2', description: 'API version to use'}
        ]
      },
      userTemplateDefaults: [
        {key: 'city', value: 'new york'},
        {key: 'language', value: 'english'},
        {key: 'age', value: '21'},
        {key: 'notifications_enabled', value: 'false'},
        {key: 'max_items', value: '25'}
      ],
      identifierOverrides: [
        {identifier: 'US-EAST', systemOverrides: [], userOverrides: []},
        {
          identifier: 'US-WEST',
          systemOverrides: [{key: 'region', value: 'us-west'}],
          userOverrides: [{key: 'city', value: 'los angeles'}]
        }
      ]
    }
  });

  // Computed: current template data for selected environment
  currentTemplateData = computed(() => {
    const env = this.selectedEnvironment();
    return env?.name ? this.templateData()[env.name] ?? null : null;
  });

  // Computed: merge schema with environment-specific defaults for display
  currentUserTemplateFields = computed<UserTemplateField[]>(() => {
    const schema = this.userTemplateSchema();
    const data = this.currentTemplateData();
    if (!data) return [];

    const defaultsMap = new Map(data.userTemplateDefaults.map(d => [d.key, d.value]));
    return schema.map(field => ({
      ...field,
      defaultValue: defaultsMap.get(field.key) ?? ''
    }));
  });

  // Computed: check if current environment has any identifier overrides
  hasIdentifierOverrides = computed(() =>
    (this.currentTemplateData()?.identifierOverrides?.length ?? 0) > 0
  );

  // Count of identifiers with system overrides
  systemOverrideCount = computed(() =>
    this.currentTemplateData()?.identifierOverrides.filter(o => o.systemOverrides.length > 0).length ?? 0
  );

  // Count of identifiers with user overrides
  userOverrideCount = computed(() =>
    this.currentTemplateData()?.identifierOverrides.filter(o => o.userOverrides.length > 0).length ?? 0
  );

  // ============================================================================
  // TEMPLATE TAB FILTERED COMPUTED PROPERTIES
  // ============================================================================

  // Filtered system template fields (for Template tab)
  filteredSystemTemplateFields = computed(() => {
    const data = this.currentTemplateData();
    if (!data) return [];

    const query = this.templateSearchQuery().toLowerCase();
    let fields = data.systemTemplate.fields;

    if (query) {
      fields = fields.filter(f => f.key.toLowerCase().includes(query));
    }

    if (this.showFieldsWithDefaults()) {
      fields = fields.filter(f => f.value?.trim());
    }

    return fields;
  });

  // Filtered user template fields (for Template tab)
  filteredUserTemplateFields = computed(() => {
    const fields = this.currentUserTemplateFields();
    const query = this.templateSearchQuery().toLowerCase();

    let filtered = fields;

    if (query) {
      filtered = filtered.filter(f => f.key.toLowerCase().includes(query));
    }

    if (this.showFieldsWithDefaults()) {
      filtered = filtered.filter(f => f.defaultValue?.trim());
    }

    return filtered;
  });

  // ============================================================================
  // MATRIX VIEW COMPUTED PROPERTIES
  // ============================================================================

  // Get all attribute keys for current template type
  matrixAttributeKeys = computed(() => {
    const data = this.currentTemplateData();
    if (!data) return [];

    const query = this.overridesSearchQuery().toLowerCase();
    const showOnlyOverrides = this.showOnlyOverrides();
    const templateType = this.overridesTemplateType();

    let keys: string[];
    if (templateType === 'system') {
      keys = data.systemTemplate.fields.map(f => f.key);
    } else {
      keys = this.userTemplateSchema().map(f => f.key);
    }

    // Filter by search query
    if (query) {
      keys = keys.filter(k => k.toLowerCase().includes(query));
    }

    // Filter to show only attributes that have at least one override
    if (showOnlyOverrides) {
      const overrides = data.identifierOverrides;
      keys = keys.filter(key => {
        return overrides.some(o => {
          if (templateType === 'system') {
            return o.systemOverrides.some(so => so.key === key);
          } else {
            return o.userOverrides.some(uo => uo.key === key);
          }
        });
      });
    }

    return keys;
  });

  // Get all identifiers for matrix rows
  matrixIdentifiers = computed(() => {
    const data = this.currentTemplateData();
    if (!data) return [];
    return data.identifierOverrides.map(o => o.identifier);
  });

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

      // Reset template override visibility when environment changes
      effect(() => {
        this.selectedEnvironment();
        this.showSystemOverrides.set(false);
        this.showUserOverrides.set(false);
      }, {allowSignalWrites: true});
    }

  // Get matrix cell value (override value or null if using default)
  getMatrixCellValue(identifier: string, attribute: string): string | null {
    const data = this.currentTemplateData();
    if (!data) return null;

    const override = data.identifierOverrides.find(o => o.identifier === identifier);
    if (!override) return null;

    const templateType = this.overridesTemplateType();
    if (templateType === 'system') {
      const field = override.systemOverrides.find(so => so.key === attribute);
      return field?.value ?? null;
    } else {
      const field = override.userOverrides.find(uo => uo.key === attribute);
      return field?.value ?? null;
    }
  }

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

  // Get default value for an attribute
  getDefaultValue(attribute: string): string {
    const data = this.currentTemplateData();
    if (!data) return '';

    const templateType = this.overridesTemplateType();
    if (templateType === 'system') {
      const field = data.systemTemplate.fields.find(f => f.key === attribute);
      return field?.value ?? '';
    } else {
      const defaultField = data.userTemplateDefaults.find(d => d.key === attribute);
      return defaultField?.value ?? '';
    }
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
      // Show the key - set flag before fetch to avoid race condition
      this.showReadKey.set(true);
      await this.fetchReadKey();
    }
  }

  async toggleWriteKeyVisibility(): Promise<void> {
    if (this.showWriteKey()) {
      // Hide the key - clear the state
      this.showWriteKey.set(false);
      this.writeKey.set(null);
    } else {
      // Show the key - set flag before fetch to avoid race condition
      this.showWriteKey.set(true);
      await this.fetchWriteKey();
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

  // ============================================================================
  // TEMPLATE METHODS
  // ============================================================================

  // Card collapse toggles
  toggleApiAttributesCard(): void {
    this.apiAttributesExpanded.update(v => !v);
  }

  toggleUsageOverviewCard(): void {
    this.usageOverviewExpanded.update(v => !v);
  }

  // Tab navigation
  setActiveTab(tab: 'overview' | 'configuration' | 'overrides'): void {
    this.activeTab.set(tab);
  }

  // Matrix cell editing
  startEditMatrixCell(identifier: string, attribute: string, currentValue: string | null): void {
    this.editingMatrixCell.set({identifier, attribute});
    this.editValue.set(currentValue ?? this.getDefaultValue(attribute));
  }

  saveMatrixCellEdit(): void {
    const editing = this.editingMatrixCell();
    if (!editing) return;

    const {identifier, attribute} = editing;
    const newValue = this.editValue().trim();
    const envName = this.selectedEnvironment()?.name;
    if (!envName) return;

    const templateType = this.overridesTemplateType();
    const defaultValue = this.getDefaultValue(attribute);

    this.templateData.update(data => {
      const envData = data[envName];
      if (!envData) return data;

      const updatedOverrides = envData.identifierOverrides.map(override => {
        if (override.identifier !== identifier) return override;

        if (templateType === 'system') {
          // Check if we're setting to default (remove override) or different (add/update override)
          if (newValue === defaultValue) {
            // Remove override
            return {
              ...override,
              systemOverrides: override.systemOverrides.filter(o => o.key !== attribute)
            };
          } else {
            // Add or update override
            const existing = override.systemOverrides.find(o => o.key === attribute);
            if (existing) {
              return {
                ...override,
                systemOverrides: override.systemOverrides.map(o =>
                  o.key === attribute ? {...o, value: newValue} : o
                )
              };
            } else {
              return {
                ...override,
                systemOverrides: [...override.systemOverrides, {key: attribute, value: newValue}]
              };
            }
          }
        } else {
          // User template
          if (newValue === defaultValue) {
            return {
              ...override,
              userOverrides: override.userOverrides.filter(o => o.key !== attribute)
            };
          } else {
            const existing = override.userOverrides.find(o => o.key === attribute);
            if (existing) {
              return {
                ...override,
                userOverrides: override.userOverrides.map(o =>
                  o.key === attribute ? {...o, value: newValue} : o
                )
              };
            } else {
              return {
                ...override,
                userOverrides: [...override.userOverrides, {key: attribute, value: newValue}]
              };
            }
          }
        }
      });

      return {
        ...data,
        [envName]: {...envData, identifierOverrides: updatedOverrides}
      };
    });

    this.editingMatrixCell.set(null);
    this.editValue.set('');
  }

  cancelMatrixCellEdit(): void {
    this.editingMatrixCell.set(null);
    this.editValue.set('');
  }

  // Template tab cell editing
  startEditTemplateCell(fieldKey: string, property: string, currentValue: string): void {
    this.editingTemplateCell.set({fieldKey, property});
    this.editValue.set(currentValue ?? '');
  }

  saveTemplateCellEdit(): void {
    const editing = this.editingTemplateCell();
    if (!editing) return;

    const {fieldKey, property} = editing;
    const newValue = this.editValue().trim();
    const envName = this.selectedEnvironment()?.name;
    if (!envName) return;

    const templateType = this.templateTemplateType();

    if (templateType === 'system') {
      // Update system template field
      this.templateData.update(data => {
        const envData = data[envName];
        if (!envData) return data;

        const updatedFields = envData.systemTemplate.fields.map(f => {
          if (f.key !== fieldKey) return f;
          if (property === 'key') return {...f, key: newValue};
          if (property === 'value') return {...f, value: newValue};
          if (property === 'description') return {...f, description: newValue};
          return f;
        });

        return {
          ...data,
          [envName]: {
            ...envData,
            systemTemplate: {fields: updatedFields}
          }
        };
      });
    } else {
      // Update user template field
      if (property === 'key') {
        // Update schema key across all environments
        this.userTemplateSchema.update(schema =>
          schema.map(f => f.key === fieldKey ? {...f, key: newValue} : f)
        );
        // Update key in all environment defaults
        this.templateData.update(data => {
          const updated = {...data};
          for (const env of Object.keys(updated)) {
            updated[env] = {
              ...updated[env],
              userTemplateDefaults: updated[env].userTemplateDefaults.map(d =>
                d.key === fieldKey ? {...d, key: newValue} : d
              )
            };
          }
          return updated;
        });
      } else if (property === 'type') {
        this.userTemplateSchema.update(schema =>
          schema.map(f => f.key === fieldKey
            ? {...f, type: newValue as FieldType, constraints: undefined}
            : f
          )
        );
      } else if (property === 'defaultValue') {
        this.templateData.update(data => {
          const envData = data[envName];
          if (!envData) return data;

          return {
            ...data,
            [envName]: {
              ...envData,
              userTemplateDefaults: envData.userTemplateDefaults.map(d =>
                d.key === fieldKey ? {...d, value: newValue} : d
              )
            }
          };
        });
      } else if (property === 'description') {
        this.userTemplateSchema.update(schema =>
          schema.map(f => f.key === fieldKey ? {...f, description: newValue} : f)
        );
      }
    }

    this.cancelTemplateCellEdit();
  }

  cancelTemplateCellEdit(): void {
    this.editingTemplateCell.set(null);
    this.editValue.set('');
  }

  // Override section toggles
  toggleSystemOverrides(): void {
    this.showSystemOverrides.update(show => !show);
  }

  toggleUserOverrides(): void {
    this.showUserOverrides.update(show => !show);
  }

  // Helper to format constraints for display
  formatConstraints(field: UserTemplateField): string {
    if (!field.constraints) return '';
    switch (field.type) {
      case 'number':
        const {min, max} = field.constraints;
        if (min !== undefined && max !== undefined) return `${min}–${max}`;
        if (min !== undefined) return `≥${min}`;
        if (max !== undefined) return `≤${max}`;
        return '';
      case 'string':
        return field.constraints.maxLength ? `max ${field.constraints.maxLength} chars` : '';
      case 'enum':
        return field.constraints.options?.join(', ') ?? '';
      default:
        return '';
    }
  }

  // Helper to get override value for a key
  getOverrideValue(overrides: { key: string; value: string }[], key: string): string | undefined {
    return overrides.find(o => o.key === key)?.value;
  }

  // ============================================================================
  // CRUD METHODS FOR TEMPLATES
  // ============================================================================

  // --- System Template Field CRUD ---

  startEditSystemField(key: string, field: 'key' | 'value', currentValue: string): void {
    this.editingSystemField.set({key, field});
    this.editValue.set(currentValue);
  }

  // Alias for template binding (key parameter not needed but keeps API consistent)
  saveSystemField(_key?: string): void {
    this.saveSystemFieldEdit();
  }

  saveSystemFieldEdit(): void {
    const editing = this.editingSystemField();
    const envName = this.selectedEnvironment()?.name;
    if (!editing || !envName) return;

    const newValue = this.editValue().trim();
    if (!newValue) {
      this.cancelEdit();
      return;
    }

    this.templateData.update(data => {
      const envData = data[envName];
      if (!envData) return data;

      const updatedFields = envData.systemTemplate.fields.map(f => {
        if (f.key === editing.key) {
          return editing.field === 'key'
            ? {...f, key: newValue}
            : {...f, value: newValue};
        }
        return f;
      });

      return {
        ...data,
        [envName]: {
          ...envData,
          systemTemplate: {fields: updatedFields}
        }
      };
    });

    this.cancelEdit();
  }

  addSystemField(): void {
    const envName = this.selectedEnvironment()?.name;
    const key = this.newSystemFieldKey().trim();
    const value = this.newSystemFieldValue().trim();

    if (!envName || !key) return;

    this.templateData.update(data => {
      const envData = data[envName];
      if (!envData) return data;

      return {
        ...data,
        [envName]: {
          ...envData,
          systemTemplate: {
            fields: [...envData.systemTemplate.fields, {key, value}]
          }
        }
      };
    });

    this.newSystemFieldKey.set('');
    this.newSystemFieldValue.set('');
    this.showAddSystemField.set(false);
  }

  deleteSystemField(key: string): void {
    const envName = this.selectedEnvironment()?.name;
    if (!envName) return;

    this.templateData.update(data => {
      const envData = data[envName];
      if (!envData) return data;

      return {
        ...data,
        [envName]: {
          ...envData,
          systemTemplate: {
            fields: envData.systemTemplate.fields.filter(f => f.key !== key)
          }
        }
      };
    });
  }

  // --- User Template Field CRUD ---

  startEditUserField(key: string, field: 'key' | 'type' | 'defaultValue', currentValue: string): void {
    this.editingUserField.set({key, field});
    this.editValue.set(currentValue);
  }

  // Alias for template binding (key parameter not needed but keeps API consistent)
  saveUserField(_key?: string): void {
    this.saveUserFieldEdit();
  }

  saveUserFieldEdit(): void {
    const editing = this.editingUserField();
    const envName = this.selectedEnvironment()?.name;
    if (!editing || !envName) return;

    const newValue = this.editValue().trim();
    if (!newValue && editing.field !== 'defaultValue') {
      this.cancelEdit();
      return;
    }

    if (editing.field === 'key') {
      // Update schema (app-level) - update key in all environments
      this.userTemplateSchema.update(schema =>
        schema.map(f => f.key === editing.key ? {...f, key: newValue} : f)
      );
      // Also update the key in all environment defaults
      this.templateData.update(data => {
        const updated = {...data};
        for (const env of Object.keys(updated)) {
          updated[env] = {
            ...updated[env],
            userTemplateDefaults: updated[env].userTemplateDefaults.map(d =>
              d.key === editing.key ? {...d, key: newValue} : d
            )
          };
        }
        return updated;
      });
    } else if (editing.field === 'type') {
      // Update schema type
      this.userTemplateSchema.update(schema =>
        schema.map(f => f.key === editing.key ? {...f, type: newValue as FieldType, constraints: undefined} : f)
      );
    } else if (editing.field === 'defaultValue') {
      // Update environment-specific default
      this.templateData.update(data => {
        const envData = data[envName];
        if (!envData) return data;

        return {
          ...data,
          [envName]: {
            ...envData,
            userTemplateDefaults: envData.userTemplateDefaults.map(d =>
              d.key === editing.key ? {...d, value: newValue} : d
            )
          }
        };
      });
    }

    this.cancelEdit();
  }

  addUserField(): void {
    const envName = this.selectedEnvironment()?.name;
    const key = this.newUserFieldKey().trim();
    const type = this.newUserFieldType();

    if (!envName || !key) return;

    // Add to schema (app-level)
    this.userTemplateSchema.update(schema => [
      ...schema,
      {key, type}
    ]);

    // Add default value to all environments
    this.templateData.update(data => {
      const updated = {...data};
      for (const env of Object.keys(updated)) {
        updated[env] = {
          ...updated[env],
          userTemplateDefaults: [
            ...updated[env].userTemplateDefaults,
            {key, value: ''}
          ]
        };
      }
      return updated;
    });

    this.newUserFieldKey.set('');
    this.newUserFieldType.set('string');
    this.showAddUserField.set(false);
  }

  deleteUserField(key: string): void {
    // Remove from schema
    this.userTemplateSchema.update(schema => schema.filter(f => f.key !== key));

    // Remove from all environments
    this.templateData.update(data => {
      const updated = {...data};
      for (const env of Object.keys(updated)) {
        updated[env] = {
          ...updated[env],
          userTemplateDefaults: updated[env].userTemplateDefaults.filter(d => d.key !== key)
        };
      }
      return updated;
    });
  }

  // --- Identifier Override CRUD ---

  addIdentifier(): void {
    const envName = this.selectedEnvironment()?.name;
    const identifier = this.newIdentifierName().trim();

    if (!envName || !identifier) return;

    this.templateData.update(data => {
      const envData = data[envName];
      if (!envData) return data;

      return {
        ...data,
        [envName]: {
          ...envData,
          identifierOverrides: [
            ...envData.identifierOverrides,
            {identifier, systemOverrides: [], userOverrides: []}
          ]
        }
      };
    });

    this.newIdentifierName.set('');
    this.showAddIdentifier.set(false);
  }

  deleteIdentifier(identifier: string): void {
    const envName = this.selectedEnvironment()?.name;
    if (!envName) return;

    this.templateData.update(data => {
      const envData = data[envName];
      if (!envData) return data;

      return {
        ...data,
        [envName]: {
          ...envData,
          identifierOverrides: envData.identifierOverrides.filter(o => o.identifier !== identifier)
        }
      };
    });
  }

  startEditIdentifierOverride(identifier: string, template: 'system' | 'user', fieldKey: string, currentValue: string): void {
    this.editingIdentifier.set({identifier, template, field: fieldKey});
    this.editValue.set(currentValue);
  }

  saveIdentifierOverrideEdit(): void {
    const editing = this.editingIdentifier();
    const envName = this.selectedEnvironment()?.name;
    if (!editing || !envName) return;

    const newValue = this.editValue().trim();

    this.templateData.update(data => {
      const envData = data[envName];
      if (!envData) return data;

      const updatedOverrides = envData.identifierOverrides.map(override => {
        if (override.identifier !== editing.identifier) return override;

        if (editing.template === 'system') {
          const existingOverride = override.systemOverrides.find(o => o.key === editing.field);
          if (newValue) {
            if (existingOverride) {
              return {
                ...override,
                systemOverrides: override.systemOverrides.map(o =>
                  o.key === editing.field ? {...o, value: newValue} : o
                )
              };
            } else {
              return {
                ...override,
                systemOverrides: [...override.systemOverrides, {key: editing.field, value: newValue}]
              };
            }
          } else {
            // Remove override if empty
            return {
              ...override,
              systemOverrides: override.systemOverrides.filter(o => o.key !== editing.field)
            };
          }
        } else {
          const existingOverride = override.userOverrides.find(o => o.key === editing.field);
          if (newValue) {
            if (existingOverride) {
              return {
                ...override,
                userOverrides: override.userOverrides.map(o =>
                  o.key === editing.field ? {...o, value: newValue} : o
                )
              };
            } else {
              return {
                ...override,
                userOverrides: [...override.userOverrides, {key: editing.field, value: newValue}]
              };
            }
          } else {
            return {
              ...override,
              userOverrides: override.userOverrides.filter(o => o.key !== editing.field)
            };
          }
        }
      });

      return {
        ...data,
        [envName]: {
          ...envData,
          identifierOverrides: updatedOverrides
        }
      };
    });

    this.cancelEdit();
  }

  // Alias for template binding
  startEditIdentifier(identifier: string, template: 'system' | 'user', fieldKey: string, currentValue: string): void {
    this.startEditIdentifierOverride(identifier, template, fieldKey, currentValue);
  }

  // Alias for template binding
  saveIdentifierOverride(_identifier?: string, _template?: 'system' | 'user', _fieldKey?: string): void {
    this.saveIdentifierOverrideEdit();
  }

  // Add an override value for an identifier
  addIdentifierOverride(identifier: string, template: 'system' | 'user', fieldKey: string): void {
    if (!fieldKey) return;

    const envName = this.selectedEnvironment()?.name;
    if (!envName) return;

    // Get default value for the field
    let defaultValue = '';
    if (template === 'system') {
      const field = this.currentTemplateData()?.systemTemplate.fields.find(f => f.key === fieldKey);
      defaultValue = field?.value ?? '';
    } else {
      const field = this.currentUserTemplateFields().find(f => f.key === fieldKey);
      defaultValue = field?.defaultValue ?? '';
    }

    this.templateData.update(data => {
      const envData = data[envName];
      if (!envData) return data;

      const updatedOverrides = envData.identifierOverrides.map(override => {
        if (override.identifier !== identifier) return override;

        if (template === 'system') {
          // Check if override already exists
          if (override.systemOverrides.some(o => o.key === fieldKey)) return override;
          return {
            ...override,
            systemOverrides: [...override.systemOverrides, {key: fieldKey, value: defaultValue}]
          };
        } else {
          if (override.userOverrides.some(o => o.key === fieldKey)) return override;
          return {
            ...override,
            userOverrides: [...override.userOverrides, {key: fieldKey, value: defaultValue}]
          };
        }
      });

      return {
        ...data,
        [envName]: {
          ...envData,
          identifierOverrides: updatedOverrides
        }
      };
    });
  }

  // Delete an override value for an identifier
  deleteIdentifierOverride(identifier: string, template: 'system' | 'user', fieldKey: string): void {
    const envName = this.selectedEnvironment()?.name;
    if (!envName) return;

    this.templateData.update(data => {
      const envData = data[envName];
      if (!envData) return data;

      const updatedOverrides = envData.identifierOverrides.map(override => {
        if (override.identifier !== identifier) return override;

        if (template === 'system') {
          return {
            ...override,
            systemOverrides: override.systemOverrides.filter(o => o.key !== fieldKey)
          };
        } else {
          return {
            ...override,
            userOverrides: override.userOverrides.filter(o => o.key !== fieldKey)
          };
        }
      });

      return {
        ...data,
        [envName]: {
          ...envData,
          identifierOverrides: updatedOverrides
        }
      };
    });
  }

  // --- Common Edit Helpers ---

  cancelEdit(): void {
    this.editingSystemField.set(null);
    this.editingUserField.set(null);
    this.editingIdentifier.set(null);
    this.editValue.set('');
  }

  onEditKeydown(event: KeyboardEvent): void {
    if (event.key === 'Enter') {
      event.preventDefault();
      if (this.editingSystemField()) {
        this.saveSystemFieldEdit();
      } else if (this.editingUserField()) {
        this.saveUserFieldEdit();
      } else if (this.editingIdentifier()) {
        this.saveIdentifierOverrideEdit();
      }
    } else if (event.key === 'Escape') {
      this.cancelEdit();
    }
  }

  // Helper to get override value for an identifier
  getIdentifierOverrideValue(identifier: string, template: 'system' | 'user', fieldKey: string): string {
    const data = this.currentTemplateData();
    if (!data) return '';

    const override = data.identifierOverrides.find(o => o.identifier === identifier);
    if (!override) return '';

    if (template === 'system') {
      return override.systemOverrides.find(o => o.key === fieldKey)?.value ?? '';
    } else {
      return override.userOverrides.find(o => o.key === fieldKey)?.value ?? '';
    }
  }
}
