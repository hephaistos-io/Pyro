import {Component, input, signal} from '@angular/core';

export interface ApiKeyRefreshOverlayData {
  keyType: 'read' | 'write';
  onConfirm: () => Promise<void>;
}

@Component({
  selector: 'app-api-key-refresh-overlay',
  standalone: true,
  imports: [],
  templateUrl: './api-key-refresh-overlay.component.html',
  styleUrl: './api-key-refresh-overlay.component.scss'
})
export class ApiKeyRefreshOverlayComponent {
  data = input.required<ApiKeyRefreshOverlayData>();
  close = input.required<() => void>();

  isRefreshing = signal(false);

  async onConfirm(): Promise<void> {
    this.isRefreshing.set(true);
    try {
      await this.data().onConfirm();
      this.close()();
    } catch (error) {
      console.error('Error refreshing key:', error);
      this.isRefreshing.set(false);
    }
  }

  onCancel(): void {
    this.close()();
  }
}
