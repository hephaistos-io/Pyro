import {Component, computed, inject, OnInit, signal} from '@angular/core';
import {OnboardingOverlayComponent} from '../../components/onboarding-overlay/onboarding-overlay.component';
import {CompanyCreationFormComponent} from '../../components/company-creation-form/company-creation-form.component';
import {AppCardComponent} from '../../components/app-card/app-card.component';
import {UserService} from '../../services/user.service';
import {CompanyResponse} from '../../api/generated/models';

@Component({
  selector: 'app-dashboard',
  standalone: true,
  imports: [OnboardingOverlayComponent, CompanyCreationFormComponent, AppCardComponent],
  templateUrl: './dashboard.component.html',
  styleUrl: './dashboard.component.scss'
})
export class DashboardComponent implements OnInit {
  showSuccessMessage = signal(false);
  private userService = inject(UserService);
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
  }

  async onCompanyCreated(company: CompanyResponse): Promise<void> {
    this.showSuccessMessage.set(true);

    // Show success message briefly, then refresh profile
    await new Promise(resolve => setTimeout(resolve, 2000));
    await this.userService.fetchProfile();
    this.showSuccessMessage.set(false);
  }
}
