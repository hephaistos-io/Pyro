import {Component} from '@angular/core';
import {HeroComponent} from '../../components/hero/hero.component';
import {FeaturesComponent} from '../../components/features/features.component';
import {HowItWorksComponent} from '../../components/how-it-works/how-it-works.component';
import {CtaComponent} from '../../components/cta/cta.component';

@Component({
  selector: 'app-home',
  standalone: true,
  imports: [
    HeroComponent,
    FeaturesComponent,
    HowItWorksComponent,
    CtaComponent
  ],
  templateUrl: './home.component.html',
  styleUrl: './home.component.scss'
})
export class HomeComponent {
}
