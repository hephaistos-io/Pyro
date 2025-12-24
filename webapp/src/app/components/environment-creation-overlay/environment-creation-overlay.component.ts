import {Component, computed, input, signal} from '@angular/core';

export interface EnvironmentCreationOverlayData {
  onConfirm: (name: string, description?: string) => Promise<void>;
  mode?: 'create' | 'edit';
  initialName?: string;
  initialDescription?: string;
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

  mode = computed(() => this.data().mode ?? 'create');
  title = computed(() => this.mode() === 'edit' ? 'Edit Environment' : 'Add New Environment');
  submitButtonText = computed(() => this.mode() === 'edit' ? 'Save Changes' : 'Create Environment');
  submittingText = computed(() => this.mode() === 'edit' ? 'Saving...' : 'Creating...');

  async onConfirm(name: string, description?: string): Promise<void> {
    this.isSubmitting.set(true);
    try {
      await this.data().onConfirm(name, description);
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
