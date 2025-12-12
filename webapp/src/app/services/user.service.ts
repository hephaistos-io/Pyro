import {computed, inject, Injectable, signal} from '@angular/core';
import {Api} from '../api/generated/api';
import {getCompanyForCurrentUser, profile} from '../api/generated/functions';
import {CompanyResponse, UserResponse} from '../api/generated/models';

@Injectable({providedIn: 'root'})
export class UserService {
  userProfile = signal<UserResponse | null>(null);
  userCompany = signal<CompanyResponse | null>(null);
  isProfileLoading = signal(false);
  profileError = signal<string | null>(null);

  hasCompany = computed(() => !!this.userProfile()?.companyId);

  private api = inject(Api);

  async fetchProfile(): Promise<UserResponse | null> {
    this.isProfileLoading.set(true);
    this.profileError.set(null);

    try {
      const response = await this.api.invoke(profile);
      this.userProfile.set(response);

      // Fetch company if user has one
      if (response.companyId) {
        await this.fetchCompany();
      }

      return response;
    } catch {
      this.profileError.set('Failed to load profile');
      return null;
    } finally {
      this.isProfileLoading.set(false);
    }
  }

  async fetchCompany(): Promise<CompanyResponse | null> {
    try {
      const response = await this.api.invoke(getCompanyForCurrentUser);
      this.userCompany.set(response);
      return response;
    } catch {
      return null;
    }
  }

  clearProfile(): void {
    this.userProfile.set(null);
    this.userCompany.set(null);
    this.profileError.set(null);
  }
}
