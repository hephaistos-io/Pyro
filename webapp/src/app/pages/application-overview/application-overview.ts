import {Component, computed, effect, inject, OnInit, signal} from '@angular/core';
import {Router} from '@angular/router';
import {
  ApiKeyResponse,
  ApplicationResponse,
  BooleanTemplateField,
  EnumTemplateField,
  EnvironmentResponse,
  NumberTemplateField,
  StringTemplateField,
  TemplateField,
  TemplateResponse,
  TemplateType,
  TemplateValuesResponse
} from '../../api/generated/models';
import {Api} from '../../api/generated/api';
import {createEnvironment} from '../../api/generated/fn/environments/create-environment';
import {deleteEnvironment} from '../../api/generated/fn/environments/delete-environment';
import {getApiKeyByType} from '../../api/generated/fn/api-keys/get-api-key-by-type';
import {regenerateApiKey} from '../../api/generated/fn/api-keys/regenerate-api-key';
import {FormsModule} from '@angular/forms';
import {TemplateService} from '../../services/template.service';
import {OverlayService} from '../../services/overlay.service';
import {
  AddFieldOverlayComponent,
  AddFieldOverlayData
} from '../../components/add-field-overlay/add-field-overlay.component';
import {
  DeleteFieldOverlayComponent,
  DeleteFieldOverlayData
} from '../../components/delete-field-overlay/delete-field-overlay.component';
import {
  EditFieldOverlayComponent,
  EditFieldOverlayData
} from '../../components/edit-field-overlay/edit-field-overlay.component';
import {
  CopyOverridesOverlayComponent,
  CopyOverridesOverlayData
} from '../../components/copy-overrides-overlay/copy-overrides-overlay.component';

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

// Helper type for displaying field values with their defaults
interface FieldDisplayValue {
  key: string;
  value: unknown;
  hasOverride: boolean;
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
  // TEMPLATE STATE - API-backed
  // ============================================================================

  templateService = inject(TemplateService); // Public so template can access type checking methods
  // Copy overrides state
  showCopyOverridesDialog = signal(false);

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
  copySourceEnvironmentId = signal<string | null>(null);
  copyOverwriteExisting = signal(false);
  isCopyingOverrides = signal(false);
  editError = signal<string | null>(null);

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
  // New identifier form
  showAddIdentifier = signal(false);
  // Loading states
  isLoadingTemplates = signal(false);
  newIdentifierName = signal('');
  isLoadingOverrides = signal(false);
  isSavingTemplate = signal(false);
  // API-loaded templates (SYSTEM and USER)
  systemTemplate = signal<TemplateResponse | null>(null);
  userTemplate = signal<TemplateResponse | null>(null);
  // API-loaded overrides per environment (keyed by environmentId)
  systemOverrides = signal<TemplateValuesResponse[]>([]);
  userOverrides = signal<TemplateValuesResponse[]>([]);
  // Computed: System template fields
  systemTemplateFields = computed<TemplateField[]>(() => {
    return this.systemTemplate()?.schema?.fields ?? [];
  });
  // Computed: User template fields
  userTemplateFields = computed<TemplateField[]>(() => {
    return this.userTemplate()?.schema?.fields ?? [];
  });
  // Computed: check if current environment has any identifier overrides
  hasIdentifierOverrides = computed(() => {
    const systemOverrides = this.systemOverrides();
    const userOverrides = this.userOverrides();
    return systemOverrides.length > 0 || userOverrides.length > 0;
  });
  // Count of identifiers with system overrides
  systemOverrideCount = computed(() => this.systemOverrides().length);
  // Count of identifiers with user overrides
  userOverrideCount = computed(() => this.userOverrides().length);
  // Get all unique identifiers from overrides
  allIdentifiers = computed<string[]>(() => {
    const systemIds = this.systemOverrides().map(o => o.identifier).filter((id): id is string => !!id);
    const userIds = this.userOverrides().map(o => o.identifier).filter((id): id is string => !!id);
    return [...new Set([...systemIds, ...userIds])];
  });
  // Filtered system template fields (for Template tab)
  filteredSystemTemplateFields = computed<TemplateField[]>(() => {
    const fields = this.systemTemplateFields();
    const query = this.templateSearchQuery().toLowerCase();

    let filtered = fields;

    if (query) {
      filtered = filtered.filter(f => f.key?.toLowerCase().includes(query));
    }

    if (this.showFieldsWithDefaults()) {
      filtered = filtered.filter(f => {
        const defaultValue = this.templateService.getDefaultValue(f);
        return defaultValue !== undefined && defaultValue !== null && defaultValue !== '';
      });
    }

    return filtered;
  });

  // ============================================================================
  // TEMPLATE TAB FILTERED COMPUTED PROPERTIES
  // ============================================================================
  // Filtered user template fields (for Template tab)
  filteredUserTemplateFields = computed<TemplateField[]>(() => {
    const fields = this.userTemplateFields();
    const query = this.templateSearchQuery().toLowerCase();

    let filtered = fields;

    if (query) {
      filtered = filtered.filter(f => f.key?.toLowerCase().includes(query));
    }

    if (this.showFieldsWithDefaults()) {
      filtered = filtered.filter(f => {
        const defaultValue = this.templateService.getDefaultValue(f);
        return defaultValue !== undefined && defaultValue !== null && defaultValue !== '';
      });
    }

    return filtered;
  });
  // Get all attribute keys for current template type
  matrixAttributeKeys = computed<string[]>(() => {
    const query = this.overridesSearchQuery().toLowerCase();
    const showOnlyOverrides = this.showOnlyOverrides();
    const templateType = this.overridesTemplateType();

    let keys: string[];
    if (templateType === 'system') {
      keys = this.systemTemplateFields().map(f => f.key).filter((k): k is string => !!k);
    } else {
      keys = this.userTemplateFields().map(f => f.key).filter((k): k is string => !!k);
    }

    // Filter by search query
    if (query) {
      keys = keys.filter(k => k.toLowerCase().includes(query));
    }

    // Filter to show only attributes that have at least one override
    if (showOnlyOverrides) {
      const overrides = templateType === 'system' ? this.systemOverrides() : this.userOverrides();
      keys = keys.filter(key => {
        return overrides.some(o => {
          const values = o.values as Record<string, unknown> | undefined;
          return values && key in values;
        });
      });
    }

    return keys;
  });

  // ============================================================================
  // MATRIX VIEW COMPUTED PROPERTIES
  // ============================================================================
  // Get all identifiers for matrix rows
  matrixIdentifiers = computed<string[]>(() => {
    return this.allIdentifiers();
  });
  private overlayService = inject(OverlayService);

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

      // Load templates when application changes
      effect(() => {
        const app = this.application();
        if (app?.id) {
          this.loadTemplates(app.id);
        }
      }, {allowSignalWrites: true});

      // Load overrides when environment changes
      effect(() => {
        const app = this.application();
        const env = this.selectedEnvironment();
        if (app?.id && env?.id) {
          this.loadOverrides(app.id, env.id);
        }
      }, {allowSignalWrites: true});
    }

  // Get matrix cell value (override value or null if using default)
  getMatrixCellValue(identifier: string, attribute: string): string | null {
    const templateType = this.overridesTemplateType();
    const overrides = templateType === 'system' ? this.systemOverrides() : this.userOverrides();

    const override = overrides.find(o => o.identifier === identifier);
    if (!override?.values) return null;

    const values = override.values as Record<string, unknown>;
    const value = values[attribute];
    return value !== undefined ? String(value) : null;
  }

  // Get default value for an attribute (for overrides matrix view)
  getDefaultValue(attribute: string): string {
    const templateType = this.overridesTemplateType();
    const fields = templateType === 'system' ? this.systemTemplateFields() : this.userTemplateFields();

    const field = fields.find(f => f.key === attribute);
    if (!field) return '';

    const defaultValue = this.templateService.getDefaultValue(field);
    return defaultValue !== undefined && defaultValue !== null ? String(defaultValue) : '';
  }

  // Matrix cell editing
  startEditMatrixCell(identifier: string, attribute: string, currentValue: string | null): void {
    this.editingMatrixCell.set({identifier, attribute});
    this.editValue.set(currentValue ?? this.getDefaultValue(attribute));
    this.editError.set(null); // Clear any previous errors
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

  // Get the field definition for a matrix attribute
  getMatrixField(attribute: string): TemplateField | undefined {
    const templateType = this.overridesTemplateType();
    const fields = templateType === 'system' ? this.systemTemplateFields() : this.userTemplateFields();
    return fields.find(f => f.key === attribute);
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

  // Validate matrix cell value on input change
  onMatrixInputChange(newValue: string): void {
    this.editValue.set(newValue);

    const editing = this.editingMatrixCell();
    if (!editing) return;

    const field = this.getMatrixField(editing.attribute);
    if (!field) return;

    // Validate the new value
    const validationError = this.validateDefaultValue(field, newValue.trim());
    this.editError.set(validationError);
  }

  // Try to save matrix cell edit (used on blur)
  tryToSaveMatrixCellEdit(): void {
    // Don't auto-save if there's already a validation error
    if (this.editError()) {
      return;
    }
    this.saveMatrixCellEdit();
  }

  async saveMatrixCellEdit(): Promise<void> {
    const editing = this.editingMatrixCell();
    const app = this.application();
    const env = this.selectedEnvironment();
    if (!editing || !app?.id || !env?.id) return;

    const {identifier, attribute} = editing;
    const newValue = this.editValue().trim();
    const templateType = this.overridesTemplateType();

    // Validate against field constraints
    const field = this.getMatrixField(attribute);
    if (field) {
      const validationError = this.validateDefaultValue(field, newValue);
      if (validationError) {
        this.editError.set(validationError);
        return;
      }
    }

    // Clear error
    this.editError.set(null);

    const overrides = templateType === 'system' ? this.systemOverrides() : this.userOverrides();
    const existingOverride = overrides.find(o => o.identifier === identifier);
    const currentValues = (existingOverride?.values as Record<string, unknown>) ?? {};

    // Build updated values
    const updatedValues: Record<string, unknown> = {...currentValues};
    const defaultValue = this.getDefaultValue(attribute);

    // Convert value based on field type
    let finalValue: unknown = newValue;
    if (field && newValue !== defaultValue && newValue !== '') {
      if (this.templateService.isNumberField(field)) {
        finalValue = parseFloat(newValue);
      } else if (this.templateService.isBooleanField(field)) {
        finalValue = newValue.toLowerCase() === 'true';
      }
    }

    if (newValue === defaultValue || newValue === '') {
      // Remove override if setting back to default or empty
      delete updatedValues[attribute];
    } else {
      updatedValues[attribute] = finalValue;
    }

    try {
      if (Object.keys(updatedValues).length === 0) {
        // No overrides left, delete the entire override
        await this.templateService.deleteOverride(
          app.id,
          templateType === 'system' ? TemplateType.System : TemplateType.User,
          env.id,
          identifier
        );
      } else {
        // Update the override
        await this.templateService.setOverride(
          app.id,
          templateType === 'system' ? TemplateType.System : TemplateType.User,
          env.id,
          identifier,
          updatedValues
        );
      }
      // Reload overrides
      await this.loadOverrides(app.id, env.id);
    } catch (error) {
      console.error('Failed to save override:', error);
    }

    this.editingMatrixCell.set(null);
    this.editValue.set('');
    this.editError.set(null);
  }

  cancelMatrixCellEdit(): void {
    this.editingMatrixCell.set(null);
    this.editValue.set('');
    this.editError.set(null);
  }

  async saveTemplateCellEdit(): Promise<void> {
    const editing = this.editingTemplateCell();
    const app = this.application();
    if (!editing || !app?.id) return;

    const {fieldKey, property} = editing;
    const newValue = this.editValue().trim();
    const templateType = this.templateTemplateType();
    const template = templateType === 'system' ? this.systemTemplate() : this.userTemplate();

    if (!template?.schema?.fields) {
      this.cancelTemplateCellEdit();
      return;
    }

    // Find the current field to validate against
    const currentField = template.schema.fields.find(f => f.key === fieldKey);
    if (!currentField) {
      this.cancelTemplateCellEdit();
      return;
    }

    // Validate default value if editing defaultValue property
    if (property === 'value' || property === 'defaultValue') {
      const validationError = this.validateDefaultValue(currentField, newValue);
      if (validationError) {
        this.editError.set(validationError);
        return;
      }
    }

    // Clear any previous error
    this.editError.set(null);

    // Update the field in the schema
    const updatedFields = template.schema.fields.map(field => {
      if (field.key !== fieldKey) return field;

      // Clone the field and update the property
      const updatedField = {...field};
      if (property === 'description') {
        updatedField.description = newValue;
      } else if (property === 'value' || property === 'defaultValue') {
        // 'value' is used for system template display, but it maps to defaultValue
        // Type-aware default value update
        if (this.templateService.isStringField(field)) {
          (updatedField as StringTemplateField).defaultValue = newValue;
        } else if (this.templateService.isNumberField(field)) {
          (updatedField as NumberTemplateField).defaultValue = parseFloat(newValue) || 0;
        } else if (this.templateService.isBooleanField(field)) {
          (updatedField as BooleanTemplateField).defaultValue = newValue.toLowerCase() === 'true';
        } else if (this.templateService.isEnumField(field)) {
          (updatedField as EnumTemplateField).defaultValue = newValue;
        }
      }
      return updatedField;
    });

    this.isSavingTemplate.set(true);
    try {
      await this.templateService.updateTemplate(
        app.id,
        templateType === 'system' ? TemplateType.System : TemplateType.User,
        {
          fields: updatedFields,
          defaultValues: template.schema.defaultValues
        }
      );
      // Reload templates
      await this.loadTemplates(app.id);
    } catch (error) {
      console.error('Failed to save template:', error);
    } finally {
      this.isSavingTemplate.set(false);
    }

    this.cancelTemplateCellEdit();
  }

  cancelTemplateCellEdit(): void {
    this.editingTemplateCell.set(null);
    this.editValue.set('');
    this.editError.set(null);
  }

  // Template tab cell editing
  startEditTemplateCell(fieldKey: string, property: string, currentValue: string): void {
    this.editingTemplateCell.set({fieldKey, property});
    this.editValue.set(currentValue ?? '');
  }

  // Helper to format constraints for display
  formatConstraints(field: TemplateField): string {
    return this.templateService.formatConstraints(field);
  }

  // Helper to get default value as string (for templates)
  getFieldDefaultValue(field: TemplateField): string {
    const value = this.templateService.getDefaultValue(field);
    return value !== undefined && value !== null ? String(value) : '';
  }

  async saveSystemFieldEdit(): Promise<void> {
    // System field editing is handled via saveTemplateCellEdit now
    // This method is kept for backward compatibility
    this.cancelEdit();
  }

  // Override section toggles
  toggleSystemOverrides(): void {
    this.showSystemOverrides.update(show => !show);
  }

  toggleUserOverrides(): void {
    this.showUserOverrides.update(show => !show);
  }

  openAddFieldOverlay(templateType: 'system' | 'user'): void {
    const fields = templateType === 'system'
      ? this.systemTemplateFields()
      : this.userTemplateFields();

    const existingKeys = fields
      .map(f => f.key)
      .filter((k): k is string => !!k);

    this.overlayService.open<AddFieldOverlayData>({
      component: AddFieldOverlayComponent,
      data: {
        templateType,
        existingFieldKeys: existingKeys,
        onSubmit: (field: TemplateField) => this.handleAddField(field, templateType)
      },
      maxWidth: '500px'
    });
  }

  openEditFieldOverlay(field: TemplateField, templateType: 'system' | 'user'): void {
    const fields = templateType === 'system'
      ? this.systemTemplateFields()
      : this.userTemplateFields();

    // Exclude current field key from existing keys to allow keeping the same key
    const existingKeys = fields
      .map(f => f.key)
      .filter((k): k is string => !!k && k !== field.key);

    this.overlayService.open<EditFieldOverlayData>({
      component: EditFieldOverlayComponent,
      data: {
        templateType,
        field,
        existingFieldKeys: existingKeys,
        onSubmit: (updatedField: TemplateField) => this.handleEditField(field.key!, updatedField, templateType)
      },
      maxWidth: '500px'
    });
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

  openDeleteFieldOverlay(key: string, templateType: 'system' | 'user'): void {
    const appName = this.applicationName();

    this.overlayService.open<DeleteFieldOverlayData>({
      component: DeleteFieldOverlayComponent,
      data: {
        applicationName: appName,
        fieldKey: key,
        templateType,
        onConfirm: () => this.handleDeleteField(key, templateType)
      },
      maxWidth: '450px'
    });
  }

  // --- Add Field Overlay ---

  async deleteSystemField(key: string): Promise<void> {
    const app = this.application();
    if (!app?.id) return;

    const template = this.systemTemplate();
    if (!template?.schema?.fields) return;

    const updatedFields = template.schema.fields.filter(f => f.key !== key);

    this.isSavingTemplate.set(true);
    try {
      await this.templateService.updateTemplate(app.id, TemplateType.System, {
        fields: updatedFields,
        defaultValues: template.schema.defaultValues
      });
      await this.loadTemplates(app.id);
    } catch (error) {
      console.error('Failed to delete system field:', error);
    } finally {
      this.isSavingTemplate.set(false);
    }
  }

  async saveUserFieldEdit(): Promise<void> {
    // User field editing is handled via saveTemplateCellEdit now
    // This method is kept for backward compatibility
    this.cancelEdit();
  }

  // --- Edit Field Overlay ---

  async deleteUserField(key: string): Promise<void> {
    const app = this.application();
    if (!app?.id) return;

    const template = this.userTemplate();
    if (!template?.schema?.fields) return;

    const updatedFields = template.schema.fields.filter(f => f.key !== key);

    this.isSavingTemplate.set(true);
    try {
      await this.templateService.updateTemplate(app.id, TemplateType.User, {
        fields: updatedFields,
        defaultValues: template.schema.defaultValues
      });
      await this.loadTemplates(app.id);
    } catch (error) {
      console.error('Failed to delete user field:', error);
    } finally {
      this.isSavingTemplate.set(false);
    }
  }

  async addIdentifier(): Promise<void> {
    const app = this.application();
    const env = this.selectedEnvironment();
    const identifier = this.newIdentifierName().trim();

    if (!app?.id || !env?.id || !identifier) return;

    // Create an empty override for both system and user templates
    try {
      // Create override with empty values (just to establish the identifier)
      await this.templateService.setOverride(app.id, TemplateType.System, env.id, identifier, {});
      await this.loadOverrides(app.id, env.id);
    } catch (error) {
      console.error('Failed to add identifier:', error);
    }

    this.newIdentifierName.set('');
    this.showAddIdentifier.set(false);
  }

  // --- Delete Field Overlay ---

  async deleteIdentifier(identifier: string): Promise<void> {
    const app = this.application();
    const env = this.selectedEnvironment();
    if (!app?.id || !env?.id) return;

    try {
      // Delete overrides for both system and user templates
      await Promise.all([
        this.templateService.deleteOverride(app.id, TemplateType.System, env.id, identifier).catch(() => {
        }),
        this.templateService.deleteOverride(app.id, TemplateType.User, env.id, identifier).catch(() => {
        })
      ]);
      await this.loadOverrides(app.id, env.id);
    } catch (error) {
      console.error('Failed to delete identifier:', error);
    }
  }

  // Copy overrides overlay methods
  openCopyOverridesOverlay(): void {
    const app = this.application();
    const env = this.selectedEnvironment();

    if (!app?.id || !env?.id) return;

    this.overlayService.open<CopyOverridesOverlayData>({
      component: CopyOverridesOverlayComponent,
      data: {
        applicationId: app.id,
        applicationName: app.name ?? 'Application',
        environments: this.environments(),
        currentEnvironmentId: env.id,
        allIdentifiers: this.allIdentifiers(),
        onSuccess: () => this.handleCopyOverridesSuccess()
      },
      maxWidth: '500px'
    });
  }

  async saveIdentifierOverrideEdit(): Promise<void> {
    const editing = this.editingIdentifier();
    const app = this.application();
    const env = this.selectedEnvironment();
    if (!editing || !app?.id || !env?.id) return;

    const newValue = this.editValue().trim();
    const overrides = editing.template === 'system' ? this.systemOverrides() : this.userOverrides();
    const existingOverride = overrides.find(o => o.identifier === editing.identifier);
    const currentValues = (existingOverride?.values as Record<string, unknown>) ?? {};

    // Build updated values
    const updatedValues: Record<string, unknown> = {...currentValues};
    if (newValue) {
      updatedValues[editing.field] = newValue;
    } else {
      delete updatedValues[editing.field];
    }

    try {
      if (Object.keys(updatedValues).length === 0) {
        await this.templateService.deleteOverride(
          app.id,
          editing.template === 'system' ? TemplateType.System : TemplateType.User,
          env.id,
          editing.identifier
        );
      } else {
        await this.templateService.setOverride(
          app.id,
          editing.template === 'system' ? TemplateType.System : TemplateType.User,
          env.id,
          editing.identifier,
          updatedValues
        );
      }
      await this.loadOverrides(app.id, env.id);
    } catch (error) {
      console.error('Failed to save identifier override:', error);
    }

    this.cancelEdit();
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

  // Add an override value for an identifier
  async addIdentifierOverride(identifier: string, template: 'system' | 'user', fieldKey: string): Promise<void> {
    if (!fieldKey) return;

    const app = this.application();
    const env = this.selectedEnvironment();
    if (!app?.id || !env?.id) return;

    // Get default value for the field
    const fields = template === 'system' ? this.systemTemplateFields() : this.userTemplateFields();
    const field = fields.find(f => f.key === fieldKey);
    const defaultValue = field ? this.templateService.getDefaultValue(field) : '';

    // Get existing override values
    const overrides = template === 'system' ? this.systemOverrides() : this.userOverrides();
    const existingOverride = overrides.find(o => o.identifier === identifier);
    const currentValues = (existingOverride?.values as Record<string, unknown>) ?? {};

    // Add the new override value
    const updatedValues: Record<string, unknown> = {
      ...currentValues,
      [fieldKey]: defaultValue
    };

    try {
      await this.templateService.setOverride(
        app.id,
        template === 'system' ? TemplateType.System : TemplateType.User,
        env.id,
        identifier,
        updatedValues
      );
      await this.loadOverrides(app.id, env.id);
    } catch (error) {
      console.error('Failed to add identifier override:', error);
    }
  }

  // Delete an override value for an identifier
  async deleteIdentifierOverride(identifier: string, template: 'system' | 'user', fieldKey: string): Promise<void> {
    const app = this.application();
    const env = this.selectedEnvironment();
    if (!app?.id || !env?.id) return;

    // Get existing override values
    const overrides = template === 'system' ? this.systemOverrides() : this.userOverrides();
    const existingOverride = overrides.find(o => o.identifier === identifier);
    const currentValues = (existingOverride?.values as Record<string, unknown>) ?? {};

    // Remove the field from values
    const updatedValues: Record<string, unknown> = {...currentValues};
    delete updatedValues[fieldKey];

    try {
      if (Object.keys(updatedValues).length === 0) {
        await this.templateService.deleteOverride(
          app.id,
          template === 'system' ? TemplateType.System : TemplateType.User,
          env.id,
          identifier
        );
      } else {
        await this.templateService.setOverride(
          app.id,
          template === 'system' ? TemplateType.System : TemplateType.User,
          env.id,
          identifier,
          updatedValues
        );
      }
      await this.loadOverrides(app.id, env.id);
    } catch (error) {
      console.error('Failed to delete identifier override:', error);
    }
  }

  // --- Identifier Override CRUD ---

  // Helper to get override value for an identifier
  getIdentifierOverrideValue(identifier: string, template: 'system' | 'user', fieldKey: string): string {
    const overrides = template === 'system' ? this.systemOverrides() : this.userOverrides();
    const override = overrides.find(o => o.identifier === identifier);
    if (!override?.values) return '';

    const values = override.values as Record<string, unknown>;
    const value = values[fieldKey];
    return value !== undefined ? String(value) : '';
  }

  // Toggle editable property for a user field
  async toggleUserFieldEditable(key: string): Promise<void> {
    const app = this.application();
    if (!app?.id) return;

    const template = this.userTemplate();
    if (!template?.schema?.fields) return;

    const updatedFields = template.schema.fields.map(field => {
      if (field.key !== key) return field;
      return {...field, editable: !field.editable};
    });

    this.isSavingTemplate.set(true);
    try {
      await this.templateService.updateTemplate(app.id, TemplateType.User, {
        fields: updatedFields,
        defaultValues: template.schema.defaultValues
      });
      await this.loadTemplates(app.id);
    } catch (error) {
      console.error('Failed to toggle field editable:', error);
    } finally {
      this.isSavingTemplate.set(false);
    }
  }

  // Load templates from API
  private async loadTemplates(applicationId: string): Promise<void> {
    this.isLoadingTemplates.set(true);
    try {
      const templates = await this.templateService.getTemplates(applicationId);
      const systemTemplate = templates.find(t => t.type === TemplateType.System);
      const userTemplate = templates.find(t => t.type === TemplateType.User);
      this.systemTemplate.set(systemTemplate ?? null);
      this.userTemplate.set(userTemplate ?? null);
    } catch (error) {
      console.error('Failed to load templates:', error);
    } finally {
      this.isLoadingTemplates.set(false);
    }
  }

  // Load overrides from API
  private async loadOverrides(applicationId: string, environmentId: string): Promise<void> {
    this.isLoadingOverrides.set(true);
    try {
      const [systemOverrides, userOverrides] = await Promise.all([
        this.templateService.getOverrides(applicationId, TemplateType.System, environmentId),
        this.templateService.getOverrides(applicationId, TemplateType.User, environmentId)
      ]);
      this.systemOverrides.set(systemOverrides);
      this.userOverrides.set(userOverrides);
    } catch (error) {
      console.error('Failed to load overrides:', error);
    } finally {
      this.isLoadingOverrides.set(false);
    }
  }

  startEditIdentifierOverride(identifier: string, template: 'system' | 'user', fieldKey: string, currentValue: string): void {
    this.editingIdentifier.set({identifier, template, field: fieldKey});
    this.editValue.set(currentValue);
  }

  /**
   * Validates a default value against the field's constraints.
   * Returns an error message if invalid, or null if valid.
   */
  private validateDefaultValue(field: TemplateField, newValue: string): string | null {
    if (this.templateService.isStringField(field)) {
      const stringField = field as StringTemplateField;
      // Empty string is allowed (means no default)
      if (!newValue) return null;

      const minLength = stringField.minLength ?? 0;
      const maxLength = stringField.maxLength ?? Infinity;

      if (newValue.length < minLength) {
        return `Value must be at least ${minLength} characters`;
      }
      if (newValue.length > maxLength) {
        return `Value must be at most ${maxLength} characters`;
      }
    } else if (this.templateService.isNumberField(field)) {
      const numberField = field as NumberTemplateField;

      // Empty string is allowed (means no default)
      if (!newValue) return null;

      const numValue = parseFloat(newValue);
      if (isNaN(numValue)) {
        return 'Value must be a valid number';
      }

      const minValue = numberField.minValue ?? -Infinity;
      const maxValue = numberField.maxValue ?? Infinity;
      const incrementAmount = numberField.incrementAmount ?? 1;

      if (numValue < minValue) {
        return `Value must be at least ${minValue}`;
      }
      if (numValue > maxValue) {
        return `Value must be at most ${maxValue}`;
      }

      // Check increment alignment
      if (incrementAmount > 0 && numberField.minValue !== undefined) {
        const diff = numValue - minValue;
        const remainder = Math.abs(diff % incrementAmount);
        const tolerance = incrementAmount * 1e-9;
        if (remainder > tolerance && remainder < incrementAmount - tolerance) {
          return `Value must align with increment of ${incrementAmount} (starting from ${minValue})`;
        }
      }
    } else if (this.templateService.isEnumField(field)) {
      const enumField = field as EnumTemplateField;
      // Empty string is allowed (means no default)
      if (!newValue) return null;

      if (enumField.options && !enumField.options.includes(newValue)) {
        return `Value must be one of: ${enumField.options.join(', ')}`;
      }
    }

    return null;
  }

  // Alias for template binding
  startEditIdentifier(identifier: string, template: 'system' | 'user', fieldKey: string, currentValue: string): void {
    this.startEditIdentifierOverride(identifier, template, fieldKey, currentValue);
  }

  // Alias for template binding
  saveIdentifierOverride(_identifier?: string, _template?: 'system' | 'user', _fieldKey?: string): void {
    this.saveIdentifierOverrideEdit();
  }

  private async handleAddField(field: TemplateField, templateType: 'system' | 'user'): Promise<void> {
    const app = this.application();
    if (!app?.id) {
      return;
    }

    const template = templateType === 'system' ? this.systemTemplate() : this.userTemplate();

    // Allow adding fields even if template has no schema yet - we'll create a new schema
    const existingFields = template?.schema?.fields ?? [];
    const updatedFields = [...existingFields, field];

    this.isSavingTemplate.set(true);
    try {
      await this.templateService.updateTemplate(
        app.id,
        templateType === 'system' ? TemplateType.System : TemplateType.User,
        {
          fields: updatedFields,
          defaultValues: template?.schema?.defaultValues ?? {}
        }
      );
      await this.loadTemplates(app.id);
    } catch (error) {
      console.error('Failed to add field:', error);
    } finally {
      this.isSavingTemplate.set(false);
    }
  }

  private async handleEditField(originalKey: string, updatedField: TemplateField, templateType: 'system' | 'user'): Promise<void> {
    const app = this.application();
    if (!app?.id) {
      return;
    }

    const template = templateType === 'system' ? this.systemTemplate() : this.userTemplate();
    if (!template?.schema?.fields) {
      return;
    }

    // Replace the field with the updated one
    const updatedFields = template.schema.fields.map(f => {
      if (f.key === originalKey) {
        return updatedField;
      }
      return f;
    });

    this.isSavingTemplate.set(true);
    try {
      await this.templateService.updateTemplate(
        app.id,
        templateType === 'system' ? TemplateType.System : TemplateType.User,
        {
          fields: updatedFields,
          defaultValues: template.schema.defaultValues ?? {}
        }
      );
      await this.loadTemplates(app.id);
    } catch (error) {
      console.error('Failed to edit field:', error);
    } finally {
      this.isSavingTemplate.set(false);
    }
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

  private async handleDeleteField(key: string, templateType: 'system' | 'user'): Promise<void> {
    const app = this.application();
    if (!app?.id) return;

    const template = templateType === 'system' ? this.systemTemplate() : this.userTemplate();
    if (!template?.schema?.fields) return;

    const updatedFields = template.schema.fields.filter(f => f.key !== key);

    this.isSavingTemplate.set(true);
    try {
      await this.templateService.updateTemplate(
        app.id,
        templateType === 'system' ? TemplateType.System : TemplateType.User,
        {
          fields: updatedFields,
          defaultValues: template.schema.defaultValues
        }
      );
      await this.loadTemplates(app.id);
    } catch (error) {
      console.error('Failed to delete field:', error);
    } finally {
      this.isSavingTemplate.set(false);
    }
  }

  private async handleCopyOverridesSuccess(): Promise<void> {
    const app = this.application();
    const env = this.selectedEnvironment();
    if (!app?.id || !env?.id) return;

    await this.loadOverrides(app.id, env.id);
  }
}
