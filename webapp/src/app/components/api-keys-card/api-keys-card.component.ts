import {Component, effect, inject, input, signal} from '@angular/core';
import {Api} from '../../api/generated/api';
import {ApiKeyResponse} from '../../api/generated/models';
import {getApiKeyByType} from '../../api/generated/fn/api-keys/get-api-key-by-type';
import {regenerateApiKey} from '../../api/generated/fn/api-keys/regenerate-api-key';
import {OverlayService} from '../../services/overlay.service';
import {
  ApiKeyRefreshOverlayComponent,
  ApiKeyRefreshOverlayData
} from '../api-key-refresh-overlay/api-key-refresh-overlay.component';

@Component({
  selector: 'app-api-keys-card',
  standalone: true,
  imports: [],
  templateUrl: './api-keys-card.component.html',
  styleUrl: './api-keys-card.component.scss'
})
export class ApiKeysCardComponent {
  // Inputs
  applicationId = input.required<string>();
  environmentId = input.required<string>();
  // API key visibility and state
  showReadKey = signal(false);
  showWriteKey = signal(false);
  readKey = signal<string | null>(null);
  writeKey = signal<string | null>(null);
  readKeyData = signal<ApiKeyResponse | null>(null);
  writeKeyData = signal<ApiKeyResponse | null>(null);
  isLoadingReadKey = signal(false);
  isLoadingWriteKey = signal(false);
  private api = inject(Api);
  private overlayService = inject(OverlayService);

  // Removed - now using overlay service

  constructor() {
    // Clear API key state when environment changes
    effect(() => {
      // Just reading environmentId triggers the effect when it changes
      this.environmentId();
      // Clear all key state
      this.showReadKey.set(false);
      this.showWriteKey.set(false);
      this.readKey.set(null);
      this.writeKey.set(null);
      this.readKeyData.set(null);
      this.writeKeyData.set(null);
    }, {allowSignalWrites: true});
  }

  async toggleReadKeyVisibility(): Promise<void> {
    if (this.showReadKey()) {
      // Hide the key - clear the state
      this.showReadKey.set(false);
      this.readKey.set(null);
    } else {
      // Show the key - set flag before fetch to avoid race condition
      this.showReadKey.set(true);
      await this.fetchReadKey();
    }
  }

  async toggleWriteKeyVisibility(): Promise<void> {
    if (this.showWriteKey()) {
      // Hide the key - clear the state
      this.showWriteKey.set(false);
      this.writeKey.set(null);
    } else {
      // Show the key - set flag before fetch to avoid race condition
      this.showWriteKey.set(true);
      await this.fetchWriteKey();
    }
  }

  getKeyPlaceholder(): string {
    return '••••••••••••••••••••••••';
  }

  requestKeyRefresh(keyType: 'read' | 'write'): void {
    this.overlayService.open<ApiKeyRefreshOverlayData>({
      component: ApiKeyRefreshOverlayComponent,
      data: {
        keyType,
        onConfirm: async () => {
          const appId = this.applicationId();
          const envId = this.environmentId();

          if (!appId || !envId) return undefined;

          const response = await this.api.invoke(regenerateApiKey, {
            applicationId: appId,
            environmentId: envId,
            keyType: keyType === 'read' ? 'READ' : 'WRITE'
          });

          // Update the key and key data with the new secret
          if (keyType === 'read') {
            this.readKey.set(response.secretKey ?? null);
            this.readKeyData.set(response);
            this.showReadKey.set(true);
          } else {
            this.writeKey.set(response.secretKey ?? null);
            this.writeKeyData.set(response);
            this.showWriteKey.set(true);
          }

          // Return the expiration date of the old key
          return response.expirationDate;
        }
      },
      maxWidth: '450px'
    });
  }

  private async fetchReadKey(): Promise<void> {
    const appId = this.applicationId();
    const envId = this.environmentId();
    if (!appId || !envId) return;

    this.isLoadingReadKey.set(true);
    try {
      const response = await this.api.invoke(getApiKeyByType, {
        applicationId: appId,
        environmentId: envId,
        keyType: 'READ'
      });
      this.readKeyData.set(response);
      this.readKey.set(response.secretKey ?? null);
    } catch (error) {
      console.error('Failed to fetch read key:', error);
      this.readKey.set(null);
    } finally {
      this.isLoadingReadKey.set(false);
    }
  }

  private async fetchWriteKey(): Promise<void> {
    const appId = this.applicationId();
    const envId = this.environmentId();
    if (!appId || !envId) return;

    this.isLoadingWriteKey.set(true);
    try {
      const response = await this.api.invoke(getApiKeyByType, {
        applicationId: appId,
        environmentId: envId,
        keyType: 'WRITE'
      });
      this.writeKeyData.set(response);
      this.writeKey.set(response.secretKey ?? null);
    } catch (error) {
      console.error('Failed to fetch write key:', error);
      this.writeKey.set(null);
    } finally {
      this.isLoadingWriteKey.set(false);
    }
  }
}
