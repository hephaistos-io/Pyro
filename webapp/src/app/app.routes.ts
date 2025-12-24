import {Routes} from '@angular/router';
import {HomeComponent} from './pages/home/home.component';
import {PricingComponent} from './pages/pricing/pricing.component';
import {ComingSoonComponent} from './pages/coming-soon/coming-soon.component';
import {RegisterComponent} from './pages/register/register.component';
import {LoginComponent} from './pages/login/login.component';
import {DashboardComponent} from './pages/dashboard/dashboard.component';
import {DashboardLayoutComponent} from './layouts/dashboard-layout/dashboard-layout.component';
import {ApplicationOverview} from './pages/application-overview/application-overview';
import {authGuard} from './guards/auth.guard';
import {profileResolver} from './guards/profile.resolver';

export const routes: Routes = [
  {path: '', component: HomeComponent},
  {path: 'pricing', component: PricingComponent},
  {path: 'coming-soon', component: ComingSoonComponent},
  {path: 'register', component: RegisterComponent},
  {path: 'login', component: LoginComponent},
  {
    path: 'dashboard',
    component: DashboardLayoutComponent,
    canActivate: [authGuard],
    resolve: {profile: profileResolver},
    data: {isDashboard: true},
    children: [
      {path: '', component: DashboardComponent},
      {path: 'application/:id', component: ApplicationOverview}
    ]
  },
  {path: '**', redirectTo: ''}
];
