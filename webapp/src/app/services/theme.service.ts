import {Injectable, signal} from '@angular/core';

@Injectable({
  providedIn: 'root'
})
export class ThemeService {
  isDarkMode = signal(this.getInitialTheme());
  private readonly STORAGE_KEY = 'theme_preference';

  constructor() {
    if (this.isDarkMode()) {
      document.body.classList.add('dark-mode');
    }
  }

  toggleTheme(isDark: boolean): void {
    this.isDarkMode.set(isDark);
    localStorage.setItem(this.STORAGE_KEY, isDark ? 'dark' : 'light');

    if (isDark) {
      document.body.classList.add('dark-mode');
    } else {
      document.body.classList.remove('dark-mode');
    }
  }

  private getInitialTheme(): boolean {
    const stored = localStorage.getItem(this.STORAGE_KEY);
    if (stored) {
      return stored === 'dark';
    }
    return window.matchMedia('(prefers-color-scheme: dark)').matches;
  }
}
