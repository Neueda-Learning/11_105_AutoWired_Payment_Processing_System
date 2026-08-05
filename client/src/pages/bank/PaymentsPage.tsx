import { useCallback, useEffect, useState } from 'react';
import { paymentsApi } from '../../api/paymentsApi';
import PaymentList from '../../components/PaymentList';
import PaymentDetails from '../../components/PaymentDetails';
import type { Payment, PaymentStatus } from '../../types/payment';

const POLL_INTERVAL_MS = 8000;

export default function PaymentsPage() {
    const [payments, setPayments] = useState<Payment[]>([]);
    const [statusFilter, setStatusFilter] = useState<PaymentStatus | 'ALL'>(
        'ALL',
    );
    const [search, setSearch] = useState('');
    const [selectedId, setSelectedId] = useState<number | null>(null);
    const [loading, setLoading] = useState(true);
    const [loadError, setLoadError] = useState<string | null>(null);

    const loadPayments = useCallback(async () => {
        try {
            const data = await paymentsApi.getAll(statusFilter);
            setPayments(data);
            setLoadError(null);
        } catch {
            setLoadError('Unable to reach the payments API.');
        } finally {
            setLoading(false);
        }
    }, [statusFilter]);

    useEffect(() => {
        let cancelled = false;
        async function tick() {
            if (!cancelled) await loadPayments();
        }
        tick();
        const interval = setInterval(tick, POLL_INTERVAL_MS);
        return () => {
            cancelled = true;
            clearInterval(interval);
        };
    }, [loadPayments]);

    return (
        <div className="space-y-6">
            {loadError && (
                <p className="flex items-center gap-2 rounded-md border border-amber-200 bg-amber-50 px-3 py-2 text-sm text-amber-700">
                    <span>⚠️</span> {loadError}
                </p>
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
