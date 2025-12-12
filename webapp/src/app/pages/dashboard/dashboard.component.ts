import {Component, computed, inject, OnInit, signal} from '@angular/core';
import {Router} from '@angular/router';
import {OnboardingOverlayComponent} from '../../components/onboarding-overlay/onboarding-overlay.component';
import {CompanyCreationFormComponent} from '../../components/company-creation-form/company-creation-form.component';
import {
  ApplicationCreationFormComponent
} from '../../components/application-creation-form/application-creation-form.component';
import {AppCardComponent} from '../../components/app-card/app-card.component';
import {UserService} from '../../services/user.service';
import {Api} from '../../api/generated/api';
import {getApplications} from '../../api/generated/functions';
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
  private userService = inject(UserService);
  private router = inject(Router);
  showApplicationCreation = signal(false);

  showSuccessMessage = signal(false);
  applications = signal<ApplicationResponse[]>([]);
  private api = inject(Api);

  showCompanyOnboarding = computed(() =>
    !this.userService.isProfileLoading() && !this.userService.hasCompany()
  );

  greeting = computed(() => {
    const profile = this.userService.userProfile();
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

  companyName = computed(() => this.userService.userCompany()?.name ?? '');

  async ngOnInit(): Promise<void> {
    await this.userService.fetchProfile();
    if (this.userService.hasCompany()) {
      await this.fetchApplications();
    }
  }

  async onCompanyCreated(company: CompanyResponse): Promise<void> {
    this.showSuccessMessage.set(true);

    // Show success message briefly, then refresh profile
    await new Promise(resolve => setTimeout(resolve, SUCCESS_MESSAGE_DURATION_MS));
    await this.userService.fetchProfile();
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
