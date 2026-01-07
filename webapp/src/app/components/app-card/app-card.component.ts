import {Component, input, output} from '@angular/core';
import {ApplicationListResponse} from '../../api/generated/models';
import {formatFullDate, getRelativeTime} from '../../utils/time.util';

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

  getRelativeUpdatedTime(): string {
    const app = this.application();
    return getRelativeTime(app?.updatedAt);
  }

  getTooltip(): string {
    const app = this.application();
    if (!app?.createdAt) return '';

    let tooltip = `Created: ${formatFullDate(app.createdAt)}`;
    if (app.createdByName) tooltip += ` by ${app.createdByName}`;
    if (app.updatedAt) {
      tooltip += `\nUpdated: ${formatFullDate(app.updatedAt)}`;
      if (app.updatedByName) tooltip += ` by ${app.updatedByName}`;
    }
    return tooltip;
  }
}