import { useEffect, useMemo, useState } from 'react';
import type { AxiosError } from 'axios';
import { authApi } from '../../api/authApi';
import { usersApi } from '../../api/usersApi';
import { feeRulesApi } from '../../api/feeRulesApi';
import { useUserSession } from '../../context/UserContext';
import type { ApiErrorResponse, Payment } from '../../types/payment';
import type {
    AuthMethod,
    BankAccount,
    InitiatePaymentRequest,
    PaymentMethodType,
    TransactionFeeRule,
    User,
} from '../../types/banking';
import { DEFAULT_CURRENCY, formatINR, toINR } from '../../utils/currency';
import { calculateFeePreview } from '../../utils/feeCalculation';

function newIdempotencyKey() {
    return typeof crypto !== 'undefined' && 'randomUUID' in crypto
        ? crypto.randomUUID()
        : `key-${Date.now()}-${Math.random().toString(16).slice(2)}`;
}

interface FormState {
    sourcePaymentMethodId: number | '';
    destinationAccountId: number | '';
    amount: number | '';
    currency: string;
    authMethod: AuthMethod;
    reference: string;
    cvv: string;
}

export default function MakePayment() {
    const { user, bankAccounts, paymentMethods } = useUserSession();
    const [otherAccounts, setOtherAccounts] = useState<BankAccount[]>([]);
    const [accountHolders, setAccountHolders] = useState<Map<number, string>>(
        new Map(),
    );
    const [accountsLoading, setAccountsLoading] = useState(true);
    const [feeRules, setFeeRules] = useState<TransactionFeeRule[]>([]);
    const [form, setForm] = useState<FormState>({
        sourcePaymentMethodId: '',
        destinationAccountId: '',
        amount: '',
        currency: DEFAULT_CURRENCY,
        authMethod: 'PIN',
        reference: '',
        cvv: '',
    });
    const [errors, setErrors] = useState<string[]>([]);
    const [submitting, setSubmitting] = useState(false);
    const [pendingPayment, setPendingPayment] = useState<Payment | null>(null);
    const [code, setCode] = useState('');
    const [authError, setAuthError] = useState<string | null>(null);
    const [authenticating, setAuthenticating] = useState(false);
    const [completed, setCompleted] = useState<Payment | null>(null);

    useEffect(() => {
        let cancelled = false;
        async function load() {
            try {
                const [all, users] = await Promise.all([
                    usersApi.getAllBankAccounts(),
                    usersApi.getAll(),
                ]);
                if (!cancelled) {
                    setOtherAccounts(all.filter((a) => a.userId !== user?.id));
                    setAccountHolders(
                        new Map(users.map((u: User) => [u.id, u.fullName])),
                    );
                }
            } finally {
                if (!cancelled) setAccountsLoading(false);
            }
        }
        load();
        return () => {
            cancelled = true;
        };
    }, [user?.id]);

    useEffect(() => {
        let cancelled = false;
        feeRulesApi
            .getAll()
            .then((rules) => {
                if (!cancelled) setFeeRules(rules);
            })
            .catch(() => {
                // Fee preview is best-effort; ignore failures here.
            });
        return () => {
            cancelled = true;
        };
    }, []);

    if (!user) {
        return (
            <p className="flex items-center gap-2 rounded-xl border border-amber-200 bg-amber-50 p-4 text-sm text-amber-700 shadow-sm">
                <span>⚠️</span> Please log in as a customer first.
            </p>
        );
    }

    if (paymentMethods.length === 0 || bankAccounts.length === 0) {
        return (
            <p className="flex items-center gap-2 rounded-xl border border-amber-200 bg-amber-50 p-4 text-sm text-amber-700 shadow-sm">
                <span>⚠️</span> Link a bank account and add a payment
                method before making a payment.
            </p>
        );
    }

    function update<K extends keyof FormState>(key: K, value: FormState[K]) {
        setForm((f) => ({ ...f, [key]: value }));
    }

    function methodTypeToPaymentMethod(
        type: PaymentMethodType,
    ): InitiatePaymentRequest['paymentMethod'] {
        if (type === 'CARD') return 'CREDIT_CARD';
        if (type === 'NETBANKING') return 'NETBANKING';
        return 'UPI';
    }

    const selectedSourceMethod = paymentMethods.find(
        (m) => m.id === form.sourcePaymentMethodId,
    );
    const feePreview = useMemo(() => {
        if (form.amount === '' || !selectedSourceMethod) return null;
        return calculateFeePreview(
            feeRules,
            methodTypeToPaymentMethod(selectedSourceMethod.type),
            Number(form.amount),
        );
    }, [feeRules, form.amount, selectedSourceMethod]);

    async function handleInitiate(e: React.FormEvent) {
        e.preventDefault();
        setErrors([]);

        const selectedMethod = paymentMethods.find(
            (m) => m.id === form.sourcePaymentMethodId,
        );
        const selectedSourceAccount = bankAccounts.find(
            (a) => a.id === selectedMethod?.bankAccountId,
        );
        const selectedDestination = otherAccounts.find(
            (a) => a.id === form.destinationAccountId,
        );

        if (!selectedMethod || !selectedSourceAccount) {
            setErrors(['Please select a payment method to pay from.']);
            return;
        }
        if (!selectedDestination) {
            setErrors(['Please select a destination account.']);
            return;
        }
        if (selectedMethod.type === 'CARD' && !/^\d{3,4}$/.test(form.cvv)) {
            setErrors(['Please enter a valid 3-4 digit CVV for this card.']);
            return;
        }

        setSubmitting(true);
        try {
            const payload: InitiatePaymentRequest = {
                payerUserId: user!.id,
                payeeUserId: selectedDestination.userId,
                sourceAccount: selectedSourceAccount.accountNumber,
                destinationAccount: selectedDestination.accountNumber,
                sourcePaymentMethodId: selectedMethod.id,
                amount: Number(form.amount),
                currency: form.currency,
                paymentMethod: methodTypeToPaymentMethod(selectedMethod.type),
                reference: form.reference || undefined,
                idempotencyKey: newIdempotencyKey(),
                authMethod: form.authMethod,
                upiId: selectedMethod.upiId,
                bankName:
                    selectedMethod.type === 'NETBANKING'
                        ? selectedMethod.linkedBankName
                        : undefined,
                cvv: selectedMethod.type === 'CARD' ? form.cvv : undefined,
            };

            const payment = await authApi.initiate(payload);
            setPendingPayment(payment);
        } catch (err) {
            const data = (err as AxiosError<ApiErrorResponse>).response?.data;
            setErrors(
                data?.details && data.details.length > 0
                    ? data.details
                    : [data?.message ?? 'Failed to initiate payment'],
            );
        } finally {
            setSubmitting(false);
        }
    }

    async function handleAuthenticate(e: React.FormEvent) {
        e.preventDefault();
        if (!pendingPayment) return;
        setAuthError(null);
        setAuthenticating(true);
        try {
            const payload =
                form.authMethod === 'OTP'
                    ? { method: 'OTP' as const, otp: code }
                    : { method: 'PIN' as const, pin: code };
            const result = await authApi.authenticate(pendingPayment.id, payload);
            setCompleted(result);
            setPendingPayment(null);
        } catch (err) {
            const data = (err as AxiosError<ApiErrorResponse>).response?.data;
            setAuthError(data?.message ?? 'Authentication failed');
        } finally {
            setAuthenticating(false);
        }
    }

    async function handleResend() {
        if (!pendingPayment) return;
        try {
            await authApi.resendOtp(pendingPayment.id);
            setAuthError('A new OTP has been sent.');
        } catch (err) {
            const data = (err as AxiosError<ApiErrorResponse>).response?.data;
            setAuthError(data?.message ?? 'Failed to resend OTP');
        }
    }

    if (completed) {
        return (
            <div className="mx-auto max-w-md overflow-hidden rounded-xl border border-emerald-200 bg-white text-center shadow-sm">
                <div className="bg-linear-to-r from-emerald-500 to-teal-500 px-6 py-8 text-white">
                    <p className="text-4xl">✅</p>
                    <h2 className="mt-2 text-lg font-bold">
                        Payment #{completed.id} authenticated!
                    </h2>
                </div>
                <div className="px-6 py-5">
                    <p className="text-sm text-slate-500">
                        Amount Sent
                    </p>
                    <p className="text-2xl font-bold text-slate-800">
                        {completed.amount.toFixed(2)} {completed.currency}
                    </p>
                    {completed.currency !== 'INR' && (
                        <p className="text-xs text-slate-400">
                            ≈ {formatINR(toINR(completed.amount, completed.currency))}
                        </p>
                    )}

                    {completed.feeAmount != null && completed.netAmount != null && (
                        <div className="mt-4 space-y-1 rounded-md border border-slate-200 bg-slate-50 p-3 text-left text-sm">
                            <div className="flex justify-between text-slate-500">
                                <span>Payment Amount</span>
                                <span>
                                    {completed.amount.toFixed(2)} {completed.currency}
                                </span>
                            </div>
                            <div className="flex justify-between text-slate-500">
                                <span>Transaction Fee</span>
                                <span>
                                    {completed.feeAmount.toFixed(2)} {completed.currency}
                                </span>
                            </div>
                            <div className="flex justify-between border-t border-slate-200 pt-1 font-semibold text-slate-800">
                                <span>Total Debited</span>
                                <span>
                                    {completed.netAmount.toFixed(2)} {completed.currency}
                                </span>
                            </div>
                        </div>
                    )}

                    <p className="mt-3 inline-block rounded-full bg-emerald-100 px-3 py-1 text-xs font-semibold text-emerald-700">
                        {completed.status}
                    </p>
                    <button
                        onClick={() => setCompleted(null)}
                        className="mt-5 w-full rounded-md bg-emerald-600 px-4 py-2 text-sm font-semibold text-white hover:bg-emerald-700"
                    >
                        Make another payment
                    </button>
                </div>
            </div>
        );
    }

    if (pendingPayment) {
        return (
            <form
                onSubmit={handleAuthenticate}
                className="mx-auto max-w-md rounded-xl border border-slate-200 bg-white p-6 shadow-sm"
            >
                <div className="flex items-center gap-2">
                    <span className="flex h-8 w-8 items-center justify-center rounded-lg bg-emerald-100 text-lg">
                        🔐
                    </span>
                    <h2 className="text-lg font-semibold text-slate-800">
                        Authenticate Payment #{pendingPayment.id}
                    </h2>
                </div>
                <p className="mt-1 text-sm text-slate-500">
                    Enter your {form.authMethod === 'OTP' ? 'OTP' : 'PIN'} to
                    confirm this payment of {pendingPayment.amount}{' '}
                    {pendingPayment.currency}
                    {pendingPayment.currency !== 'INR'
                        ? ` (≈ ${formatINR(toINR(pendingPayment.amount, pendingPayment.currency))})`
                        : ''}
                    .
                </p>

                {authError && (
                    <p className="mt-3 rounded-md border border-amber-200 bg-amber-50 p-2 text-sm text-amber-700">
                        {authError}
                    </p>
                )}

                <input
                    required
                    value={code}
                    onChange={(e) => setCode(e.target.value)}
                    placeholder={form.authMethod === 'OTP' ? '6-digit OTP' : '4-6 digit PIN'}
                    className="mt-4 w-full rounded-md border border-slate-300 px-3 py-2 text-sm focus:border-emerald-500 focus:ring-2 focus:ring-emerald-100 focus:outline-none"
                />

                <div className="mt-4 flex gap-2">
                    <button
                        type="submit"
                        disabled={authenticating}
                        className="flex-1 rounded-md bg-emerald-600 px-4 py-2 text-sm font-semibold text-white transition hover:bg-emerald-700 disabled:opacity-50"
                    >
                        {authenticating ? 'Verifying…' : 'Verify'}
                    </button>
                    {form.authMethod === 'OTP' && (
                        <button
                            type="button"
                            onClick={handleResend}
                            className="rounded-md border border-slate-300 px-4 py-2 text-sm font-semibold text-slate-600 hover:bg-slate-50"
                        >
                            Resend OTP
                        </button>
                    )}
                </div>
            </form>
        );
    }

    return (
        <form
            onSubmit={handleInitiate}
            className="mx-auto max-w-md rounded-xl border border-slate-200 bg-white p-6 shadow-sm"
        >
            <div className="flex items-center gap-2">
                <span className="flex h-8 w-8 items-center justify-center rounded-lg bg-emerald-100 text-lg">
                    📤
                </span>
                <h2 className="text-lg font-semibold text-slate-800">
                    Make a Payment
                </h2>
            </div>

            {errors.length > 0 && (
                <div className="mt-4 rounded-md border border-red-200 bg-red-50 p-3 text-sm text-red-700">
                    <ul className="list-inside list-disc">
                        {errors.map((msg) => (
                            <li key={msg}>{msg}</li>
                        ))}
                    </ul>
                </div>
            )}

            <div className="mt-4 space-y-3">
                <div>
                    <label className="mb-1 block text-sm font-medium text-slate-600">
                        Pay From
                    </label>
                    <select
                        required
                        value={form.sourcePaymentMethodId}
                        onChange={(e) =>
                            update('sourcePaymentMethodId', Number(e.target.value))
                        }
                        className="w-full rounded-md border border-slate-300 px-3 py-2 text-sm focus:border-emerald-500 focus:ring-2 focus:ring-emerald-100 focus:outline-none"
                    >
                        <option value="" disabled>
                            Select a payment method
                        </option>
                        {paymentMethods.map((m) => {
                            const account = bankAccounts.find(
                                (a) => a.id === m.bankAccountId,
                            );
                            const label =
                                m.type === 'UPI'
                                    ? `UPI — ${m.upiId}`
                                    : m.type === 'CARD'
                                        ? `Card — •••• ${m.cardLast4}`
                                        : `Net Banking — ${m.linkedBankName}`;
                            return (
                                <option key={m.id} value={m.id}>
                                    {label}
                                    {account ? ` (${account.accountNumber})` : ''}
                                    {m.default ? ' [default]' : ''}
                                </option>
                            );
                        })}
                    </select>
                </div>

                {paymentMethods.find((m) => m.id === form.sourcePaymentMethodId)
                    ?.type === 'CARD' && (
                    <div>
                        <label className="mb-1 block text-sm font-medium text-slate-600">
                            CVV
                        </label>
                        <input
                            required
                            type="password"
                            inputMode="numeric"
                            autoComplete="cc-csc"
                            maxLength={4}
                            value={form.cvv}
                            onChange={(e) =>
                                update(
                                    'cvv',
                                    e.target.value.replace(/\D/g, '').slice(0, 4),
                                )
                            }
                            placeholder="3-4 digit security code"
                            className="w-full rounded-md border border-slate-300 px-3 py-2 text-sm focus:border-emerald-500 focus:ring-2 focus:ring-emerald-100 focus:outline-none"
                        />
                    </div>
                )}

                <div>
                    <label className="mb-1 block text-sm font-medium text-slate-600">
                        Pay To
                    </label>
                    <select
                        required
                        disabled={accountsLoading}
                        value={form.destinationAccountId}
                        onChange={(e) =>
                            update('destinationAccountId', Number(e.target.value))
                        }
                        className="w-full rounded-md border border-slate-300 px-3 py-2 text-sm focus:border-emerald-500 focus:ring-2 focus:ring-emerald-100 focus:outline-none"
                    >
                        <option value="" disabled>
                            {accountsLoading
                                ? 'Loading accounts…'
                                : 'Select a destination account'}
                        </option>
                        {otherAccounts.map((a) => (
                            <option key={a.id} value={a.id}>
                                {accountHolders.get(a.userId)
                                    ? `${accountHolders.get(a.userId)} — `
                                    : ''}
                                {a.bankName} — {a.accountNumber}
                            </option>
                        ))}
                    </select>
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
                        value={form.amount}
                        onChange={(e) =>
                            update(
                                'amount',
                                e.target.value === '' ? '' : Number(e.target.value),
                            )
                        }
                        className="w-full rounded-md border border-slate-300 px-3 py-2 text-sm focus:border-emerald-500 focus:ring-2 focus:ring-emerald-100 focus:outline-none"
                    />
                    {form.amount !== '' && form.currency !== 'INR' && (
                        <p className="mt-1 text-xs text-slate-400">
                            ≈ {formatINR(toINR(Number(form.amount), form.currency))}
                        </p>
                    )}
                    {feePreview && (
                        <p className="mt-1.5 rounded-md bg-emerald-50 px-2.5 py-1.5 text-xs text-emerald-700">
                            Fee: {feePreview.feeAmount.toFixed(2)}{' '}
                            {form.currency} · You'll be debited{' '}
                            <span className="font-semibold">
                                {feePreview.totalDebit.toFixed(2)} {form.currency}
                            </span>{' '}
                            (recipient gets {Number(form.amount).toFixed(2)}{' '}
                            {form.currency})
                        </p>
                    )}
                </div>
                <div>
                    <label className="mb-1 block text-sm font-medium text-slate-600">
                        Currency
                    </label>
                    <select
                        value={form.currency}
                        onChange={(e) => update('currency', e.target.value)}
                        className="w-full rounded-md border border-slate-300 px-3 py-2 text-sm focus:border-emerald-500 focus:ring-2 focus:ring-emerald-100 focus:outline-none"
                    >
                        <option value="INR">INR (₹)</option>
                        <option value="USD">USD</option>
                        <option value="EUR">EUR</option>
                        <option value="GBP">GBP</option>
                    </select>
                </div>
                <div>
                    <label className="mb-1 block text-sm font-medium text-slate-600">
                        Authenticate with
                    </label>
                    <select
                        value={form.authMethod}
                        onChange={(e) =>
                            update('authMethod', e.target.value as AuthMethod)
                        }
                        className="w-full rounded-md border border-slate-300 px-3 py-2 text-sm focus:border-emerald-500 focus:ring-2 focus:ring-emerald-100 focus:outline-none"
                    >
                        <option value="PIN">PIN</option>
                        <option value="OTP">OTP (email)</option>
                    </select>
                </div>
                <div>
                    <label className="mb-1 block text-sm font-medium text-slate-600">
                        Reference (optional)
                    </label>
                    <input
                        value={form.reference}
                        onChange={(e) => update('reference', e.target.value)}
                        className="w-full rounded-md border border-slate-300 px-3 py-2 text-sm focus:border-emerald-500 focus:ring-2 focus:ring-emerald-100 focus:outline-none"
                    />
                </div>
            </div>

            <button
                type="submit"
                disabled={submitting}
                className="mt-5 w-full rounded-md bg-emerald-600 px-4 py-2 text-sm font-semibold text-white transition hover:bg-emerald-700 disabled:opacity-50"
            >
                {submitting ? 'Starting…' : 'Continue'}
            </button>
        </form>
    );
}

