import {Component, computed, effect, inject, input, signal} from '@angular/core';
import {DatePipe, DecimalPipe} from '@angular/common';
import {Api} from '../../api/generated/api';
import {getDailyStatistics} from '../../api/generated/functions';
import {DailyUsageStatisticsResponse} from '../../api/generated/models/daily-usage-statistics-response';

type DaysPeriod = 7 | 15 | 30;

@Component({
  selector: 'app-usage-stats-card',
  standalone: true,
  imports: [DatePipe, DecimalPipe],
  templateUrl: './usage-stats-card.component.html',
  styleUrl: './usage-stats-card.component.scss'
})
export class UsageStatsCardComponent {
  // Inputs
  applicationId = input.required<string>();
  environmentId = input.required<string>();
  // Period selector
  selectedDays = signal<DaysPeriod>(7);

  // Card collapse state
  expanded = signal(true);
  // Loading state
  loading = signal(false);
  // Hovered bar index for tooltip
  hoveredBarIndex = signal<number | null>(null);
  // Data from API
  dailyStats = signal<DailyUsageStatisticsResponse[]>([]);
  // Today's stats (most recent entry - last in the reversed array)
  todayStats = computed(() => {
    const stats = this.dailyStats();
    return stats.length > 0 ? stats[stats.length - 1] : null;
  });
  // Maximum requests in the period (for bar scaling)
  maxRequests = computed(() => {
    const stats = this.dailyStats();
    return Math.max(...stats.map(s => s.totalRequests ?? 0), 1);
  });
  // Y-axis ticks for the chart
  yAxisTicks = computed(() => {
    const max = this.maxRequests();
    if (max <= 10) return [0, Math.ceil(max / 2), max];
    // Round to nice numbers
    const magnitude = Math.pow(10, Math.floor(Math.log10(max)));
    const normalized = max / magnitude;
    let step: number;
    if (normalized <= 2) step = 0.5 * magnitude;
    else if (normalized <= 5) step = magnitude;
    else step = 2 * magnitude;
    const ticks: number[] = [];
    for (let i = 0; i <= max; i += step) {
      ticks.push(Math.round(i));
    }
    if (ticks[ticks.length - 1] < max) {
      ticks.push(Math.ceil(max / step) * step);
    }
    return ticks.slice(0, 4); // Max 4 ticks
  });
  // Total requests in the selected period
  totalRequests = computed(() => {
    return this.dailyStats().reduce((sum, s) => sum + (s.totalRequests ?? 0), 0);
  });
  // Peak requests per second across the period
  periodPeakRps = computed(() => {
    const stats = this.dailyStats();
    return Math.max(...stats.map(s => s.peakRequestsPerSecond ?? 0), 0);
  });
  // Rejection rate for today (percentage of rejected requests)
  rejectionRate = computed(() => {
    const stats = this.todayStats();
    if (!stats || !stats.totalRequests) return 0;
    const total = (stats.totalRequests ?? 0) + (stats.rejectedRequests ?? 0);
    if (total === 0) return 0;
    return ((stats.rejectedRequests ?? 0) / total) * 100;
  });
  // Dependencies
  private api = inject(Api);

  constructor() {
    // Effect to reload data when environmentId or selectedDays changes
    effect(() => {
      const envId = this.environmentId();
      const appId = this.applicationId();
      const days = this.selectedDays();
      if (envId && appId) {
        this.loadStatistics(appId, envId, days);
      }
    });
  }

  toggleCard(): void {
    this.expanded.update(v => !v);
  }

  setDays(days: DaysPeriod): void {
    this.selectedDays.set(days);
  }

  formatNumber(num: number): string {
    return num.toLocaleString();
  }

  formatDecimal(num: number | undefined): string {
    if (num === undefined || num === null) return '0';
    return num.toFixed(2);
  }

  formatCompact(num: number): string {
    if (num >= 1000000) return (num / 1000000).toFixed(1) + 'M';
    if (num >= 1000) return (num / 1000).toFixed(num >= 10000 ? 0 : 1) + 'K';
    return num.toString();
  }

  getBarHeight(requests: number | undefined): number {
    const value = requests ?? 0;
    return (value / this.maxRequests()) * 100;
  }

  onBarHover(index: number): void {
    this.hoveredBarIndex.set(index);
  }

  onBarLeave(): void {
    this.hoveredBarIndex.set(null);
  }

  private async loadStatistics(applicationId: string, environmentId: string, days: number): Promise<void> {
    this.loading.set(true);
    try {
      const stats = await this.api.invoke(getDailyStatistics, {
        applicationId,
        environmentId,
        days
      });
      // Reverse to show oldest first (left to right chronologically)
      this.dailyStats.set([...stats].reverse());
    } catch (error) {
      console.error('Failed to load daily statistics:', error);
      this.dailyStats.set([]);
    } finally {
      this.loading.set(false);
    }
  }
}
