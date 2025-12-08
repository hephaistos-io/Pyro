import {Component} from '@angular/core';
import {FormsModule} from '@angular/forms';

@Component({
  selector: 'app-pricing',
  standalone: true,
  imports: [FormsModule],
  templateUrl: './pricing.component.html',
  styleUrl: './pricing.component.scss'
})
export class PricingComponent {
  email = '';
  submitted = false;
  error = '';

  onSubmit(): void {
    if (!this.email) {
      this.error = 'Please enter your email address';
      return;
    }

    if (!this.isValidEmail(this.email)) {
      this.error = 'Please enter a valid email address';
      return;
    }

    // TODO: Integrate with actual email service
    console.log('Email submitted:', this.email);
    this.submitted = true;
    this.error = '';
  }

  private isValidEmail(email: string): boolean {
    const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
    return emailRegex.test(email);
  }
}
