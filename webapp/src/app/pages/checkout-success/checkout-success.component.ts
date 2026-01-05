import {Component, inject, OnInit, signal} from '@angular/core';
import {CommonModule} from '@angular/common';
import {ActivatedRoute, RouterLink} from '@angular/router';
import {PricingStateService} from '../../services/pricing-state.service';
import {BillingService} from '../../services/billing.service';

@Component({
  selector: 'app-checkout-success',
  standalone: true,
  imports: [CommonModule, RouterLink],
  templateUrl: './checkout-success.component.html',
  styleUrl: './checkout-success.component.scss'
})
export class CheckoutSuccessComponent implements OnInit {
  isProcessing = signal(true);
  error = signal<string | null>(null);
  private pricingState = inject(PricingStateService);
  private billingService = inject(BillingService);
  private route = inject(ActivatedRoute);

  async ngOnInit(): Promise<void> {
    // Get session ID from query params
    const sessionId = this.route.snapshot.queryParamMap.get('session_id');

    if (sessionId) {
      try {
        // Complete the checkout session (activates subscription in mock mode)
        await this.billingService.completeCheckoutSession(sessionId);
      } catch (e) {
        console.error('Failed to complete checkout session:', e);
        this.error.set('There was an issue activating your subscription. Please contact support.');
      }
    }

    // Clear the cart on successful checkout
    this.pricingState.clearCart();
    this.isProcessing.set(false);
  }
}
