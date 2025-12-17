import {Directive, effect, inject, Input, TemplateRef, ViewContainerRef} from '@angular/core';
import {CustomerRole} from '../api/generated/models';
import {CustomerService} from '../services/customer.service';

// Ordered from lowest to highest privileges. Admin is always last.
const ROLE_HIERARCHY: readonly CustomerRole[] = [
  CustomerRole.ReadOnly,
  CustomerRole.Dev,
  CustomerRole.Admin,
];

@Directive({
  selector: '[hasRole]',
  standalone: true,
})
export class HasRoleDirective {
  private templateRef = inject(TemplateRef<unknown>);
  private viewContainer = inject(ViewContainerRef);
  private customerService = inject(CustomerService);

  private hasView = false;
  private requiredRole: CustomerRole = CustomerRole.ReadOnly;

  constructor() {
    effect(() => {
      this.customerService.customerProfile();
      this.updateView();
    });
  }

  @Input() set hasRole(role: CustomerRole) {
    this.requiredRole = role;
    this.updateView();
  }

  private updateView(): void {
    const userRole = this.customerService.customerProfile()?.role as CustomerRole | undefined;
    const hasAccess = this.checkRoleAccess(userRole, this.requiredRole);

    if (hasAccess && !this.hasView) {
      this.viewContainer.createEmbeddedView(this.templateRef);
      this.hasView = true;
    } else if (!hasAccess && this.hasView) {
      this.viewContainer.clear();
      this.hasView = false;
    }
  }

  private checkRoleAccess(userRole: CustomerRole | undefined, requiredRole: CustomerRole): boolean {
    if (!userRole) return false;

    // Admin always has access to everything
    if (userRole === CustomerRole.Admin) return true;

    const userLevel = ROLE_HIERARCHY.indexOf(userRole);
    const requiredLevel = ROLE_HIERARCHY.indexOf(requiredRole);

    if (userLevel === -1 || requiredLevel === -1) return false;

    return userLevel >= requiredLevel;
  }
}
