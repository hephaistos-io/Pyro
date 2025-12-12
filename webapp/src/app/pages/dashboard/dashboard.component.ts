import {Component, computed, inject, OnInit, signal} from '@angular/core';
import {Router} from '@angular/router';
import {OnboardingOverlayComponent} from '../../components/onboarding-overlay/onboarding-overlay.component';
import {CompanyCreationFormComponent} from '../../components/company-creation-form/company-creation-form.component';
import {
  ApplicationCreationFormComponent
} from '../../components/application-creation-form/application-creation-form.component';
import {AppCardComponent} from '../../components/app-card/app-card.component';
import {CustomerService} from '../../services/customer.service';
import {Api} from '../../api/generated/api';
import {getApplications} from '../../api/generated/fn/application/get-applications';
import {ApplicationResponse, CompanyResponse} from '../../api/generated/models';

const SUCCESS_MESSAGE_DURATION_MS = 2000;

@Component({
  selector: 'app-dashboard',
  standalone: true,
  imports: [OnboardingOverlayComponent, CompanyCreationFormComponent, ApplicationCreationFormComponent, AppCardComponent],
  templateUrl: './dashboard.component.html',
  styleUrl: './dashboard.component.scss'
})
export class DashboardComponent implements OnInit {
  private customerService = inject(CustomerService);
  private router = inject(Router);
  showApplicationCreation = signal(false);

  showSuccessMessage = signal(false);
  applications = signal<ApplicationResponse[]>([]);
  private api = inject(Api);

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

  private async fetchApplications(): Promise<void> {
    try {
      const response = await this.api.invoke(getApplications, {});
      this.applications.set(response);
    } catch {
      this.applications.set([]);
    }
  }
}
