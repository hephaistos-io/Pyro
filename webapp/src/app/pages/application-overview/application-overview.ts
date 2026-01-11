import {Component, computed, effect, inject, OnInit, signal} from '@angular/core';
import {Router} from '@angular/router';
import {
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
import {getApplicationStatistics} from '../../api/generated/fn/application/get-application-statistics';
import {CurrencyPipe} from '@angular/common';
import {FormsModule} from '@angular/forms';
import {TemplateService} from '../../services/template.service';
import {RoleService} from '../../services/role.service';
import {PricingStateService, PricingTier, TIER_PRICES} from '../../services/pricing-state.service';
import {ApiKeysCardComponent} from '../../components/api-keys-card/api-keys-card.component';
import {EnvironmentManagerComponent} from '../../components/environment-manager/environment-manager.component';
import {UsageStatsCardComponent} from '../../components/usage-stats-card/usage-stats-card.component';
import {TemplateConfigComponent} from '../../components/template-config/template-config.component';
import {OverrideManagerComponent} from '../../components/override-manager/override-manager.component';

interface RateLimitTier {
    id: string;
    name: string;
  price: string;
  requestsPerMonth: string;
  rateLimit: number;
  description: string;
}

@Component({
    selector: 'app-application-overview',
  standalone: true,
  imports: [FormsModule, CurrencyPipe, ApiKeysCardComponent, EnvironmentManagerComponent, UsageStatsCardComponent, TemplateConfigComponent, OverrideManagerComponent],
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
    // Mock total users - Replace with actual stats from backend
    totalUsers = signal(0);
    // Mock total hits this month - Replace with actual stats from backend
    totalHitsThisMonth = signal(0);

  // Rate limit tiers (aligned with PricingTier enum)
  rateLimitTiers = signal<RateLimitTier[]>([
    {id: 'FREE', name: 'Free', price: '$0', requestsPerMonth: '500k/month', rateLimit: 5, description: 'Dev/Test'},
    {id: 'BASIC', name: 'Basic', price: '$10', requestsPerMonth: '2M/month', rateLimit: 20, description: 'Small apps'},
    {
      id: 'STANDARD',
      name: 'Standard',
      price: '$25',
      requestsPerMonth: '10M/month',
      rateLimit: 100,
      description: 'Growing startups'
    },
    {id: 'PRO', name: 'Pro', price: '$50', requestsPerMonth: '25M/month', rateLimit: 500, description: 'Scale-ups'},
    {
      id: 'BUSINESS',
      name: 'Business',
      price: '$100',
      requestsPerMonth: '100M/month',
      rateLimit: 2000,
      description: 'Enterprise'
    },
  ]);
  selectedRateLimitTierIndex = signal(0); // Default to Free
  originalTierIndex = signal(0); // The environment's saved tier index
  currentRateLimitTier = computed(() => this.rateLimitTiers()[this.selectedRateLimitTierIndex()]);

  // Tier change detection
  hasTierChanged = computed(() => this.selectedRateLimitTierIndex() !== this.originalTierIndex());
  priceDifference = computed(() => {
    const originalTier = this.rateLimitTiers()[this.originalTierIndex()];
    const selectedTier = this.rateLimitTiers()[this.selectedRateLimitTierIndex()];
    const originalPrice = TIER_PRICES[originalTier.id as PricingTier] ?? 0;
    const selectedPrice = TIER_PRICES[selectedTier.id as PricingTier] ?? 0;
    return selectedPrice - originalPrice;
  });

  private pricingState = inject(PricingStateService);
  roleService = inject(RoleService);
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
  private api = inject(Api);

  // ============================================================================
  // TEMPLATE STATE - API-backed
  // ============================================================================

  templateService = inject(TemplateService); // Public so template can access type checking methods

  // Card collapse state (expanded by default)
  apiAttributesExpanded = signal(true);

  // Tab navigation
  activeTab = signal<'overview' | 'configuration' | 'overrides'>('overview');

  // Overrides tab state
  overridesTemplateType = signal<'system' | 'user'>('system');
  editError = signal<string | null>(null);

  // Template (Configuration) tab state - now managed by template-config component
  templateTemplateType = signal<'system' | 'user'>('system'); // Still needed for overrides tab
  editingTemplateCell = signal<{ fieldKey: string; property: string } | null>(null);

  // Override sections expand state (legacy, kept for template cards)
  showSystemOverrides = signal(false);
  showUserOverrides = signal(false);

  // Inline editing state
  editingSystemField = signal<{ key: string; field: 'key' | 'value' } | null>(null);
  editingUserField = signal<{ key: string; field: 'key' | 'type' | 'defaultValue' } | null>(null);
  editingIdentifier = signal<{ identifier: string; template: 'system' | 'user'; field: string } | null>(null);
  editValue = signal('');
  // Loading states
  isLoadingTemplates = signal(false);
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
  // All unique identifiers (for tab badge)
  matrixIdentifiers = computed<string[]>(() => {
    const systemIds = this.systemOverrides().map(o => o.identifier).filter((id): id is string => !!id);
    const userIds = this.userOverrides().map(o => o.identifier).filter((id): id is string => !!id);
    return [...new Set([...systemIds, ...userIds])];
  });

    constructor() {
      // Initialize rate tier slider from environment's current tier
      effect(() => {
        const env = this.selectedEnvironment();
        if (env?.tier) {
          const index = this.rateLimitTiers().findIndex(t => t.id === env.tier);
          if (index >= 0) {
            this.selectedRateLimitTierIndex.set(index);
            this.originalTierIndex.set(index);
          }
        }
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

      // Load statistics when application changes
      effect(() => {
        const app = this.application();
        if (app?.id) {
          this.loadStatistics(app.id);
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

  // Environment Manager event handlers
  onEnvironmentCreated(newEnvironment: EnvironmentResponse): void {
    // Update application with new environment
    this.application.update(current => {
      if (!current) return current;
      return {
        ...current,
        environments: [...(current.environments ?? []), newEnvironment]
      };
    });
  }

  onEnvironmentUpdated(updatedEnvironment: EnvironmentResponse): void {
    // Update environment in application
    this.application.update(current => {
      if (!current) return current;
      return {
        ...current,
        environments: (current.environments ?? []).map(env =>
          env.id === updatedEnvironment.id ? updatedEnvironment : env
        )
      };
    });
  }

  onEnvironmentDeleted(environmentId: string): void {
    // Remove environment from application
    this.application.update(current => {
      if (!current) return current;
      return {
        ...current,
        environments: (current.environments ?? []).filter(env => env.id !== environmentId)
      };
    });
    }

  // Rate Limit Tier methods
  onRateLimitTierSliderChange(event: Event): void {
        const input = event.target as HTMLInputElement;
    this.selectedRateLimitTierIndex.set(parseInt(input.value, 10));
    }

  formatRateLimit(rateLimit: number): string {
    if (rateLimit >= 1000) {
      return (rateLimit / 1000) + 'k';
        }
    return rateLimit.toLocaleString();
    }

  confirmTierChange(): void {
    const env = this.selectedEnvironment();
    const app = this.application();
    const newTier = this.currentRateLimitTier();

    if (!env?.id || !app?.name) return;

    this.pricingState.addPendingEnvironment({
      environmentId: env.id,
      environmentName: env.name ?? 'Environment',
      applicationName: app.name,
      tier: newTier.id as PricingTier,
      monthlyPriceCents: TIER_PRICES[newTier.id as PricingTier]
    });

    // Update original to reflect pending change
    this.originalTierIndex.set(this.selectedRateLimitTierIndex());
  }

  // ============================================================================
  // TEMPLATE METHODS
  // ============================================================================

  // Card collapse toggles
  toggleApiAttributesCard(): void {
    this.apiAttributesExpanded.update(v => !v);
  }

  // Tab navigation
  setActiveTab(tab: 'overview' | 'configuration' | 'overrides'): void {
    this.activeTab.set(tab);
  }

  // Template Config event handlers
  onTemplateChanged(): void {
    // Reload templates when a field is added/edited/deleted
    const app = this.application();
    if (app?.id) {
      this.loadTemplates(app.id);
    }
  }

  onNavigateToOverrides(templateType: 'system' | 'user'): void {
    this.setActiveTab('overrides');
    this.overridesTemplateType.set(templateType);
  }

  // Override Manager event handlers
  async onOverridesChanged(): Promise<void> {
    // Reload overrides when they are modified
    const app = this.application();
    const env = this.selectedEnvironment();
    if (app?.id && env?.id) {
      await this.loadOverrides(app.id, env.id);
    }
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

  // Load statistics from API
  private async loadStatistics(applicationId: string): Promise<void> {
    try {
      const stats = await this.api.invoke(getApplicationStatistics, {id: applicationId});
      this.totalUsers.set(stats.totalUsers ?? 0);
      this.totalHitsThisMonth.set(stats.hitsThisMonth ?? 0);
    } catch (error) {
      console.error('Failed to load statistics:', error);
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
}
