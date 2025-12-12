import {computed, inject, Injectable, signal} from '@angular/core';
import {Api} from '../api/generated/api';
import {getCompanyForCurrentCustomer} from '../api/generated/fn/company/get-company-for-current-customer';
import {profile} from '../api/generated/fn/v-1/profile';
import {CompanyResponse, CustomerResponse} from '../api/generated/models';

@Injectable({providedIn: 'root'})
export class CustomerService {
  customerProfile = signal<CustomerResponse | null>(null);
  customerCompany = signal<CompanyResponse | null>(null);
  isProfileLoading = signal(false);
  profileError = signal<string | null>(null);

  hasCompany = computed(() => !!this.customerProfile()?.companyId);

  private api = inject(Api);

  async fetchProfile(): Promise<CustomerResponse | null> {
    this.isProfileLoading.set(true);
    this.profileError.set(null);

    try {
      const response = await this.api.invoke(profile);
      this.customerProfile.set(response);

      // Fetch company if customer has one
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
      const response = await this.api.invoke(getCompanyForCurrentCustomer);
      this.customerCompany.set(response);
      return response;
    } catch {
      return null;
    }
  }

  clearProfile(): void {
    this.customerProfile.set(null);
    this.customerCompany.set(null);
    this.profileError.set(null);
  }
}
