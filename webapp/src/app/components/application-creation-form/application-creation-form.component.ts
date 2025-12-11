import {ChangeDetectorRef, Component, inject, output} from '@angular/core';
import {FormsModule} from '@angular/forms';
import {Api} from '../../api/generated/api';
import {createApplication} from '../../api/generated/functions';
import {ApplicationResponse} from '../../api/generated/models';
import {handleApiError} from '../../utils/error-handler.util';

@Component({
  selector: 'app-application-creation-form',
  standalone: true,
  imports: [FormsModule],
  templateUrl: './application-creation-form.component.html',
  styleUrl: './application-creation-form.component.scss'
})
export class ApplicationCreationFormComponent {
  applicationName = '';
  error = '';
  isLoading = false;

  applicationCreated = output<ApplicationResponse>();

  private api = inject(Api);
  private cdr = inject(ChangeDetectorRef);

  async onSubmit(): Promise<void> {
    const validationError = this.validateForm();
    if (validationError) {
      this.error = validationError;
      return;
    }

    this.isLoading = true;
    this.error = '';

    try {
      const response = await this.api.invoke(createApplication, {
        body: {name: this.applicationName.trim()}
      });

      this.applicationCreated.emit(response);
    } catch (err: any) {
      this.isLoading = false;
      this.error = handleApiError(err, 'application');
      this.cdr.detectChanges();
    }
  }

  private validateForm(): string | null {
    const name = this.applicationName.trim();

    if (!name) {
      return 'Application name is required';
    }

    if (name.length < 2) {
      return 'Application name must be at least 2 characters';
    }

    if (name.length > 100) {
      return 'Application name must be less than 100 characters';
    }

    return null;
  }
}
