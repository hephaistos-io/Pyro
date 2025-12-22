import {Component, computed, effect, inject, input, model, output, signal} from '@angular/core';
import {FormsModule} from '@angular/forms';
import {EnvironmentResponse} from '../../api/generated/models';
import {Api} from '../../api/generated/api';
import {createEnvironment} from '../../api/generated/fn/environments/create-environment';
import {deleteEnvironment} from '../../api/generated/fn/environments/delete-environment';
import {OverlayService} from '../../services/overlay.service';
import {
  EnvironmentCreationOverlayComponent,
  EnvironmentCreationOverlayData
} from '../environment-creation-overlay/environment-creation-overlay.component';

// Type-safe PricingTier constants extracted from API models
const PricingTier = {
  FREE: 'FREE',
  BASIC: 'BASIC',
  STANDARD: 'STANDARD',
  PRO: 'PRO',
  BUSINESS: 'BUSINESS'
} as const;

@Component({
  selector: 'app-environment-manager',
  standalone: true,
  imports: [FormsModule],
  templateUrl: './environment-manager.component.html',
  styleUrl: './environment-manager.component.scss'
})
export class EnvironmentManagerComponent {
  // Inputs
  applicationId = input.required<string>();
  environments = input.required<EnvironmentResponse[]>();
  selectedEnvironment = model.required<EnvironmentResponse | null>();
  // Outputs
  environmentCreated = output<EnvironmentResponse>();
  environmentDeleted = output<string>(); // environmentId
  // Environment dropdown state
  showEnvironmentDropdown = signal(false);
  environmentSearchQuery = signal('');
  // Environment deletion state
  showEnvironmentDeletion = signal(false);
  isDeletingEnvironment = signal(false);
  // Computed: Check if selected environment can be deleted (only paid tiers, not FREE)
  canDeleteEnvironment = computed(() => {
    const env = this.selectedEnvironment();
    return env?.tier !== PricingTier.FREE;
  });
  // Computed: Filtered environments for dropdown
  filteredEnvironments = computed(() => {
    const query = this.environmentSearchQuery().toLowerCase();
    const envs = this.environments();

    if (!query) {
      return envs;
    }

    return envs.filter(env =>
      env.name?.toLowerCase().includes(query) ||
      env.description?.toLowerCase().includes(query)
    );
  });
  private api = inject(Api);
  private overlayService = inject(OverlayService);

  constructor() {
    // Auto-select first environment when environments change
    effect(() => {
      const envs = this.environments();
      const selected = this.selectedEnvironment();
      if (envs.length > 0 && !selected) {
        this.selectedEnvironment.set(envs[0]);
      }
    });
  }

  toggleEnvironmentDropdown(): void {
    this.showEnvironmentDropdown.update(show => !show);
    if (this.showEnvironmentDropdown()) {
      this.environmentSearchQuery.set('');
    }
  }

  closeEnvironmentDropdown(): void {
    this.showEnvironmentDropdown.set(false);
    this.environmentSearchQuery.set('');
  }

  selectEnvironment(environment: EnvironmentResponse): void {
    this.selectedEnvironment.set(environment);
    this.closeEnvironmentDropdown();
  }

  onSearchQueryChange(query: string): void {
    this.environmentSearchQuery.set(query);
  }

  onAddEnvironmentClick(): void {
    this.closeEnvironmentDropdown();
    this.overlayService.open<EnvironmentCreationOverlayData>({
      component: EnvironmentCreationOverlayComponent,
      data: {
        onConfirm: async (name: string, description?: string) => {
          const appId = this.applicationId();
          if (!appId) {
            return;
          }

          const newEnv = await this.api.invoke(createEnvironment, {
            applicationId: appId,
            body: {
              name: name,
              description: description ?? ''
            }
          });

          this.selectedEnvironment.set(newEnv);
          this.environmentCreated.emit(newEnv);
        }
      },
      maxWidth: '500px'
    });
  }

  requestEnvironmentDeletion(): void {
    if (this.canDeleteEnvironment()) {
      this.showEnvironmentDeletion.set(true);
    }
  }

  cancelEnvironmentDeletion(): void {
    this.showEnvironmentDeletion.set(false);
  }

  async confirmEnvironmentDeletion(): Promise<void> {
    const appId = this.applicationId();
    const currentEnv = this.selectedEnvironment();

    if (!appId || !currentEnv?.id) {
      return;
    }

    // Double-check tier before deletion
    if (currentEnv.tier === PricingTier.FREE) {
      console.error('Cannot delete FREE tier environment');
      this.showEnvironmentDeletion.set(false);
      return;
    }

    this.isDeletingEnvironment.set(true);
    try {
      await this.api.invoke(deleteEnvironment, {
        applicationId: appId,
        environmentId: currentEnv.id
      });

      // Select another environment if available
      const remaining = this.environments().filter(env => env.id !== currentEnv.id);
      if (remaining.length > 0) {
        this.selectedEnvironment.set(remaining[0]);
      } else {
        this.selectedEnvironment.set(null);
      }

      this.showEnvironmentDeletion.set(false);
      this.environmentDeleted.emit(currentEnv.id);
    } catch (error) {
      console.error('Failed to delete environment:', error);
    } finally {
      this.isDeletingEnvironment.set(false);
    }
  }
}
