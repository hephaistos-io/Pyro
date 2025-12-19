import {Component, input, output} from '@angular/core';
import {ApplicationListResponse} from '../../api/generated/models';

@Component({
  selector: 'app-card',
  standalone: true,
  templateUrl: './app-card.component.html',
  styleUrl: './app-card.component.scss'
})
export class AppCardComponent {
  name = input<string>();
  isAddCard = input(false);
  application = input<ApplicationListResponse>();

  cardClick = output<void>();
  applicationClick = output<ApplicationListResponse>();

  onClick(): void {
    if (this.isAddCard()) {
      this.cardClick.emit();
    } else {
      const app = this.application();
      if (app) {
        this.applicationClick.emit(app);
      }
    }
  }
}