import {Component, computed, input, signal} from '@angular/core';
import {FormsModule} from '@angular/forms';

export interface DeleteFieldOverlayData {
  applicationName: string;
  fieldKey: string;
  templateType: 'system' | 'user';
  onConfirm: () => void;
  type?: 'field' | 'identifier'; // Optional: defaults to 'field'
}

@Component({
  selector: 'app-delete-field-overlay',
  standalone: true,
  imports: [FormsModule],
  templateUrl: './delete-field-overlay.component.html',
  styleUrl: './delete-field-overlay.component.scss'
})
export class DeleteFieldOverlayComponent {
  data = input.required<DeleteFieldOverlayData>();
  close = input.required<() => void>();

  // Computed type (defaults to 'field')
  deleteType = computed(() => this.data().type || 'field');

  // Confirmation inputs
  applicationNameInput = signal('');
  fieldKeyInput = signal('');

  // Validation
  applicationNameMatch = computed(() =>
    this.applicationNameInput().trim() === this.data()?.applicationName
  );

  fieldKeyMatch = computed(() =>
    this.fieldKeyInput().trim() === this.data()?.fieldKey
  );

  canConfirm = computed(() =>
    this.applicationNameMatch() && this.fieldKeyMatch()
  );

  onConfirm(): void {
    if (!this.canConfirm()) return;
    this.data().onConfirm();
    this.close()();
  }

  onCancel(): void {
    this.close()();
  }
}
