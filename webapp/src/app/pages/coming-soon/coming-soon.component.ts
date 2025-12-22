import {Component, signal} from '@angular/core';
import {FormsModule} from '@angular/forms';
import {isValidEmail} from '../../utils/validators.util';

@Component({
  selector: 'app-coming-soon',
  standalone: true,
  imports: [FormsModule],
  templateUrl: './coming-soon.component.html',
  styleUrl: './coming-soon.component.scss'
})
export class ComingSoonComponent {
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
