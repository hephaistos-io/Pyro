import {Component, computed, effect, inject, input, model, output, signal} from '@angular/core';
import {FormsModule} from '@angular/forms';
import {EnvironmentResponse} from '../../api/generated/models';
import {Api} from '../../api/generated/api';
import {createEnvironment} from '../../api/generated/fn/environments/create-environment';
import {deleteEnvironment} from '../../api/generated/fn/environments/delete-environment';
import {updateEnvironment} from '../../api/generated/fn/environments/update-environment';
import {OverlayService} from '../../services/overlay.service';
import {RoleService} from '../../services/role.service';
import {PricingStateService, PricingTier, TIER_PRICES} from '../../services/pricing-state.service';
import {
  EnvironmentCreationOverlayComponent,
  EnvironmentCreationOverlayData
} from '../environment-creation-overlay/environment-creation-overlay.component';
import {
  DeleteFieldOverlayComponent,
  DeleteFieldOverlayData
} from '../delete-field-overlay/delete-field-overlay.component';

@Component({
  selector: 'app-environment-manager',
  standalone: true,
  imports: [FormsModule],
  templateUrl: './environment-manager.component.html',
  styleUrl: './environment-manager.component.scss'
})
export class EnvironmentManagerComponent {
  environmentUpdated = output<EnvironmentResponse>();

  // Inputs
  applicationId = input.required<string>();
  applicationName = input.required<string>();
  environments = input.required<EnvironmentResponse[]>();
  selectedEnvironment = model.required<EnvironmentResponse | null>();
  // Outputs
  environmentCreated = output<EnvironmentResponse>();
  environmentDeleted = output<string>(); // environmentId
  // Environment dropdown state
  showEnvironmentDropdown = signal(false);
  environmentSearchQuery = signal('');
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
  private pricingState = inject(PricingStateService);
  roleService = inject(RoleService);

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
        mode: 'create',
        onConfirm: async (name: string, description: string, tier: PricingTier) => {
          const appId = this.applicationId();
          const appName = this.applicationName();
          if (!appId) {
            return;
          }

          const newEnv = await this.api.invoke(createEnvironment, {
            applicationId: appId,
            body: {
              name: name,
              description: description,
              tier: tier
            }
          });

          // Add to cart if it's a paid tier
          if (tier !== 'FREE' && newEnv.id) {
            this.pricingState.addPendingEnvironment({
              environmentId: newEnv.id,
              environmentName: name,
              applicationName: appName,
              tier: tier,
              monthlyPriceCents: TIER_PRICES[tier]
            });
          }

          this.selectedEnvironment.set(newEnv);
          this.environmentCreated.emit(newEnv);
        }
      },
      maxWidth: '550px'
    });
  }

  onEditEnvironmentClick(): void {
    const appId = this.applicationId();
    const currentEnv = this.selectedEnvironment();

    if (!appId || !currentEnv?.id) {
      return;
    }

    this.overlayService.open<EnvironmentCreationOverlayData>({
      component: EnvironmentCreationOverlayComponent,
      data: {
        mode: 'edit',
        initialName: currentEnv.name ?? '',
        initialDescription: currentEnv.description ?? '',
        initialTier: currentEnv.tier as PricingTier ?? 'FREE',
        onConfirm: async (name: string, description: string) => {
          const updatedEnv = await this.api.invoke(updateEnvironment, {
            applicationId: appId,
            environmentId: currentEnv.id!,
            body: {
              name: name,
              description: description
            }
          });

          this.selectedEnvironment.set(updatedEnv);
          this.environmentUpdated.emit(updatedEnv);
        }
      },
      maxWidth: '550px'
    });
  }

  requestEnvironmentDeletion(): void {
    const appId = this.applicationId();
    const appName = this.applicationName();
    const currentEnv = this.selectedEnvironment();

    if (!appId || !currentEnv?.id || !currentEnv.name) {
      return;
    }

    this.overlayService.open<DeleteFieldOverlayData>({
      component: DeleteFieldOverlayComponent,
      data: {
        applicationName: appName,
        fieldKey: currentEnv.name,
        type: 'environment',
        onConfirm: async () => {
          try {
            await this.api.invoke(deleteEnvironment, {
              applicationId: appId,
              environmentId: currentEnv.id!
            });

            // Select another environment if available
            const remaining = this.environments().filter(env => env.id !== currentEnv.id);
            if (remaining.length > 0) {
              this.selectedEnvironment.set(remaining[0]);
            } else {
              this.selectedEnvironment.set(null);
            }

            this.environmentDeleted.emit(currentEnv.id!);
          } catch (error) {
            console.error('Failed to delete environment:', error);
            alert('Failed to delete environment');
          }
        }
      },
      maxWidth: '500px'
    });
  }
}
