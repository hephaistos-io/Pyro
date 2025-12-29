import {Component, input, signal} from '@angular/core';

@Component({
  selector: 'app-password-reset-success',
  standalone: true,
  imports: [],
  templateUrl: './password-reset-success.component.html',
  styleUrl: './password-reset-success.component.scss'
})
export class PasswordResetSuccessComponent {
  data = input.required<{ resetUrl: string; expiresAt: string }>();
  close = input.required<() => void>();

  urlCopied = signal(false);

  async copyUrl(): Promise<void> {
    const resetUrl = this.data().resetUrl;
    if (!resetUrl) return;

    await navigator.clipboard.writeText(resetUrl);
    this.urlCopied.set(true);
    setTimeout(() => this.urlCopied.set(false), 2000);
  }

  done(): void {
    this.close()();
  }
}
