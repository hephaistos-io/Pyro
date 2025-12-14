import {Injectable, signal} from '@angular/core';

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

const MOCK_USERS: User[] = [
  {
    id: '1',
    firstName: 'Sarah',
    lastName: 'Johnson',
    email: 'sarah.johnson@example.com',
    applications: [
      {id: 'app1', name: 'Mobile App'},
      {id: 'app2', name: 'Web Portal'}
    ],
    roles: [
      {id: 'role1', name: 'Admin', type: 'admin'},
      {id: 'role2', name: 'Developer', type: 'developer'}
    ],
    lastActive: new Date('2025-12-14T10:30:00'),
    status: 'active'
  },
  {
    id: '2',
    firstName: 'Michael',
    lastName: 'Chen',
    email: 'michael.chen@example.com',
    applications: [
      {id: 'app1', name: 'Mobile App'}
    ],
    roles: [
      {id: 'role3', name: 'Viewer', type: 'viewer'}
    ],
    lastActive: new Date('2025-12-13T16:45:00'),
    status: 'active'
  },
  {
    id: '3',
    firstName: 'Emma',
    lastName: 'Rodriguez',
    email: 'emma.rodriguez@example.com',
    applications: [],
    roles: [],
    lastActive: null,
    status: 'invited'
  },
  {
    id: '4',
    firstName: 'James',
    lastName: 'Wilson',
    email: 'james.wilson@example.com',
    applications: [
      {id: 'app2', name: 'Web Portal'},
      {id: 'app3', name: 'Admin Dashboard'}
    ],
    roles: [
      {id: 'role1', name: 'Admin', type: 'admin'}
    ],
    lastActive: new Date('2025-12-14T09:15:00'),
    status: 'active'
  },
  {
    id: '5',
    firstName: 'Olivia',
    lastName: 'Martinez',
    email: 'olivia.martinez@example.com',
    applications: [
      {id: 'app1', name: 'Mobile App'},
      {id: 'app3', name: 'Admin Dashboard'}
    ],
    roles: [
      {id: 'role2', name: 'Developer', type: 'developer'}
    ],
    lastActive: new Date('2025-12-12T14:20:00'),
    status: 'active'
  },
  {
    id: '6',
    firstName: 'Liam',
    lastName: 'Taylor',
    email: 'liam.taylor@example.com',
    applications: [
      {id: 'app2', name: 'Web Portal'}
    ],
    roles: [
      {id: 'role3', name: 'Viewer', type: 'viewer'}
    ],
    lastActive: new Date('2025-12-10T11:00:00'),
    status: 'inactive'
  },
  {
    id: '7',
    firstName: 'Sophia',
    lastName: 'Anderson',
    email: 'sophia.anderson@example.com',
    applications: [
      {id: 'app1', name: 'Mobile App'},
      {id: 'app2', name: 'Web Portal'},
      {id: 'app3', name: 'Admin Dashboard'}
    ],
    roles: [
      {id: 'role2', name: 'Developer', type: 'developer'},
      {id: 'role3', name: 'Viewer', type: 'viewer'}
    ],
    lastActive: new Date('2025-12-14T08:00:00'),
    status: 'active'
  },
  {
    id: '8',
    firstName: 'Noah',
    lastName: 'Thomas',
    email: 'noah.thomas@example.com',
    applications: [],
    roles: [],
    lastActive: null,
    status: 'invited'
  }
];

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
  isLoading = signal(false);
  private usersData = signal<User[]>(MOCK_USERS);
  users = this.usersData.asReadonly();
  private availableApps = signal<Application[]>(AVAILABLE_APPLICATIONS);
  applications = this.availableApps.asReadonly();
  private availableRolesData = signal<Role[]>(AVAILABLE_ROLES);
  availableRoles = this.availableRolesData.asReadonly();

  async fetchUsers(): Promise<void> {
    this.isLoading.set(true);
    // Simulate API delay
    await new Promise(resolve => setTimeout(resolve, 500));
    this.isLoading.set(false);
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
