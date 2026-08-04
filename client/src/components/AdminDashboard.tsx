import { useCallback, useEffect, useState } from 'react';
import { adminApi } from '../api/adminApi';
import PaymentList from './PaymentList';
import PaymentDetails from './PaymentDetails';
import type { AdminStats } from '../types/admin';
import type { Payment, PaymentStatus } from '../types/payment';

const POLL_INTERVAL_MS = 8000;

function formatCurrency(value: number) {
    return new Intl.NumberFormat('en-US', {
        style: 'currency',
        currency: 'USD',
        maximumFractionDigits: 2,
    }).format(value);
}

export default function AdminDashboard() {
    const [stats, setStats] = useState<AdminStats | null>(null);
    const [payments, setPayments] = useState<Payment[]>([]);
    const [statusFilter, setStatusFilter] = useState<PaymentStatus | 'ALL'>(
        'ALL',
    );
    const [search, setSearch] = useState('');
    const [selectedId, setSelectedId] = useState<number | null>(null);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState<string | null>(null);

    const load = useCallback(async () => {
        try {
            const [statsData, paymentsData] = await Promise.all([
                adminApi.getStats(),
                adminApi.getAllPayments(statusFilter),
            ]);
            setStats(statsData);
            setPayments(paymentsData);
            setError(null);
        } catch {
            setError('Unable to load admin data.');
        } finally {
            setLoading(false);
        }
    }, [statusFilter]);

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

    return (
        <div>
            <h2 className="mb-4 text-lg font-semibold text-slate-800">
                Admin Dashboard
            </h2>

            {error && (
                <p className="mb-6 rounded-md border border-amber-200 bg-amber-50 px-3 py-2 text-sm text-amber-700">
                    {error}
                </p>
            )}

            {stats && (
                <div className="mb-6 grid grid-cols-1 gap-3 sm:grid-cols-3">
                    <div className="rounded-xl border border-slate-200/70 bg-white/80 p-4 shadow-sm backdrop-blur transition hover:shadow-md">
                        <p className="text-xs font-medium uppercase tracking-wide text-slate-400">
                            Total Payments (platform)
                        </p>
                        <p className="mt-1 text-2xl font-bold text-slate-900">
                            {stats.totalPaymentCount}
                        </p>
                    </div>
                    <div className="rounded-xl border border-slate-200/70 bg-white/80 p-4 shadow-sm backdrop-blur transition hover:shadow-md">
                        <p className="text-xs font-medium uppercase tracking-wide text-slate-400">
                            Total Volume (platform)
                        </p>
                        <p className="mt-1 text-2xl font-bold text-indigo-600">
                            {formatCurrency(stats.totalVolume)}
                        </p>
                    </div>
                    <div className="rounded-xl border border-slate-200/70 bg-white/80 p-4 shadow-sm backdrop-blur transition hover:shadow-md">
                        <p className="text-xs font-medium uppercase tracking-wide text-slate-400">
                            Earnings (processing fee)
                        </p>
                        <p className="mt-1 text-2xl font-bold text-emerald-600">
                            {formatCurrency(stats.totalFeeEarnings)}
                        </p>
                    </div>
                </div>
            )}

            <div className="grid grid-cols-1 gap-6 lg:grid-cols-2">
                <PaymentList
                    payments={payments}
                    statusFilter={statusFilter}
                    onStatusFilterChange={setStatusFilter}
                    search={search}
                    onSearchChange={setSearch}
                    selectedId={selectedId}
                    onSelect={setSelectedId}
                    loading={loading}
                />

                {selectedId ? (
                    <PaymentDetails paymentId={selectedId} />
                ) : (
                    <div className="rounded-xl border border-dashed border-slate-300 bg-white/60 p-8 text-center text-sm text-slate-400 shadow-sm">
                        Select a payment to view its details and history.
                    </div>
                )}
            </div>
        </div>
    );
}
