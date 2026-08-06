import { useEffect, useState } from 'react';
import { paymentsApi } from '../../api/paymentsApi';
import PaymentDetails from '../../components/PaymentDetails';
import StatusBadge from '../../components/StatusBadge';
import { useUserSession } from '../../context/UserContext';
import type { Payment } from '../../types/payment';
import { formatWithInrEquivalent } from '../../utils/currency';

export default function MyPayments() {
    const { user } = useUserSession();
    const [payments, setPayments] = useState<Payment[]>([]);
    const [loading, setLoading] = useState(true);
    const [selectedId, setSelectedId] = useState<number | null>(null);

    useEffect(() => {
        let cancelled = false;
        async function load() {
            try {
                const all = await paymentsApi.getAll();
                if (!cancelled) setPayments(all);
            } finally {
                if (!cancelled) setLoading(false);
            }
        }
        load();
        const interval = setInterval(load, 8000);
        return () => {
            cancelled = true;
            clearInterval(interval);
        };
    }, []);

    if (!user) {
        return (
            <p className="rounded-md border border-amber-200 bg-amber-50 p-4 text-sm text-amber-700">
                Please log in as a customer first.
            </p>
        );
    }

    const mine = payments.filter((p) => {
        const withUsers = p as Payment & {
            payerUserId?: number;
            payeeUserId?: number;
        };
        return (
            withUsers.payerUserId === user.id || withUsers.payeeUserId === user.id
        );
    });

    const isSent = (p: Payment) =>
        (p as Payment & { payerUserId?: number }).payerUserId === user.id;

    const completedCount = mine.filter((p) => p.status === 'COMPLETED').length;
    const pendingCount = mine.filter(
        (p) => p.status === 'SENT' || p.status === 'VALIDATED' || p.status === 'CREATED',
    ).length;
    const failedCount = mine.filter((p) => p.status === 'FAILED').length;

    return (
        <div className="space-y-6">
            <div className="grid grid-cols-3 gap-4">
                <div className="rounded-xl border border-slate-200 bg-white p-4 shadow-sm">
                    <p className="text-2xl font-bold text-slate-800">{mine.length}</p>
                    <p className="mt-1 text-xs font-medium uppercase tracking-wide text-slate-400">
                        Total
                    </p>
                </div>
                <div className="rounded-xl border border-emerald-200 bg-emerald-50 p-4 shadow-sm">
                    <p className="text-2xl font-bold text-emerald-700">
                        {completedCount}
                    </p>
                    <p className="mt-1 text-xs font-medium uppercase tracking-wide text-emerald-600/80">
                        Completed
                    </p>
                </div>
                <div className="rounded-xl border border-amber-200 bg-amber-50 p-4 shadow-sm">
                    <p className="text-2xl font-bold text-amber-700">
                        {pendingCount}
                    </p>
                    <p className="mt-1 text-xs font-medium uppercase tracking-wide text-amber-600/80">
                        In progress
                    </p>
                </div>
            </div>

            <div className="grid grid-cols-1 gap-6 lg:grid-cols-2">
                <div className="rounded-xl border border-slate-200 bg-white shadow-sm">
                    <div className="flex items-center gap-2 border-b border-slate-200 p-4">
                        <span className="flex h-8 w-8 items-center justify-center rounded-lg bg-emerald-100 text-lg">
                            📜
                        </span>
                        <h2 className="text-lg font-semibold text-slate-800">
                            My Payments
                        </h2>
                        {failedCount > 0 && (
                            <span className="ml-auto rounded-full bg-red-100 px-2 py-0.5 text-[11px] font-semibold text-red-700">
                                {failedCount} failed
                            </span>
                        )}
                    </div>
                    {loading ? (
                        <p className="p-4 text-sm text-slate-400">Loading…</p>
                    ) : mine.length === 0 ? (
                        <p className="p-4 text-sm text-slate-400">
                            No payments yet.
                        </p>
                    ) : (
                        <ul className="max-h-130 divide-y divide-slate-100 overflow-y-auto">
                            {mine.map((p) => {
                                const sent = isSent(p);
                                return (
                                    <li key={p.id}>
                                        <button
                                            onClick={() => setSelectedId(p.id)}
                                            className={`flex w-full items-center justify-between gap-3 px-4 py-3 text-left text-sm transition hover:bg-emerald-50/60 ${selectedId === p.id ? 'bg-emerald-50' : ''
                                                }`}
                                        >
                                            <span>
                                                <span className="block font-medium text-slate-700">
                                                    <span
                                                        className={`mr-1.5 inline-block rounded-full px-1.5 py-0.5 text-[10px] font-semibold uppercase tracking-wide ${sent
                                                                ? 'bg-slate-100 text-slate-500'
                                                                : 'bg-emerald-100 text-emerald-700'
                                                            }`}
                                                    >
                                                        {sent ? 'Sent' : 'Received'}
                                                    </span>
                                                    #{p.id} —{' '}
                                                    {formatWithInrEquivalent(
                                                        sent ? (p.netAmount ?? p.amount) : p.amount,
                                                        p.currency,
                                                    )}
                                                </span>
                                                <span className="block text-xs text-slate-400">
                                                    {new Date(p.createdAt).toLocaleString()}
                                                </span>
                                            </span>
                                            <StatusBadge status={p.status} />
                                        </button>
                                    </li>
                                );
                            })}
                        </ul>
                    )}
                </div>

                <div>
                    {selectedId ? (
                        <PaymentDetails paymentId={selectedId} />
                    ) : (
                        <div className="rounded-xl border border-dashed border-slate-300 bg-white/60 p-8 text-center text-sm text-slate-400 shadow-sm">
                            Select a payment to view its details and history.
                        </div>
                    )}
                </div>
            </div>
        </div>
    );
}
