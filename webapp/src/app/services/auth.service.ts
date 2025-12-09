import {Injectable, signal} from '@angular/core';

@Injectable({providedIn: 'root'})
export class AuthService {
  isAuthenticated = signal(false);
  userEmail = signal<string | null>(null);
  private readonly TOKEN_KEY = 'auth_token';
  private readonly EMAIL_KEY = 'user_email';

  constructor() {
    this.initializeAuth();
  }

  setAuthToken(token: string, email: string): void {
    localStorage.setItem(this.TOKEN_KEY, token);
    localStorage.setItem(this.EMAIL_KEY, email);
    this.isAuthenticated.set(true);
    this.userEmail.set(email);
  }

  getToken(): string | null {
    return localStorage.getItem(this.TOKEN_KEY);
  }

  logout(): void {
    localStorage.removeItem(this.TOKEN_KEY);
    localStorage.removeItem(this.EMAIL_KEY);
    this.isAuthenticated.set(false);
    this.userEmail.set(null);
  }

  private initializeAuth(): void {
    const token = localStorage.getItem(this.TOKEN_KEY);
    const email = localStorage.getItem(this.EMAIL_KEY);

    if (token && email) {
      this.isAuthenticated.set(true);
      this.userEmail.set(email);
    }
  }
}
