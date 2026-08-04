import { useState } from 'react';
import type { AxiosError } from 'axios';
import { paymentsApi } from '../api/paymentsApi';
import type { CurrentUser } from '../types/user';
import type {
    ApiErrorResponse,
    CreatePaymentRequest,
    Payment,
    PaymentMethod,
} from '../types/payment';

const CURRENCIES = ['USD', 'EUR', 'GBP', 'INR'];

function newIdempotencyKey() {
    return typeof crypto !== 'undefined' && 'randomUUID' in crypto
        ? crypto.randomUUID()
        : `key-${Date.now()}-${Math.random().toString(16).slice(2)}`;
}

function maskCardNumber(value: string) {
    const digits = value.replace(/\D/g, '').slice(0, 19);
    return digits.replace(/(.{4})/g, '$1 ').trim();
}

interface Props {
    currentUser: CurrentUser;
    onCreated: (payment: Payment, wasDuplicate: boolean) => void;
}

export default function PaymentForm({ currentUser, onCreated }: Props) {
    const emptyForm = (): CreatePaymentRequest => ({
        destinationAccount: '',
        amount: 0,
        currency: 'USD',
        paymentMethod: 'UPI',
        reference: '',
        cardNumber: '',
        cardExpiry: '',
        cardHolderName: '',
        idempotencyKey: newIdempotencyKey(),
    });

    const [form, setForm] = useState<CreatePaymentRequest>(emptyForm());
    const [errors, setErrors] = useState<string[]>([]);
    const [submitting, setSubmitting] = useState(false);

    function update<K extends keyof CreatePaymentRequest>(
        key: K,
        value: CreatePaymentRequest[K],
    ) {
        setForm((f) => ({ ...f, [key]: value }));
    }

    async function handleSubmit(e: React.FormEvent) {
        e.preventDefault();
        setErrors([]);
        setSubmitting(true);
        try {
            const payment = await paymentsApi.create(form);
            onCreated(payment, false);
            setForm(emptyForm());
        } catch (err) {
            const axiosErr = err as AxiosError<ApiErrorResponse>;
            const data = axiosErr.response?.data;

            if (
                axiosErr.response?.status === 409 &&
                data?.existingPaymentId
            ) {
                try {
                    const existing = await paymentsApi.getById(
                        data.existingPaymentId,
                    );
                    onCreated(existing, true);
                    setForm(emptyForm());
                    return;
                } catch {
                    // fall through to generic error handling below
                }
            }

            setErrors(
                data?.details && data.details.length > 0
                    ? data.details
                    : [data?.message ?? 'Failed to create payment'],
            );
        } finally {
            setSubmitting(false);
        }
    }

    return (
        <form
            onSubmit={handleSubmit}
            className="rounded-xl border border-slate-200 bg-white p-5 shadow-sm"
        >
            <div className="mb-4 flex items-center justify-between">
                <h2 className="text-lg font-semibold text-slate-800">
                    Create Payment
                </h2>
                <span
                    title="A unique key is generated per submission to prevent accidental duplicate payments."
                    className="cursor-help rounded-full bg-slate-100 px-2 py-0.5 text-[11px] font-medium text-slate-500"
                >
                    Idempotency-protected
                </span>
            </div>

            <div className="mb-4 rounded-md border border-indigo-100 bg-indigo-50 px-3 py-2 text-sm text-indigo-700">
                Paying as <span className="font-semibold">{currentUser.name}</span>{' '}
                ({currentUser.accountNumber})
            </div>

            {errors.length > 0 && (
                <div className="mb-4 rounded-md border border-red-200 bg-red-50 p-3 text-sm text-red-700">
                    <ul className="list-inside list-disc">
                        {errors.map((msg) => (
                            <li key={msg}>{msg}</li>
                        ))}
                    </ul>
                </div>
            )}

            <div className="grid grid-cols-1 gap-4 sm:grid-cols-2">
                <div className="sm:col-span-2">
                    <label className="mb-1 block text-sm font-medium text-slate-600">
                        Destination Account
                    </label>
                    <input
                        required
                        value={form.destinationAccount}
                        onChange={(e) => update('destinationAccount', e.target.value)}
                        className="w-full rounded-md border border-slate-300 px-3 py-2 text-sm transition focus:border-indigo-500 focus:ring-2 focus:ring-indigo-100 focus:outline-none"
                        placeholder="ACC-2002"
                    />
                </div>
                <div>
                    <label className="mb-1 block text-sm font-medium text-slate-600">
                        Amount
                    </label>
                    <input
                        required
                        type="number"
                        min="0.01"
                        step="0.01"
                        value={form.amount || ''}
                        onChange={(e) => update('amount', Number(e.target.value))}
                        className="w-full rounded-md border border-slate-300 px-3 py-2 text-sm transition focus:border-indigo-500 focus:ring-2 focus:ring-indigo-100 focus:outline-none"
                    />
                </div>
                <div>
                    <label className="mb-1 block text-sm font-medium text-slate-600">
                        Currency
                    </label>
                    <select
                        value={form.currency}
                        onChange={(e) => update('currency', e.target.value)}
                        className="w-full rounded-md border border-slate-300 px-3 py-2 text-sm transition focus:border-indigo-500 focus:ring-2 focus:ring-indigo-100 focus:outline-none"
                    >
                        {CURRENCIES.map((c) => (
                            <option key={c} value={c}>
                                {c}
                            </option>
                        ))}
                    </select>
                </div>
                <div>
                    <label className="mb-1 block text-sm font-medium text-slate-600">
                        Payment Method
                    </label>
                    <select
                        value={form.paymentMethod}
                        onChange={(e) =>
                            update('paymentMethod', e.target.value as PaymentMethod)
                        }
                        className="w-full rounded-md border border-slate-300 px-3 py-2 text-sm transition focus:border-indigo-500 focus:ring-2 focus:ring-indigo-100 focus:outline-none"
                    >
                        <option value="UPI">UPI</option>
                        <option value="NETBANKING">NETBANKING</option>
                        <option value="CREDIT_CARD">Credit Card</option>
                    </select>
                </div>
                <div>
                    <label className="mb-1 block text-sm font-medium text-slate-600">
                        Reference (optional)
                    </label>
                    <input
                        value={form.reference}
                        onChange={(e) => update('reference', e.target.value)}
                        className="w-full rounded-md border border-slate-300 px-3 py-2 text-sm transition focus:border-indigo-500 focus:ring-2 focus:ring-indigo-100 focus:outline-none"
                        placeholder="Invoice #123"
                    />
                </div>
            </div>

            {form.paymentMethod === 'CREDIT_CARD' && (
                <div className="mt-4 grid grid-cols-1 gap-4 rounded-md border border-slate-200 bg-slate-50 p-4 sm:grid-cols-2">
                    <div className="sm:col-span-2">
                        <label className="mb-1 block text-sm font-medium text-slate-600">
                            Card Number
                        </label>
                        <input
                            required
                            inputMode="numeric"
                            autoComplete="cc-number"
                            value={maskCardNumber(form.cardNumber ?? '')}
                            onChange={(e) =>
                                update(
                                    'cardNumber',
                                    e.target.value.replace(/\D/g, '').slice(0, 19),
                                )
                            }
                            className="w-full rounded-md border border-slate-300 px-3 py-2 text-sm transition focus:border-indigo-500 focus:ring-2 focus:ring-indigo-100 focus:outline-none"
                            placeholder="4242 4242 4242 4242"
                        />
                    </div>
                    <div>
                        <label className="mb-1 block text-sm font-medium text-slate-600">
                            Expiry (MM/YYYY)
                        </label>
                        <input
                            required
                            autoComplete="cc-exp"
                            value={form.cardExpiry ?? ''}
                            onChange={(e) => update('cardExpiry', e.target.value)}
                            className="w-full rounded-md border border-slate-300 px-3 py-2 text-sm transition focus:border-indigo-500 focus:ring-2 focus:ring-indigo-100 focus:outline-none"
                            placeholder="12/2028"
                        />
                    </div>
                    <div>
                        <label className="mb-1 block text-sm font-medium text-slate-600">
                            Card Holder Name
                        </label>
                        <input
                            required
                            autoComplete="cc-name"
                            value={form.cardHolderName ?? ''}
                            onChange={(e) => update('cardHolderName', e.target.value)}
                            className="w-full rounded-md border border-slate-300 px-3 py-2 text-sm transition focus:border-indigo-500 focus:ring-2 focus:ring-indigo-100 focus:outline-none"
                            placeholder="Jane Doe"
                        />
                    </div>
                </div>
            )}

            {form.paymentMethod === 'UPI' && (
                <div className="mt-4 rounded-md border border-slate-200 bg-slate-50 p-4 text-sm text-slate-600">
                    Sending from your own UPI ID:{' '}
                    <span className="font-medium text-slate-800">
                        {currentUser.ownUpiId ?? 'not set on your profile'}
                    </span>
                </div>
            )}

            {form.paymentMethod === 'NETBANKING' && (
                <div className="mt-4 rounded-md border border-slate-200 bg-slate-50 p-4 text-sm text-slate-600">
                    Sending from your own bank:{' '}
                    <span className="font-medium text-slate-800">
                        {currentUser.ownBankName ?? 'not set on your profile'}
                    </span>
                </div>
            )}

            <button
                type="submit"
                disabled={submitting}
                className="mt-5 w-full rounded-md bg-indigo-600 px-4 py-2.5 text-sm font-semibold text-white shadow-sm transition hover:bg-indigo-700 disabled:cursor-not-allowed disabled:opacity-60 sm:w-auto"
            >
                {submitting ? 'Submitting…' : 'Submit Payment'}
            </button>
        </form>
    );
}
