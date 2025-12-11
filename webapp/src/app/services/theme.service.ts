import {Injectable, signal} from '@angular/core';

@Injectable({
  providedIn: 'root'
})
export class ThemeService {
  isDarkMode = signal(this.getInitialTheme());
  private readonly STORAGE_KEY = 'theme_preference';

  constructor() {
    this.applyTheme(this.isDarkMode());
  }

  toggleTheme(isDark: boolean): void {
    this.isDarkMode.set(isDark);
    localStorage.setItem(this.STORAGE_KEY, isDark ? 'dark' : 'light');
    this.applyTheme(isDark);
  }

  private applyTheme(isDark: boolean): void {
    // Apply to both html (for early load script) and body (for Angular)
    const elements = [document.documentElement, document.body];
    elements.forEach(el => {
      if (isDark) {
        el.classList.add('dark-mode');
      } else {
        el.classList.remove('dark-mode');
      }
    });
  }

  private getInitialTheme(): boolean {
    // Use hardcoded key since this is called during signal init (before STORAGE_KEY is defined)
    const stored = localStorage.getItem('theme_preference');
    if (stored) {
      return stored === 'dark';
    }
    return window.matchMedia('(prefers-color-scheme: dark)').matches;
  }
}
