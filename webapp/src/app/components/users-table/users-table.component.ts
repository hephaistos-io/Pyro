import {Component, computed, inject, input, OnInit, output, signal} from '@angular/core';
import {CommonModule} from '@angular/common';
import {FormsModule} from '@angular/forms';
import {User, UsersService} from '../../services/users.service';
import {UserTagComponent} from '../user-tag/user-tag.component';
import {OverlayService} from '../../services/overlay.service';
import {DeleteUserConfirmationComponent} from '../delete-user-confirmation/delete-user-confirmation.component';
import {DeleteInviteConfirmationComponent} from '../delete-invite-confirmation/delete-invite-confirmation.component';
import {RegenerateInviteSuccessComponent} from '../regenerate-invite-success/regenerate-invite-success.component';
import {getRelativeTime} from '../../utils/time.util';

@Component({
  selector: 'app-users-table',
  standalone: true,
  imports: [CommonModule, FormsModule, UserTagComponent],
  templateUrl: './users-table.component.html',
  styleUrl: './users-table.component.scss'
})
export class UsersTableComponent implements OnInit {
  /** When true, hides action buttons (invite, edit, delete). Dev users can see users but not manage them. */
  readOnly = input(false);
  searchQuery = signal('');
  addUserClick = output<void>();
  editUserClick = output<User>();
  private overlayService = inject(OverlayService);
  filteredUsers = computed(() => {
    const query = this.searchQuery().toLowerCase().trim();
    if (!query) return this.users();

    return this.users().filter(user =>
      user.firstName.toLowerCase().includes(query) ||
      user.lastName.toLowerCase().includes(query) ||
      user.email.toLowerCase().includes(query)
    );
  });
  private usersService = inject(UsersService);
  users = this.usersService.users;
  isLoading = this.usersService.isLoading;

  async ngOnInit(): Promise<void> {
    await this.usersService.fetchUsers();
  }

  onSearchChange(value: string): void {
    this.searchQuery.set(value);
  }

  getRelativeTime(date: Date | null): string {
    return getRelativeTime(date);
  }

  getRoleVariant(roleType: 'admin' | 'developer' | 'viewer'): 'role-admin' | 'role-developer' | 'role-viewer' {
    return `role-${roleType}` as 'role-admin' | 'role-developer' | 'role-viewer';
  }

  onAddUserClick(): void {
    this.addUserClick.emit();
  }

  onEditUserClick(user: User): void {
    this.editUserClick.emit(user);
  }

  onDeleteUserClick(user: User): void {
    this.overlayService.open({
      component: DeleteUserConfirmationComponent,
      data: {user},
      maxWidth: '440px'
    });
  }

  async onRegenerateClick(user: User): Promise<void> {
    const response = await this.usersService.regenerateInvite(user.id);
    this.overlayService.open({
      component: RegenerateInviteSuccessComponent,
      data: {invite: response},
      maxWidth: '500px'
    });
  }

  onDeleteInviteClick(user: User): void {
    this.overlayService.open({
      component: DeleteInviteConfirmationComponent,
      data: {user},
      maxWidth: '440px'
    });
  }

  isInvitedUser(user: User): boolean {
    return user.status === 'invited' || user.status === 'expired';
  }

  isAdminUser(user: User): boolean {
    return user.roles.some(role => role.type === 'admin');
  }
}
