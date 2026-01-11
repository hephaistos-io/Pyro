import {computed, inject, Injectable} from '@angular/core';
import {CustomerRole} from '../api/generated/models';
import {CustomerService} from './customer.service';

/**
 * Service for role-based access control.
 * Provides computed signals for checking user permissions.
 *
 * Role hierarchy: ReadOnly < Dev < Admin
 * - ReadOnly: Can only view applications, switch environments, and see tier (cannot change anything)
 * - Dev: Can manage templates and overrides. Can see users and environments but not manage them.
 * - Admin: Full access to everything including environment and user management
 */
@Injectable({providedIn: 'root'})
export class RoleService {
  private customerService = inject(CustomerService);

  /** Current user's role */
  readonly currentRole = computed(() => this.customerService.customerProfile()?.role as CustomerRole | undefined);

  /** Check if current user has ReadOnly role */
  readonly isReadOnly = computed(() => this.currentRole() === CustomerRole.ReadOnly);

  /** Check if current user has Dev role */
  readonly isDev = computed(() => this.currentRole() === CustomerRole.Dev);

  /** Check if current user has Admin role */
  readonly isAdmin = computed(() => this.currentRole() === CustomerRole.Admin);

  /** Check if current user has at least Dev role (Dev or Admin) */
  readonly isAtLeastDev = computed(() => {
    const role = this.currentRole();
    return role === CustomerRole.Dev || role === CustomerRole.Admin;
  });

  // ============================================================================
  // Permission Checks - These determine what UI elements to show
  // ============================================================================

  /** Can user access the Users tab in the dashboard? Dev can view, Admin can manage */
  readonly canAccessUsersTab = computed(() => this.isAtLeastDev());

  /** Can user invite, edit, or delete users? Admin only */
  readonly canManageUsers = computed(() => this.isAdmin());

  /** Can user access the Billing tab? Admin only */
  readonly canAccessBillingTab = computed(() => this.isAdmin());

  /** Can user create, edit, or delete environments? Admin only */
  readonly canManageEnvironments = computed(() => this.isAdmin());

  /** Can user change the tier of an environment? Admin only */
  readonly canChangeTier = computed(() => this.isAdmin());

  /** Can user view API keys? Dev and Admin */
  readonly canViewApiKeys = computed(() => this.isAtLeastDev());

  /** Can user add, edit, or delete template fields? Dev and Admin */
  readonly canManageTemplates = computed(() => this.isAtLeastDev());

  /** Can user add, edit, or delete overrides? Dev and Admin */
  readonly canManageOverrides = computed(() => this.isAtLeastDev());

  /** Can user create new applications? Dev and Admin */
  readonly canCreateApplications = computed(() => this.isAtLeastDev());

  /**
   * Check if user has at least the required role level.
   * Useful for dynamic checks where the required role varies.
   */
  hasAtLeastRole(requiredRole: CustomerRole): boolean {
    const role = this.currentRole();
    if (!role) return false;
    if (role === CustomerRole.Admin) return true;

    const hierarchy = [CustomerRole.ReadOnly, CustomerRole.Dev, CustomerRole.Admin];
    const userLevel = hierarchy.indexOf(role);
    const requiredLevel = hierarchy.indexOf(requiredRole);

    return userLevel >= requiredLevel;
  }
}
