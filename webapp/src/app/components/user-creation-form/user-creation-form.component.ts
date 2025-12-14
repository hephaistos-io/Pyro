import {Component, inject, output, signal} from '@angular/core';
import {FormsModule} from '@angular/forms';
import {User, UsersService} from '../../services/users.service';

@Component({
  selector: 'app-user-creation-form',
  standalone: true,
  imports: [FormsModule],
  templateUrl: './user-creation-form.component.html',
  styleUrl: './user-creation-form.component.scss'
})
export class UserCreationFormComponent {
  email = signal('');
  selectedApplicationIds = signal<Set<string>>(new Set());
  selectedRoleIds = signal<Set<string>>(new Set());
  error = signal('');
  isLoading = signal(false);
  userCreated = output<User>();
  private usersService = inject(UsersService);
  availableApplications = this.usersService.applications;
  availableRoles = this.usersService.availableRoles;

  onSubmit(): void {
    const validationError = this.validateForm();
    if (validationError) {
      this.error.set(validationError);
      return;
    }

    this.isLoading.set(true);
    this.error.set('');

    try {
      const user = this.usersService.createUser(
        this.email().trim(),
        Array.from(this.selectedApplicationIds()),
        Array.from(this.selectedRoleIds())
      );

      this.isLoading.set(false);
      this.userCreated.emit(user);
    } catch (err: unknown) {
      this.isLoading.set(false);
      this.error.set(err instanceof Error ? err.message : 'Failed to create user');
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
    const email = this.email().trim();

    if (!email) {
      return 'Email is required';
    }

    const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
    if (!emailRegex.test(email)) {
      return 'Please enter a valid email address';
    }

    if (this.selectedRoleIds().size === 0) {
      return 'Please select at least one role';
    }

    return null;
  }
}
