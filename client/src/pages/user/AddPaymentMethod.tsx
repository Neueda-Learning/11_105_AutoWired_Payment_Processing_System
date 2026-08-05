import { useState } from 'react';
import type { AxiosError } from 'axios';
import { usersApi } from '../../api/usersApi';
import { useUserSession } from '../../context/UserContext';
import Modal from '../../components/Modal';
import { formatWithInrEquivalent } from '../../utils/currency';
import type { ApiErrorResponse } from '../../types/payment';
import type {
    CreatePaymentMethodRequest,
    PaymentMethodType,
} from '../../types/banking';

const EMPTY_FORM: CreatePaymentMethodRequest = {
    bankAccountId: 0,
    type: 'UPI',
    upiId: '',
    cardNumber: '',
    linkedBankName: '',
    isDefault: false,
};

const METHOD_ICON: Record<PaymentMethodType, string> = {
    UPI: '📲',
    CARD: '💳',
    NETBANKING: '🏛️',
};

export default function AddPaymentMethod() {
    const { user, bankAccounts, paymentMethods, refresh } = useUserSession();
    const [modalOpen, setModalOpen] = useState(false);
    const [form, setForm] = useState<CreatePaymentMethodRequest>(EMPTY_FORM);
    const [errors, setErrors] = useState<string[]>([]);
    const [submitting, setSubmitting] = useState(false);
    const [success, setSuccess] = useState<string | null>(null);

    function update<K extends keyof CreatePaymentMethodRequest>(
        key: K,
        value: CreatePaymentMethodRequest[K],
    ) {
        setForm((f) => ({ ...f, [key]: value }));
    }

    if (!user) {
        return (
            <p className="flex items-center gap-2 rounded-xl border border-amber-200 bg-amber-50 p-4 text-sm text-amber-700 shadow-sm">
                <span>⚠️</span> Please log in as a customer first.
            </p>
        );
    }

    if (bankAccounts.length === 0) {
        return (
            <p className="flex items-center gap-2 rounded-xl border border-amber-200 bg-amber-50 p-4 text-sm text-amber-700 shadow-sm">
                <span>⚠️</span> Link a bank account before adding a
                payment method.
            </p>
        );
    }

    function openModal() {
        setForm(EMPTY_FORM);
        setErrors([]);
        setSuccess(null);
        setModalOpen(true);
    }

    async function handleSubmit(e: React.FormEvent) {
        e.preventDefault();
        setErrors([]);
        setSubmitting(true);
        try {
            const method = await usersApi.addPaymentMethod(user!.id, form);
            await refresh();
            setModalOpen(false);
            setSuccess(`Added ${method.type} payment method.`);
        } catch (err) {
            const data = (err as AxiosError<ApiErrorResponse>).response?.data;
            setErrors(
                data?.details && data.details.length > 0
                    ? data.details
                    : [data?.message ?? 'Failed to add payment method'],
            );
        } finally {
            setSubmitting(false);
        }
    }

    return (
        <div className="space-y-6">
            {success && (
                <p className="flex items-center gap-2 rounded-md border border-emerald-200 bg-emerald-50 p-2 text-sm text-emerald-700">
                    <span>✅</span> {success}
                </p>
            )}

            <div className="flex items-center justify-between">
                <div>
                    <h2 className="text-lg font-semibold text-slate-800">
                        Your Payment Methods
                    </h2>
                    <p className="text-sm text-slate-500">
                        {paymentMethods.length} method
                        {paymentMethods.length === 1 ? '' : 's'} added
                    </p>
                </div>
                <button
                    onClick={openModal}
                    className="flex items-center gap-1.5 rounded-md bg-emerald-600 px-4 py-2 text-sm font-semibold text-white transition hover:bg-emerald-700"
                >
                    <span>➕</span> Add Method
                </button>
            </div>

            {paymentMethods.length === 0 ? (
                <div className="rounded-xl border border-dashed border-slate-300 bg-white/60 p-10 text-center shadow-sm">
                    <p className="text-3xl">💳</p>
                    <p className="mt-2 text-sm font-medium text-slate-600">
                        No payment methods yet
                    </p>
                    <p className="mt-1 text-sm text-slate-400">
                        Add a UPI ID, card, or net banking method to make
                        payments.
                    </p>
                    <button
                        onClick={openModal}
                        className="mt-4 rounded-md bg-emerald-600 px-4 py-2 text-sm font-semibold text-white transition hover:bg-emerald-700"
                    >
                        Add your first method
                    </button>
                </div>
            ) : (
                <div className="grid grid-cols-1 gap-4 sm:grid-cols-2">
                    {paymentMethods.map((m) => {
                        const account = bankAccounts.find(
                            (a) => a.id === m.bankAccountId,
                        );
                        return (
                            <div
                                key={m.id}
                                className="rounded-xl border border-slate-200 bg-white p-5 shadow-sm transition hover:shadow-md"
                            >
                                <div className="flex items-start justify-between">
                                    <div className="flex items-center gap-2">
                                        <span className="flex h-9 w-9 items-center justify-center rounded-lg bg-indigo-100 text-lg">
                                            {METHOD_ICON[m.type]}
                                        </span>
                                        <div>
                                            <p className="font-semibold text-slate-800">
                                                {m.type === 'UPI'
                                                    ? m.upiId
                                                    : m.type === 'CARD'
                                                        ? `•••• ${m.cardLast4}`
                                                        : m.linkedBankName}
                                            </p>
                                            <p className="text-xs text-slate-400">
                                                {m.type}
                                            </p>
                                        </div>
                                    </div>
                                    {m.default && (
                                        <span className="rounded-full bg-indigo-100 px-2 py-0.5 text-[11px] font-semibold text-indigo-700">
                                            Default
                                        </span>
                                    )}
                                </div>
                                {account && (
                                    <div className="mt-4 flex items-center justify-between border-t border-slate-100 pt-3 text-sm">
                                        <span className="text-slate-500">
                                            {account.bankName} — {account.accountNumber}
                                        </span>
                                        <span className="font-semibold text-slate-800">
                                            {formatWithInrEquivalent(account.balance, 'INR')}
                                        </span>
                                    </div>
                                )}
                            </div>
                        );
                    })}
                </div>
            )}

            <Modal
                open={modalOpen}
                onClose={() => setModalOpen(false)}
                title="Add a Payment Method"
                icon="💳"
            >
                <form onSubmit={handleSubmit}>
                    {errors.length > 0 && (
                        <div className="mb-4 rounded-md border border-red-200 bg-red-50 p-3 text-sm text-red-700">
                            <ul className="list-inside list-disc">
                                {errors.map((msg) => (
                                    <li key={msg}>{msg}</li>
                                ))}
                            </ul>
                        </div>
                    )}

                    <div className="space-y-3">
                        <div>
                            <label className="mb-1 block text-sm font-medium text-slate-600">
                                Bank Account
                            </label>
                            <select
                                required
                                value={form.bankAccountId || ''}
                                onChange={(e) =>
                                    update('bankAccountId', Number(e.target.value))
                                }
                                className="w-full rounded-md border border-slate-300 px-3 py-2 text-sm focus:border-emerald-500 focus:ring-2 focus:ring-emerald-100 focus:outline-none"
                            >
                                <option value="" disabled>
                                    Select account
                                </option>
                                {bankAccounts.map((a) => (
                                    <option key={a.id} value={a.id}>
                                        {a.bankName} — {a.accountNumber}
                                    </option>
                                ))}
                            </select>
                        </div>
                        <div>
                            <label className="mb-1 block text-sm font-medium text-slate-600">
                                Type
                            </label>
                            <select
                                value={form.type}
                                onChange={(e) =>
                                    update('type', e.target.value as PaymentMethodType)
                                }
                                className="w-full rounded-md border border-slate-300 px-3 py-2 text-sm focus:border-emerald-500 focus:ring-2 focus:ring-emerald-100 focus:outline-none"
                            >
                                <option value="UPI">UPI</option>
                                <option value="CARD">Card</option>
                                <option value="NETBANKING">Net Banking</option>
                            </select>
                        </div>

                        {form.type === 'UPI' && (
                            <div>
                                <label className="mb-1 block text-sm font-medium text-slate-600">
                                    UPI ID
                                </label>
                                <input
                                    required
                                    value={form.upiId}
                                    onChange={(e) => update('upiId', e.target.value)}
                                    placeholder="name@bank"
                                    className="w-full rounded-md border border-slate-300 px-3 py-2 text-sm focus:border-emerald-500 focus:ring-2 focus:ring-emerald-100 focus:outline-none"
                                />
                            </div>
                        )}
                        {form.type === 'CARD' && (
                            <div>
                                <label className="mb-1 block text-sm font-medium text-slate-600">
                                    Card Number
                                </label>
                                <input
                                    required
                                    value={form.cardNumber}
                                    onChange={(e) =>
                                        update('cardNumber', e.target.value)
                                    }
                                    placeholder="4111 1111 1111 1111"
                                    className="w-full rounded-md border border-slate-300 px-3 py-2 text-sm focus:border-emerald-500 focus:ring-2 focus:ring-emerald-100 focus:outline-none"
                                />
                            </div>
                        )}
                        {form.type === 'NETBANKING' && (
                            <div>
                                <label className="mb-1 block text-sm font-medium text-slate-600">
                                    Linked Bank Name
                                </label>
                                <input
                                    required
                                    value={form.linkedBankName}
                                    onChange={(e) =>
                                        update('linkedBankName', e.target.value)
                                    }
                                    className="w-full rounded-md border border-slate-300 px-3 py-2 text-sm focus:border-emerald-500 focus:ring-2 focus:ring-emerald-100 focus:outline-none"
                                />
                            </div>
                        )}

                        <label className="flex items-center gap-2 rounded-lg border border-slate-100 bg-slate-50 px-3 py-2 text-sm text-slate-600">
                            <input
                                type="checkbox"
                                checked={form.isDefault}
                                onChange={(e) =>
                                    update('isDefault', e.target.checked)
                                }
                            />
                            Set as default method
                        </label>
                    </div>

                    <button
                        type="submit"
                        disabled={submitting}
                        className="mt-5 w-full rounded-md bg-emerald-600 px-4 py-2 text-sm font-semibold text-white transition hover:bg-emerald-700 disabled:opacity-50"
                    >
                        {submitting ? 'Adding…' : 'Add Payment Method'}
                    </button>
                </form>
            </Modal>
        </div>
    );
}

