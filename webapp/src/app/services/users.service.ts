import {inject, Injectable, signal} from '@angular/core';
import {Api} from '../api/generated/api';
import {getCustomers} from '../api/generated/functions';
import {CustomerResponse} from '../api/generated/models/customer-response';

export interface User {
  id: string;
  firstName: string;
  lastName: string;
  email: string;
  applications: Application[];
  roles: Role[];
  lastActive: Date | null;
  status: 'active' | 'inactive' | 'invited';
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

const AVAILABLE_APPLICATIONS: Application[] = [
  {id: 'app1', name: 'Mobile App'},
  {id: 'app2', name: 'Web Portal'},
  {id: 'app3', name: 'Admin Dashboard'},
  {id: 'app4', name: 'API Gateway'}
];

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
  private availableApps = signal<Application[]>(AVAILABLE_APPLICATIONS);
  applications = this.availableApps.asReadonly();
  private availableRolesData = signal<Role[]>(AVAILABLE_ROLES);
  availableRoles = this.availableRolesData.asReadonly();

  async fetchUsers(): Promise<void> {
    this.isLoading.set(true);
    try {
      const customers = await this.api.invoke(getCustomers);
      const users = customers.map(customer => this.mapCustomerToUser(customer));
      this.usersData.set(users);
    } finally {
      this.isLoading.set(false);
    }
  }

  private mapCustomerToUser(customer: CustomerResponse): User {
    return {
      id: customer.email ?? crypto.randomUUID(),
      firstName: customer.firstName ?? 'Unknown',
      lastName: customer.lastName ?? 'User',
      email: customer.email ?? '',
      applications: [],
      roles: this.mapRole(customer.role),
      lastActive: null,
      status: 'active'
    };
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

  updateUser(userId: string, applicationIds: string[], roleIds: string[]): User | null {
    const selectedApps = this.availableApps().filter(app => applicationIds.includes(app.id));
    const selectedRoles = this.availableRolesData().filter(role => roleIds.includes(role.id));

    let updatedUser: User | null = null;

    this.usersData.update(users =>
      users.map(u => {
        if (u.id !== userId) return u;
        updatedUser = {
          ...u,
          applications: selectedApps,
          roles: selectedRoles
        };
        return updatedUser;
      })
    );

    return updatedUser;
  }
}
