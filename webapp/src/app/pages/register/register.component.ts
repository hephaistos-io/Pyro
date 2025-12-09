import {Component, inject} from '@angular/core';
import {FormsModule} from '@angular/forms';
import {Api} from '../../api/generated/api';
import {register} from '../../api/generated/functions';
import {UserRegistrationRequest} from '../../api/generated/models';
import zxcvbn from 'zxcvbn';

@Component({
  selector: 'app-register',
  standalone: true,
  imports: [FormsModule],
  templateUrl: './register.component.html',
  styleUrl: './register.component.scss'
})
export class RegisterComponent {
  email = '';
  firstName = '';
  lastName = '';
  password = '';
  confirmPassword = '';
  submitted = false;
  error = '';
  isLoading = false;
  private api = inject(Api);

  get passwordStrength(): { score: number; label: string; feedback: string } {
    if (!this.password) {
      return {score: 0, label: '', feedback: ''};
    }

    if (this.password.length < 8) {
      return {score: 0, label: 'Too short', feedback: 'At least 8 characters required'};
    }

    const result = zxcvbn(this.password);
    const labels = ['Too weak', 'Weak', 'Fair', 'Good', 'Strong'];
    const feedback = result.feedback.warning || result.feedback.suggestions[0] || '';

    return {
      score: result.score,
      label: labels[result.score],
      feedback: feedback
    };
  }

  async onSubmit(): Promise<void> {
    // Validate form
    const validationError = this.validateForm();
    if (validationError) {
      this.error = validationError;
      return;
    }

    // Set loading state
    this.isLoading = true;
    this.error = '';

    try {
      // Call API
      const registrationRequest: UserRegistrationRequest = {
        email: this.email,
        firstName: this.firstName,
        lastName: this.lastName,
        password: this.password
      };

      await this.api.invoke(register, {body: registrationRequest});

      // Handle success
      this.submitted = true;
      this.isLoading = false;
    } catch (err: any) {
      // Handle error
      this.isLoading = false;
      this.error = this.getErrorMessage(err);
    }
  }

  private validateForm(): string | null {
    if (!this.firstName || !this.lastName || !this.email || !this.password || !this.confirmPassword) {
      return 'All fields are required';
    }

    if (!this.isValidName(this.firstName)) {
      return 'First name must be 2-50 characters and contain only letters';
    }

    if (!this.isValidName(this.lastName)) {
      return 'Last name must be 2-50 characters and contain only letters';
    }

    if (!this.isValidEmail(this.email)) {
      return 'Please enter a valid email address';
    }

    const passwordValidation = this.isValidPassword(this.password);
    if (!passwordValidation.valid) {
      return passwordValidation.message || 'Password is invalid';
    }

    if (this.password !== this.confirmPassword) {
      return 'Passwords do not match';
    }

    return null;
  }

  private isValidEmail(email: string): boolean {
    const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
    return emailRegex.test(email);
  }

  private isValidName(name: string): boolean {
    return name.length >= 2 &&
      name.length <= 50 &&
      /^[a-zA-Z\s\-']+$/.test(name);
  }

  private isValidPassword(password: string): { valid: boolean; message?: string } {
    if (password.length < 8) {
      return {valid: false, message: 'Password must be at least 8 characters'};
    }

    const result = zxcvbn(password);

    if (result.score < 3) {
      return {
        valid: false,
        message: result.feedback.warning || 'Password is too weak. Try a longer or more complex password.'
      };
    }

    return {valid: true};
  }

  private getErrorMessage(err: any): string {
    // Parse backend error response
    if (err.error && err.error.code) {
      const code = err.error.code;
      const message = err.error.message;

      switch (code) {
        case 'DUPLICATE_RESOURCE':
          return 'This email is already registered';
        case 'VALIDATION_ERROR':
          return message;
        case 'INTERNAL_ERROR':
          return 'Server error. Please try again later';
        default:
          return message || 'An error occurred';
      }
    }

    // Fallback to HTTP status codes for unexpected responses
    switch (err.status) {
      case 0:
        return 'Cannot connect to server. Please check your connection';
      case 400:
        return 'Invalid registration data. Please check your inputs';
      case 409:
        return 'This email is already registered';
      case 500:
        return 'Server error. Please try again later';
      default:
        return 'Registration failed. Please try again';
    }
  }
}
