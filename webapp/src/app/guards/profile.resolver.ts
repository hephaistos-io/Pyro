import {inject} from '@angular/core';
import {ResolveFn} from '@angular/router';
import {CustomerService} from '../services/customer.service';
import {CustomerResponse} from '../api/generated/models';

/**
 * Resolver that ensures the customer profile is loaded before activating a route.
 * This is needed for role-based UI elements (HasRoleDirective) to work correctly
 * when navigating directly to protected routes.
 */
export const profileResolver: ResolveFn<CustomerResponse | null> = async () => {
  const customerService = inject(CustomerService);

  // Only fetch if not already loaded
  if (!customerService.customerProfile()) {
    return customerService.fetchProfile();
  }

  return customerService.customerProfile();
};
