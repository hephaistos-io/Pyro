import {Component, computed, input} from '@angular/core';
import zxcvbn from 'zxcvbn';

@Component({
  selector: 'app-password-strength-meter',
  standalone: true,
  imports: [],
  templateUrl: './password-strength-meter.component.html',
  styleUrl: './password-strength-meter.component.scss'
})
export class PasswordStrengthMeterComponent {
  password = input.required<string>();

  passwordStrength = computed(() => {
    const pwd = this.password();
    if (!pwd) {
      return {score: 0, label: '', feedback: ''};
    }

    if (pwd.length < 8) {
      return {score: 0, label: 'Too short', feedback: 'At least 8 characters required'};
    }

    const result = zxcvbn(pwd);
    const labels = ['Too weak', 'Weak', 'Fair', 'Good', 'Strong'];
    const feedback = result.feedback.warning || result.feedback.suggestions[0] || '';

    return {
      score: result.score,
      label: labels[result.score],
      feedback: feedback
    };
  });
}