import {Component, inject, OnInit, signal} from '@angular/core';
import {ActivatedRoute, Router, RouterLink} from '@angular/router';
import {handleApiError} from '../../utils/error-handler.util';
import {Api} from '../../api/generated/api';
import {verifyRegistration} from '../../api/generated/fn/registration-verification/verify-registration';

@Component({
  selector: 'app-verify-registration',
  standalone: true,
  imports: [RouterLink],
  templateUrl: './verify-registration.component.html',
  styleUrl: './verify-registration.component.scss'
})
export class VerifyRegistrationComponent implements OnInit {
  isVerifying = signal(true);
  isVerified = signal(false);
  error = signal('');

  private route = inject(ActivatedRoute);
  private router = inject(Router);
  private api = inject(Api);

  async ngOnInit(): Promise<void> {
    const token = this.route.snapshot.queryParams['token'] || '';

    if (!token) {
      this.isVerifying.set(false);
      this.error.set('No verification token provided');
      return;
    }

    try {
      await this.api.invoke(verifyRegistration, {token});
      this.isVerified.set(true);
    } catch (err: unknown) {
      this.error.set(handleApiError(err, 'email verification'));
    } finally {
      this.isVerifying.set(false);
    }
  }

  goToLogin(): void {
    this.router.navigateByUrl('/login');
  }
}
