import {Component, input, signal} from '@angular/core';
import {DatePipe} from '@angular/common';

export interface ApiKeyRefreshOverlayData {
  keyType: 'read' | 'write';
  onConfirm: () => Promise<string | undefined>;
}

@Component({
  selector: 'app-api-key-refresh-overlay',
  standalone: true,
  imports: [DatePipe],
  templateUrl: './api-key-refresh-overlay.component.html',
  styleUrl: './api-key-refresh-overlay.component.scss'
})
export class ApiKeyRefreshOverlayComponent {
  data = input.required<ApiKeyRefreshOverlayData>();
  close = input.required<() => void>();

  isRefreshing = signal(false);
  isSuccess = signal(false);
  expirationDate = signal<string | undefined>(undefined);

  async onConfirm(): Promise<void> {
    this.isRefreshing.set(true);
    try {
      const expiration = await this.data().onConfirm();
      this.expirationDate.set(expiration);
      this.isSuccess.set(true);
      this.isRefreshing.set(false);
    } catch (error) {
      console.error('Error refreshing key:', error);
      this.isRefreshing.set(false);
    }
  }

  onCancel(): void {
    this.close()();
  }

  onClose(): void {
    this.close()();
  }
}
