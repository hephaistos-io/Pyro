import {Component, computed, input, signal} from '@angular/core';
import {FormsModule} from '@angular/forms';
import {
  BooleanTemplateField,
  EnumTemplateField,
  FieldType,
  NumberTemplateField,
  StringTemplateField,
  TemplateField
} from '../../api/generated/models';

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

export interface AddFieldOverlayData {
  templateType: 'system' | 'user';
  existingFieldKeys: string[];
  onSubmit: (field: TemplateField) => void;
}

@Component({
  selector: 'app-add-field-overlay',
  standalone: true,
  imports: [FormsModule],
  templateUrl: './add-field-overlay.component.html',
  styleUrl: './add-field-overlay.component.scss'
})
export class AddFieldOverlayComponent {
  data = input.required<AddFieldOverlayData>();
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

  // Validation
  keyError = computed(() => {
    const key = this.fieldKey().trim();
    if (!key) return 'Field key is required';
    if (!/^[a-zA-Z][a-zA-Z0-9_]*$/.test(key)) {
      return 'Key must start with a letter and contain only letters, numbers, and underscores';
    }
    if (this.data()?.existingFieldKeys?.includes(key)) {
      return 'A field with this key already exists';
    }
    return null;
  });

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

  isValid = computed(() => !this.keyError() && !this.constraintError());

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
