import {Component, input, output} from '@angular/core';

@Component({
  selector: 'app-form-overlay',
  standalone: true,
  imports: [],
  templateUrl: './form-overlay.component.html',
  styleUrl: './form-overlay.component.scss'
})
export class FormOverlayComponent {
  closable = input(true);
  maxWidth = input('600px');
  closeOverlay = output<void>();

  onClose(): void {
    this.closeOverlay.emit();
  }

  onBackdropClick(event: MouseEvent): void {
    if (this.closable() && (event.target as HTMLElement).classList.contains('form-overlay')) {
      this.closeOverlay.emit();
    }
  }
}
