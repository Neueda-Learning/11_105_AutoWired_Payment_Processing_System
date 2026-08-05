import { useCallback, useEffect, useMemo, useState } from 'react';
import { paymentsApi } from '../../api/paymentsApi';
import PaymentDetails from '../../components/PaymentDetails';
import RiskMeter from '../../components/RiskMeter';
import StatusBadge from '../../components/StatusBadge';
import type { Payment } from '../../types/payment';
import { formatWithInrEquivalent } from '../../utils/currency';

const POLL_INTERVAL_MS = 8000;
const REVIEW_THRESHOLD = 70;

type ActionState = { id: number; kind: 'approve' | 'reject' } | null;

function reasonsFor(p: Payment): string[] {
    const reasons: string[] = [];
    if (p.amount >= 50000) reasons.push('High-value transfer (≥ 50,000)');
    const hour = new Date(p.createdAt).getHours();
    if (hour >= 0 && hour <= 5) reasons.push('Initiated during odd hours (12am–5am)');
    if (p.riskScore >= REVIEW_THRESHOLD) reasons.push('Composite risk score above review threshold');
    if (reasons.length === 0) reasons.push('Elevated velocity / spike vs. account history');
    return reasons;
}

export default function FlaggedTransactions() {
    const [payments, setPayments] = useState<Payment[]>([]);
    const [loading, setLoading] = useState(true);
    const [loadError, setLoadError] = useState<string | null>(null);
    const [selectedId, setSelectedId] = useState<number | null>(null);
    const [search, setSearch] = useState('');
    const [actionState, setActionState] = useState<ActionState>(null);
    const [actionError, setActionError] = useState<string | null>(null);

    const load = useCallback(async () => {
        try {
            const data = await paymentsApi.getFlagged();
            setPayments(data);
            setLoadError(null);
        } catch {
            setLoadError('Unable to reach the fraud monitoring API.');
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

    const filtered = useMemo(() => {
        if (!search.trim()) return payments;
        const term = search.trim().toLowerCase();
        return payments.filter(
            (p) =>
                String(p.id).includes(term) ||
                (p.reference ?? '').toLowerCase().includes(term) ||
                p.sourceAccount.toLowerCase().includes(term) ||
                p.destinationAccount.toLowerCase().includes(term),
        );
    }, [payments, search]);

    const summary = useMemo(() => {
        const pending = payments.filter((p) => p.status === 'SENT').length;
        const approved = payments.filter((p) => p.status === 'COMPLETED').length;
        const rejected = payments.filter((p) => p.status === 'FAILED').length;
        const avgScore = payments.length
            ? Math.round(
                payments.reduce((sum, p) => sum + p.riskScore, 0) / payments.length,
            )
            : 0;
        return { total: payments.length, pending, approved, rejected, avgScore };
    }, [payments]);

    async function handleDecision(id: number, kind: 'approve' | 'reject') {
        setActionState({ id, kind });
        setActionError(null);
        try {
            await paymentsApi.updateStatus(id, {
                status: kind === 'approve' ? 'COMPLETED' : 'FAILED',
                notes:
                    kind === 'approve'
                        ? 'Manually cleared after fraud review'
                        : 'Rejected by bank admin during fraud review',
            });
            await load();
        } catch {
            setActionError('Could not update the payment status. Please try again.');
        } finally {
            setActionState(null);
        }
    }

    return (
        <div className="space-y-6">
            <div>
                <div className="flex items-center gap-2">
                    <span className="flex h-8 w-8 items-center justify-center rounded-lg bg-red-100 text-lg">
                        🚨
                    </span>
                    <h1 className="text-xl font-semibold text-slate-800">
                        Fraud &amp; Flagged Transactions
                    </h1>
                </div>
                <p className="mt-1 text-sm text-slate-500">
                    Every payment whose composite risk score met or exceeded the
                    review threshold ({REVIEW_THRESHOLD}), across all statuses.
                </p>
            </div>

            {loadError && (
                <p className="rounded-md border border-amber-200 bg-amber-50 px-3 py-2 text-sm text-amber-700">
                    {loadError}
                </p>
            )}
            {actionError && (
                <p className="rounded-md border border-red-200 bg-red-50 px-3 py-2 text-sm text-red-700">
                    {actionError}
                </p>
            )}

            <div className="grid grid-cols-2 gap-4 sm:grid-cols-4">
                <SummaryCard label="Total flagged" value={summary.total} accent="slate" />
                <SummaryCard
                    label="Awaiting review"
                    value={summary.pending}
                    accent="amber"
                />
                <SummaryCard
                    label="Cleared"
                    value={summary.approved}
                    accent="emerald"
                />
                <SummaryCard label="Rejected" value={summary.rejected} accent="red" />
            </div>

            <div className="grid grid-cols-1 gap-6 lg:grid-cols-2">
                <div className="rounded-xl border border-slate-200 bg-white shadow-sm">
                    <div className="flex flex-col gap-3 border-b border-slate-200 p-4 sm:flex-row sm:items-center sm:justify-between">
                        <h2 className="text-lg font-semibold text-slate-800">
                            Flagged payments
                        </h2>
                        <input
                            value={search}
                            onChange={(e) => setSearch(e.target.value)}
                            placeholder="Search by ID, reference, or account"
                            className="rounded-md border border-slate-300 px-3 py-1.5 text-sm transition focus:border-indigo-500 focus:ring-2 focus:ring-indigo-100 focus:outline-none"
                        />
                    </div>

                    <div className="max-h-150 overflow-y-auto divide-y divide-slate-100">
                        {filtered.map((p) => {
                            const isBusy = actionState?.id === p.id;
                            return (
                                <div
                                    key={p.id}
                                    onClick={() => setSelectedId(p.id)}
                                    className={`cursor-pointer p-4 transition hover:bg-red-50/40 ${selectedId === p.id ? 'bg-red-50/70' : ''
                                        }`}
                                >
                                    <div className="flex items-start justify-between gap-3">
                                        <div>
                                            <p className="font-medium text-slate-700">
                                                #{p.id}{' '}
                                                <span className="text-slate-400">
                                                    · {formatWithInrEquivalent(p.amount, p.currency)}
                                                </span>
                                            </p>
                                            <p className="mt-0.5 text-xs text-slate-500">
                                                {p.sourceAccount} → {p.destinationAccount}
                                            </p>
                                        </div>
                                        <StatusBadge status={p.status} />
                                    </div>

                                    <div className="mt-3 max-w-xs">
                                        <RiskMeter score={p.riskScore} />
                                    </div>

                                    <ul className="mt-2 flex flex-wrap gap-1.5">
                                        {reasonsFor(p).map((r) => (
                                            <li
                                                key={r}
                                                className="rounded-full bg-slate-100 px-2 py-0.5 text-[11px] text-slate-600"
                                            >
                                                {r}
                                            </li>
                                        ))}
                                    </ul>

                                    {p.status === 'SENT' && (
                                        <div className="mt-3 flex gap-2">
                                            <button
                                                type="button"
                                                disabled={isBusy}
                                                onClick={(e) => {
                                                    e.stopPropagation();
                                                    handleDecision(p.id, 'approve');
                                                }}
                                                className="rounded-md bg-emerald-600 px-3 py-1.5 text-xs font-semibold text-white transition hover:bg-emerald-700 disabled:opacity-50"
                                            >
                                                {isBusy && actionState?.kind === 'approve'
                                                    ? 'Approving…'
                                                    : '✓ Approve'}
                                            </button>
                                            <button
                                                type="button"
                                                disabled={isBusy}
                                                onClick={(e) => {
                                                    e.stopPropagation();
                                                    handleDecision(p.id, 'reject');
                                                }}
                                                className="rounded-md bg-red-600 px-3 py-1.5 text-xs font-semibold text-white transition hover:bg-red-700 disabled:opacity-50"
                                            >
                                                {isBusy && actionState?.kind === 'reject'
                                                    ? 'Rejecting…'
                                                    : '✕ Reject'}
                                            </button>
                                        </div>
                                    )}
                                </div>
                            );
                        })}

                        {filtered.length === 0 && !loading && (
                            <div className="px-4 py-12 text-center text-sm text-slate-400">
                                {payments.length === 0
                                    ? 'No flagged transactions right now. 🎉'
                                    : 'No results match your search.'}
                            </div>
                        )}
                        {loading && (
                            <div className="px-4 py-12 text-center text-sm text-slate-400">
                                Loading…
                            </div>
                        )}
                    </div>
                </div>

                {selectedId ? (
                    <PaymentDetails paymentId={selectedId} />
                ) : (
                    <div className="rounded-xl border border-dashed border-slate-300 bg-white/60 p-8 text-center text-sm text-slate-400 shadow-sm">
                        Select a flagged payment to view its full details and history.
                    </div>
                )}
            </div>
        </div>
    );
}

function SummaryCard({
    label,
    value,
    accent,
}: {
    label: string;
    value: number;
    accent: 'slate' | 'amber' | 'emerald' | 'red';
}) {
    const styles: Record<string, string> = {
        slate: 'border-slate-200 bg-white text-slate-800',
        amber: 'border-amber-200 bg-amber-50 text-amber-700',
        emerald: 'border-emerald-200 bg-emerald-50 text-emerald-700',
        red: 'border-red-200 bg-red-50 text-red-700',
    };
    return (
        <div className={`rounded-xl border p-4 shadow-sm ${styles[accent]}`}>
            <p className="text-2xl font-bold">{value}</p>
            <p className="mt-1 text-xs font-medium uppercase tracking-wide opacity-80">
                {label}
            </p>
        </div>
    );
}
