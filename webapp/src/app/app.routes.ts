import {Routes} from '@angular/router';
import {HomeComponent} from './pages/home/home.component';
import {PricingComponent} from './pages/pricing/pricing.component';
import {RegisterComponent} from './pages/register/register.component';
import {LoginComponent} from './pages/login/login.component';
import {DashboardComponent} from './pages/dashboard/dashboard.component';
import {authGuard} from './guards/auth.guard';

export const routes: Routes = [
  {path: '', component: HomeComponent},
  {path: 'pricing', component: PricingComponent},
  {path: 'register', component: RegisterComponent},
  {path: 'login', component: LoginComponent},
  {path: 'dashboard', component: DashboardComponent, canActivate: [authGuard]},
  {path: '**', redirectTo: ''}
];
