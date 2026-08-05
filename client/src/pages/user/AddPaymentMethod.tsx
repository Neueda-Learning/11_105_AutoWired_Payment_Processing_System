import { useState } from 'react';
import type { AxiosError } from 'axios';
import { usersApi } from '../../api/usersApi';
import { useUserSession } from '../../context/UserContext';
import Modal from '../../components/Modal';
import { formatWithInrEquivalent } from '../../utils/currency';
import type { ApiErrorResponse } from '../../types/payment';
import type {
    CreatePaymentMethodRequest,
    PaymentMethodEntity,
    PaymentMethodType,
    UpdatePaymentMethodRequest,
} from '../../types/banking';

const EMPTY_FORM: CreatePaymentMethodRequest = {
    bankAccountId: 0,
    type: 'UPI',
    upiId: '',
    cardNumber: '',
    cardExpiry: '',
    cardHolderName: '',
    linkedBankName: '',
    isDefault: false,
};

const EMPTY_EDIT_FORM: UpdatePaymentMethodRequest = {
    upiId: '',
    cardNumber: '',
    cardExpiry: '',
    cardHolderName: '',
    linkedBankName: '',
    isDefault: false,
};

const METHOD_ICON: Record<PaymentMethodType, string> = {
    UPI: '📲',
    CARD: '💳',
    NETBANKING: '🏛️',
};

// Must match PaymentValidationService.SUPPORTED_BANKS on the server, or
// net banking payments will always fail bank-name validation.
const NETBANKING_BANKS = [
    'HDFC Bank',
    'ICICI Bank',
    'State Bank of India',
    'Axis Bank',
    'Kotak Mahindra Bank',
    'Punjab National Bank',
    'Bank of Baroda',
    'Yes Bank',
];

export default function AddPaymentMethod() {
    const { user, bankAccounts, paymentMethods, refresh } = useUserSession();
    const [modalOpen, setModalOpen] = useState(false);
    const [form, setForm] = useState<CreatePaymentMethodRequest>(EMPTY_FORM);
    const [errors, setErrors] = useState<string[]>([]);
    const [submitting, setSubmitting] = useState(false);
    const [success, setSuccess] = useState<string | null>(null);

    const [editingMethod, setEditingMethod] = useState<PaymentMethodEntity | null>(
        null,
    );
    const [editForm, setEditForm] = useState<UpdatePaymentMethodRequest>(
        EMPTY_EDIT_FORM,
    );
    const [editErrors, setEditErrors] = useState<string[]>([]);
    const [editSubmitting, setEditSubmitting] = useState(false);

    const [deletingId, setDeletingId] = useState<number | null>(null);
    const [deleteError, setDeleteError] = useState<string | null>(null);
    const [deleting, setDeleting] = useState(false);

    function update<K extends keyof CreatePaymentMethodRequest>(
        key: K,
        value: CreatePaymentMethodRequest[K],
    ) {
        setForm((f) => ({ ...f, [key]: value }));
    }

    function updateEdit<K extends keyof UpdatePaymentMethodRequest>(
        key: K,
        value: UpdatePaymentMethodRequest[K],
    ) {
        setEditForm((f) => ({ ...f, [key]: value }));
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

    function openEditModal(method: PaymentMethodEntity) {
        setEditingMethod(method);
        setEditForm({
            upiId: method.upiId ?? '',
            cardNumber: '',
            cardExpiry: method.cardExpiry ?? '',
            cardHolderName: method.cardHolderName ?? '',
            linkedBankName: method.linkedBankName ?? '',
            isDefault: method.default,
        });
        setEditErrors([]);
        setSuccess(null);
    }

    async function handleEditSubmit(e: React.FormEvent) {
        e.preventDefault();
        if (!editingMethod) return;
        setEditErrors([]);
        setEditSubmitting(true);
        try {
            await usersApi.updatePaymentMethod(
                user!.id,
                editingMethod.id,
                editForm,
            );
            await refresh();
            setEditingMethod(null);
            setSuccess('Payment method updated.');
        } catch (err) {
            const data = (err as AxiosError<ApiErrorResponse>).response?.data;
            setEditErrors(
                data?.details && data.details.length > 0
                    ? data.details
                    : [data?.message ?? 'Failed to update payment method'],
            );
        } finally {
            setEditSubmitting(false);
        }
    }

    async function handleDelete(methodId: number) {
        setDeleteError(null);
        setDeleting(true);
        try {
            await usersApi.deletePaymentMethod(user!.id, methodId);
            await refresh();
            setDeletingId(null);
            setSuccess('Payment method removed.');
        } catch (err) {
            const data = (err as AxiosError<ApiErrorResponse>).response?.data;
            setDeleteError(data?.message ?? 'Failed to remove payment method');
        } finally {
            setDeleting(false);
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
                                    <div className="flex items-center gap-2">
                                        {m.default && (
                                            <span className="rounded-full bg-indigo-100 px-2 py-0.5 text-[11px] font-semibold text-indigo-700">
                                                Default
                                            </span>
                                        )}
                                        <button
                                            type="button"
                                            onClick={() => openEditModal(m)}
                                            title="Edit"
                                            className="rounded-md p-1.5 text-slate-400 transition hover:bg-slate-100 hover:text-slate-600"
                                        >
                                            ✏️
                                        </button>
                                        <button
                                            type="button"
                                            onClick={() => {
                                                setDeleteError(null);
                                                setDeletingId(m.id);
                                            }}
                                            title="Remove"
                                            className="rounded-md p-1.5 text-slate-400 transition hover:bg-red-50 hover:text-red-600"
                                        >
                                            🗑️
                                        </button>
                                    </div>
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
                                {deletingId === m.id && (
                                    <div className="mt-3 rounded-md border border-red-200 bg-red-50 p-3 text-sm text-red-700">
                                        {deleteError && (
                                            <p className="mb-2">{deleteError}</p>
                                        )}
                                        <p className="mb-2">
                                            Remove this payment method?
                                        </p>
                                        <div className="flex gap-2">
                                            <button
                                                type="button"
                                                disabled={deleting}
                                                onClick={() => handleDelete(m.id)}
                                                className="rounded-md bg-red-600 px-3 py-1.5 text-xs font-semibold text-white transition hover:bg-red-700 disabled:opacity-50"
                                            >
                                                {deleting ? 'Removing…' : 'Yes, remove'}
                                            </button>
                                            <button
                                                type="button"
                                                onClick={() => {
                                                    setDeletingId(null);
                                                    setDeleteError(null);
                                                }}
                                                className="rounded-md border border-slate-300 px-3 py-1.5 text-xs font-semibold text-slate-600 hover:bg-slate-50"
                                            >
                                                Cancel
                                            </button>
                                        </div>
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
                            <>
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
                                <div>
                                    <label className="mb-1 block text-sm font-medium text-slate-600">
                                        Card Holder Name
                                    </label>
                                    <input
                                        required
                                        value={form.cardHolderName}
                                        onChange={(e) =>
                                            update('cardHolderName', e.target.value)
                                        }
                                        placeholder="Jane Doe"
                                        className="w-full rounded-md border border-slate-300 px-3 py-2 text-sm focus:border-emerald-500 focus:ring-2 focus:ring-emerald-100 focus:outline-none"
                                    />
                                </div>
                                <div>
                                    <label className="mb-1 block text-sm font-medium text-slate-600">
                                        Card Expiry (MM/YYYY)
                                    </label>
                                    <input
                                        required
                                        value={form.cardExpiry}
                                        onChange={(e) =>
                                            update('cardExpiry', e.target.value)
                                        }
                                        placeholder="12/2028"
                                        pattern="(0[1-9]|1[0-2])/[0-9]{4}"
                                        title="Format: MM/YYYY"
                                        className="w-full rounded-md border border-slate-300 px-3 py-2 text-sm focus:border-emerald-500 focus:ring-2 focus:ring-emerald-100 focus:outline-none"
                                    />
                                </div>
                            </>
                        )}
                        {form.type === 'NETBANKING' && (
                            <div>
                                <label className="mb-1 block text-sm font-medium text-slate-600">
                                    Linked Bank Name
                                </label>
                                <select
                                    required
                                    value={form.linkedBankName}
                                    onChange={(e) =>
                                        update('linkedBankName', e.target.value)
                                    }
                                    className="w-full rounded-md border border-slate-300 px-3 py-2 text-sm focus:border-emerald-500 focus:ring-2 focus:ring-emerald-100 focus:outline-none"
                                >
                                    <option value="" disabled>
                                        Select a bank
                                    </option>
                                    {NETBANKING_BANKS.map((bank) => (
                                        <option key={bank} value={bank}>
                                            {bank}
                                        </option>
                                    ))}
                                </select>
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

            <Modal
                open={editingMethod !== null}
                onClose={() => setEditingMethod(null)}
                title="Edit Payment Method"
                icon="✏️"
            >
                {editingMethod && (
                    <form onSubmit={handleEditSubmit}>
                        {editErrors.length > 0 && (
                            <div className="mb-4 rounded-md border border-red-200 bg-red-50 p-3 text-sm text-red-700">
                                <ul className="list-inside list-disc">
                                    {editErrors.map((msg) => (
                                        <li key={msg}>{msg}</li>
                                    ))}
                                </ul>
                            </div>
                        )}

                        <div className="space-y-3">
                            <p className="text-sm text-slate-500">
                                Type:{' '}
                                <span className="font-semibold text-slate-700">
                                    {editingMethod.type}
                                </span>
                            </p>

                            {editingMethod.type === 'UPI' && (
                                <div>
                                    <label className="mb-1 block text-sm font-medium text-slate-600">
                                        UPI ID
                                    </label>
                                    <input
                                        required
                                        value={editForm.upiId}
                                        onChange={(e) =>
                                            updateEdit('upiId', e.target.value)
                                        }
                                        placeholder="name@bank"
                                        className="w-full rounded-md border border-slate-300 px-3 py-2 text-sm focus:border-emerald-500 focus:ring-2 focus:ring-emerald-100 focus:outline-none"
                                    />
                                </div>
                            )}
                            {editingMethod.type === 'CARD' && (
                                <>
                                    <div>
                                        <label className="mb-1 block text-sm font-medium text-slate-600">
                                            New Card Number
                                        </label>
                                        <input
                                            value={editForm.cardNumber}
                                            onChange={(e) =>
                                                updateEdit('cardNumber', e.target.value)
                                            }
                                            placeholder={`Leave blank to keep •••• ${editingMethod.cardLast4}`}
                                            className="w-full rounded-md border border-slate-300 px-3 py-2 text-sm focus:border-emerald-500 focus:ring-2 focus:ring-emerald-100 focus:outline-none"
                                        />
                                    </div>
                                    <div>
                                        <label className="mb-1 block text-sm font-medium text-slate-600">
                                            Card Holder Name
                                        </label>
                                        <input
                                            required
                                            value={editForm.cardHolderName}
                                            onChange={(e) =>
                                                updateEdit('cardHolderName', e.target.value)
                                            }
                                            placeholder="Jane Doe"
                                            className="w-full rounded-md border border-slate-300 px-3 py-2 text-sm focus:border-emerald-500 focus:ring-2 focus:ring-emerald-100 focus:outline-none"
                                        />
                                    </div>
                                    <div>
                                        <label className="mb-1 block text-sm font-medium text-slate-600">
                                            Card Expiry (MM/YYYY)
                                        </label>
                                        <input
                                            required
                                            value={editForm.cardExpiry}
                                            onChange={(e) =>
                                                updateEdit('cardExpiry', e.target.value)
                                            }
                                            placeholder="12/2028"
                                            pattern="(0[1-9]|1[0-2])/[0-9]{4}"
                                            title="Format: MM/YYYY"
                                            className="w-full rounded-md border border-slate-300 px-3 py-2 text-sm focus:border-emerald-500 focus:ring-2 focus:ring-emerald-100 focus:outline-none"
                                        />
                                    </div>
                                </>
                            )}
                            {editingMethod.type === 'NETBANKING' && (
                                <div>
                                    <label className="mb-1 block text-sm font-medium text-slate-600">
                                        Linked Bank Name
                                    </label>
                                    <select
                                        required
                                        value={editForm.linkedBankName}
                                        onChange={(e) =>
                                            updateEdit(
                                                'linkedBankName',
                                                e.target.value,
                                            )
                                        }
                                        className="w-full rounded-md border border-slate-300 px-3 py-2 text-sm focus:border-emerald-500 focus:ring-2 focus:ring-emerald-100 focus:outline-none"
                                    >
                                        <option value="" disabled>
                                            Select a bank
                                        </option>
                                        {NETBANKING_BANKS.map((bank) => (
                                            <option key={bank} value={bank}>
                                                {bank}
                                            </option>
                                        ))}
                                    </select>
                                </div>
                            )}

                            <label className="flex items-center gap-2 rounded-lg border border-slate-100 bg-slate-50 px-3 py-2 text-sm text-slate-600">
                                <input
                                    type="checkbox"
                                    checked={editForm.isDefault}
                                    onChange={(e) =>
                                        updateEdit('isDefault', e.target.checked)
                                    }
                                />
                                Set as default method
                            </label>
                        </div>

                        <button
                            type="submit"
                            disabled={editSubmitting}
                            className="mt-5 w-full rounded-md bg-emerald-600 px-4 py-2 text-sm font-semibold text-white transition hover:bg-emerald-700 disabled:opacity-50"
                        >
                            {editSubmitting ? 'Saving…' : 'Save Changes'}
                        </button>
                    </form>
                )}
            </Modal>
        </div>
    );
}

