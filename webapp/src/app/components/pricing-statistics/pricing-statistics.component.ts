import {Component, computed, inject, OnInit, signal} from '@angular/core';
import {CommonModule} from '@angular/common';
import {Api} from '../../api/generated/api';
import {getCompanyStatistics} from '../../api/generated/fn/company/get-company-statistics';
import {ApplicationStatistics} from '../../api/generated/models/application-statistics';

@Component({
  selector: 'app-pricing-statistics',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './pricing-statistics.component.html',
  styleUrl: './pricing-statistics.component.scss'
})
export class PricingStatisticsComponent implements OnInit {
  statistics = signal<ApplicationStatistics[]>([]);
  totalMonthlyPrice = signal<number>(0);
  isLoading = signal(true);
  maxAppCost = computed(() => {
    const costs = this.statistics().map(app => app.totalMonthlyPriceUsd || 0);
    return Math.max(1, ...costs);
  });
  private api = inject(Api);

  async ngOnInit(): Promise<void> {
    await this.fetchStatistics();
  }

  private async fetchStatistics(): Promise<void> {
    try {
      this.isLoading.set(true);
      const response = await this.api.invoke(getCompanyStatistics, {});
      this.statistics.set(response.applications || []);
      this.totalMonthlyPrice.set(response.totalMonthlyPriceUsd || 0);
    } catch (error) {
      console.error('Failed to fetch pricing statistics:', error);
      this.statistics.set([]);
      this.totalMonthlyPrice.set(0);
    } finally {
      this.isLoading.set(false);
    }
  }
}
