import {Component, inject} from '@angular/core';
import {DomSanitizer, SafeHtml} from '@angular/platform-browser';

interface Feature {
  icon: SafeHtml;
  title: string;
  description: string;
}

@Component({
  selector: 'app-features',
  standalone: true,
  templateUrl: './features.component.html',
  styleUrl: './features.component.scss'
})
export class FeaturesComponent {
  private sanitizer = inject(DomSanitizer);
  features: Feature[];

  constructor() {
    this.features = [
      {
        icon: this.sanitizer.bypassSecurityTrustHtml(`<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
          <circle cx="12" cy="12" r="3"/>
          <path d="M12 1v8m0 6v8M1 12h8m6 0h8"/>
        </svg>`),
        title: 'Granular Targeting',
        description: 'Target features by user, segment, percentage, geography, device, or any custom attribute you define.'
      },
      {
        icon: this.sanitizer.bypassSecurityTrustHtml(`<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
          <path d="M13 2L3 14h9l-1 8 10-12h-9l1-8z"/>
        </svg>`),
        title: 'Instant Propagation',
        description: 'Changes go live in milliseconds. No deployments, no cache delays. Just flip the switch.'
      },
      {
        icon: this.sanitizer.bypassSecurityTrustHtml(`<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
          <path d="M16 18l6-6-6-6"/>
          <path d="M8 6l-6 6 6 6"/>
        </svg>`),
        title: 'Simple Integration',
        description: 'OpenAPI definitions that make sense and work, so you can build with the tools you know.'
      },
      {
        icon: this.sanitizer.bypassSecurityTrustHtml(`<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
          <rect x="8" y="8" width="12" height="12" rx="2"/>
          <rect x="5" y="5" width="12" height="12" rx="2"/>
          <rect x="2" y="2" width="12" height="12" rx="2"/>
        </svg>`),
        title: 'Template Everything',
        description: 'Define different feature sets for different target groups, we handle the matching'
      },
      {
        icon: this.sanitizer.bypassSecurityTrustHtml(`<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
          <path d="M17 21v-2a4 4 0 00-4-4H5a4 4 0 00-4 4v2"/>
          <circle cx="9" cy="7" r="4"/>
          <path d="M23 21v-2a4 4 0 00-3-3.87M16 3.13a4 4 0 010 7.75"/>
        </svg>`),
        title: 'Team Collaboration',
        description: 'Role-based access, approval workflows, and environment isolation. Built for teams.'
      },
      {
        icon: this.sanitizer.bypassSecurityTrustHtml(`<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
          <path d="M18 20V10M12 20V4M6 20v-6"/>
        </svg>`),
        title: 'Built-in Analytics',
        description: 'See how features perform. Track adoption, measure impact, and make data-driven decisions.'
      }
    ];
  }
}
