import {Component, inject, OnInit, signal} from '@angular/core';
import {CommonModule} from '@angular/common';
import {BillingService, InvoiceResponse, SubscriptionStatusResponse} from '../../services/billing.service';

@Component({
  selector: 'app-billing-tab',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './billing-tab.component.html',
  styleUrl: './billing-tab.component.scss'
})
export class BillingTabComponent implements OnInit {
  subscription = signal<SubscriptionStatusResponse | null>(null);
  invoices = signal<InvoiceResponse[]>([]);
  error = signal('');
  private billingService = inject(BillingService);
  loading = this.billingService.loading;

  async ngOnInit(): Promise<void> {
    await this.loadBillingData();
  }

  async loadBillingData(): Promise<void> {
    this.error.set('');
    try {
      const [subscriptionStatus, invoiceList] = await Promise.all([
        this.billingService.getSubscriptionStatus(),
        this.billingService.getInvoices(10)
      ]);
      this.subscription.set(subscriptionStatus);
      this.invoices.set(invoiceList);
    } catch (e) {
      console.error('Failed to load billing data:', e);
      this.error.set('Failed to load billing information. Please try again.');
    }
  }

  formatPrice(cents: number): string {
    return `$${(cents / 100).toFixed(2)}`;
  }

  formatDate(dateStr: string | undefined): string {
    if (!dateStr) return '-';
    return new Date(dateStr).toLocaleDateString('en-US', {
      year: 'numeric',
      month: 'short',
      day: 'numeric'
    });
  }

  getStatusClass(status: string): string {
    switch (status?.toLowerCase()) {
      case 'active':
        return 'status-active';
      case 'past_due':
        return 'status-warning';
      case 'canceled':
      case 'unpaid':
        return 'status-error';
      default:
        return '';
    }
  }

  getInvoiceStatusClass(status: string): string {
    switch (status?.toLowerCase()) {
      case 'paid':
        return 'status-paid';
      case 'open':
        return 'status-open';
      case 'draft':
        return 'status-draft';
      default:
        return '';
    }
  }

  async openCustomerPortal(): Promise<void> {
    try {
      const returnUrl = window.location.href;
      const portalUrl = await this.billingService.createPortalSession(returnUrl);
      this.billingService.redirectToPortal(portalUrl);
    } catch (e) {
      console.error('Failed to open customer portal:', e);
      this.error.set('Failed to open billing portal. Please try again.');
    }
  }
}
