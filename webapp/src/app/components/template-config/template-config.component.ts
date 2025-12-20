import {Component, computed, inject, input, model, output, signal} from '@angular/core';
import {FormsModule} from '@angular/forms';
import {TemplateField, TemplateResponse, TemplateType} from '../../api/generated/models';
import {TemplateService} from '../../services/template.service';
import {OverlayService} from '../../services/overlay.service';
import {FieldOverlayComponent, FieldOverlayData} from '../field-overlay/field-overlay.component';
import {
  DeleteFieldOverlayComponent,
  DeleteFieldOverlayData
} from '../delete-field-overlay/delete-field-overlay.component';
import {Api} from '../../api/generated/api';
import {updateTemplate} from '../../api/generated/fn/templates/update-template';

@Component({
  selector: 'app-template-config',
  standalone: true,
  imports: [FormsModule],
  templateUrl: './template-config.component.html',
  styleUrl: './template-config.component.scss'
})
export class TemplateConfigComponent {
  // Inputs
  applicationId = input.required<string>();
  applicationName = input.required<string>();
  systemTemplate = input.required<TemplateResponse | null>();
  userTemplate = input.required<TemplateResponse | null>();
  systemOverrideCount = input.required<number>();
  userOverrideCount = input.required<number>();
  hasIdentifierOverrides = input.required<boolean>();
  // Two-way binding for template type
  templateType = model<'system' | 'user'>('system');
  // Outputs
  templateChanged = output<void>();
  navigateToOverrides = output<'system' | 'user'>();
  // Internal state
  searchQuery = signal('');
  showFieldsWithDefaults = signal(false);
  // Computed: System template fields
  systemTemplateFields = computed<TemplateField[]>(() => {
    return this.systemTemplate()?.schema?.fields ?? [];
  });
  // Computed: User template fields
  userTemplateFields = computed<TemplateField[]>(() => {
    return this.userTemplate()?.schema?.fields ?? [];
  });
  private overlayService = inject(OverlayService);
  private templateService = inject(TemplateService);
  // Filtered system template fields
  filteredSystemTemplateFields = computed<TemplateField[]>(() => {
    const fields = this.systemTemplateFields();
    const query = this.searchQuery().toLowerCase();

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
  // Filtered user template fields
  filteredUserTemplateFields = computed<TemplateField[]>(() => {
    const fields = this.userTemplateFields();
    const query = this.searchQuery().toLowerCase();

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
  private api = inject(Api);

  // Helper to format constraints for display
  formatConstraints(field: TemplateField): string {
    return this.templateService.formatConstraints(field);
  }

  // Helper to get default value as string
  getFieldDefaultValue(field: TemplateField): string {
    const value = this.templateService.getDefaultValue(field);
    return value !== undefined && value !== null ? String(value) : '';
  }

  openAddFieldOverlay(templateType: 'system' | 'user'): void {
    const fields = templateType === 'system'
      ? this.systemTemplateFields()
      : this.userTemplateFields();

    const existingKeys = fields
      .map(f => f.key)
      .filter((k): k is string => !!k);

    this.overlayService.open<FieldOverlayData>({
      component: FieldOverlayComponent,
      data: {
        mode: 'add',
        templateType,
        existingFieldKeys: existingKeys,
        onSubmit: async (newField: TemplateField) => {
          // Add new field to existing fields array
          const updatedFields = [...fields, newField];

          // Call API to update template
          await this.api.invoke(updateTemplate, {
            applicationId: this.applicationId(),
            type: templateType === 'system' ? TemplateType.System : TemplateType.User,
            body: {
              schema: {
                fields: updatedFields
              }
            }
          });

          // Notify parent to reload
          this.templateChanged.emit();
        }
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

    this.overlayService.open<FieldOverlayData>({
      component: FieldOverlayComponent,
      data: {
        mode: 'edit',
        templateType,
        existingFieldKeys: existingKeys,
        field,
        onSubmit: async (updatedField: TemplateField) => {
          // Replace the edited field in the array
          const updatedFields = fields.map(f =>
            f.key === field.key ? updatedField : f
          );

          // Call API to update template
          await this.api.invoke(updateTemplate, {
            applicationId: this.applicationId(),
            type: templateType === 'system' ? TemplateType.System : TemplateType.User,
            body: {
              schema: {
                fields: updatedFields
              }
            }
          });

          // Notify parent to reload
          this.templateChanged.emit();
        }
      },
      maxWidth: '500px'
    });
  }

  openDeleteFieldOverlay(key: string, templateType: 'system' | 'user'): void {
    const appName = this.applicationName();
    const appId = this.applicationId();

    this.overlayService.open<DeleteFieldOverlayData>({
      component: DeleteFieldOverlayComponent,
      data: {
        fieldKey: key,
        applicationName: appName,
        templateType,
        onConfirm: async () => {
          try {
            // Get the current template
            const template = templateType === 'system' ? this.systemTemplate() : this.userTemplate();
            if (!template || !template.schema) return;

            // Remove the field from the schema
            const updatedFields = template.schema.fields?.filter(f => f.key !== key) ?? [];

            // Update the template via API
            await this.api.invoke(updateTemplate, {
              applicationId: appId,
              type: templateType === 'system' ? TemplateType.System : TemplateType.User,
              body: {
                schema: {
                  fields: updatedFields
                }
              }
            });

            // Emit change event to trigger parent reload
            this.templateChanged.emit();
          } catch (error) {
            console.error('Error deleting field:', error);
            alert('Failed to delete field');
          }
        }
      },
      maxWidth: '450px'
    });
  }

  onNavigateToOverrides(templateType: 'system' | 'user'): void {
    this.navigateToOverrides.emit(templateType);
  }
}
