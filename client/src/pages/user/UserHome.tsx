import { useEffect, useState } from 'react';
import type { AxiosError } from 'axios';
import { usersApi } from '../../api/usersApi';
import { useUserSession } from '../../context/UserContext';
import Modal from '../../components/Modal';
import type { ApiErrorResponse } from '../../types/payment';
import type { UpdatePinRequest, User } from '../../types/banking';

const EMPTY_PIN_FORM = { currentPin: '', newPin: '', confirmPin: '' };

export default function UserHome() {
    const { user, bankAccounts, paymentMethods, loading, selectUser } =
        useUserSession();
    const [allUsers, setAllUsers] = useState<User[]>([]);
    const [usersLoading, setUsersLoading] = useState(true);
    const [selecting, setSelecting] = useState(false);
    const [error, setError] = useState<string | null>(null);

    const [pinModalOpen, setPinModalOpen] = useState(false);
    const [pinForm, setPinForm] = useState(EMPTY_PIN_FORM);
    const [pinErrors, setPinErrors] = useState<string[]>([]);
    const [pinSubmitting, setPinSubmitting] = useState(false);
    const [pinSuccess, setPinSuccess] = useState<string | null>(null);

    useEffect(() => {
        let cancelled = false;
        async function load() {
            try {
                const data = await usersApi.getAll();
                if (!cancelled) setAllUsers(data);
            } catch {
                if (!cancelled) setError('Unable to load users.');
            } finally {
                if (!cancelled) setUsersLoading(false);
            }
        }
        load();
        return () => {
            cancelled = true;
        };
    }, []);

    async function handleSelect(userId: number) {
        setSelecting(true);
        setError(null);
        try {
            await selectUser(userId);
        } catch {
            setError('Unable to log in as that user.');
        } finally {
            setSelecting(false);
        }
    }

    function openPinModal() {
        setPinForm(EMPTY_PIN_FORM);
        setPinErrors([]);
        setPinModalOpen(true);
    }

    async function handlePinSubmit(e: React.FormEvent) {
        e.preventDefault();
        setPinErrors([]);

        if (!/^\d{4,6}$/.test(pinForm.newPin)) {
            setPinErrors(['New PIN must be 4-6 digits.']);
            return;
        }
        if (pinForm.newPin !== pinForm.confirmPin) {
            setPinErrors(['New PIN and confirmation do not match.']);
            return;
        }

        setPinSubmitting(true);
        try {
            const payload: UpdatePinRequest = {
                currentPin: pinForm.currentPin,
                newPin: pinForm.newPin,
            };
            await usersApi.updatePin(user!.id, payload);
            setPinModalOpen(false);
            setPinSuccess('Your PIN has been updated.');
            setPinForm(EMPTY_PIN_FORM);
        } catch (err) {
            const data = (err as AxiosError<ApiErrorResponse>).response?.data;
            setPinErrors(
                data?.details && data.details.length > 0
                    ? data.details
                    : [data?.message ?? 'Failed to update PIN'],
            );
        } finally {
            setPinSubmitting(false);
        }
    }

    function updatePinField<K extends keyof typeof EMPTY_PIN_FORM>(
        key: K,
        value: string,
    ) {
        setPinForm((f) => ({ ...f, [key]: value }));
    }

    if (loading) {
        return <p className="text-sm text-slate-400">Loading…</p>;
    }

    if (user) {
        const initials = user.fullName
            .split(' ')
            .map((p) => p[0])
            .slice(0, 2)
            .join('')
            .toUpperCase();

        return (
            <div className="space-y-6">
                <div className="overflow-hidden rounded-xl border border-slate-200 bg-white shadow-sm">
                    <div className="flex items-center gap-4 bg-linear-to-r from-emerald-600 to-teal-600 px-6 py-6 text-white">
                        <span className="flex h-14 w-14 shrink-0 items-center justify-center rounded-full bg-white/15 text-xl font-bold ring-2 ring-white/30">
                            {initials}
                        </span>
                        <div>
                            <h2 className="text-xl font-bold">{user.fullName}</h2>
                            <p className="text-sm text-emerald-50/90">{user.email}</p>
                        </div>
                        <span className="ml-auto rounded-full bg-white/15 px-3 py-1 text-xs font-semibold uppercase tracking-wide">
                            {user.kycStatus}
                        </span>
                    </div>

                    <dl className="grid grid-cols-2 gap-4 p-6 sm:grid-cols-4">
                        <div>
                            <dt className="text-xs font-medium uppercase tracking-wide text-slate-400">
                                User ID
                            </dt>
                            <dd className="mt-1 text-sm font-semibold text-slate-800">
                                #{user.id}
                            </dd>
                        </div>
                        <div>
                            <dt className="text-xs font-medium uppercase tracking-wide text-slate-400">
                                Bank Accounts
                            </dt>
                            <dd className="mt-1 text-sm font-semibold text-slate-800">
                                {bankAccounts.length}
                            </dd>
                        </div>
                        <div>
                            <dt className="text-xs font-medium uppercase tracking-wide text-slate-400">
                                Payment Methods
                            </dt>
                            <dd className="mt-1 text-sm font-semibold text-slate-800">
                                {paymentMethods.length}
                            </dd>
                        </div>
                        <div>
                            <dt className="text-xs font-medium uppercase tracking-wide text-slate-400">
                                KYC Status
                            </dt>
                            <dd className="mt-1 text-sm font-semibold text-slate-800">
                                {user.kycStatus}
                            </dd>
                        </div>
                    </dl>
                </div>

                <div className="grid grid-cols-1 gap-4 sm:grid-cols-2">
                    <div className="rounded-xl border border-slate-200 bg-white p-5 shadow-sm">
                        <div className="flex items-center gap-2">
                            <span className="flex h-7 w-7 items-center justify-center rounded-md bg-emerald-100 text-sm">
                                🏦
                            </span>
                            <p className="text-sm font-semibold text-slate-700">
                                Bank Accounts ({bankAccounts.length})
                            </p>
                        </div>
                        {bankAccounts.length === 0 ? (
                            <p className="mt-3 text-sm text-slate-400">
                                No accounts linked yet.
                            </p>
                        ) : (
                            <ul className="mt-3 space-y-2">
                                {bankAccounts.map((a) => (
                                    <li
                                        key={a.id}
                                        className="flex items-center justify-between rounded-lg border border-slate-100 bg-slate-50 px-3 py-2 text-sm"
                                    >
                                        <span className="text-slate-700">
                                            {a.bankName} — {a.accountNumber}
                                        </span>
                                        {a.primary && (
                                            <span className="rounded-full bg-emerald-100 px-2 py-0.5 text-[11px] font-semibold text-emerald-700">
                                                Primary
                                            </span>
                                        )}
                                    </li>
                                ))}
                            </ul>
                        )}
                    </div>
                    <div className="rounded-xl border border-slate-200 bg-white p-5 shadow-sm">
                        <div className="flex items-center gap-2">
                            <span className="flex h-7 w-7 items-center justify-center rounded-md bg-indigo-100 text-sm">
                                💳
                            </span>
                            <p className="text-sm font-semibold text-slate-700">
                                Payment Methods ({paymentMethods.length})
                            </p>
                        </div>
                        {paymentMethods.length === 0 ? (
                            <p className="mt-3 text-sm text-slate-400">
                                No payment methods added yet.
                            </p>
                        ) : (
                            <ul className="mt-3 space-y-2">
                                {paymentMethods.map((m) => (
                                    <li
                                        key={m.id}
                                        className="flex items-center justify-between rounded-lg border border-slate-100 bg-slate-50 px-3 py-2 text-sm"
                                    >
                                        <span className="text-slate-700">
                                            {m.type}
                                            {m.upiId ? ` — ${m.upiId}` : ''}
                                            {m.cardLast4 ? ` — •••• ${m.cardLast4}` : ''}
                                        </span>
                                        {m.default && (
                                            <span className="rounded-full bg-indigo-100 px-2 py-0.5 text-[11px] font-semibold text-indigo-700">
                                                Default
                                            </span>
                                        )}
                                    </li>
                                ))}
                            </ul>
                        )}
                    </div>
                </div>

                <div className="rounded-xl border border-slate-200 bg-white p-5 shadow-sm">
                    {pinSuccess && (
                        <p className="mb-4 flex items-center gap-2 rounded-md border border-emerald-200 bg-emerald-50 p-2 text-sm text-emerald-700">
                            <span>✅</span> {pinSuccess}
                        </p>
                    )}
                    <div className="flex items-center justify-between">
                        <div className="flex items-center gap-2">
                            <span className="flex h-7 w-7 items-center justify-center rounded-md bg-amber-100 text-sm">
                                🔒
                            </span>
                            <div>
                                <p className="text-sm font-semibold text-slate-700">
                                    Security
                                </p>
                                <p className="text-xs text-slate-400">
                                    Update the PIN used to authenticate your
                                    payments.
                                </p>
                            </div>
                        </div>
                        <button
                            onClick={openPinModal}
                            className="flex items-center gap-1.5 rounded-md bg-slate-800 px-4 py-2 text-sm font-semibold text-white transition hover:bg-slate-900"
                        >
                            Change PIN
                        </button>
                    </div>
                </div>

                <Modal
                    open={pinModalOpen}
                    onClose={() => setPinModalOpen(false)}
                    title="Change PIN"
                    icon="🔒"
                >
                    <form onSubmit={handlePinSubmit} className="space-y-4">
                        {pinErrors.length > 0 && (
                            <ul className="space-y-1 rounded-md border border-red-200 bg-red-50 p-3 text-sm text-red-700">
                                {pinErrors.map((err, i) => (
                                    <li key={i}>{err}</li>
                                ))}
                            </ul>
                        )}
                        <div>
                            <label className="mb-1 block text-xs font-medium uppercase tracking-wide text-slate-500">
                                Current PIN
                            </label>
                            <input
                                type="password"
                                inputMode="numeric"
                                maxLength={6}
                                required
                                value={pinForm.currentPin}
                                onChange={(e) =>
                                    updatePinField('currentPin', e.target.value)
                                }
                                className="w-full rounded-md border border-slate-300 px-3 py-2 text-sm focus:border-emerald-500 focus:outline-none focus:ring-1 focus:ring-emerald-500"
                            />
                        </div>
                        <div>
                            <label className="mb-1 block text-xs font-medium uppercase tracking-wide text-slate-500">
                                New PIN
                            </label>
                            <input
                                type="password"
                                inputMode="numeric"
                                maxLength={6}
                                required
                                value={pinForm.newPin}
                                onChange={(e) =>
                                    updatePinField('newPin', e.target.value)
                                }
                                className="w-full rounded-md border border-slate-300 px-3 py-2 text-sm focus:border-emerald-500 focus:outline-none focus:ring-1 focus:ring-emerald-500"
                            />
                        </div>
                        <div>
                            <label className="mb-1 block text-xs font-medium uppercase tracking-wide text-slate-500">
                                Confirm New PIN
                            </label>
                            <input
                                type="password"
                                inputMode="numeric"
                                maxLength={6}
                                required
                                value={pinForm.confirmPin}
                                onChange={(e) =>
                                    updatePinField('confirmPin', e.target.value)
                                }
                                className="w-full rounded-md border border-slate-300 px-3 py-2 text-sm focus:border-emerald-500 focus:outline-none focus:ring-1 focus:ring-emerald-500"
                            />
                        </div>
                        <button
                            type="submit"
                            disabled={pinSubmitting}
                            className="w-full rounded-md bg-emerald-600 px-4 py-2 text-sm font-semibold text-white transition hover:bg-emerald-700 disabled:opacity-50"
                        >
                            {pinSubmitting ? 'Updating…' : 'Update PIN'}
                        </button>
                    </form>
                </Modal>
            </div>
        );
    }

    return (
        <div className="mx-auto max-w-lg rounded-xl border border-slate-200 bg-white shadow-sm">
            <div className="flex items-center gap-3 border-b border-slate-100 px-6 py-5">
                <span className="flex h-10 w-10 items-center justify-center rounded-lg bg-emerald-100 text-lg">
                    🙋
                </span>
                <div>
                    <h2 className="text-lg font-semibold text-slate-800">Log in as</h2>
                    <p className="text-sm text-slate-500">
                        Select an existing customer. New customers are
                        onboarded by a bank admin.
                    </p>
                </div>
            </div>

            <div className="p-6 pt-4">
                {error && (
                    <p className="mb-4 rounded-md border border-red-200 bg-red-50 p-2 text-sm text-red-700">
                        {error}
                    </p>
                )}

                {usersLoading ? (
                    <p className="text-sm text-slate-400">Loading users…</p>
                ) : allUsers.length === 0 ? (
                    <p className="text-sm text-slate-400">
                        No customers registered yet. Ask a bank admin to
                        register one.
                    </p>
                ) : (
                    <ul className="divide-y divide-slate-100 overflow-hidden rounded-lg border border-slate-100">
                        {allUsers.map((u) => {
                            const initials = u.fullName
                                .split(' ')
                                .map((p) => p[0])
                                .slice(0, 2)
                                .join('')
                                .toUpperCase();
                            return (
                                <li key={u.id}>
                                    <button
                                        disabled={selecting}
                                        onClick={() => handleSelect(u.id)}
                                        className="flex w-full items-center gap-3 px-4 py-3 text-left text-sm transition hover:bg-emerald-50 disabled:opacity-50"
                                    >
                                        <span className="flex h-9 w-9 shrink-0 items-center justify-center rounded-full bg-emerald-100 text-xs font-bold text-emerald-700">
                                            {initials}
                                        </span>
                                        <span className="min-w-0 flex-1">
                                            <span className="block truncate font-medium text-slate-800">
                                                {u.fullName}
                                            </span>
                                            <span className="block truncate text-xs text-slate-400">
                                                {u.email}
                                            </span>
                                        </span>
                                        <span className="shrink-0 text-xs font-semibold text-emerald-600">
                                            Log in as →
                                        </span>
                                    </button>
                                </li>
                            );
                        })}
                    </ul>
                )}
            </div>
        </div>
    );
}

