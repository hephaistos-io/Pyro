import {Component, computed, input, OnInit, signal} from '@angular/core';
import {FormsModule} from '@angular/forms';
import {
  BooleanTemplateField,
  EnumTemplateField,
  FieldType,
  NumberTemplateField,
  StringTemplateField,
  TemplateField
} from '../../api/generated/models';
import {FIELD_DEFAULTS} from '../add-field-overlay/add-field-overlay.component';

export interface EditFieldOverlayData {
  templateType: 'system' | 'user';
  field: TemplateField;
  existingFieldKeys: string[];
  onSubmit: (field: TemplateField) => void;
}

@Component({
  selector: 'app-edit-field-overlay',
  standalone: true,
  imports: [FormsModule],
  templateUrl: './edit-field-overlay.component.html',
  styleUrl: './edit-field-overlay.component.scss'
})
export class EditFieldOverlayComponent implements OnInit {
  data = input.required<EditFieldOverlayData>();
  close = input.required<() => void>();

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
  constraintError = computed(() => {
    const type = this.fieldType();
    if (type === 'STRING') {
      if (this.minLength() < 0) {
        return 'Min length cannot be negative';
      }
      if (this.maxLength() <= 0) {
        return 'Max length must be positive';
      }
      if (this.minLength() > this.maxLength()) {
        return 'Min length cannot exceed max length';
      }
      // Validate default value against constraints
      const defaultVal = this.stringDefaultValue();
      if (defaultVal) {
        if (defaultVal.length < this.minLength()) {
          return `Default value must be at least ${this.minLength()} characters`;
        }
        if (defaultVal.length > this.maxLength()) {
          return `Default value must be at most ${this.maxLength()} characters`;
        }
      }
    }
    if (type === 'NUMBER') {
      if (this.minValue() > this.maxValue()) {
        return 'Min value cannot exceed max value';
      }
      if (this.incrementAmount() <= 0) {
        return 'Increment must be positive';
      }
      // Validate default value against constraints
      const defaultVal = this.numberDefaultValue();
      if (defaultVal < this.minValue()) {
        return `Default value must be at least ${this.minValue()}`;
      }
      if (defaultVal > this.maxValue()) {
        return `Default value must be at most ${this.maxValue()}`;
      }
      // Check increment alignment
      const increment = this.incrementAmount();
      const diff = defaultVal - this.minValue();
      // Use tolerance for floating point comparison
      const remainder = Math.abs(diff % increment);
      const tolerance = increment * 1e-9;
      if (remainder > tolerance && remainder < increment - tolerance) {
        return `Default value must align with increment of ${increment} (starting from ${this.minValue()})`;
      }
    }
    if (type === 'ENUM') {
      if (this.enumOptions().length === 0) {
        return 'At least one option is required';
      }
      if (this.enumOptions().some(opt => !opt.trim())) {
        return 'Options cannot be empty';
      }
    }
    return null;
  });
  isValid = computed(() => !this.constraintError());

  // Key is not editable, so no validation needed for it
  // Original key for validation (to allow keeping the same key)
  private originalKey = '';

  ngOnInit(): void {
    const field = this.data().field;
    this.originalKey = field.key ?? '';

    // Populate form with existing field data
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

  // Get display name for field type
  getTypeDisplayName(): string {
    switch (this.fieldType()) {
      case 'STRING':
        return 'String';
      case 'NUMBER':
        return 'Number';
      case 'BOOLEAN':
        return 'Boolean';
      case 'ENUM':
        return 'Enum';
      default:
        return this.fieldType();
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
