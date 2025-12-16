import {Injectable, signal, Type} from '@angular/core';

export interface OverlayConfig<T = unknown> {
  component: Type<unknown>;
  data?: T;
  closable?: boolean;
  maxWidth?: string;
}

@Injectable({providedIn: 'root'})
export class OverlayService {
  private activeOverlay = signal<OverlayConfig | null>(null);
  overlay = this.activeOverlay.asReadonly();

  open<T>(config: OverlayConfig<T>): void {
    this.activeOverlay.set(config);
  }

  close(): void {
    this.activeOverlay.set(null);
  }
}
