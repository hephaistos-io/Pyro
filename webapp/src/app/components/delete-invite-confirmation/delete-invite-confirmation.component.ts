import {Component, inject, input} from '@angular/core';
import {User, UsersService} from '../../services/users.service';

@Component({
  selector: 'app-delete-invite-confirmation',
  standalone: true,
  imports: [],
  templateUrl: './delete-invite-confirmation.component.html',
  styleUrl: './delete-invite-confirmation.component.scss'
})
export class DeleteInviteConfirmationComponent {
  data = input.required<{ user: User }>();
  close = input.required<() => void>();

  private usersService = inject(UsersService);

  cancel(): void {
    this.close()();
  }

  async confirm(): Promise<void> {
    await this.usersService.deleteInvite(this.data().user.id);
    this.close()();
  }
}
