import { useEffect, useState } from 'react';
import { paymentsApi } from '../api/paymentsApi';
import type { Payment, PaymentStatusHistory } from '../types/payment';
import StatusBadge from './StatusBadge';
import RiskMeter from './RiskMeter';

interface Props {
    paymentId: number;
}

export default function PaymentDetails({ paymentId }: Props) {
    const [payment, setPayment] = useState<Payment | null>(null);
    const [history, setHistory] = useState<PaymentStatusHistory[]>([]);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState<string | null>(null);

    async function load() {
        setLoading(true);
        setError(null);
        try {
            const [p, h] = await Promise.all([
                paymentsApi.getById(paymentId),
                paymentsApi.getHistory(paymentId),
            ]);
            setPayment(p);
            setHistory(h);
        } catch {
            setError('Failed to load payment details');
        } finally {
            setLoading(false);
        }
    }

    useEffect(() => {
        let cancelled = false;

        async function tick() {
            if (!cancelled) await load();
        }

        tick();
        const interval = setInterval(tick, 8000);
        return () => {
            cancelled = true;
            clearInterval(interval);
        };
        // eslint-disable-next-line react-hooks/exhaustive-deps
    }, [paymentId]);

    if (loading && !payment) {
        return (
            <div className="rounded-xl border border-slate-200 bg-white p-5 text-sm text-slate-400 shadow-sm">
                Loading…
            </div>
        );
    }

    if (error || !payment) {
        return (
            <div className="rounded-xl border border-red-200 bg-red-50 p-5 text-sm text-red-700 shadow-sm">
                {error ?? 'Payment not found'}
            </div>
        );
    }

    return (
        <div className="rounded-xl border border-slate-200 bg-white p-5 shadow-sm">
            <div className="mb-4 flex items-center justify-between">
                <h2 className="text-lg font-semibold text-slate-800">
                    Payment #{payment.id}
                </h2>
                <StatusBadge status={payment.status} />
            </div>

            <dl className="grid grid-cols-2 gap-x-4 gap-y-2 text-sm">
                <dt className="text-slate-500">Amount</dt>
                <dd className="text-right font-medium text-slate-800">
                    {payment.amount.toFixed(2)} {payment.currency}
                </dd>
                {payment.processingFee != null && (
                    <>
                        <dt className="text-slate-500">Processing Fee (0.2%)</dt>
                        <dd className="text-right text-slate-800">
                            {payment.processingFee.toFixed(2)} {payment.currency}
                        </dd>
                    </>
                )}
                {payment.totalDebit != null && (
                    <>
                        <dt className="text-slate-500">Total Debit</dt>
                        <dd className="text-right font-medium text-slate-800">
                            {payment.totalDebit.toFixed(2)} {payment.currency}
                        </dd>
                    </>
                )}
                <dt className="text-slate-500">Source</dt>
                <dd className="text-right text-slate-800">{payment.sourceAccount}</dd>
                <dt className="text-slate-500">Destination</dt>
                <dd className="text-right text-slate-800">{payment.destinationAccount}</dd>
                <dt className="text-slate-500">Method</dt>
                <dd className="text-right text-slate-800">{payment.paymentMethod}</dd>
                {payment.paymentMethod === 'UPI' && payment.upiId && (
                    <>
                        <dt className="text-slate-500">UPI ID</dt>
                        <dd className="text-right text-slate-800">{payment.upiId}</dd>
                    </>
                )}
                {payment.paymentMethod === 'NETBANKING' && payment.bankName && (
                    <>
                        <dt className="text-slate-500">Bank</dt>
                        <dd className="text-right text-slate-800">{payment.bankName}</dd>
                    </>
                )}
                {payment.paymentMethod === 'CREDIT_CARD' && payment.cardLast4 && (
                    <>
                        <dt className="text-slate-500">Card</dt>
                        <dd className="text-right text-slate-800">
                            •••• {payment.cardLast4}
                            {payment.cardExpiry ? ` (exp ${payment.cardExpiry})` : ''}
                        </dd>
                    </>
                )}
                <dt className="text-slate-500">Reference</dt>
                <dd className="text-right text-slate-800">{payment.reference || '—'}</dd>
                <dt className="text-slate-500">Created</dt>
                <dd className="text-right text-slate-800">
                    {new Date(payment.createdAt).toLocaleString()}
                </dd>
                <dt className="text-slate-500">Updated</dt>
                <dd className="text-right text-slate-800">
                    {new Date(payment.updatedAt).toLocaleString()}
                </dd>
            </dl>

            <div className="mt-4 rounded-lg border border-slate-100 bg-slate-50 p-3">
                <RiskMeter score={payment.riskScore} />
            </div>

            <h3 className="mt-5 mb-2 text-sm font-semibold text-slate-700">
                Status History
            </h3>
            <ol className="space-y-2 border-l border-slate-200 pl-4">
                {history.map((h) => (
                    <li key={h.id} className="relative text-sm">
                        <span className="absolute -left-5.25 top-1.5 h-2 w-2 rounded-full bg-indigo-500" />
                        <div className="flex items-center gap-2">
                            <StatusBadge status={h.status} />
                            <span className="text-xs text-slate-400">
                                {new Date(h.timestamp).toLocaleString()}
                            </span>
                        </div>
                        {h.notes && (
                            <p className="mt-1 text-slate-600">{h.notes}</p>
                        )}
                    </li>
                ))}
                {history.length === 0 && (
                    <li className="text-sm text-slate-400">No history yet</li>
                )}
            </ol>

            {payment.status === 'FAILED' && (
                <div className="mt-4 rounded-md border border-red-200 bg-red-50 p-3 text-sm text-red-700">
                    This payment failed. See the latest history entry above for the
                    error details.
                </div>
            )}
        </div>
    );
}
