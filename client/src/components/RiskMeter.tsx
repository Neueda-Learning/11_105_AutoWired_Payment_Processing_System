interface Props {
    score: number;
}

function getTier(score: number): { label: string; color: string; bar: string } {
    if (score >= 70) {
        return { label: 'High Risk', color: 'text-red-600', bar: 'bg-red-500' };
    }
    if (score >= 40) {
        return { label: 'Medium Risk', color: 'text-amber-600', bar: 'bg-amber-500' };
    }
    return { label: 'Low Risk', color: 'text-emerald-600', bar: 'bg-emerald-500' };
}

export default function RiskMeter({ score }: Props) {
    const tier = getTier(score);
    const pct = Math.min(100, Math.max(0, score));

    return (
        <div>
            <div className="mb-1 flex items-center justify-between text-xs">
                <span className={`font-semibold ${tier.color}`}>{tier.label}</span>
                <span className="text-slate-500">{score}/100</span>
            </div>
            <div className="h-1.5 w-full overflow-hidden rounded-full bg-slate-100">
                <div
                    className={`h-full rounded-full ${tier.bar} transition-all`}
                    style={{ width: `${pct}%` }}
                />
            </div>
        </div>
    );
}
