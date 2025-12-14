import {Component, computed, inject, OnInit, output, signal} from '@angular/core';
import {CommonModule} from '@angular/common';
import {FormsModule} from '@angular/forms';
import {User, UsersService} from '../../services/users.service';
import {UserTagComponent} from '../user-tag/user-tag.component';

@Component({
  selector: 'app-users-table',
  standalone: true,
  imports: [CommonModule, FormsModule, UserTagComponent],
  templateUrl: './users-table.component.html',
  styleUrl: './users-table.component.scss'
})
export class UsersTableComponent implements OnInit {
  searchQuery = signal('');
  userToDelete = signal<User | null>(null);
  addUserClick = output<void>();
  editUserClick = output<User>();
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
    if (!date) return 'Never';

    const now = new Date();
    const diffMs = now.getTime() - new Date(date).getTime();
    const diffMins = Math.floor(diffMs / 60000);
    const diffHours = Math.floor(diffMins / 60);
    const diffDays = Math.floor(diffHours / 24);

    if (diffMins < 1) return 'Just now';
    if (diffMins < 60) return `${diffMins}m ago`;
    if (diffHours < 24) return `${diffHours}h ago`;
    if (diffDays < 7) return `${diffDays}d ago`;
    if (diffDays < 30) return `${Math.floor(diffDays / 7)}w ago`;
    return `${Math.floor(diffDays / 30)}mo ago`;
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
    this.userToDelete.set(user);
  }

  confirmDeleteUser(): void {
    const user = this.userToDelete();
    if (!user) return;

    this.usersService.removeUser(user.id);
    this.userToDelete.set(null);
  }

  cancelDeleteUser(): void {
    this.userToDelete.set(null);
  }
}
