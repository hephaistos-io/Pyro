import {Component, signal} from '@angular/core';
import {FormsModule} from '@angular/forms';
import {isValidEmail} from '../../utils/validators.util';

@Component({
  selector: 'app-pricing',
  standalone: true,
  imports: [FormsModule],
  templateUrl: './pricing.component.html',
  styleUrl: './pricing.component.scss'
})
export class PricingComponent {
  email = signal('');
  submitted = signal(false);
  error = signal('');

  onSubmit(): void {
    if (!this.email()) {
      this.error.set('Please enter your email address');
      return;
    }

    if (!isValidEmail(this.email())) {
      this.error.set('Please enter a valid email address');
      return;
    }

    // TODO: Integrate with actual email service
    this.submitted.set(true);
    this.error.set('');
  }
}