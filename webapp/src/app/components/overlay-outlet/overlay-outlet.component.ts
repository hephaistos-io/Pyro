import {Component, inject} from '@angular/core';
import {CommonModule} from '@angular/common';
import {OverlayConfig, OverlayService} from '../../services/overlay.service';

@Component({
  selector: 'app-overlay-outlet',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './overlay-outlet.component.html',
  styleUrl: './overlay-outlet.component.scss'
})
export class OverlayOutletComponent {
  protected overlayService = inject(OverlayService);
  protected closeFn = (): void => this.overlayService.close();

  protected getInputs(config: OverlayConfig): Record<string, unknown> {
    return {
      data: config.data,
      close: this.closeFn
    };
  }

  protected onBackdropClick(event: MouseEvent, config: OverlayConfig): void {
    if (config.closable !== false && (event.target as HTMLElement).classList.contains('overlay-backdrop')) {
      this.overlayService.close();
    }
  }
}
