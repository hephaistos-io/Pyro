import {computed, Injectable, signal} from '@angular/core';

/**
 * Pricing tier options (matches backend PricingTier enum).
 */
export type PricingTier = 'FREE' | 'BASIC' | 'STANDARD' | 'PRO' | 'BUSINESS';

/**
 * Price in cents for each tier.
 */
export const TIER_PRICES: Record<PricingTier, number> = {
  FREE: 0,
  BASIC: 1000,     // $10
  STANDARD: 2500,  // $25
  PRO: 5000,       // $50
  BUSINESS: 10000  // $100
};

/**
 * Represents an environment pending payment.
 */
export interface PendingEnvironment {
  environmentId: string;
  environmentName: string;
  applicationName: string;
  tier: PricingTier;
  monthlyPriceCents: number;
}

/**
 * Service for managing the pricing/checkout cart state.
 *
 * Persists cart items to localStorage for cross-session persistence.
 * Used when users create paid environments that need to be checked out.
 */
@Injectable({providedIn: 'root'})
export class PricingStateService {
  private readonly STORAGE_KEY = 'pending_environments';

  private _pendingEnvironments = signal<PendingEnvironment[]>([]);

  /** All pending environments in the cart */
  pendingEnvironments = this._pendingEnvironments.asReadonly();

  /** Number of items in the cart */
  cartCount = computed(() => this._pendingEnvironments().length);

  /** Total monthly price in cents */
  totalMonthlyPriceCents = computed(() =>
    this._pendingEnvironments().reduce((sum, item) => sum + item.monthlyPriceCents, 0)
  );

  /** Whether the cart has any items */
  hasItems = computed(() => this._pendingEnvironments().length > 0);

  constructor() {
    this.loadFromStorage();
  }

  /**
   * Adds or updates an environment in the checkout cart.
   * If the environment already exists, updates its tier and price.
   */
  addPendingEnvironment(item: PendingEnvironment): void {
    const current = this._pendingEnvironments();
    const existingIndex = current.findIndex(e => e.environmentId === item.environmentId);

    let updated: PendingEnvironment[];
    if (existingIndex >= 0) {
      // Update existing item
      updated = [...current];
      updated[existingIndex] = item;
    } else {
      // Add new item
      updated = [...current, item];
    }

    this._pendingEnvironments.set(updated);
    this.saveToStorage();
  }

  /**
   * Removes an environment from the checkout cart.
   */
  removePendingEnvironment(environmentId: string): void {
    const updated = this._pendingEnvironments().filter(e => e.environmentId !== environmentId);
    this._pendingEnvironments.set(updated);
    this.saveToStorage();
  }

  /**
   * Clears all pending environments from the cart.
   * Called after successful checkout.
   */
  clearCart(): void {
    this._pendingEnvironments.set([]);
    this.saveToStorage();
  }

  /**
   * Gets the price in cents for a given tier.
   */
  getPriceForTier(tier: PricingTier): number {
    const prices: Record<PricingTier, number> = {
      FREE: 0,
      BASIC: 1000,     // $10
      STANDARD: 2500,  // $25
      PRO: 5000,       // $50
      BUSINESS: 10000  // $100
    };
    return prices[tier] || 0;
  }

  /**
   * Checks if a tier is paid (requires checkout).
   */
  isPaidTier(tier: PricingTier): boolean {
    return tier !== 'FREE';
  }

  private saveToStorage(): void {
    try {
      localStorage.setItem(this.STORAGE_KEY, JSON.stringify(this._pendingEnvironments()));
    } catch (e) {
      console.error('Failed to save pending environments to localStorage', e);
    }
  }

  private loadFromStorage(): void {
    try {
      const stored = localStorage.getItem(this.STORAGE_KEY);
      if (stored) {
        const parsed = JSON.parse(stored) as PendingEnvironment[];
        this._pendingEnvironments.set(parsed);
      }
    } catch (e) {
      console.error('Failed to load pending environments from localStorage', e);
      this._pendingEnvironments.set([]);
    }
  }
}
