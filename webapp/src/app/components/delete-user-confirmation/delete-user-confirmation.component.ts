import {Component, inject, input} from '@angular/core';
import {User, UsersService} from '../../services/users.service';

@Component({
  selector: 'app-delete-user-confirmation',
  standalone: true,
  imports: [],
  templateUrl: './delete-user-confirmation.component.html',
  styleUrl: './delete-user-confirmation.component.scss'
})
export class DeleteUserConfirmationComponent {
  data = input.required<{ user: User }>();
  close = input.required<() => void>();

  private usersService = inject(UsersService);

  cancel(): void {
    this.close()();
  }

  confirm(): void {
    this.usersService.removeUser(this.data().user.id);
    this.close()();
  }
}
