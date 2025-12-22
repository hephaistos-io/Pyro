import {Component} from '@angular/core';
import {RouterLink} from '@angular/router';

interface PricingTier {
  name: string;
  price: string;
  requests: string;
  rateLimit: string;
  description: string;
}

@Component({
  selector: 'app-pricing',
  standalone: true,
  imports: [RouterLink],
  templateUrl: './pricing.component.html',
  styleUrl: './pricing.component.scss'
})
export class PricingComponent {
  tiers: PricingTier[] = [
    {
      name: 'Free',
      price: '$0',
      requests: '500k requests/month',
      rateLimit: '5 req/sec',
      description: 'Perfect for dev/test'
    },
    {
      name: 'Basic',
      price: '$10',
      requests: '2M requests/month',
      rateLimit: '20 req/sec',
      description: 'Small production apps'
    },
    {
      name: 'Standard',
      price: '$40',
      requests: '10M requests/month',
      rateLimit: '100 req/sec',
      description: 'Growing startups'
    },
    {
      name: 'Pro',
      price: '$100',
      requests: '25M requests/month',
      rateLimit: '500 req/sec',
      description: 'Scale-ups and SMBs'
    },
    {
      name: 'Business',
      price: '$400',
      requests: '100M requests/month',
      rateLimit: '2,000 req/sec',
      description: 'High-traffic enterprise'
    }
  ];

  features: string[] = [
    'Unlimited users (no MAU charges)',
    'User-level configuration (not just flags)',
    'No per-seat pricing (unlimited team members)',
    'EU data residency (GDPR compliant)'
  ];
}