import {Component, inject, OnInit, signal} from '@angular/core';
import {FormsModule} from '@angular/forms';
import {ActivatedRoute, Router, RouterLink} from '@angular/router';
import {Api} from '../../api/generated/api';
import {validateToken} from '../../api/generated/fn/password-reset/validate-token';
import {resetPassword} from '../../api/generated/fn/password-reset/reset-password';
import {handleApiError} from '../../utils/error-handler.util';
import {
  PasswordStrengthMeterComponent
} from '../../components/password-strength-meter/password-strength-meter.component';

@Component({
  selector: 'app-reset-password',
  standalone: true,
  imports: [FormsModule, RouterLink, PasswordStrengthMeterComponent],
  templateUrl: './reset-password.component.html',
  styleUrl: './reset-password.component.scss'
})
export class ResetPasswordComponent implements OnInit {
  password = signal('');
  confirmPassword = signal('');
  error = signal('');
  isLoading = signal(false);
  isValidating = signal(true);
  isTokenValid = signal(false);
  isResetComplete = signal(false);

  private token = '';
  private api = inject(Api);
  private route = inject(ActivatedRoute);
  private router = inject(Router);

  async ngOnInit(): Promise<void> {
    this.token = this.route.snapshot.queryParams['token'] || '';

    if (!this.token) {
      this.isValidating.set(false);
      this.isTokenValid.set(false);
      return;
    }

    try {
      const response = await this.api.invoke(validateToken, {token: this.token});
      this.isTokenValid.set(response['valid']);
    } catch {
      this.isTokenValid.set(false);
    } finally {
      this.isValidating.set(false);
    }
  }

  async onSubmit(): Promise<void> {
    const validationError = this.validateForm();
    if (validationError) {
      this.error.set(validationError);
      return;
    }

    this.isLoading.set(true);
    this.error.set('');

    try {
      await this.api.invoke(resetPassword, {
        body: {
          token: this.token,
          newPassword: this.password()
        }
      });
      this.isResetComplete.set(true);
    } catch (err: unknown) {
      this.isLoading.set(false);
      this.error.set(handleApiError(err, 'password reset'));
    }
  }

  goToLogin(): void {
    this.router.navigateByUrl('/login');
  }

  private validateForm(): string | null {
    if (!this.password() || !this.confirmPassword()) {
      return 'Both password fields are required';
    }

    if (this.password().length < 8) {
      return 'Password must be at least 8 characters';
    }

    if (this.password() !== this.confirmPassword()) {
      return 'Passwords do not match';
    }

    return null;
  }
}
