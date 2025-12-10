import {Component, signal} from '@angular/core';
import {takeUntilDestroyed} from '@angular/core/rxjs-interop';
import {RouterLink, RouterOutlet} from '@angular/router';
import {fromEvent} from 'rxjs';

@Component({
  selector: 'app-dashboard-layout',
  standalone: true,
  imports: [RouterOutlet, RouterLink],
  templateUrl: './dashboard-layout.component.html',
  styleUrl: './dashboard-layout.component.scss'
})
export class DashboardLayoutComponent {
  isMobile = signal(window.innerWidth <= 768);

  constructor() {
    fromEvent(window, 'resize')
      .pipe(takeUntilDestroyed())
      .subscribe(() => {
        this.isMobile.set(window.innerWidth <= 768);
      });
  }
}
