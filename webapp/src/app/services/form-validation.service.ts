import {Injectable} from '@angular/core';

/**
 * Centralized form validation service
 * Eliminates duplicated validation logic across form components
 */
@Injectable({
  providedIn: 'root'
})
export class FormValidationService {

  /**
   * Validate email address format
   */
  validateEmail(email: string): string | null {
    const trimmed = email.trim();
    if (!trimmed) {
      return 'Email is required';
    }

    const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
    if (!emailRegex.test(trimmed)) {
      return 'Please enter a valid email address';
    }

    return null;
  }

  /**
   * Validate name field (for companies, applications, users, etc.)
   */
  validateName(name: string, minLength = 2, maxLength = 100, label = 'Name'): string | null {
    const trimmed = name.trim();

    if (!trimmed) {
      return `${label} is required`;
    }

    if (trimmed.length < minLength) {
      return `${label} must be at least ${minLength} characters`;
    }

    if (trimmed.length > maxLength) {
      return `${label} must be less than ${maxLength} characters`;
    }

    return null;
  }

  /**
   * Validate field key for template fields
   * Must start with letter and contain only letters, numbers, underscores
   */
  validateFieldKey(key: string, existingKeys: string[] = []): string | null {
    const trimmed = key.trim();

    if (!trimmed) {
      return 'Field key is required';
    }

    if (!/^[a-zA-Z][a-zA-Z0-9_]*$/.test(trimmed)) {
      return 'Key must start with a letter and contain only letters, numbers, and underscores';
    }

    if (existingKeys.includes(trimmed)) {
      return 'A field with this key already exists';
    }

    return null;
  }

  /**
   * Validate string field constraints
   */
  validateStringConstraints(
    minLength: number,
    maxLength: number,
    defaultValue?: string
  ): string | null {
    if (minLength < 0) {
      return 'Min length cannot be negative';
    }

    if (maxLength <= 0) {
      return 'Max length must be positive';
    }

    if (minLength > maxLength) {
      return 'Min length cannot exceed max length';
    }

    if (defaultValue) {
      if (defaultValue.length < minLength) {
        return `Default value must be at least ${minLength} characters`;
      }
      if (defaultValue.length > maxLength) {
        return `Default value must be at most ${maxLength} characters`;
      }
    }

    return null;
  }

  /**
   * Validate number field constraints including increment alignment
   */
  validateNumberConstraints(
    minValue: number,
    maxValue: number,
    increment: number,
    defaultValue?: number
  ): string | null {
    if (minValue > maxValue) {
      return 'Min value cannot exceed max value';
    }

    if (increment <= 0) {
      return 'Increment must be positive';
    }

    if (defaultValue !== undefined) {
      if (defaultValue < minValue) {
        return `Default value must be at least ${minValue}`;
      }

      if (defaultValue > maxValue) {
        return `Default value must be at most ${maxValue}`;
      }

      // Check increment alignment
      const diff = defaultValue - minValue;
      const remainder = Math.abs(diff % increment);
      const tolerance = increment * 1e-9;

      if (remainder > tolerance && remainder < increment - tolerance) {
        return `Default value must align with increment of ${increment} (starting from ${minValue})`;
      }
    }

    return null;
  }

  /**
   * Validate enum options
   */
  validateEnumOptions(options: string[]): string | null {
    if (options.length === 0) {
      return 'At least one option is required';
    }

    if (options.some(opt => !opt.trim())) {
      return 'Options cannot be empty';
    }

    return null;
  }

  /**
   * Validate password strength
   * Returns null if valid, error message otherwise
   */
  validatePassword(password: string, minLength = 8): string | null {
    if (!password) {
      return 'Password is required';
    }

    if (password.length < minLength) {
      return `Password must be at least ${minLength} characters`;
    }

    return null;
  }

  /**
   * Check if passwords match
   */
  validatePasswordMatch(password: string, confirmPassword: string): string | null {
    if (password !== confirmPassword) {
      return 'Passwords do not match';
    }

    return null;
  }

  /**
   * Validate required field
   */
  validateRequired(value: string | null | undefined, fieldName = 'This field'): string | null {
    const trimmed = value?.trim();
    if (!trimmed) {
      return `${fieldName} is required`;
    }

    return null;
  }
}
