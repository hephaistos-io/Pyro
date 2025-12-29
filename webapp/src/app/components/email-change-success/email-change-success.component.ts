import {Component, input, signal} from '@angular/core';

@Component({
  selector: 'app-email-change-success',
  standalone: true,
  imports: [],
  templateUrl: './email-change-success.component.html',
  styleUrl: './email-change-success.component.scss'
})
export class EmailChangeSuccessComponent {
  data = input.required<{ verificationUrl: string; newEmail: string; expiresAt: string }>();
  close = input.required<() => void>();

  urlCopied = signal(false);

  async copyUrl(): Promise<void> {
    const verificationUrl = this.data().verificationUrl;
    if (!verificationUrl) return;

    await navigator.clipboard.writeText(verificationUrl);
    this.urlCopied.set(true);
    setTimeout(() => this.urlCopied.set(false), 2000);
  }

  done(): void {
    this.close()();
  }
}
