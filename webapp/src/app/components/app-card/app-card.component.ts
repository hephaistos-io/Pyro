import {Component, input, output} from '@angular/core';

@Component({
  selector: 'app-card',
  standalone: true,
  templateUrl: './app-card.component.html',
  styleUrl: './app-card.component.scss'
})
export class AppCardComponent {
  name = input<string>();
  isAddCard = input(false);

  cardClick = output<void>();

  onClick(): void {
    this.cardClick.emit();
  }
}