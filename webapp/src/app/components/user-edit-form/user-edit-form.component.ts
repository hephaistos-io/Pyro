import {Component, inject, input, OnInit, output, signal} from '@angular/core';
import {FormsModule} from '@angular/forms';
import {User, UsersService} from '../../services/users.service';

@Component({
  selector: 'app-user-edit-form',
  standalone: true,
  imports: [FormsModule],
  templateUrl: './user-edit-form.component.html',
  styleUrl: './user-edit-form.component.scss'
})
export class UserEditFormComponent implements OnInit {
  user = input.required<User>();
  selectedApplicationIds = signal<Set<string>>(new Set());
  selectedRoleIds = signal<Set<string>>(new Set());
  error = signal('');
  isLoading = signal(false);
  userUpdated = output<User>();
  private usersService = inject(UsersService);
  availableApplications = this.usersService.applications;
  availableRoles = this.usersService.availableRoles;

  async ngOnInit(): Promise<void> {
    // Load applications if not already loaded
    if (this.availableApplications().length === 0) {
      await this.usersService.fetchApplications();
    }

    // Initialize selections from user's current applications and roles
    this.selectedApplicationIds.set(new Set(this.user().applications.map(a => a.id)));
    this.selectedRoleIds.set(new Set(this.user().roles.map(r => r.id)));
  }

  onSubmit(): void {
    const validationError = this.validateForm();
    if (validationError) {
      this.error.set(validationError);
      return;
    }

    this.isLoading.set(true);
    this.error.set('');

    try {
      const updatedUser = this.usersService.updateUser(
        this.user().id,
        Array.from(this.selectedApplicationIds()),
        Array.from(this.selectedRoleIds())
      );

      this.isLoading.set(false);
      if (updatedUser) {
        this.userUpdated.emit(updatedUser);
      }
    } catch (err: unknown) {
      this.isLoading.set(false);
      this.error.set(err instanceof Error ? err.message : 'Failed to update user');
    }
  }

  toggleApplication(appId: string): void {
    this.selectedApplicationIds.update(ids => {
      const newIds = new Set(ids);
      if (newIds.has(appId)) {
        newIds.delete(appId);
      } else {
        newIds.add(appId);
      }
      return newIds;
    });
  }

  toggleRole(roleId: string): void {
    this.selectedRoleIds.update(ids => {
      const newIds = new Set(ids);
      if (newIds.has(roleId)) {
        newIds.delete(roleId);
      } else {
        newIds.add(roleId);
      }
      return newIds;
    });
  }

  isApplicationSelected(appId: string): boolean {
    return this.selectedApplicationIds().has(appId);
  }

  isRoleSelected(roleId: string): boolean {
    return this.selectedRoleIds().has(roleId);
  }

  private validateForm(): string | null {
    if (this.selectedRoleIds().size === 0) {
      return 'Please select at least one role';
    }

    return null;
  }
}
