import {ChangeDetectorRef, Component, inject, OnInit} from '@angular/core';
import {FormsModule} from '@angular/forms';
import {ActivatedRoute, Router, RouterLink} from '@angular/router';
import {Api} from '../../api/generated/api';
import {authenticate} from '../../api/generated/functions';
import {UserAuthenticationRequest} from '../../api/generated/models';
import {AuthService} from '../../services/auth.service';
import {handleApiError} from '../../utils/error-handler.util';

@Component({
  selector: 'app-login',
  standalone: true,
  imports: [FormsModule, RouterLink],
  templateUrl: './login.component.html',
  styleUrl: './login.component.scss'
})
export class LoginComponent implements OnInit {
  email = '';
  password = '';
  error = '';
  isLoading = false;
  private returnUrl = '/dashboard';

  private api = inject(Api);
  private authService = inject(AuthService);
  private router = inject(Router);
  private route = inject(ActivatedRoute);
  private cdr = inject(ChangeDetectorRef);

  ngOnInit(): void {
    // Get return URL from route parameters or default to dashboard
    this.returnUrl = this.route.snapshot.queryParams['returnUrl'] || '/dashboard';
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
      const authRequest: UserAuthenticationRequest = {
        email: this.email,
        password: this.password
      };

      const response = await this.api.invoke(authenticate, {body: authRequest});

      // Save token and redirect
      if (response.token && response.email) {
        this.authService.setAuthToken(response.token, response.email);
        await this.router.navigateByUrl(this.returnUrl);
      } else {
        this.error = 'Login failed. Please try again';
        this.isLoading = false;
        this.cdr.detectChanges();
      }
    } catch (err: any) {
      // Handle error
      this.isLoading = false;
      this.error = handleApiError(err, 'login');
      //required, as otherwise it doesn't see the change to the message and it wont be displayed.
      this.cdr.detectChanges();
    }
  }

  private validateForm(): string | null {
    if (!this.email || !this.password) {
      return 'Email and password are required';
    }

    if (!this.isValidEmail(this.email)) {
      return 'Please enter a valid email address';
    }

    return null;
  }

  private isValidEmail(email: string): boolean {
    const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
    return emailRegex.test(email);
  }
}
