import {Component, inject, OnInit, signal} from '@angular/core';
import {FormsModule} from '@angular/forms';
import {ActivatedRoute, Router, RouterLink} from '@angular/router';
import {Api} from '../../api/generated/api';
import {authenticate} from '../../api/generated/fn/authorization/authenticate';
import {CustomerAuthenticationRequest} from '../../api/generated/models';
import {AuthService} from '../../services/auth.service';
import {handleApiError} from '../../utils/error-handler.util';
import {isValidEmail} from '../../utils/validators.util';

@Component({
  selector: 'app-login',
  standalone: true,
  imports: [FormsModule, RouterLink],
  templateUrl: './login.component.html',
  styleUrl: './login.component.scss'
})
export class LoginComponent implements OnInit {
  email = signal('');
  password = signal('');
  error = signal('');
  isLoading = signal(false);
  private returnUrl = '/dashboard';

  private api = inject(Api);
  private authService = inject(AuthService);
  private router = inject(Router);
  private route = inject(ActivatedRoute);

  ngOnInit(): void {
    // Redirect to dashboard if already authenticated
    if (this.authService.isAuthenticated()) {
      this.router.navigateByUrl('/dashboard');
      return;
    }

    // Get return URL from route parameters or default to dashboard
    this.returnUrl = this.route.snapshot.queryParams['returnUrl'] || '/dashboard';
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
      // Call API
      const authRequest: CustomerAuthenticationRequest = {
        email: this.email(),
        password: this.password()
      };

      const response = await this.api.invoke(authenticate, {body: authRequest});

      // Save token and redirect
      if (response.token && response.email) {
        this.authService.setAuthToken(response.token, response.email);
        await this.router.navigateByUrl(this.returnUrl);
      } else {
        this.error.set('Login failed. Please try again');
        this.isLoading.set(false);
      }
    } catch (err: unknown) {
      // Handle error
      this.isLoading.set(false);
      this.error.set(handleApiError(err, 'login'));
    }
  }

  private validateForm(): string | null {
    if (!this.email() || !this.password()) {
      return 'Email and password are required';
    }

    if (!isValidEmail(this.email())) {
      return 'Please enter a valid email address';
    }

    return null;
  }
}
