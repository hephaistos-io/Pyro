import {Component, inject, OnInit, signal} from '@angular/core';
import {FormsModule} from '@angular/forms';
import {ActivatedRoute, Router, RouterLink} from '@angular/router';
import {Api} from '../../api/generated/api';
import {register} from '../../api/generated/fn/authorization/register';
import {validateInvite} from '../../api/generated/fn/invite/validate-invite';
import {CustomerRegistrationRequest, InviteValidationResponse} from '../../api/generated/models';
import zxcvbn from 'zxcvbn';
import {handleApiError} from '../../utils/error-handler.util';
import {isValidEmail} from '../../utils/validators.util';
import {AuthService} from '../../services/auth.service';

@Component({
  selector: 'app-register',
  standalone: true,
  imports: [FormsModule, RouterLink],
  templateUrl: './register.component.html',
  styleUrl: './register.component.scss'
})
export class RegisterComponent implements OnInit {
  email = '';
  firstName = '';
  lastName = '';
  password = '';
  confirmPassword = '';
  submitted = signal(false);
  error = signal('');
  isLoading = signal(false);

  // Invite-related state
  inviteToken = signal<string | null>(null);
  inviteData = signal<InviteValidationResponse | null>(null);
  isValidatingInvite = signal(false);
  inviteError = signal<string | null>(null);

  private api = inject(Api);
  private router = inject(Router);
  private route = inject(ActivatedRoute);
  private authService = inject(AuthService);

  async ngOnInit(): Promise<void> {
    // Redirect to dashboard if already authenticated
    if (this.authService.isAuthenticated()) {
      this.router.navigateByUrl('/dashboard');
      return;
    }

    // Check for invite token in query params
    const token = this.route.snapshot.queryParamMap.get('invite');
    if (token) {
      this.inviteToken.set(token);
      await this.validateInviteToken(token);
    }
  }

  isInviteFlow(): boolean {
    return this.inviteToken() !== null && this.inviteData() !== null;
  }

  async onSubmit(): Promise<void> {
    // Validate form
    const validationError = this.validateForm();
    if (validationError) {
      this.error.set(validationError);
      return;
    }

    // Set loading state
    this.isLoading.set(true);
    this.error.set('');
    try {
      // Build registration request
      const registrationRequest: CustomerRegistrationRequest = {
        firstName: this.firstName,
        lastName: this.lastName,
        password: this.password
      };

      // Include invite token or email depending on flow
      if (this.isInviteFlow()) {
        registrationRequest.inviteToken = this.inviteToken()!;
      } else {
        registrationRequest.email = this.email;
      }

      await this.api.invoke(register, {body: registrationRequest});

      // Handle success
      this.submitted.set(true);
      this.isLoading.set(false);
      await this.router.navigate(['/login']);
    } catch (err: unknown) {
      // Handle error
      this.submitted.set(false);
      this.isLoading.set(false);
      this.error.set(handleApiError(err, 'registration'));
    }
  }

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

  private async validateInviteToken(token: string): Promise<void> {
    this.isValidatingInvite.set(true);
    this.inviteError.set(null);

    try {
      const response = await this.api.invoke(validateInvite, {token});
      if (response.valid) {
        this.inviteData.set(response);
        // Pre-fill email from invite
        this.email = response.email ?? '';
      } else {
        // Handle invalid invite
        const reason = response.reason;
        if (reason === 'NOT_FOUND') {
          this.inviteError.set('This invitation link is invalid.');
        } else if (reason === 'EXPIRED') {
          this.inviteError.set('This invitation has expired. Please ask for a new invite.');
        } else if (reason === 'ALREADY_USED') {
          this.inviteError.set('This invitation has already been used.');
        } else {
          this.inviteError.set('This invitation is not valid.');
        }
      }
    } catch {
      this.inviteError.set('Failed to validate invitation. Please try again.');
    } finally {
      this.isValidatingInvite.set(false);
    }
  }

  private validateForm(): string | null {
    // For invite flow, we only need name and password
    if (this.isInviteFlow()) {
      if (!this.firstName || !this.lastName || !this.password || !this.confirmPassword) {
        return 'All fields are required';
      }
    } else {
      if (!this.firstName || !this.lastName || !this.email || !this.password || !this.confirmPassword) {
        return 'All fields are required';
      }

      if (!isValidEmail(this.email)) {
        return 'Please enter a valid email address';
      }
    }

    if (!this.isValidName(this.firstName)) {
      return 'First name must be 2-50 characters and contain only letters';
    }

    if (!this.isValidName(this.lastName)) {
      return 'Last name must be 2-50 characters and contain only letters';
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
}
