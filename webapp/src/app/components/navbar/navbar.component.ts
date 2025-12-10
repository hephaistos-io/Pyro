import {Component, inject} from '@angular/core';
import {Router, RouterLink} from '@angular/router';
import {toSignal} from '@angular/core/rxjs-interop';
import {map} from 'rxjs';
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

  isDashboard = toSignal(
    this.router.events.pipe(
      map(() => this.router.url === '/dashboard')
    ),
    {initialValue: this.router.url === '/dashboard'}
  );

  onLogout(): void {
    this.authService.logout();
    this.router.navigate(['/']);
  }
}
