import {Component, computed, inject, signal} from '@angular/core';
import {Router, RouterLink} from '@angular/router';
import {takeUntilDestroyed, toSignal} from '@angular/core/rxjs-interop';
import {fromEvent, map} from 'rxjs';
import {AuthService} from '../../services/auth.service';
import {PricingStateService} from '../../services/pricing-state.service';

@Component({
  selector: 'app-navbar',
  standalone: true,
  imports: [RouterLink],
  templateUrl: './navbar.component.html',
  styleUrl: './navbar.component.scss'
})
export class NavbarComponent {
  authService = inject(AuthService);
  pricingState = inject(PricingStateService);
  private router = inject(Router);

  isMobile = signal(window.innerWidth <= 768);

  pageTitle = computed(() => {
    if (this.isApplicationPage()) {
      return 'Application';
    }
    return 'Dashboard';
  });
  private currentUrl = toSignal(
    this.router.events.pipe(
      map(() => this.router.url)
    ),
    {initialValue: this.router.url}
  );
  isDashboard = computed(() => this.currentUrl()?.startsWith('/dashboard') ?? false);
  isApplicationPage = computed(() =>
    this.currentUrl()?.includes('/dashboard/application/') ?? false
  );

  constructor() {
    fromEvent(window, 'resize')
      .pipe(takeUntilDestroyed())
      .subscribe(() => {
        this.isMobile.set(window.innerWidth <= 768);
      });
  }

  onLogout(): void {
    this.authService.logout();
    this.router.navigate(['/']);
  }
}
