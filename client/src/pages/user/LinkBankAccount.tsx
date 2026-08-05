import { useState } from 'react';
import type { AxiosError } from 'axios';
import { usersApi } from '../../api/usersApi';
import { useUserSession } from '../../context/UserContext';
import Modal from '../../components/Modal';
import { formatWithInrEquivalent } from '../../utils/currency';
import type { ApiErrorResponse } from '../../types/payment';
import type { CreateBankAccountRequest } from '../../types/banking';

const EMPTY_FORM: CreateBankAccountRequest = {
    accountNumber: '',
    ifscCode: '',
    bankName: '',
    balance: 0,
    isPrimary: false,
};

export default function LinkBankAccount() {
    const { user, bankAccounts, refresh } = useUserSession();
    const [modalOpen, setModalOpen] = useState(false);
    const [form, setForm] = useState<CreateBankAccountRequest>(EMPTY_FORM);
    const [errors, setErrors] = useState<string[]>([]);
    const [submitting, setSubmitting] = useState(false);
    const [success, setSuccess] = useState<string | null>(null);

    function update<K extends keyof CreateBankAccountRequest>(
        key: K,
        value: CreateBankAccountRequest[K],
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
            const account = await usersApi.addBankAccount(user!.id, form);
            await refresh();
            setModalOpen(false);
            setSuccess(`Linked account ${account.accountNumber}.`);
            setForm(EMPTY_FORM);
        } catch (err) {
            const data = (err as AxiosError<ApiErrorResponse>).response?.data;
            setErrors(
                data?.details && data.details.length > 0
                    ? data.details
                    : [data?.message ?? 'Failed to link bank account'],
            );
        } finally {
            setSubmitting(false);
        }
    }

    const totalBalance = bankAccounts.reduce((sum, a) => sum + a.balance, 0);

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
                        Your Bank Accounts
                    </h2>
                    <p className="text-sm text-slate-500">
                        {bankAccounts.length} account
                        {bankAccounts.length === 1 ? '' : 's'} linked · Total
                        balance{' '}
                        <span className="font-semibold text-slate-700">
                            {formatWithInrEquivalent(totalBalance, 'INR')}
                        </span>
                    </p>
                </div>
                <button
                    onClick={openModal}
                    className="flex items-center gap-1.5 rounded-md bg-emerald-600 px-4 py-2 text-sm font-semibold text-white transition hover:bg-emerald-700"
                >
                    <span>➕</span> Link Account
                </button>
            </div>

            {bankAccounts.length === 0 ? (
                <div className="rounded-xl border border-dashed border-slate-300 bg-white/60 p-10 text-center shadow-sm">
                    <p className="text-3xl">🏦</p>
                    <p className="mt-2 text-sm font-medium text-slate-600">
                        No bank accounts linked yet
                    </p>
                    <p className="mt-1 text-sm text-slate-400">
                        Link a bank account to start making payments.
                    </p>
                    <button
                        onClick={openModal}
                        className="mt-4 rounded-md bg-emerald-600 px-4 py-2 text-sm font-semibold text-white transition hover:bg-emerald-700"
                    >
                        Link your first account
                    </button>
                </div>
            ) : (
                <div className="grid grid-cols-1 gap-4 sm:grid-cols-2">
                    {bankAccounts.map((a) => (
                        <div
                            key={a.id}
                            className="rounded-xl border border-slate-200 bg-white p-5 shadow-sm transition hover:shadow-md"
                        >
                            <div className="flex items-start justify-between">
                                <div className="flex items-center gap-2">
                                    <span className="flex h-9 w-9 items-center justify-center rounded-lg bg-emerald-100 text-lg">
                                        🏦
                                    </span>
                                    <div>
                                        <p className="font-semibold text-slate-800">
                                            {a.bankName}
                                        </p>
                                        <p className="text-xs text-slate-400">
                                            {a.accountNumber}
                                        </p>
                                    </div>
                                </div>
                                {a.primary && (
                                    <span className="rounded-full bg-emerald-100 px-2 py-0.5 text-[11px] font-semibold text-emerald-700">
                                        Primary
                                    </span>
                                )}
                            </div>
                            <div className="mt-4 flex items-center justify-between border-t border-slate-100 pt-3">
                                <span className="text-xs font-medium uppercase tracking-wide text-slate-400">
                                    Balance
                                </span>
                                <span className="text-lg font-bold text-slate-800">
                                    {formatWithInrEquivalent(a.balance, 'INR')}
                                </span>
                            </div>
                            <div className="mt-2 flex items-center justify-between text-xs text-slate-400">
                                <span>{a.ifscCode ?? '—'}</span>
                                <span
                                    className={
                                        a.status === 'ACTIVE'
                                            ? 'font-semibold text-emerald-600'
                                            : 'font-semibold text-amber-600'
                                    }
                                >
                                    {a.status}
                                </span>
                            </div>
                        </div>
                    ))}
                </div>
            )}

            <Modal
                open={modalOpen}
                onClose={() => setModalOpen(false)}
                title="Link a Bank Account"
                icon="🏦"
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
                                Bank Name
                            </label>
                            <input
                                required
                                value={form.bankName}
                                onChange={(e) => update('bankName', e.target.value)}
                                className="w-full rounded-md border border-slate-300 px-3 py-2 text-sm focus:border-emerald-500 focus:ring-2 focus:ring-emerald-100 focus:outline-none"
                            />
                        </div>
                        <div>
                            <label className="mb-1 block text-sm font-medium text-slate-600">
                                Account Number
                            </label>
                            <input
                                required
                                value={form.accountNumber}
                                onChange={(e) =>
                                    update('accountNumber', e.target.value)
                                }
                                className="w-full rounded-md border border-slate-300 px-3 py-2 text-sm focus:border-emerald-500 focus:ring-2 focus:ring-emerald-100 focus:outline-none"
                            />
                        </div>
                        <div>
                            <label className="mb-1 block text-sm font-medium text-slate-600">
                                IFSC Code
                            </label>
                            <input
                                value={form.ifscCode}
                                onChange={(e) => update('ifscCode', e.target.value)}
                                className="w-full rounded-md border border-slate-300 px-3 py-2 text-sm focus:border-emerald-500 focus:ring-2 focus:ring-emerald-100 focus:outline-none"
                            />
                        </div>
                        <div>
                            <label className="mb-1 block text-sm font-medium text-slate-600">
                                Opening Balance
                            </label>
                            <input
                                type="number"
                                min="0"
                                step="0.01"
                                value={form.balance}
                                onChange={(e) =>
                                    update('balance', Number(e.target.value))
                                }
                                className="w-full rounded-md border border-slate-300 px-3 py-2 text-sm focus:border-emerald-500 focus:ring-2 focus:ring-emerald-100 focus:outline-none"
                            />
                        </div>
                        <label className="flex items-center gap-2 rounded-lg border border-slate-100 bg-slate-50 px-3 py-2 text-sm text-slate-600">
                            <input
                                type="checkbox"
                                checked={form.isPrimary}
                                onChange={(e) =>
                                    update('isPrimary', e.target.checked)
                                }
                            />
                            Set as primary account
                        </label>
                    </div>

                    <button
                        type="submit"
                        disabled={submitting}
                        className="mt-5 w-full rounded-md bg-emerald-600 px-4 py-2 text-sm font-semibold text-white transition hover:bg-emerald-700 disabled:opacity-50"
                    >
                        {submitting ? 'Linking…' : 'Link Account'}
                    </button>
                </form>
            </Modal>
        </div>
    );
}
