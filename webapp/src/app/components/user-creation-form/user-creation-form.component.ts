import {Component, inject, OnInit, output, signal} from '@angular/core';
import {FormsModule} from '@angular/forms';
import {Api} from '../../api/generated/api';
import {createInvite} from '../../api/generated/functions';
import {getApplications} from '../../api/generated/fn/application/get-applications';
import {ApplicationResponse} from '../../api/generated/models/application-response';
import {InviteCreationResponse} from '../../api/generated/models/invite-creation-response';

type RoleType = 'READ_ONLY' | 'DEV' | 'ADMIN';

interface RoleOption {
  value: RoleType;
  label: string;
  description: string;
}

const ROLE_OPTIONS: RoleOption[] = [
  {value: 'ADMIN', label: 'Admin', description: 'Full access to all features'},
  {value: 'DEV', label: 'Developer', description: 'Can manage flags and environments'},
  {value: 'READ_ONLY', label: 'Viewer', description: 'Read-only access'}
];

@Component({
  selector: 'app-user-creation-form',
  standalone: true,
  imports: [FormsModule],
  templateUrl: './user-creation-form.component.html',
  styleUrl: './user-creation-form.component.scss'
})
export class UserCreationFormComponent implements OnInit {
  // Form state
  email = signal('');
  selectedRole = signal<RoleType | null>(null);
  // Data
  applications = signal<ApplicationResponse[]>([]);
  selectedApplicationIds = signal<Set<string>>(new Set());
  error = signal('');
  isLoading = signal(false);
  roleOptions = ROLE_OPTIONS;
  // Success state
  inviteCreated = signal<InviteCreationResponse | null>(null);
  copied = signal(false);
  // Events
  userCreated = output<void>();
  private api = inject(Api);

  async ngOnInit(): Promise<void> {
    await this.loadApplications();
  }

  async onSubmit(): Promise<void> {
    const validationError = this.validateForm();
    if (validationError) {
      this.error.set(validationError);
      return;
    }

    this.isLoading.set(true);
    this.error.set('');

    try {
      const response = await this.api.invoke(createInvite, {
        body: {
          email: this.email().trim(),
          role: this.selectedRole()!,
          applicationIds: Array.from(this.selectedApplicationIds())
        }
      });

      this.inviteCreated.set(response);
      this.isLoading.set(false);
    } catch (err: unknown) {
      this.isLoading.set(false);
      this.error.set(err instanceof Error ? err.message : 'Failed to create invite');
    }
  }

  selectRole(role: RoleType): void {
    this.selectedRole.set(role);
  }

  async copyInviteUrl(): Promise<void> {
    const url = this.inviteCreated()?.inviteUrl;
    if (url) {
      await navigator.clipboard.writeText(url);
      this.copied.set(true);
      setTimeout(() => this.copied.set(false), 2000);
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

  isApplicationSelected(appId: string): boolean {
    return this.selectedApplicationIds().has(appId);
  }

  formatExpirationDate(isoDate?: string): string {
    if (!isoDate) return 'Unknown';
    const date = new Date(isoDate);
    return date.toLocaleDateString('en-US', {
      weekday: 'long',
      year: 'numeric',
      month: 'long',
      day: 'numeric',
      hour: '2-digit',
      minute: '2-digit'
    });
  }

  onDone(): void {
    this.userCreated.emit();
  }

  private async loadApplications(): Promise<void> {
    try {
      const apps = await this.api.invoke(getApplications, {});
      this.applications.set(apps);
    } catch {
      // Silently fail - applications are optional
      this.applications.set([]);
    }
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

    if (!this.selectedRole()) {
      return 'Please select a role';
    }

    return null;
  }
}
