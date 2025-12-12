import {Component, inject, output, signal} from '@angular/core';
import {FormsModule} from '@angular/forms';
import {Api} from '../../api/generated/api';
import {createCompanyForCurrentUser} from '../../api/generated/functions';
import {CompanyResponse} from '../../api/generated/models';
import {handleApiError} from '../../utils/error-handler.util';

@Component({
  selector: 'app-company-creation-form',
  standalone: true,
  imports: [FormsModule],
  templateUrl: './company-creation-form.component.html',
  styleUrl: './company-creation-form.component.scss'
})
export class CompanyCreationFormComponent {
  companyName = '';
  error = signal('');
  isLoading = signal(false);

  companyCreated = output<CompanyResponse>();

  private api = inject(Api);

  async onSubmit(): Promise<void> {
    const validationError = this.validateForm();
    if (validationError) {
      this.error.set(validationError);
      return;
    }

    this.isLoading.set(true);
    this.error.set('');

    try {
      const response = await this.api.invoke(createCompanyForCurrentUser, {
        body: {companyName: this.companyName.trim()}
      });

      this.companyCreated.emit(response);
    } catch (err: unknown) {
      this.isLoading.set(false);
      this.error.set(handleApiError(err, 'company'));
    }
  }

  private validateForm(): string | null {
    const name = this.companyName.trim();

    if (!name) {
      return 'Company name is required';
    }

    if (name.length < 2) {
      return 'Company name must be at least 2 characters';
    }

    if (name.length > 100) {
      return 'Company name must be less than 100 characters';
    }

    return null;
  }
}
