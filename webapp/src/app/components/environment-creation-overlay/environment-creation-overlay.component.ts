import {Component, computed, effect, input, signal} from '@angular/core';
import {PricingTier, TIER_PRICES} from '../../services/pricing-state.service';

export interface EnvironmentCreationOverlayData {
  onConfirm: (name: string, description: string, tier: PricingTier) => Promise<void>;
  mode?: 'create' | 'edit';
  initialName?: string;
  initialDescription?: string;
  initialTier?: PricingTier;
}

@Component({
  selector: 'app-environment-creation-overlay',
  standalone: true,
  imports: [],
  templateUrl: './environment-creation-overlay.component.html',
  styleUrl: './environment-creation-overlay.component.scss'
})
export class EnvironmentCreationOverlayComponent {
  data = input.required<EnvironmentCreationOverlayData>();
  close = input.required<() => void>();
  isSubmitting = signal(false);
  selectedTier = signal<PricingTier>('FREE');

  mode = computed(() => this.data().mode ?? 'create');
  title = computed(() => this.mode() === 'edit' ? 'Edit Environment' : 'Add New Environment');
  submitButtonText = computed(() => {
    if (this.mode() === 'edit') return 'Save Changes';
    const tier = this.selectedTier();
    if (tier === 'FREE') return 'Create Environment';
    return `Create Environment - $${(TIER_PRICES[tier] / 100).toFixed(0)}/mo`;
  });
  submittingText = computed(() => this.mode() === 'edit' ? 'Saving...' : 'Creating...');

  tiers: { value: PricingTier; label: string; price: string }[] = [
    {value: 'FREE', label: 'Free', price: '$0/mo'},
    {value: 'BASIC', label: 'Basic', price: '$10/mo'},
    {value: 'STANDARD', label: 'Standard', price: '$25/mo'},
    {value: 'PRO', label: 'Pro', price: '$50/mo'},
    {value: 'BUSINESS', label: 'Business', price: '$100/mo'}
  ];

  constructor() {
    // Initialize tier from data when it becomes available
    effect(() => {
      const initialTier = this.data().initialTier;
      if (initialTier) {
        this.selectedTier.set(initialTier);
      }
    }, {allowSignalWrites: true});
  }

  onTierChange(tier: PricingTier): void {
    this.selectedTier.set(tier);
  }

  async onConfirm(name: string, description: string): Promise<void> {
    this.isSubmitting.set(true);
    try {
      await this.data().onConfirm(name, description, this.selectedTier());
      this.close()();
    } catch (error) {
      console.error('Error saving environment:', error);
      this.isSubmitting.set(false);
    }
  }

  onCancel(): void {
    this.close()();
  }
}
