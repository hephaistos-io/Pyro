import {Routes} from '@angular/router';
import {HomeComponent} from './pages/home/home.component';
import {PricingComponent} from './pages/pricing/pricing.component';
import {RegisterComponent} from './pages/register/register.component';

export const routes: Routes = [
  {path: '', component: HomeComponent},
  {path: 'pricing', component: PricingComponent},
  {path: 'register', component: RegisterComponent},
  {path: '**', redirectTo: ''}
];
