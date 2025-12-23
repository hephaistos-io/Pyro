import {inject, Injectable, signal} from '@angular/core';
import {Api} from '../api/generated/api';
import {getTeam} from '../api/generated/fn/customer/get-team';
import {regenerateInvite} from '../api/generated/fn/invite/regenerate-invite';
import {deleteInvite} from '../api/generated/fn/invite/delete-invite';
import {getApplications} from '../api/generated/fn/application/get-applications';
import {updateCustomer} from '../api/generated/fn/customer/update-customer';
import {CustomerResponse} from '../api/generated/models/customer-response';
import {PendingInviteResponse} from '../api/generated/models/pending-invite-response';
import {InviteCreationResponse} from '../api/generated/models/invite-creation-response';
import {ApplicationListResponse} from '../api/generated/models/application-list-response';
import {CustomerRole} from '../api/generated/models/customer-role';

export interface User {
  id: string;
  firstName: string;
  lastName: string;
  email: string;
  applications: Application[];
  roles: Role[];
  lastActive: Date | null;
  status: 'active' | 'inactive' | 'invited' | 'expired';
}

export interface Application {
  id: string;
  name: string;
}

export interface Role {
  id: string;
  name: string;
  type: 'admin' | 'developer' | 'viewer';
}

const AVAILABLE_ROLES: Role[] = [
  {id: 'role1', name: 'Admin', type: 'admin'},
  {id: 'role2', name: 'Developer', type: 'developer'},
  {id: 'role3', name: 'Viewer', type: 'viewer'}
];

@Injectable({
  providedIn: 'root'
})
export class UsersService {
  private api = inject(Api);

  isLoading = signal(false);
  private usersData = signal<User[]>([]);
  users = this.usersData.asReadonly();
  private availableApps = signal<Application[]>([]);
  applications = this.availableApps.asReadonly();
  private availableRolesData = signal<Role[]>(AVAILABLE_ROLES);
  availableRoles = this.availableRolesData.asReadonly();

  async fetchUsers(): Promise<void> {
    this.isLoading.set(true);
    try {
      // Ensure applications are loaded first so we can map invite applicationIds to names
      if (this.availableApps().length === 0) {
        await this.fetchApplications();
      }

      const team = await this.api.invoke(getTeam);
      const members = (team.members ?? []).map(customer => this.mapCustomerToUser(customer));
      const invites = (team.pendingInvites ?? []).map(invite => this.mapInviteToUser(invite));
      this.usersData.set([...members, ...invites]);
    } finally {
      this.isLoading.set(false);
    }
  }

  async fetchApplications(): Promise<void> {
    try {
      const apps = await this.api.invoke(getApplications, {});
      this.availableApps.set(apps.map(app => this.mapApplicationListResponseToApplication(app)));
    } catch {
      this.availableApps.set([]);
    }
  }

  private mapApplicationListResponseToApplication(app: ApplicationListResponse): Application {
    return {
      id: app.id ?? '',
      name: app.name ?? ''
    };
  }

  async updateUser(userId: string, applicationIds: string[], roleIds: string[]): Promise<User | null> {
    // Map frontend role IDs to backend role enum
    const roleTypeMap: Record<string, CustomerRole> = {
      'role1': CustomerRole.Admin,
      'role2': CustomerRole.Dev,
      'role3': CustomerRole.ReadOnly
    };

    // Get the role (assuming single role)
    const role = roleIds.length > 0 ? roleTypeMap[roleIds[0]] : undefined;

    try {
      // Call backend API to update user
      await this.api.invoke(updateCustomer, {
        customerId: userId,
        body: {
          applicationIds,
          role
        }
      });

      // Refresh users list from backend
      await this.fetchUsers();

      // Find and return the updated user
      return this.users().find(u => u.id === userId) ?? null;
    } catch (error) {
      console.error('Failed to update user:', error);
      throw error;
    }
  }

  private mapRole(role?: 'READ_ONLY' | 'DEV' | 'ADMIN'): Role[] {
    if (!role) return [];
    const roleMap: Record<string, Role> = {
      'ADMIN': {id: 'role1', name: 'Admin', type: 'admin'},
      'DEV': {id: 'role2', name: 'Developer', type: 'developer'},
      'READ_ONLY': {id: 'role3', name: 'Viewer', type: 'viewer'}
    };
    return roleMap[role] ? [roleMap[role]] : [];
  }

  async regenerateInvite(inviteId: string): Promise<InviteCreationResponse> {
    return this.api.invoke(regenerateInvite, {id: inviteId});
  }

  createUser(email: string, applicationIds: string[], roleIds: string[]): User {
    // Check if user with this email already exists
    if (this.users().some(u => u.email.toLowerCase() === email.toLowerCase())) {
      throw new Error('A user with this email already exists');
    }

    // Get selected applications and roles
    const selectedApps = this.availableApps().filter(app => applicationIds.includes(app.id));
    const selectedRoles = this.availableRolesData().filter(role => roleIds.includes(role.id));

    // Generate a unique ID
    const newId = `user-${Date.now()}-${Math.random().toString(36).slice(2, 11)}`;

    // Extract first and last name from email (simple approach)
    const emailParts = email.split('@')[0].split('.');
    const firstName = emailParts[0] ? emailParts[0].charAt(0).toUpperCase() + emailParts[0].slice(1) : 'New';
    const lastName = emailParts[1] ? emailParts[1].charAt(0).toUpperCase() + emailParts[1].slice(1) : 'User';

    const newUser: User = {
      id: newId,
      firstName,
      lastName,
      email,
      applications: selectedApps,
      roles: selectedRoles,
      lastActive: null,
      status: 'invited'
    };

    // Add to users array
    this.usersData.update(users => [...users, newUser]);

    return newUser;
  }

  removeUser(userId: string): void {
    this.usersData.update(users => users.filter(u => u.id !== userId));
  }

  async deleteInvite(inviteId: string): Promise<void> {
    await this.api.invoke(deleteInvite, {id: inviteId});
    this.usersData.update(users => users.filter(u => u.id !== inviteId));
  }

  private mapInviteToUser(invite: PendingInviteResponse): User {
    const isExpired = invite.expiresAt ? new Date(invite.expiresAt) < new Date() : false;
    const emailParts = invite.email?.split('@')[0]?.split('.') ?? ['Invited', 'User'];
    const firstName = emailParts[0] ? emailParts[0].charAt(0).toUpperCase() + emailParts[0].slice(1) : 'Invited';
    const lastName = emailParts[1] ? emailParts[1].charAt(0).toUpperCase() + emailParts[1].slice(1) : 'User';

    // Map applicationIds to Application objects using the availableApps list
    const applications = (invite.applicationIds ?? [])
      .map(appId => this.availableApps().find(app => app.id === appId))
      .filter((app): app is Application => app !== undefined);

    return {
      id: invite.id ?? crypto.randomUUID(),
      firstName,
      lastName,
      email: invite.email ?? '',
      applications,
      roles: this.mapRole(invite.role),
      lastActive: null,
      status: isExpired ? 'expired' : 'invited'
    };
  }

  private mapCustomerToUser(customer: CustomerResponse): User {
    const applications = (customer.applications ?? []).map(app => ({
      id: app.id ?? '',
      name: app.name ?? ''
    }));

    return {
      id: customer.id ?? crypto.randomUUID(),
      firstName: customer.firstName ?? 'Unknown',
      lastName: customer.lastName ?? 'User',
      email: customer.email ?? '',
      applications,
      roles: this.mapRole(customer.role),
      lastActive: null,
      status: 'active'
    };
  }
}
