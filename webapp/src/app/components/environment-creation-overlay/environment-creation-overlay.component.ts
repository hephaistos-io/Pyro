import {Component, input, signal} from '@angular/core';

export interface EnvironmentCreationOverlayData {
  onConfirm: (name: string, description?: string) => Promise<void>;
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
  isCreating = signal(false);

  async onConfirm(name: string, description?: string): Promise<void> {
    this.isCreating.set(true);
    try {
      await this.data().onConfirm(name, description);
      this.close()();
    } catch (error) {
      console.error('Error creating environment:', error);
      this.isCreating.set(false);
    }
  }

  onCancel(): void {
    this.close()();
  }
}
