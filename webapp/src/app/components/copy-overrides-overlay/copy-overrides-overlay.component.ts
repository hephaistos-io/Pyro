import {Component, computed, inject, input, OnInit, signal} from '@angular/core';
import {FormsModule} from '@angular/forms';
import {EnvironmentResponse, TemplateType} from '../../api/generated/models';
import {TemplateService} from '../../services/template.service';

export interface CopyOverridesOverlayData {
  applicationId: string;
  applicationName: string;
  environments: EnvironmentResponse[];
  currentEnvironmentId: string;
  allIdentifiers: string[];
  onSuccess: () => void;
}

@Component({
  selector: 'app-copy-overrides-overlay',
  standalone: true,
  imports: [FormsModule],
  templateUrl: './copy-overrides-overlay.component.html',
  styleUrl: './copy-overrides-overlay.component.scss'
})
export class CopyOverridesOverlayComponent implements OnInit {
  data = input.required<CopyOverridesOverlayData>();
  close = input.required<() => void>();
  // Form state signals
  sourceEnvironmentId = signal<string>('');
  selectedIdentifier = signal<string>('');
  targetEnvironmentId = signal<string>('');
  // Loading and message states
  isCopying = signal(false);
  successMessage = signal<string | null>(null);
  errorMessage = signal<string | null>(null);
  // Get available identifiers from the source environment
  availableIdentifiers = computed<string[]>(() => {
    // For MVP, we'll show all identifiers since source is always the current environment
    // In future, if source can change, we'd need to load identifiers per environment
    return this.data().allIdentifiers;
  });
  // Get target environment options (excluding source)
  targetEnvironmentOptions = computed<EnvironmentResponse[]>(() => {
    const sourceId = this.sourceEnvironmentId();
    return this.data().environments.filter(env => env.id !== sourceId);
  });
  // Get source environment name
  sourceEnvironmentName = computed<string>(() => {
    const sourceId = this.sourceEnvironmentId();
    const env = this.data().environments.find(e => e.id === sourceId);
    return env?.name ?? 'Unknown';
  });
  // Get target environment name
  targetEnvironmentName = computed<string>(() => {
    const targetId = this.targetEnvironmentId();
    const env = this.data().environments.find(e => e.id === targetId);
    return env?.name ?? '';
  });
  // Validation
  isValid = computed(() => {
    return this.selectedIdentifier().trim() !== '' && this.targetEnvironmentId() !== '';
  });
  // Summary message
  summaryMessage = computed<string | null>(() => {
    const identifier = this.selectedIdentifier();
    const targetName = this.targetEnvironmentName();

    if (!identifier || !targetName) return null;

    return `This will copy both SYSTEM and USER template overrides for identifier "${identifier}" from ${this.sourceEnvironmentName()} to ${targetName}. Existing overrides in the target environment will be replaced.`;
  });
  private templateService = inject(TemplateService);

  ngOnInit(): void {
    // Initialize source environment to current environment
    this.sourceEnvironmentId.set(this.data().currentEnvironmentId);
  }

  // Handle identifier selection change
  onIdentifierChange(): void {
    // Clear target environment when identifier changes to require user confirmation
    this.targetEnvironmentId.set('');
    this.clearMessages();
  }

  // Submit the form
  async onSubmit(): Promise<void> {
    if (!this.isValid() || this.isCopying()) {
      return;
    }

    const appId = this.data().applicationId;
    const sourceEnvId = this.sourceEnvironmentId();
    const targetEnvId = this.targetEnvironmentId();
    const identifier = this.selectedIdentifier();

    this.clearMessages();
    this.isCopying.set(true);

    try {
      const response = await this.templateService.copyOverrides(
        appId,
        sourceEnvId,
        targetEnvId,
        [TemplateType.System, TemplateType.User], // Copy both types
        true, // Overwrite
        [identifier] // Single identifier
      );

      this.successMessage.set(
        `Successfully copied ${response.copiedCount} override(s) to ${this.targetEnvironmentName()}.`
      );

      // Auto-close after 1.5 seconds
      setTimeout(() => {
        this.data().onSuccess();
        this.close()();
      }, 1500);
    } catch (error) {
      console.error('Failed to copy overrides:', error);
      this.errorMessage.set('Failed to copy overrides. Please try again.');
    } finally {
      this.isCopying.set(false);
    }
  }

  // Cancel the form
  onCancel(): void {
    this.close()();
  }

  // Clear messages
  private clearMessages(): void {
    this.successMessage.set(null);
    this.errorMessage.set(null);
  }
}
