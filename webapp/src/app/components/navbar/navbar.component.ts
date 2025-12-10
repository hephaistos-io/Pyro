import {Component, inject, signal} from '@angular/core';
import {Router, RouterLink} from '@angular/router';
import {takeUntilDestroyed, toSignal} from '@angular/core/rxjs-interop';
import {fromEvent, map} from 'rxjs';
import {AuthService} from '../../services/auth.service';

@Component({
  selector: 'app-navbar',
  standalone: true,
  imports: [RouterLink],
  templateUrl: './navbar.component.html',
  styleUrl: './navbar.component.scss'
})
export class NavbarComponent {
  authService = inject(AuthService);
  private router = inject(Router);

  isMobile = signal(window.innerWidth <= 768);

  isDashboard = toSignal(
    this.router.events.pipe(
      map(() => this.router.url.startsWith('/dashboard'))
    ),
    {initialValue: this.router.url.startsWith('/dashboard')}
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
