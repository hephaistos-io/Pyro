import {Component, computed, inject, input, model, output, signal} from '@angular/core';
import {FormsModule} from '@angular/forms';
import {
  EnumTemplateField,
  NumberTemplateField,
  StringTemplateField,
  TemplateField,
  TemplateResponse,
  TemplateType,
  TemplateValuesResponse
} from '../../api/generated/models';
import {TemplateService} from '../../services/template.service';
import {OverlayService} from '../../services/overlay.service';
import {
  CopyOverridesOverlayComponent,
  CopyOverridesOverlayData
} from '../copy-overrides-overlay/copy-overrides-overlay.component';
import {
  DeleteFieldOverlayComponent,
  DeleteFieldOverlayData
} from '../delete-field-overlay/delete-field-overlay.component';

@Component({
  selector: 'app-override-manager',
  standalone: true,
  imports: [FormsModule],
  templateUrl: './override-manager.component.html',
  styleUrl: './override-manager.component.scss'
})
export class OverrideManagerComponent {
  templateService = inject(TemplateService);
  // Inputs
  applicationId = input.required<string>();
  applicationName = input.required<string>();
  environmentId = input.required<string>();
  environments = input.required<{ id?: string; name?: string }[]>();
  systemTemplate = input.required<TemplateResponse | null>();
  userTemplate = input.required<TemplateResponse | null>();
  systemOverrides = input.required<TemplateValuesResponse[]>();
  userOverrides = input.required<TemplateValuesResponse[]>();
  // Two-way binding for template type
  templateType = model<'system' | 'user'>('system');
  // Outputs
  overridesChanged = output<void>();
  // Internal state
  searchQuery = signal('');
  showOnlyOverrides = signal(false);
  editingMatrixCell = signal<{ identifier: string; attribute: string } | null>(null);
  showAddIdentifier = signal(false);
  newIdentifierName = signal('');
  editValue = signal('');
  editError = signal<string | null>(null);
  // Computed: System template fields
  systemTemplateFields = computed<TemplateField[]>(() => {
    return this.systemTemplate()?.schema?.fields ?? [];
  });
  // Computed: User template fields
  userTemplateFields = computed<TemplateField[]>(() => {
    return this.userTemplate()?.schema?.fields ?? [];
  });
  // Computed: All identifiers
  allIdentifiers = computed<string[]>(() => {
    const systemIds = this.systemOverrides().map(o => o.identifier).filter((id): id is string => !!id);
    const userIds = this.userOverrides().map(o => o.identifier).filter((id): id is string => !!id);
    return [...new Set([...systemIds, ...userIds])];
  });
  // Computed: Check if has any identifier overrides
  hasIdentifierOverrides = computed(() => {
    const systemOverrides = this.systemOverrides();
    const userOverrides = this.userOverrides();
    return systemOverrides.length > 0 || userOverrides.length > 0;
  });
  // Computed: Matrix attribute keys (filtered)
  matrixAttributeKeys = computed<string[]>(() => {
    const query = this.searchQuery().toLowerCase();
    const showOnlyOverrides = this.showOnlyOverrides();
    const templateType = this.templateType();

    let keys: string[];
    if (templateType === 'system') {
      keys = this.systemTemplateFields().map(f => f.key).filter((k): k is string => !!k);
    } else {
      keys = this.userTemplateFields().map(f => f.key).filter((k): k is string => !!k);
    }

    // Filter by search query
    if (query) {
      keys = keys.filter(key => key.toLowerCase().includes(query));
    }

    // Filter to show only attributes that have at least one override
    if (showOnlyOverrides) {
      const overrides = templateType === 'system' ? this.systemOverrides() : this.userOverrides();
      keys = keys.filter(key => {
        return overrides.some(override => {
          const values = override.values as Record<string, unknown> | undefined;
          return values && key in values;
        });
      });
    }

    return keys;
  });
  // Computed: Matrix identifiers
  matrixIdentifiers = computed<string[]>(() => {
    return this.allIdentifiers();
  });
  private overlayService = inject(OverlayService);

  // Get matrix cell value (override value or null if using default)
  getMatrixCellValue(identifier: string, attribute: string): string | null {
    const templateType = this.templateType();
    const overrides = templateType === 'system' ? this.systemOverrides() : this.userOverrides();

    const identifierOverrides = overrides.find(o => o.identifier === identifier);
    if (!identifierOverrides?.values) return null;

    const values = identifierOverrides.values as Record<string, unknown>;
    const value = values[attribute];
    return value !== undefined ? String(value) : null;
  }

  // Get default value for an attribute
  getDefaultValue(attribute: string): string {
    const templateType = this.templateType();
    const fields = templateType === 'system' ? this.systemTemplateFields() : this.userTemplateFields();

    const field = fields.find(f => f.key === attribute);
    if (!field) return '';

    const value = this.templateService.getDefaultValue(field);
    return value !== undefined && value !== null ? String(value) : '';
  }

  // Get the field definition for a matrix attribute
  getMatrixField(attribute: string): TemplateField | undefined {
    const templateType = this.templateType();
    const fields = templateType === 'system' ? this.systemTemplateFields() : this.userTemplateFields();
    return fields.find(f => f.key === attribute);
  }

  // Matrix cell editing
  startEditMatrixCell(identifier: string, attribute: string, currentValue: string | null): void {
    this.editingMatrixCell.set({identifier, attribute});
    this.editValue.set(currentValue ?? this.getDefaultValue(attribute));
    this.editError.set(null);
  }

  onMatrixInputChange(newValue: string): void {
    this.editValue.set(newValue);

    const editing = this.editingMatrixCell();
    if (!editing) return;

    const {attribute} = editing;
    const field = this.getMatrixField(attribute);
    if (!field) return;

    // Validate the new value
    const error = this.validateDefaultValue(field, newValue.trim());
    this.editError.set(error);
  }

  async saveMatrixCellEdit(): Promise<void> {
    const editing = this.editingMatrixCell();
    const appId = this.applicationId();
    const envId = this.environmentId();

    if (!editing || !appId || !envId) return;

    const {identifier, attribute} = editing;
    const newValue = this.editValue().trim();
    const templateType = this.templateType();

    // Validate against field constraints
    const field = this.getMatrixField(attribute);
    if (!field) return;

    const validationError = this.validateDefaultValue(field, newValue);
    if (validationError) {
      this.editError.set(validationError);
      return;
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
          appId,
          templateType === 'system' ? TemplateType.System : TemplateType.User,
          envId,
          identifier
        );
      } else {
        // Update the override
        await this.templateService.setOverride(
          appId,
          templateType === 'system' ? TemplateType.System : TemplateType.User,
          envId,
          identifier,
          updatedValues
        );
      }
      this.overridesChanged.emit();
    } catch (error) {
      console.error('Error saving matrix cell:', error);
      this.editError.set('Failed to save override');
      return;
    }

    this.editingMatrixCell.set(null);
    this.editValue.set('');
    this.editError.set(null);
  }

  tryToSaveMatrixCellEdit(): void {
    if (this.editError()) {
      return;
    }
    this.saveMatrixCellEdit();
  }

  cancelMatrixCellEdit(): void {
    this.editingMatrixCell.set(null);
    this.editValue.set('');
    this.editError.set(null);
  }

  async addIdentifier(): Promise<void> {
    const appId = this.applicationId();
    const envId = this.environmentId();
    const identifier = this.newIdentifierName().trim();

    if (!appId || !envId || !identifier) return;

    // Check if identifier already exists
    if (this.allIdentifiers().includes(identifier)) {
      alert(`Identifier "${identifier}" already exists in this environment.`);
      return;
    }

    const templateType = this.templateType();

    try {
      // Create override with empty values (just to establish the identifier)
      await this.templateService.setOverride(
        appId,
        templateType === 'system' ? TemplateType.System : TemplateType.User,
        envId,
        identifier,
        {}
      );

      this.newIdentifierName.set('');
      this.showAddIdentifier.set(false);
      this.overridesChanged.emit();
    } catch (error) {
      console.error('Error adding identifier:', error);
      alert('Failed to add identifier');
    }
  }

  deleteIdentifier(identifier: string): void {
    const appId = this.applicationId();
    const envId = this.environmentId();
    const appName = this.applicationName();
    if (!appId || !envId || !appName) return;

    const templateType = this.templateType();

    this.overlayService.open<DeleteFieldOverlayData>({
      component: DeleteFieldOverlayComponent,
      data: {
        fieldKey: identifier,
        applicationName: appName,
        templateType: templateType,
        type: 'identifier',
        onConfirm: async () => {
          try {
            await this.templateService.deleteOverride(
              appId,
              templateType === 'system' ? TemplateType.System : TemplateType.User,
              envId,
              identifier
            );

            this.overridesChanged.emit();
          } catch (error) {
            console.error('Error deleting identifier:', error);
            alert('Failed to delete identifier');
          }
        }
      },
      maxWidth: '500px'
    });
  }

  openCopyOverridesOverlay(): void {
    const appId = this.applicationId();
    const envId = this.environmentId();
    const appName = this.applicationName();

    if (!appId || !envId) return;

    this.overlayService.open<CopyOverridesOverlayData>({
      component: CopyOverridesOverlayComponent,
      data: {
        applicationId: appId,
        applicationName: appName,
        currentEnvironmentId: envId,
        environments: this.environments(),
        allIdentifiers: this.allIdentifiers(),
        onSuccess: () => {
          this.overridesChanged.emit();
        }
      },
      maxWidth: '500px'
    });
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
}
