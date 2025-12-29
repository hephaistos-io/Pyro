import {Component, inject, signal} from '@angular/core';
import {FormsModule} from '@angular/forms';
import {RouterLink} from '@angular/router';
import {Api} from '../../api/generated/api';
import {requestPasswordReset} from '../../api/generated/fn/password-reset/request-password-reset';
import {isValidEmail} from '../../utils/validators.util';

@Component({
  selector: 'app-forgot-password',
  standalone: true,
  imports: [FormsModule, RouterLink],
  templateUrl: './forgot-password.component.html',
  styleUrl: './forgot-password.component.scss'
})
export class ForgotPasswordComponent {
  email = signal('');
  error = signal('');
  isLoading = signal(false);
  isSubmitted = signal(false);

  private api = inject(Api);

  async onSubmit(): Promise<void> {
    const validationError = this.validateForm();
    if (validationError) {
      this.error.set(validationError);
      return;
    }

    this.isLoading.set(true);
    this.error.set('');

    try {
      await this.api.invoke(requestPasswordReset, {body: {email: this.email()}});
      this.isSubmitted.set(true);
    } catch {
      // Always show success to prevent account enumeration
      this.isSubmitted.set(true);
    } finally {
      this.isLoading.set(false);
    }
  }

  private validateForm(): string | null {
    if (!this.email()) {
      return 'Email is required';
    }

    if (!isValidEmail(this.email())) {
      return 'Please enter a valid email address';
    }

    return null;
  }
}
