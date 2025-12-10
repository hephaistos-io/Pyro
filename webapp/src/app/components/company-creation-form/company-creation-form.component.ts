import {ChangeDetectorRef, Component, inject, output} from '@angular/core';
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
  error = '';
  isLoading = false;

  companyCreated = output<CompanyResponse>();

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
      const response = await this.api.invoke(createCompanyForCurrentUser, {
        body: {companyName: this.companyName.trim()}
      });

      this.companyCreated.emit(response);
    } catch (err: any) {
      this.isLoading = false;
      this.error = handleApiError(err, 'company');
      this.cdr.detectChanges();
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
