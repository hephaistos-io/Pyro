import {Component, input, output} from '@angular/core';

@Component({
  selector: 'app-onboarding-overlay',
  standalone: true,
  imports: [],
  templateUrl: './onboarding-overlay.component.html',
  styleUrl: './onboarding-overlay.component.scss'
})
export class OnboardingOverlayComponent {
  closable = input(false);
  closeOverlay = output<void>();

  onClose(): void {
    this.closeOverlay.emit();
  }

  onBackdropClick(event: MouseEvent): void {
    if (this.closable() && (event.target as HTMLElement).classList.contains('onboarding-overlay')) {
      this.closeOverlay.emit();
    }
  }
}
