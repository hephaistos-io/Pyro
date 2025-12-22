import {Component, computed, inject, OnInit, signal} from '@angular/core';
import {Router} from '@angular/router';
import {OnboardingOverlayComponent} from '../../components/onboarding-overlay/onboarding-overlay.component';
import {FormOverlayComponent} from '../../components/form-overlay/form-overlay.component';
import {CompanyCreationFormComponent} from '../../components/company-creation-form/company-creation-form.component';
import {
  ApplicationCreationFormComponent
} from '../../components/application-creation-form/application-creation-form.component';
import {UserCreationFormComponent} from '../../components/user-creation-form/user-creation-form.component';
import {UserEditFormComponent} from '../../components/user-edit-form/user-edit-form.component';
import {AppCardComponent} from '../../components/app-card/app-card.component';
import {UsersTableComponent} from '../../components/users-table/users-table.component';
import {CustomerService} from '../../services/customer.service';
import {Api} from '../../api/generated/api';
import {getApplications} from '../../api/generated/fn/application/get-applications';
import {ApplicationListResponse, ApplicationResponse, CompanyResponse, CustomerRole} from '../../api/generated/models';
import {User, UsersService} from '../../services/users.service';
import {CommonModule} from '@angular/common';
import {HasRoleDirective} from '../../directives/has-role.directive';

const SUCCESS_MESSAGE_DURATION_MS = 2000;
const COST_PER_ADDITIONAL_APP = 29; // $29/month per additional app

// Type-safe PricingTier constants extracted from API models
const PricingTier = {
  FREE: 'FREE',
  BASIC: 'BASIC',
  STANDARD: 'STANDARD',
  PRO: 'PRO',
  BUSINESS: 'BUSINESS'
} as const;

@Component({
  selector: 'app-dashboard',
  standalone: true,
  imports: [CommonModule, OnboardingOverlayComponent, FormOverlayComponent, CompanyCreationFormComponent, ApplicationCreationFormComponent, UserCreationFormComponent, UserEditFormComponent, AppCardComponent, UsersTableComponent, HasRoleDirective],
  templateUrl: './dashboard.component.html',
  styleUrl: './dashboard.component.scss'
})
export class DashboardComponent implements OnInit {
  protected readonly CustomerRole = CustomerRole;

  private customerService = inject(CustomerService);
  private router = inject(Router);
  showApplicationCreation = signal(false);
  showUserCreation = signal(false);
  userToEdit = signal<User | null>(null);
  private usersService = inject(UsersService);

  showSuccessMessage = signal(false);
  applications = signal<ApplicationListResponse[]>([]);
  private api = inject(Api);
  activeTab = signal<'applications' | 'users'>('applications');

  // Cost overview computed values - uses pricingTier from API
  applicationCosts = computed(() => {
    return this.applications().map(app => {
      const isFree = app.pricingTier === PricingTier.FREE;
      return {
        id: app.id,
        name: app.name,
        cost: isFree ? 0 : COST_PER_ADDITIONAL_APP,
        isFree
      };
    });
  });

  totalMonthlyCost = computed(() => {
    const paidApps = this.applications().filter(app => app.pricingTier !== PricingTier.FREE);
    return paidApps.length * COST_PER_ADDITIONAL_APP;
  });

  maxAppCost = computed(() => {
    return Math.max(COST_PER_ADDITIONAL_APP, ...this.applicationCosts().map(a => a.cost));
  });

  showCompanyOnboarding = computed(() =>
    !this.customerService.isProfileLoading() && !this.customerService.hasCompany()
  );

  greeting = computed(() => {
    const profile = this.customerService.customerProfile();
    if (!profile) return '';

    const hour = new Date().getHours();
    let timeGreeting: string;

    if (hour < 12) {
      timeGreeting = 'Good morning';
    } else if (hour < 17) {
      timeGreeting = 'Good afternoon';
    } else {
      timeGreeting = 'Good evening';
    }

    const name = [profile.firstName, profile.lastName].filter(Boolean).join(' ');
    return name ? `${timeGreeting}, ${name}.` : timeGreeting;
  });

  companyName = computed(() => this.customerService.customerCompany()?.name ?? '');

  async ngOnInit(): Promise<void> {
    await this.customerService.fetchProfile();
    if (this.customerService.hasCompany()) {
      await this.fetchApplications();
    }
  }

  async onCompanyCreated(company: CompanyResponse): Promise<void> {
    this.showSuccessMessage.set(true);

    // Show success message briefly, then refresh profile
    await new Promise(resolve => setTimeout(resolve, SUCCESS_MESSAGE_DURATION_MS));
    await this.customerService.fetchProfile();
    this.showSuccessMessage.set(false);
  }

  onAddApplicationClick(): void {
    this.showApplicationCreation.set(true);
  }

  onCloseApplicationCreation(): void {
    this.showApplicationCreation.set(false);
  }

  async onApplicationCreated(application: ApplicationResponse): Promise<void> {
    this.showApplicationCreation.set(false);
    await this.fetchApplications();
  }

  onApplicationClick(application: ApplicationListResponse): void {
    this.router.navigate(['/dashboard/application', application.id], {
      state: {application}
    });
  }

  setActiveTab(tab: 'applications' | 'users'): void {
    this.activeTab.set(tab);
  }

  onAddUserClick(): void {
    this.showUserCreation.set(true);
  }

  onCloseUserCreation(): void {
    this.showUserCreation.set(false);
  }

  async onUserCreated(): Promise<void> {
    this.showUserCreation.set(false);
    await this.usersService.fetchUsers();
  }

  onEditUserClick(user: User): void {
    this.userToEdit.set(user);
  }

  onCloseUserEdit(): void {
    this.userToEdit.set(null);
  }

  onUserUpdated(user: User): void {
    this.userToEdit.set(null);
  }

  private async fetchApplications(): Promise<void> {
    try {
      const response = await this.api.invoke(getApplications, {});
      this.applications.set(response);
    } catch {
      this.applications.set([]);
    }
  }
}
