import {ChangeDetectorRef, Component, inject, OnInit} from '@angular/core';
import {FormsModule} from '@angular/forms';
import {Router, RouterLink} from '@angular/router';
import {Api} from '../../api/generated/api';
import {register} from '../../api/generated/functions';
import {UserRegistrationRequest} from '../../api/generated/models';
import zxcvbn from 'zxcvbn';
import {handleApiError} from '../../utils/error-handler.util';
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
  submitted = false;
  error = '';
  isLoading = false;
  private api = inject(Api);
  private router = inject(Router);
  private cdr = inject(ChangeDetectorRef);
  private authService = inject(AuthService);

  ngOnInit(): void {
    // Redirect to dashboard if already authenticated
    if (this.authService.isAuthenticated()) {
      this.router.navigateByUrl('/dashboard');
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
      await this.router.navigate(['/login']);
    } catch (err: any) {
      // Handle error
      this.submitted = false;
      this.isLoading = false;
      this.error = handleApiError(err, 'registration');
      //required, as otherwise it doesn't see the change to the message and it wont be displayed.
      this.cdr.detectChanges();
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
}
