import {Component, input, signal} from '@angular/core';
import {InviteCreationResponse} from '../../api/generated/models';

@Component({
  selector: 'app-regenerate-invite-success',
  standalone: true,
  imports: [],
  templateUrl: './regenerate-invite-success.component.html',
  styleUrl: './regenerate-invite-success.component.scss'
})
export class RegenerateInviteSuccessComponent {
  data = input.required<{ invite: InviteCreationResponse }>();
  close = input.required<() => void>();

  urlCopied = signal(false);

  async copyUrl(): Promise<void> {
    const inviteUrl = this.data().invite.inviteUrl;
    if (!inviteUrl) return;

    await navigator.clipboard.writeText(inviteUrl);
    this.urlCopied.set(true);
    setTimeout(() => this.urlCopied.set(false), 2000);
  }

  done(): void {
    this.close()();
  }
}
