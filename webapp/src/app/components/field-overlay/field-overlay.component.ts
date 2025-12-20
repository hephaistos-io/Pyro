import {Component, computed, inject, input, OnInit, signal} from '@angular/core';
import {FormsModule} from '@angular/forms';
import {
  BooleanTemplateField,
  EnumTemplateField,
  FieldType,
  NumberTemplateField,
  StringTemplateField,
  TemplateField
} from '../../api/generated/models';
import {FormValidationService} from '../../services/form-validation.service';

// Default values for field constraints - used in form initialization
export const FIELD_DEFAULTS = {
  string: {
    minLength: 0,
    maxLength: 50
  },
  number: {
    minValue: 0,
    maxValue: 100,
    increment: 1,
    defaultValue: 0
  },
  boolean: {
    defaultValue: false as boolean
  },
  enum: {
    options: ['option1'] as string[]
  }
};

export interface FieldOverlayData {
  mode: 'add' | 'edit';
  templateType: 'system' | 'user';
  existingFieldKeys: string[];
  field?: TemplateField; // Required for edit mode
  onSubmit: (field: TemplateField) => void;
}

@Component({
  selector: 'app-field-overlay',
  standalone: true,
  imports: [FormsModule],
  templateUrl: './field-overlay.component.html',
  styleUrl: './field-overlay.component.scss'
})
export class FieldOverlayComponent implements OnInit {
  data = input.required<FieldOverlayData>();
  close = input.required<() => void>();
  // Computed mode helpers
  mode = computed(() => this.data().mode);
  isEditMode = computed(() => this.mode() === 'edit');
  title = computed(() => this.isEditMode() ? 'Edit Field' : 'Add New Field');
  // Expose defaults to template
  readonly defaults = FIELD_DEFAULTS;
  // Form state signals
  fieldKey = signal('');
  fieldType = signal<'STRING' | 'NUMBER' | 'BOOLEAN' | 'ENUM'>('STRING');
  description = signal('');
  editable = signal(true);
  // Type-specific default values
  stringDefaultValue = signal('');
  numberDefaultValue = signal<number>(FIELD_DEFAULTS.number.defaultValue);
  booleanDefaultValue = signal(FIELD_DEFAULTS.boolean.defaultValue);
  enumDefaultValue = signal('');
  // STRING constraints
  minLength = signal<number>(FIELD_DEFAULTS.string.minLength);
  maxLength = signal<number>(FIELD_DEFAULTS.string.maxLength);
  // NUMBER constraints
  minValue = signal<number>(FIELD_DEFAULTS.number.minValue);
  maxValue = signal<number>(FIELD_DEFAULTS.number.maxValue);
  incrementAmount = signal<number>(FIELD_DEFAULTS.number.increment);
  // ENUM options
  enumOptions = signal<string[]>([...FIELD_DEFAULTS.enum.options]);
  newEnumOption = signal('');
  private validationService = inject(FormValidationService);
  // Validation - Key validation only in add mode
  keyError = computed(() => {
    // Skip validation in edit mode (key is readonly)
    if (this.isEditMode()) {
      return null;
    }

    return this.validationService.validateFieldKey(
      this.fieldKey(),
      this.data()?.existingFieldKeys ?? []
    );
  });
  constraintError = computed(() => {
    const type = this.fieldType();

    if (type === 'STRING') {
      return this.validationService.validateStringConstraints(
        this.minLength(),
        this.maxLength(),
        this.stringDefaultValue() || undefined
      );
    }

    if (type === 'NUMBER') {
      return this.validationService.validateNumberConstraints(
        this.minValue(),
        this.maxValue(),
        this.incrementAmount(),
        this.numberDefaultValue()
      );
    }

    if (type === 'ENUM') {
      return this.validationService.validateEnumOptions(this.enumOptions());
    }

    return null;
  });
  isValid = computed(() => !this.keyError() && !this.constraintError());
  // Original key for validation (edit mode only)
  private originalKey = '';

  ngOnInit(): void {
    // Populate form with existing field data in edit mode
    if (this.isEditMode()) {
      const field = this.data().field;
      if (!field) {
        console.error('Edit mode requires a field to be provided');
        return;
      }

      this.originalKey = field.key ?? '';
      this.fieldKey.set(field.key ?? '');
      this.description.set(field.description ?? '');
      this.editable.set(field.editable ?? true);

      // Set type and type-specific values
      switch (field.type) {
        case FieldType.String:
          this.fieldType.set('STRING');
          const stringField = field as StringTemplateField;
          this.stringDefaultValue.set(stringField.defaultValue ?? '');
          this.minLength.set(stringField.minLength ?? FIELD_DEFAULTS.string.minLength);
          this.maxLength.set(stringField.maxLength ?? FIELD_DEFAULTS.string.maxLength);
          break;
        case FieldType.Number:
          this.fieldType.set('NUMBER');
          const numberField = field as NumberTemplateField;
          this.numberDefaultValue.set(numberField.defaultValue ?? FIELD_DEFAULTS.number.defaultValue);
          this.minValue.set(numberField.minValue ?? FIELD_DEFAULTS.number.minValue);
          this.maxValue.set(numberField.maxValue ?? FIELD_DEFAULTS.number.maxValue);
          this.incrementAmount.set(numberField.incrementAmount ?? FIELD_DEFAULTS.number.increment);
          break;
        case FieldType.Boolean:
          this.fieldType.set('BOOLEAN');
          const booleanField = field as BooleanTemplateField;
          this.booleanDefaultValue.set(booleanField.defaultValue ?? FIELD_DEFAULTS.boolean.defaultValue);
          break;
        case FieldType.Enum:
          this.fieldType.set('ENUM');
          const enumField = field as EnumTemplateField;
          this.enumOptions.set([...(enumField.options ?? FIELD_DEFAULTS.enum.options)]);
          this.enumDefaultValue.set(enumField.defaultValue ?? '');
          break;
      }
    }
  }

  // ENUM option management
  addEnumOption(): void {
    const option = this.newEnumOption().trim();
    if (option && !this.enumOptions().includes(option)) {
      this.enumOptions.update(opts => [...opts, option]);
      this.newEnumOption.set('');
    }
  }

  removeEnumOption(index: number): void {
    this.enumOptions.update(opts => opts.filter((_, i) => i !== index));
    // If we removed the default value, reset it
    const currentDefault = this.enumDefaultValue();
    if (!this.enumOptions().includes(currentDefault)) {
      this.enumDefaultValue.set(this.enumOptions()[0] ?? '');
    }
  }

  // Form submission
  onSubmit(): void {
    if (!this.isValid()) {
      return;
    }

    const field = this.buildField();
    this.data().onSubmit(field);
    this.close()();
  }

  onCancel(): void {
    this.close()();
  }

  private buildField(): TemplateField {
    const type = this.fieldType();
    const key = this.fieldKey().trim();
    const desc = this.description().trim() || undefined;
    // For SYSTEM templates, editable is always false
    const edit = this.data().templateType === 'system' ? false : this.editable();

    switch (type) {
      case 'STRING':
        return {
          key,
          type: FieldType.String,
          description: desc,
          editable: edit,
          defaultValue: this.stringDefaultValue() || undefined,
          minLength: this.minLength(),
          maxLength: this.maxLength()
        } as StringTemplateField;
      case 'NUMBER':
        return {
          key,
          type: FieldType.Number,
          description: desc,
          editable: edit,
          defaultValue: this.numberDefaultValue(),
          minValue: this.minValue(),
          maxValue: this.maxValue(),
          incrementAmount: this.incrementAmount()
        } as NumberTemplateField;
      case 'BOOLEAN':
        return {
          key,
          type: FieldType.Boolean,
          description: desc,
          editable: edit,
          defaultValue: this.booleanDefaultValue()
        } as BooleanTemplateField;
      case 'ENUM':
        return {
          key,
          type: FieldType.Enum,
          description: desc,
          editable: edit,
          defaultValue: this.enumDefaultValue() || this.enumOptions()[0] || undefined,
          options: [...this.enumOptions()]
        } as EnumTemplateField;
    }
  }
}
