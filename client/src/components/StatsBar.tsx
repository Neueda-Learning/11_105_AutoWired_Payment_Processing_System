import type { PaymentStats } from '../types/payment';

interface Props {
    stats: PaymentStats;
}

function formatCurrency(value: number) {
    return new Intl.NumberFormat('en-US', {
        style: 'currency',
        currency: 'USD',
        maximumFractionDigits: 0,
    }).format(value);
}

const CARDS: {
    key: keyof PaymentStats | 'volume';
    label: string;
    accent: string;
}[] = [
        { key: 'totalCount', label: 'Total Payments', accent: 'text-slate-900' },
        { key: 'volume', label: 'Total Volume', accent: 'text-indigo-600' },
        { key: 'successRate', label: 'Success Rate', accent: 'text-emerald-600' },
        { key: 'avgRiskScore', label: 'Avg Risk Score', accent: 'text-amber-600' },
    ];

export default function StatsBar({ stats }: Props) {
    return (
        <div className="grid grid-cols-2 gap-3 sm:grid-cols-4">
            {CARDS.map((card) => {
                let value: string;
                if (card.key === 'volume') {
                    value = formatCurrency(stats.totalVolume);
                } else if (card.key === 'successRate') {
                    value = `${stats.successRate.toFixed(0)}%`;
                } else if (card.key === 'avgRiskScore') {
                    value = stats.avgRiskScore.toFixed(1);
                } else {
                    value = String(stats.totalCount);
                }

                return (
                    <div
                        key={card.key}
                        className="rounded-xl border border-slate-200/70 bg-white/80 p-4 shadow-sm backdrop-blur transition hover:shadow-md"
                    >
                        <p className="text-xs font-medium uppercase tracking-wide text-slate-400">
                            {card.label}
                        </p>
                        <p className={`mt-1 text-2xl font-bold ${card.accent}`}>
                            {value}
                        </p>
                    </div>
                );
            })}
        </div>
    );
}
