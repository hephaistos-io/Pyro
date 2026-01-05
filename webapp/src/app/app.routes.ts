import {Routes} from '@angular/router';
import {HomeComponent} from './pages/home/home.component';
import {PricingComponent} from './pages/pricing/pricing.component';
import {ComingSoonComponent} from './pages/coming-soon/coming-soon.component';
import {RegisterComponent} from './pages/register/register.component';
import {LoginComponent} from './pages/login/login.component';
import {ForgotPasswordComponent} from './pages/forgot-password/forgot-password.component';
import {ResetPasswordComponent} from './pages/reset-password/reset-password.component';
import {VerifyEmailComponent} from './pages/verify-email/verify-email.component';
import {VerifyRegistrationComponent} from './pages/verify-registration/verify-registration.component';
import {DashboardComponent} from './pages/dashboard/dashboard.component';
import {DashboardLayoutComponent} from './layouts/dashboard-layout/dashboard-layout.component';
import {ApplicationOverview} from './pages/application-overview/application-overview';
import {ProfileSettingsComponent} from './pages/profile-settings/profile-settings.component';
import {CheckoutComponent} from './pages/checkout/checkout.component';
import {CheckoutSuccessComponent} from './pages/checkout-success/checkout-success.component';
import {authGuard} from './guards/auth.guard';
import {profileResolver} from './guards/profile.resolver';

export const routes: Routes = [
  {path: '', component: HomeComponent},
  {path: 'pricing', component: PricingComponent},
  {path: 'coming-soon', component: ComingSoonComponent},
  {path: 'register', component: RegisterComponent},
  {path: 'login', component: LoginComponent},
  {path: 'forgot-password', component: ForgotPasswordComponent},
  {path: 'reset-password', component: ResetPasswordComponent},
  {path: 'verify-email', component: VerifyEmailComponent},
  {path: 'verify-registration', component: VerifyRegistrationComponent},
  {
    path: 'dashboard',
    component: DashboardLayoutComponent,
    canActivate: [authGuard],
    resolve: {profile: profileResolver},
    data: {isDashboard: true},
    children: [
      {path: '', component: DashboardComponent},
      {path: 'users', component: DashboardComponent, data: {tab: 'users'}},
      {path: 'billing', component: DashboardComponent, data: {tab: 'billing'}},
      {path: 'application/:id', component: ApplicationOverview},
      {path: 'profile', component: ProfileSettingsComponent},
      {path: 'checkout', component: CheckoutComponent},
      {path: 'checkout/success', component: CheckoutSuccessComponent}
    ]
  },
  {path: '**', redirectTo: ''}
];
