import {Component, computed, inject, OnInit, signal} from '@angular/core';
import {FormsModule} from '@angular/forms';
import {RouterLink} from '@angular/router';
import {Api} from '../../api/generated/api';
import {updateProfile} from '../../api/generated/fn/profile/update-profile';
import {requestEmailChange} from '../../api/generated/fn/profile/request-email-change';
import {requestPasswordResetAuthenticated} from '../../api/generated/fn/profile/request-password-reset-authenticated';
import {CustomerService} from '../../services/customer.service';
import {handleApiError} from '../../utils/error-handler.util';
import {isValidEmail} from '../../utils/validators.util';

@Component({
  selector: 'app-profile-settings',
  standalone: true,
  imports: [FormsModule, RouterLink],
  templateUrl: './profile-settings.component.html',
  styleUrl: './profile-settings.component.scss'
})
export class ProfileSettingsComponent implements OnInit {
  firstName = signal('');
  lastName = signal('');
  newEmail = signal('');

  isProfileLoading = signal(false);
  isEmailLoading = signal(false);
  isPasswordLoading = signal(false);

  profileError = signal('');
  emailError = signal('');
  passwordError = signal('');

  profileSuccess = signal(false);
  passwordSuccess = signal(false);
  emailSuccess = signal(false);

  private api = inject(Api);
  private customerService = inject(CustomerService);

  currentEmail = computed(() => this.customerService.customerProfile()?.email ?? '');

  ngOnInit(): void {
    const profile = this.customerService.customerProfile();
    if (profile) {
      this.firstName.set(profile.firstName ?? '');
      this.lastName.set(profile.lastName ?? '');
    }
  }

  async saveProfile(): Promise<void> {
    this.isProfileLoading.set(true);
    this.profileError.set('');
    this.profileSuccess.set(false);

    try {
      await this.api.invoke(updateProfile, {
        body: {
          firstName: this.firstName(),
          lastName: this.lastName()
        }
      });
      await this.customerService.fetchProfile();
      this.profileSuccess.set(true);
      setTimeout(() => this.profileSuccess.set(false), 3000);
    } catch (err: unknown) {
      this.profileError.set(handleApiError(err, 'profile update'));
    } finally {
      this.isProfileLoading.set(false);
    }
  }

  async requestEmailChange(): Promise<void> {
    const validationError = this.validateEmail();
    if (validationError) {
      this.emailError.set(validationError);
      return;
    }

    this.isEmailLoading.set(true);
    this.emailError.set('');
    this.emailSuccess.set(false);

    try {
      await this.api.invoke(requestEmailChange, {
        body: {newEmail: this.newEmail()}
      });

      this.emailSuccess.set(true);
      this.newEmail.set('');
      setTimeout(() => this.emailSuccess.set(false), 5000);
    } catch (err: unknown) {
      this.emailError.set(handleApiError(err, 'email change request'));
    } finally {
      this.isEmailLoading.set(false);
    }
  }

  async requestPasswordReset(): Promise<void> {
    this.isPasswordLoading.set(true);
    this.passwordError.set('');
    this.passwordSuccess.set(false);

    try {
      await this.api.invoke(requestPasswordResetAuthenticated, {});
      this.passwordSuccess.set(true);
      setTimeout(() => this.passwordSuccess.set(false), 5000);
    } catch (err: unknown) {
      this.passwordError.set(handleApiError(err, 'password reset request'));
    } finally {
      this.isPasswordLoading.set(false);
    }
  }

  private validateEmail(): string | null {
    if (!this.newEmail()) {
      return 'Email is required';
    }

    if (!isValidEmail(this.newEmail())) {
      return 'Please enter a valid email address';
    }

    if (this.newEmail() === this.currentEmail()) {
      return 'New email must be different from current email';
    }

    return null;
  }
}
