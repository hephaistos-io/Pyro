import {Component, inject, OnInit, signal} from '@angular/core';
import {ActivatedRoute, Router, RouterLink} from '@angular/router';
import {Api} from '../../api/generated/api';
import {validateToken1} from '../../api/generated/fn/email-verification/validate-token-1';
import {confirmEmailChange} from '../../api/generated/fn/email-verification/confirm-email-change';
import {handleApiError} from '../../utils/error-handler.util';

@Component({
  selector: 'app-verify-email',
  standalone: true,
  imports: [RouterLink],
  templateUrl: './verify-email.component.html',
  styleUrl: './verify-email.component.scss'
})
export class VerifyEmailComponent implements OnInit {
  error = signal('');
  isValidating = signal(true);
  isTokenValid = signal(false);
  isConfirming = signal(false);
  isConfirmed = signal(false);
  newEmail = signal('');

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
      const response = await this.api.invoke(validateToken1, {token: this.token});
      this.isTokenValid.set(response['valid'] === true);
      if (response['newEmail']) {
        this.newEmail.set(response['newEmail'] as string);
      }
    } catch {
      this.isTokenValid.set(false);
    } finally {
      this.isValidating.set(false);
    }
  }

  async confirmChange(): Promise<void> {
    this.isConfirming.set(true);
    this.error.set('');

    try {
      await this.api.invoke(confirmEmailChange, {token: this.token});
      this.isConfirmed.set(true);
    } catch (err: unknown) {
      this.isConfirming.set(false);
      this.error.set(handleApiError(err, 'email verification'));
    }
  }

  goToLogin(): void {
    this.router.navigateByUrl('/login');
  }
}
