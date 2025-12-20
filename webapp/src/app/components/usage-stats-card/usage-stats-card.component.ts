import {Component, computed, input, signal} from '@angular/core';

interface UsageStats {
  fetchesToday: number;
  dailyLimit: number;
  users: number;
  totalThisMonth: number;
  avgResponseTime: number;
}

interface LeniencyStats {
  used: number;
  allowed: number;
}

interface WeeklyFetch {
  day: string;
  fetches: number;
  limit: number;
}

@Component({
  selector: 'app-usage-stats-card',
  standalone: true,
  imports: [],
  templateUrl: './usage-stats-card.component.html',
  styleUrl: './usage-stats-card.component.scss'
})
export class UsageStatsCardComponent {
  // Inputs
  environmentId = input.required<string>();

  // Card collapse state
  expanded = signal(true);

  // Mock usage statistics - Replace with: this.api.invoke(getUsageStats, {envId})
  usageStats = signal<UsageStats>({
    fetchesToday: 12847,
    dailyLimit: 50000,
    users: 7954,
    totalThisMonth: 287432,
    avgResponseTime: 42, // ms
  });

  // Mock leniency tracking - Replace with: this.api.invoke(getLeniencyStats, {envId})
  leniencyStats = signal<LeniencyStats>({
    used: 2,
    allowed: 2,
  });

  // Mock weekly fetch data - Replace with: this.api.invoke(getWeeklyFetches, {envId})
  weeklyFetches = signal<WeeklyFetch[]>([
    {day: 'Mon', fetches: 38420, limit: 50000},
    {day: 'Tue', fetches: 50000, limit: 50000},  // Hit limit
    {day: 'Wed', fetches: 35890, limit: 50000},
    {day: 'Thu', fetches: 51200, limit: 50000},  // Exceeded limit
    {day: 'Fri', fetches: 31560, limit: 50000},
    {day: 'Sat', fetches: 18940, limit: 50000},
    {day: 'Sun', fetches: 12847, limit: 50000},
  ]);

  // Computed high risk alert based on leniency usage
  highRiskAlert = computed(() => {
    const leniency = this.leniencyStats();
    if (leniency.used >= leniency.allowed) {
      return {
        message: `We offer leniency for exceeded limits ${leniency.allowed} times per month to prevent application downtime. This allowance has been fully used. Your application is at high risk of service interruption.`
      };
    }
    return null;
  });

  // Computed usage percentage
  usagePercentage = computed(() => {
    const stats = this.usageStats();
    return Math.round((stats.fetchesToday / stats.dailyLimit) * 100);
  });

  // Computed recommendation based on usage patterns
  usageRecommendation = computed(() => {
    const fetches = this.weeklyFetches();
    const daysAtLimit = fetches.filter(day => day.fetches >= day.limit).length;

    if (daysAtLimit >= 2) {
      return {
        type: 'warning' as const,
        message: `You've hit the limit ${daysAtLimit} times this week. Consider upgrading to a higher tier.`
      };
    } else if (daysAtLimit === 1) {
      return {
        type: 'info' as const,
        message: 'You hit your daily limit once this week. Monitor your usage to avoid interruptions.'
      };
    } else if (this.usagePercentage() > 80) {
      return {
        type: 'info' as const,
        message: 'You\'re approaching your daily limit. Consider your usage patterns.'
      };
    }
    return null;
  });

  toggleCard(): void {
    this.expanded.update(v => !v);
  }

  formatNumber(num: number): string {
    return num.toLocaleString();
  }
}
