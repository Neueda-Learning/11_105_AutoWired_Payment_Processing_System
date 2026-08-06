import { useCallback, useEffect, useMemo, useState } from 'react';
import { NavLink } from 'react-router-dom';
import { paymentsApi } from '../../api/paymentsApi';
import { feeRulesApi } from '../../api/feeRulesApi';
import StatusBadge from '../../components/StatusBadge';
import { computeLocalStats } from '../../utils/stats';
import { formatINR, formatWithInrEquivalent, toINR } from '../../utils/currency';
import type { Payment, PaymentStats } from '../../types/payment';

const POLL_INTERVAL_MS = 8000;

export default function BankOverview() {
    const [payments, setPayments] = useState<Payment[]>([]);
    const [stats, setStats] = useState<PaymentStats | null>(null);
    const [activeRuleCount, setActiveRuleCount] = useState<number | null>(null);
    const [loading, setLoading] = useState(true);
    const [loadError, setLoadError] = useState<string | null>(null);
    const [now, setNow] = useState(() => Date.now());

    const load = useCallback(async () => {
        try {
            const data = await paymentsApi.getAll('ALL');
            setPayments(data);
            setNow(Date.now());
            setLoadError(null);
            try {
                const remoteStats = await paymentsApi.getStats();
                setStats(remoteStats);
            } catch {
                setStats(computeLocalStats(data));
            }
        } catch {
            setLoadError('Unable to reach the payments API.');
        } finally {
            setLoading(false);
        }
    }, []);

    useEffect(() => {
        let cancelled = false;
        async function tick() {
            if (!cancelled) await load();
        }
        tick();
        const interval = setInterval(tick, POLL_INTERVAL_MS);
        return () => {
            cancelled = true;
            clearInterval(interval);
        };
    }, [load]);

    useEffect(() => {
        let cancelled = false;
        feeRulesApi
            .getAll()
            .then((rules) => {
                if (!cancelled) {
                    setActiveRuleCount(rules.filter((r) => r.active).length);
                }
            })
            .catch(() => {
                if (!cancelled) setActiveRuleCount(null);
            });
        return () => {
            cancelled = true;
        };
    }, []);

    const insights = useMemo(() => {
        const flagged = payments.filter((p) => p.riskScore >= 70);
        const pendingReview = flagged.filter((p) => p.status === 'SENT').length;
        const completed = payments.filter((p) => p.status === 'COMPLETED');
        const failed = payments.filter((p) => p.status === 'FAILED').length;
        // Commission is only ever earned on successfully COMPLETED payments,
        // never on ones still in-flight, held for review, or failed. Amounts
        // are converted to INR since payments can be made in different
        // currencies (USD/EUR/GBP/INR).
        const commissionInr = completed.reduce(
            (sum, p) => sum + toINR(p.feeAmount ?? 0, p.currency),
            0,
        );
        const totalVolumeInr = payments.reduce(
            (sum, p) => sum + toINR(p.netAmount ?? p.amount, p.currency),
            0,
        );
        const last24h = payments.filter(
            (p) => now - new Date(p.createdAt).getTime() < 24 * 60 * 60 * 1000,
        ).length;

        const methodCounts = payments.reduce<Record<string, number>>(
            (acc, p) => {
                acc[p.paymentMethod] = (acc[p.paymentMethod] ?? 0) + 1;
                return acc;
            },
            {},
        );
        const topMethod =
            Object.entries(methodCounts).sort((a, b) => b[1] - a[1])[0]?.[0] ??
            '—';

        return {
            flaggedCount: flagged.length,
            pendingReview,
            failed,
            commissionInr,
            totalVolumeInr,
            last24h,
            topMethod,
        };
    }, [payments, now]);

    const recent = useMemo(
        () =>
            [...payments]
                .sort(
                    (a, b) =>
                        new Date(b.createdAt).getTime() -
                        new Date(a.createdAt).getTime(),
                )
                .slice(0, 6),
        [payments],
    );

    const commission = insights.commissionInr;

    return (
        <div className="space-y-6">
            {loadError && (
                <p className="flex items-center gap-2 rounded-md border border-amber-200 bg-amber-50 px-3 py-2 text-sm text-amber-700">
                    <span>⚠️</span> {loadError}
                </p>
            )}

            {/* Hero stat row */}
            <div className="grid grid-cols-1 gap-4 sm:grid-cols-2 lg:grid-cols-4">
                <HeroCard
                    icon="📊"
                    label="Total Transactions"
                    value={loading ? '—' : String(stats?.totalCount ?? payments.length)}
                    sub={`${insights.last24h} in last 24h`}
                    accent="indigo"
                />
                <HeroCard
                    icon="💰"
                    label="Commission Earned"
                    value={loading ? '—' : formatINR(commission)}
                    sub="From completed payments only"
                    accent="emerald"
                />
                <HeroCard
                    icon="✅"
                    label="Success Rate"
                    value={loading ? '—' : `${(stats?.successRate ?? 0).toFixed(0)}%`}
                    sub={`${insights.failed} failed`}
                    accent="teal"
                />
                <HeroCard
                    icon="🚨"
                    label="Fraud Alerts"
                    value={loading ? '—' : String(insights.flaggedCount)}
                    sub={`${insights.pendingReview} awaiting review`}
                    accent="red"
                    link={insights.flaggedCount > 0 ? '/bank/flagged' : undefined}
                />
            </div>

            <div className="grid grid-cols-1 gap-6 lg:grid-cols-3">
                {/* Recent activity */}
                <div className="rounded-xl border border-slate-200 bg-white shadow-sm lg:col-span-2">
                    <div className="flex items-center justify-between border-b border-slate-200 p-4">
                        <div className="flex items-center gap-2">
                            <span className="flex h-8 w-8 items-center justify-center rounded-lg bg-indigo-100 text-lg">
                                🕒
                            </span>
                            <h2 className="text-lg font-semibold text-slate-800">
                                Recent Activity
                            </h2>
                        </div>
                        <NavLink
                            to="/bank/payments"
                            className="text-xs font-semibold text-indigo-600 hover:underline"
                        >
                            View all →
                        </NavLink>
                    </div>
                    {loading ? (
                        <p className="p-4 text-sm text-slate-400">Loading…</p>
                    ) : recent.length === 0 ? (
                        <p className="p-4 text-sm text-slate-400">No payments yet.</p>
                    ) : (
                        <ul className="divide-y divide-slate-100">
                            {recent.map((p) => (
                                <li
                                    key={p.id}
                                    className="flex items-center justify-between gap-3 px-4 py-3 text-sm"
                                >
                                    <div>
                                        <p className="font-medium text-slate-700">
                                            #{p.id}{' '}
                                            <span className="text-slate-400">
                                                ·{' '}
                                                {formatWithInrEquivalent(
                                                    p.netAmount ?? p.amount,
                                                    p.currency,
                                                )}
                                            </span>
                                        </p>
                                        <p className="mt-0.5 text-xs text-slate-400">
                                            {p.sourceAccount} → {p.destinationAccount}
                                            {' · '}
                                            {new Date(p.createdAt).toLocaleString()}
                                        </p>
                                    </div>
                                    <StatusBadge status={p.status} />
                                </li>
                            ))}
                        </ul>
                    )}
                </div>

                {/* Platform snapshot */}
                <div className="space-y-6">
                    <div className="rounded-xl border border-slate-200 bg-white p-5 shadow-sm">
                        <div className="flex items-center gap-2">
                            <span className="flex h-7 w-7 items-center justify-center rounded-md bg-slate-100 text-sm">
                                📈
                            </span>
                            <h3 className="text-sm font-semibold text-slate-800">
                                Platform Snapshot
                            </h3>
                        </div>
                        <dl className="mt-4 space-y-3 text-sm">
                            <div className="flex items-center justify-between">
                                <dt className="text-slate-500">Total Volume (Debited)</dt>
                                <dd className="font-semibold text-slate-800">
                                    {formatINR(insights.totalVolumeInr)}
                                </dd>
                            </div>
                            <div className="flex items-center justify-between">
                                <dt className="text-slate-500">Avg. Risk Score</dt>
                                <dd className="font-semibold text-slate-800">
                                    {(stats?.avgRiskScore ?? 0).toFixed(1)} / 100
                                </dd>
                            </div>
                            <div className="flex items-center justify-between">
                                <dt className="text-slate-500">Top Payment Method</dt>
                                <dd className="font-semibold text-slate-800">
                                    {insights.topMethod}
                                </dd>
                            </div>
                            <div className="flex items-center justify-between">
                                <dt className="text-slate-500">Active Fee Rules</dt>
                                <dd className="font-semibold text-slate-800">
                                    {activeRuleCount ?? '—'}
                                </dd>
                            </div>
                        </dl>
                    </div>

                    <div className="rounded-xl border border-slate-200 bg-white p-5 shadow-sm">
                        <div className="flex items-center gap-2">
                            <span className="flex h-7 w-7 items-center justify-center rounded-md bg-slate-100 text-sm">
                                📋
                            </span>
                            <h3 className="text-sm font-semibold text-slate-800">
                                Status Breakdown
                            </h3>
                        </div>
                        <ul className="mt-4 space-y-2.5">
                            {(
                                ['CREATED', 'VALIDATED', 'SENT', 'COMPLETED', 'FAILED'] as const
                            ).map((s) => {
                                const count = stats?.statusCounts?.[s] ?? 0;
                                const total = stats?.totalCount || 1;
                                const pct = Math.round((count / total) * 100);
                                return (
                                    <li key={s}>
                                        <div className="flex items-center justify-between text-xs">
                                            <StatusBadge status={s} />
                                            <span className="text-slate-500">
                                                {count} ({pct}%)
                                            </span>
                                        </div>
                                        <div className="mt-1 h-1.5 w-full overflow-hidden rounded-full bg-slate-100">
                                            <div
                                                className="h-full rounded-full bg-indigo-500 transition-all"
                                                style={{ width: `${pct}%` }}
                                            />
                                        </div>
                                    </li>
                                );
                            })}
                        </ul>
                    </div>
                </div>
            </div>
        </div>
    );
}

function HeroCard({
    icon,
    label,
    value,
    sub,
    accent,
    link,
}: {
    icon: string;
    label: string;
    value: string;
    sub: string;
    accent: 'indigo' | 'emerald' | 'teal' | 'red';
    link?: string;
}) {
    const styles: Record<string, { bg: string; ring: string; text: string }> = {
        indigo: { bg: 'bg-indigo-50', ring: 'ring-indigo-100', text: 'text-indigo-700' },
        emerald: { bg: 'bg-emerald-50', ring: 'ring-emerald-100', text: 'text-emerald-700' },
        teal: { bg: 'bg-teal-50', ring: 'ring-teal-100', text: 'text-teal-700' },
        red: { bg: 'bg-red-50', ring: 'ring-red-100', text: 'text-red-700' },
    };
    const s = styles[accent];

    const content = (
        <div className="rounded-xl border border-slate-200 bg-white p-5 shadow-sm transition hover:-translate-y-0.5 hover:shadow-md">
            <div className="flex items-center gap-2">
                <span
                    className={`flex h-9 w-9 items-center justify-center rounded-lg text-lg ring-1 ${s.bg} ${s.ring}`}
                >
                    {icon}
                </span>
                <p className="text-xs font-medium uppercase tracking-wide text-slate-400">
                    {label}
                </p>
            </div>
            <p className={`mt-3 text-2xl font-bold ${s.text}`}>{value}</p>
            <p className="mt-1 text-xs text-slate-400">{sub}</p>
        </div>
    );

    return link ? <NavLink to={link}>{content}</NavLink> : content;
}

