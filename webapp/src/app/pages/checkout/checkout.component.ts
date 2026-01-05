import {Component, computed, inject} from '@angular/core';
import {CommonModule} from '@angular/common';
import {Router, RouterLink} from '@angular/router';
import {PricingStateService} from '../../services/pricing-state.service';
import {BillingService} from '../../services/billing.service';
import {CustomerService} from '../../services/customer.service';

@Component({
  selector: 'app-checkout',
  standalone: true,
  imports: [CommonModule, RouterLink],
  templateUrl: './checkout.component.html',
  styleUrl: './checkout.component.scss'
})
export class CheckoutComponent {
  error = '';
  checkoutInProgress = false;
  private pricingState = inject(PricingStateService);
  pendingEnvironments = this.pricingState.pendingEnvironments;
  totalMonthlyPriceCents = this.pricingState.totalMonthlyPriceCents;
  totalFormatted = computed(() => {
    const cents = this.totalMonthlyPriceCents();
    return `$${(cents / 100).toFixed(2)}`;
  });
  hasItems = this.pricingState.hasItems;
  private billingService = inject(BillingService);
  loading = this.billingService.loading;
  private customerService = inject(CustomerService);
  private router = inject(Router);

  formatPrice(cents: number): string {
    return `$${(cents / 100).toFixed(2)}`;
  }

  removeItem(environmentId: string): void {
    this.pricingState.removePendingEnvironment(environmentId);
    // Show empty cart state instead of redirecting
  }

  async proceedToCheckout(): Promise<void> {
    if (this.checkoutInProgress) return;

    this.error = '';
    this.checkoutInProgress = true;

    try {
      // Ensure we have a Stripe customer
      const company = this.customerService.customerCompany();
      const profile = this.customerService.customerProfile();

      if (!company || !profile) {
        this.error = 'Unable to load company information. Please try again.';
        this.checkoutInProgress = false;
        return;
      }

      await this.billingService.ensureCustomer(company.name ?? 'Company', profile.email ?? '');

      // Create checkout session
      const items = this.pendingEnvironments();
      const baseUrl = window.location.origin;

      const response = await this.billingService.createCheckoutSession({
        pendingEnvironments: items.map(item => ({
          environmentId: item.environmentId,
          environmentName: item.environmentName,
          tier: item.tier
        })),
        successUrl: `${baseUrl}/dashboard/checkout/success`,
        cancelUrl: `${baseUrl}/dashboard/checkout`
      });

      // Check if the URL is internal (same origin) - happens in mock mode
      if (response.checkoutUrl.startsWith(baseUrl)) {
        // Use Angular Router for internal navigation to preserve app state
        const url = new URL(response.checkoutUrl);
        await this.router.navigate([url.pathname], {
          queryParams: {session_id: url.searchParams.get('session_id')}
        });
      } else {
        // External URL (Stripe Checkout) - use full page redirect
        this.billingService.redirectToCheckout(response.checkoutUrl);
      }

    } catch (e: unknown) {
      console.error('Checkout error:', e);
      this.error = e instanceof Error ? e.message : 'An error occurred during checkout. Please try again.';
      this.checkoutInProgress = false;
    }
  }
}
