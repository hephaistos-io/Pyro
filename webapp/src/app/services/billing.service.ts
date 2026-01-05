import {inject, Injectable, signal} from '@angular/core';
import {HttpClient} from '@angular/common/http';
import {firstValueFrom} from 'rxjs';
import {ApiConfiguration} from '../api/generated/api-configuration';
import {PricingTier} from './pricing-state.service';

/**
 * Request to create a Stripe Checkout session.
 */
export interface CheckoutRequest {
  pendingEnvironments: PendingEnvironmentDto[];
  successUrl: string;
  cancelUrl: string;
}

export interface PendingEnvironmentDto {
  environmentId: string;
  environmentName: string;
  tier: PricingTier;
}

/**
 * Response from creating a checkout session.
 */
export interface CheckoutSessionResponse {
  sessionId: string;
  checkoutUrl: string;
}

/**
 * Response containing Stripe publishable key.
 */
export interface PaymentConfigResponse {
  publishableKey: string;
}

/**
 * Response from creating a customer portal session.
 */
export interface PortalSessionResponse {
  url: string;
}

/**
 * Subscription status response.
 */
export interface SubscriptionStatusResponse {
  status: string;
  totalMonthlyPriceCents: number;
  currentPeriodEnd: string;
  cancelAtPeriodEnd: boolean;
  items: SubscriptionItemDto[];
}

export interface SubscriptionItemDto {
  environmentId: string;
  environmentName: string;
  tier: string;
  monthlyPriceCents: number;
}

/**
 * Invoice response from payment history.
 */
export interface InvoiceResponse {
  id: string;
  amountCents: number;
  currency: string;
  status: string;
  invoicePdfUrl?: string;
  hostedInvoiceUrl?: string;
  periodStart?: string;
  periodEnd?: string;
  paidAt?: string;
  createdAt?: string;
}

/**
 * Service for managing billing and payment operations.
 *
 * Provides methods for:
 * - Creating Stripe Checkout sessions
 * - Creating Customer Portal sessions
 * - Fetching subscription status
 * - Fetching invoices
 */
@Injectable({providedIn: 'root'})
export class BillingService {
  /** Current subscription status */
  subscriptionStatus = signal<SubscriptionStatusResponse | null>(null);
  /** Loading state */
  loading = signal(false);
  private http = inject(HttpClient);
  private config = inject(ApiConfiguration);

  private get baseUrl(): string {
    return this.config.rootUrl;
  }

  /**
   * Gets the Stripe publishable key for client-side integration.
   */
  async getPaymentConfig(): Promise<PaymentConfigResponse> {
    return firstValueFrom(
      this.http.get<PaymentConfigResponse>(`${this.baseUrl}/v1/payment/config`)
    );
  }

  /**
   * Creates or gets the Stripe customer for the current company.
   * Should be called before checkout to ensure a customer exists.
   */
  async ensureCustomer(companyName: string, email: string): Promise<{ customerId: string }> {
    return firstValueFrom(
      this.http.post<{ customerId: string }>(`${this.baseUrl}/v1/payment/customer`, {
        companyName,
        email
      })
    );
  }

  /**
   * Creates a Stripe Checkout session for the pending environments.
   * Redirects the user to Stripe's hosted checkout page.
   */
  async createCheckoutSession(request: CheckoutRequest): Promise<CheckoutSessionResponse> {
    this.loading.set(true);
    try {
      return await firstValueFrom(
        this.http.post<CheckoutSessionResponse>(`${this.baseUrl}/v1/payment/checkout`, request)
      );
    } finally {
      this.loading.set(false);
    }
  }

  /**
   * Creates a Stripe Customer Portal session.
   * Returns a URL to redirect the user to manage their billing.
   */
  async createPortalSession(returnUrl: string): Promise<string> {
    this.loading.set(true);
    try {
      const response = await firstValueFrom(
        this.http.post<PortalSessionResponse>(`${this.baseUrl}/v1/payment/portal`, {
          returnUrl
        })
      );
      return response.url;
    } finally {
      this.loading.set(false);
    }
  }

  /**
   * Fetches the current subscription status.
   */
  async getSubscriptionStatus(): Promise<SubscriptionStatusResponse | null> {
    this.loading.set(true);
    try {
      const status = await firstValueFrom(
        this.http.get<SubscriptionStatusResponse | null>(`${this.baseUrl}/v1/payment/subscription`)
      );
      this.subscriptionStatus.set(status);
      return status;
    } finally {
      this.loading.set(false);
    }
  }

  /**
   * Fetches invoice history.
   */
  async getInvoices(limit = 10): Promise<InvoiceResponse[]> {
    this.loading.set(true);
    try {
      return await firstValueFrom(
        this.http.get<InvoiceResponse[]>(`${this.baseUrl}/v1/payment/invoices`, {
          params: {limit: limit.toString()}
        })
      );
    } finally {
      this.loading.set(false);
    }
  }

  /**
   * Updates an environment's pricing tier.
   */
  async updateEnvironmentTier(environmentId: string, tier: PricingTier): Promise<void> {
    this.loading.set(true);
    try {
      await firstValueFrom(
        this.http.put(`${this.baseUrl}/v1/payment/environments/${environmentId}/tier`, {
          tier
        })
      );
    } finally {
      this.loading.set(false);
    }
  }

  /**
   * Redirects to Stripe Checkout.
   * Call this after createCheckoutSession to navigate the user.
   */
  redirectToCheckout(checkoutUrl: string): void {
    window.location.href = checkoutUrl;
  }

  /**
   * Redirects to Stripe Customer Portal.
   */
  redirectToPortal(portalUrl: string): void {
    window.location.href = portalUrl;
  }

  /**
   * Completes a checkout session (triggers subscription activation in mock mode).
   * In production, this is handled by Stripe webhooks.
   */
  async completeCheckoutSession(sessionId: string): Promise<void> {
    await firstValueFrom(
      this.http.post(`${this.baseUrl}/v1/payment/checkout/complete`, {sessionId})
    );
  }
}
