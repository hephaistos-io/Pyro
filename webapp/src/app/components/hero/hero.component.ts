import {Component, inject} from '@angular/core';
import {RouterLink} from '@angular/router';
import {ThemeService} from '../../services/theme.service';

@Component({
  selector: 'app-hero',
  standalone: true,
    imports: [RouterLink],
  templateUrl: './hero.component.html',
  styleUrl: './hero.component.scss'
})
export class HeroComponent {
  // Feature states
  quality: 'FHD' | '4K' = 'FHD';
  devices = 4;
  offlinePlay = false;
  private themeService = inject(ThemeService);
  design: 'light' | 'dark' = this.themeService.isDarkMode() ? 'dark' : 'light';

  setQuality(value: 'FHD' | '4K'): void {
    this.quality = value;
  }

  updateDevices(event: Event): void {
    this.devices = +(event.target as HTMLInputElement).value;
  }

  toggleOfflinePlay(): void {
    this.offlinePlay = !this.offlinePlay;
  }

  setDesign(value: 'light' | 'dark'): void {
    this.design = value;
    this.themeService.toggleTheme(value === 'dark');
  }
}
