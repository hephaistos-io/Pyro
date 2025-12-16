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
import {ApplicationResponse, CompanyResponse, InviteCreationResponse} from '../../api/generated/models';
import {User, UsersService} from '../../services/users.service';
import {CommonModule} from '@angular/common';

const SUCCESS_MESSAGE_DURATION_MS = 2000;
const COST_PER_ADDITIONAL_APP = 29; // $29/month per additional app

@Component({
  selector: 'app-dashboard',
  standalone: true,
  imports: [CommonModule, OnboardingOverlayComponent, FormOverlayComponent, CompanyCreationFormComponent, ApplicationCreationFormComponent, UserCreationFormComponent, UserEditFormComponent, AppCardComponent, UsersTableComponent],
  templateUrl: './dashboard.component.html',
  styleUrl: './dashboard.component.scss'
})
export class DashboardComponent implements OnInit {
  private customerService = inject(CustomerService);
  userToDelete = signal<User | null>(null);
  private router = inject(Router);
  showApplicationCreation = signal(false);
  showUserCreation = signal(false);
  userToEdit = signal<User | null>(null);
  inviteToDelete = signal<User | null>(null);
  regeneratedInvite = signal<InviteCreationResponse | null>(null);
  isRegenerating = signal(false);
  urlCopied = signal(false);
  private usersService = inject(UsersService);

  showSuccessMessage = signal(false);
  applications = signal<ApplicationResponse[]>([]);
  private api = inject(Api);
  activeTab = signal<'applications' | 'users'>('applications');

  // Cost overview computed values - uses pricingTier from API
  applicationCosts = computed(() => {
    return this.applications().map(app => {
      const isFree = app.pricingTier === 'FREE';
      return {
        id: app.id,
        name: app.name,
        cost: isFree ? 0 : COST_PER_ADDITIONAL_APP,
        isFree
      };
    });
  });

  totalMonthlyCost = computed(() => {
    const paidApps = this.applications().filter(app => app.pricingTier === 'PAID');
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

  onApplicationClick(application: ApplicationResponse): void {
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

  onDeleteUserClick(user: User): void {
    this.userToDelete.set(user);
  }

  confirmDeleteUser(): void {
    const user = this.userToDelete();
    if (!user) return;

    this.usersService.removeUser(user.id);
    this.userToDelete.set(null);
  }

  cancelDeleteUser(): void {
    this.userToDelete.set(null);
  }

  onDeleteInviteClick(user: User): void {
    this.inviteToDelete.set(user);
  }

  async confirmDeleteInvite(): Promise<void> {
    const invite = this.inviteToDelete();
    if (!invite) return;

    await this.usersService.deleteInvite(invite.id);
    this.inviteToDelete.set(null);
  }

  cancelDeleteInvite(): void {
    this.inviteToDelete.set(null);
  }

  async onRegenerateInviteClick(user: User): Promise<void> {
    this.isRegenerating.set(true);
    try {
      const response = await this.usersService.regenerateInvite(user.id);
      this.regeneratedInvite.set(response);
    } finally {
      this.isRegenerating.set(false);
    }
  }

  closeRegenerateOverlay(): void {
    this.regeneratedInvite.set(null);
    this.urlCopied.set(false);
  }

  async copyRegeneratedUrl(): Promise<void> {
    const invite = this.regeneratedInvite();
    if (!invite?.inviteUrl) return;

    await navigator.clipboard.writeText(invite.inviteUrl);
    this.urlCopied.set(true);
    setTimeout(() => this.urlCopied.set(false), 2000);
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
