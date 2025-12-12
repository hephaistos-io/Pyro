import {inject, Injectable, signal} from '@angular/core';
import {CustomerService} from './customer.service';

@Injectable({providedIn: 'root'})
export class AuthService {
  isAuthenticated = signal(false);
  customerEmail = signal<string | null>(null);
  private readonly TOKEN_KEY = 'auth_token';
  private readonly EMAIL_KEY = 'customer_email';

  private customerService = inject(CustomerService);

  constructor() {
    this.initializeAuth();
  }

  setAuthToken(token: string, email: string): void {
    localStorage.setItem(this.TOKEN_KEY, token);
    localStorage.setItem(this.EMAIL_KEY, email);
    this.isAuthenticated.set(true);
    this.customerEmail.set(email);
  }

  getToken(): string | null {
    return localStorage.getItem(this.TOKEN_KEY);
  }

  logout(): void {
    localStorage.removeItem(this.TOKEN_KEY);
    localStorage.removeItem(this.EMAIL_KEY);
    this.isAuthenticated.set(false);
    this.customerEmail.set(null);
    this.customerService.clearProfile();
  }

  private initializeAuth(): void {
    const token = localStorage.getItem(this.TOKEN_KEY);
    const email = localStorage.getItem(this.EMAIL_KEY);

    if (token && email) {
      this.isAuthenticated.set(true);
      this.customerEmail.set(email);
    }
  }
}
