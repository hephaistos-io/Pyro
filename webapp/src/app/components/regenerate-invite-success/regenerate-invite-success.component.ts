import {Component, input} from '@angular/core';
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

  done(): void {
    this.close()();
  }
}
