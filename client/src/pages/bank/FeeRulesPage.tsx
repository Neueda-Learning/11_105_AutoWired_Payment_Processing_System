import { useCallback, useEffect, useState } from 'react';
import type { AxiosError } from 'axios';
import { feeRulesApi } from '../../api/feeRulesApi';
import type { ApiErrorResponse } from '../../types/payment';
import type { FeeRuleRequest, TransactionFeeRule } from '../../types/banking';

const EMPTY: FeeRuleRequest = {
    paymentMethod: 'UPI',
    minAmount: 0,
    maxAmount: null,
    feeType: 'FLAT',
    feeValue: 0,
    minFeeCap: null,
    maxFeeCap: null,
    active: true,
};

export default function FeeRulesPage() {
    const [rules, setRules] = useState<TransactionFeeRule[]>([]);
    const [loading, setLoading] = useState(true);
    const [form, setForm] = useState<FeeRuleRequest>(EMPTY);
    const [editingId, setEditingId] = useState<number | null>(null);
    const [errors, setErrors] = useState<string[]>([]);
    const [submitting, setSubmitting] = useState(false);

    const load = useCallback(async () => {
        setLoading(true);
        try {
            const data = await feeRulesApi.getAll();
            setRules(data);
        } finally {
            setLoading(false);
        }
    }, []);

    useEffect(() => {
        load();
    }, [load]);

    function update<K extends keyof FeeRuleRequest>(
        key: K,
        value: FeeRuleRequest[K],
    ) {
        setForm((f) => ({ ...f, [key]: value }));
    }

    function startEdit(rule: TransactionFeeRule) {
        setEditingId(rule.id);
        setForm({
            paymentMethod: rule.paymentMethod,
            minAmount: rule.minAmount,
            maxAmount: rule.maxAmount ?? null,
            feeType: rule.feeType,
            feeValue: rule.feeValue,
            minFeeCap: rule.minFeeCap ?? null,
            maxFeeCap: rule.maxFeeCap ?? null,
            active: rule.active,
        });
    }

    function cancelEdit() {
        setEditingId(null);
        setForm(EMPTY);
    }

    async function handleSubmit(e: React.FormEvent) {
        e.preventDefault();
        setErrors([]);
        setSubmitting(true);
        try {
            if (editingId != null) {
                await feeRulesApi.update(editingId, form);
            } else {
                await feeRulesApi.create(form);
            }
            cancelEdit();
            await load();
        } catch (err) {
            const data = (err as AxiosError<ApiErrorResponse>).response?.data;
            setErrors(
                data?.details && data.details.length > 0
                    ? data.details
                    : [data?.message ?? 'Failed to save fee rule'],
            );
        } finally {
            setSubmitting(false);
        }
    }

    return (
        <div className="grid grid-cols-1 gap-6 lg:grid-cols-3">
            <div className="lg:col-span-2">
                <div className="rounded-xl border border-slate-200 bg-white shadow-sm">
                    <div className="flex items-center gap-2 border-b border-slate-200 p-4">
                        <span className="flex h-8 w-8 items-center justify-center rounded-lg bg-indigo-100 text-lg">
                            ⚙️
                        </span>
                        <h2 className="text-lg font-semibold text-slate-800">
                            Transaction Fee Rules
                        </h2>
                    </div>
                    {loading ? (
                        <p className="p-4 text-sm text-slate-400">Loading…</p>
                    ) : rules.length === 0 ? (
                        <p className="p-4 text-sm text-slate-400">
                            No fee rules configured.
                        </p>
                    ) : (
                        <table className="w-full text-left text-sm">
                            <thead className="border-b border-slate-100 bg-slate-50 text-xs uppercase tracking-wide text-slate-500">
                                <tr>
                                    <th className="px-4 py-2.5">Method</th>
                                    <th className="px-4 py-2.5">Range</th>
                                    <th className="px-4 py-2.5">Fee</th>
                                    <th className="px-4 py-2.5">Cap</th>
                                    <th className="px-4 py-2.5">Active</th>
                                    <th className="px-4 py-2.5"></th>
                                </tr>
                            </thead>
                            <tbody className="divide-y divide-slate-100">
                                {rules.map((r) => (
                                    <tr
                                        key={r.id}
                                        className="transition hover:bg-indigo-50/40"
                                    >
                                        <td className="px-4 py-2.5 font-medium text-slate-700">
                                            {r.paymentMethod}
                                        </td>
                                        <td className="px-4 py-2.5 text-slate-500">
                                            {r.minAmount} – {r.maxAmount ?? '∞'}
                                        </td>
                                        <td className="px-4 py-2.5 text-slate-500">
                                            {r.feeType === 'FLAT'
                                                ? `${r.feeValue} flat`
                                                : `${r.feeValue}%`}
                                        </td>
                                        <td className="px-4 py-2.5 text-slate-500">
                                            {r.minFeeCap ?? '—'} / {r.maxFeeCap ?? '—'}
                                        </td>
                                        <td className="px-4 py-2.5">
                                            {r.active ? (
                                                <span className="rounded-full bg-emerald-100 px-2 py-0.5 text-[11px] font-semibold text-emerald-700">
                                                    Active
                                                </span>
                                            ) : (
                                                <span className="rounded-full bg-slate-100 px-2 py-0.5 text-[11px] font-semibold text-slate-500">
                                                    Inactive
                                                </span>
                                            )}
                                        </td>
                                        <td className="px-4 py-2.5">
                                            <button
                                                onClick={() => startEdit(r)}
                                                className="rounded-md px-2 py-1 text-xs font-semibold text-indigo-600 transition hover:bg-indigo-100"
                                            >
                                                Edit
                                            </button>
                                        </td>
                                    </tr>
                                ))}
                            </tbody>
                        </table>
                    )}
                </div>
            </div>

            <form
                onSubmit={handleSubmit}
                className="rounded-xl border border-slate-200 bg-white p-5 shadow-sm"
            >
                <div className="flex items-center gap-2">
                    <span className="flex h-7 w-7 items-center justify-center rounded-md bg-indigo-100 text-sm">
                        {editingId != null ? '✏️' : '➕'}
                    </span>
                    <h3 className="text-sm font-semibold text-slate-800">
                        {editingId != null ? `Edit Rule #${editingId}` : 'New Rule'}
                    </h3>
                </div>

                {errors.length > 0 && (
                    <div className="mt-3 rounded-md border border-red-200 bg-red-50 p-3 text-sm text-red-700">
                        <ul className="list-inside list-disc">
                            {errors.map((msg) => (
                                <li key={msg}>{msg}</li>
                            ))}
                        </ul>
                    </div>
                )}

                <div className="mt-3 space-y-3">
                    <div>
                        <label className="mb-1 block text-xs font-medium text-slate-500">
                            Payment Method
                        </label>
                        <select
                            value={form.paymentMethod}
                            onChange={(e) => update('paymentMethod', e.target.value)}
                            className="w-full rounded-md border border-slate-300 px-3 py-2 text-sm focus:border-indigo-500 focus:ring-2 focus:ring-indigo-100 focus:outline-none"
                        >
                            <option value="UPI">UPI</option>
                            <option value="NETBANKING">Net Banking</option>
                            <option value="CREDIT_CARD">Credit Card</option>
                            <option value="ALL">All</option>
                        </select>
                    </div>
                    <div className="grid grid-cols-2 gap-2">
                        <div>
                            <label className="mb-1 block text-xs font-medium text-slate-500">
                                Min Amount
                            </label>
                            <input
                                type="number"
                                step="0.01"
                                value={form.minAmount}
                                onChange={(e) =>
                                    update('minAmount', Number(e.target.value))
                                }
                                className="w-full rounded-md border border-slate-300 px-3 py-2 text-sm focus:border-indigo-500 focus:ring-2 focus:ring-indigo-100 focus:outline-none"
                            />
                        </div>
                        <div>
                            <label className="mb-1 block text-xs font-medium text-slate-500">
                                Max Amount
                            </label>
                            <input
                                type="number"
                                step="0.01"
                                value={form.maxAmount ?? ''}
                                onChange={(e) =>
                                    update(
                                        'maxAmount',
                                        e.target.value === ''
                                            ? null
                                            : Number(e.target.value),
                                    )
                                }
                                className="w-full rounded-md border border-slate-300 px-3 py-2 text-sm focus:border-indigo-500 focus:ring-2 focus:ring-indigo-100 focus:outline-none"
                            />
                        </div>
                    </div>
                    <div>
                        <label className="mb-1 block text-xs font-medium text-slate-500">
                            Fee Type
                        </label>
                        <select
                            value={form.feeType}
                            onChange={(e) =>
                                update(
                                    'feeType',
                                    e.target.value as FeeRuleRequest['feeType'],
                                )
                            }
                            className="w-full rounded-md border border-slate-300 px-3 py-2 text-sm focus:border-indigo-500 focus:ring-2 focus:ring-indigo-100 focus:outline-none"
                        >
                            <option value="FLAT">Flat</option>
                            <option value="PERCENTAGE">Percentage</option>
                        </select>
                    </div>
                    <div>
                        <label className="mb-1 block text-xs font-medium text-slate-500">
                            Fee Value
                        </label>
                        <input
                            type="number"
                            step="0.01"
                            value={form.feeValue}
                            onChange={(e) =>
                                update('feeValue', Number(e.target.value))
                            }
                            className="w-full rounded-md border border-slate-300 px-3 py-2 text-sm focus:border-indigo-500 focus:ring-2 focus:ring-indigo-100 focus:outline-none"
                        />
                    </div>
                    <div className="grid grid-cols-2 gap-2">
                        <div>
                            <label className="mb-1 block text-xs font-medium text-slate-500">
                                Min Fee Cap
                            </label>
                            <input
                                type="number"
                                step="0.01"
                                value={form.minFeeCap ?? ''}
                                onChange={(e) =>
                                    update(
                                        'minFeeCap',
                                        e.target.value === ''
                                            ? null
                                            : Number(e.target.value),
                                    )
                                }
                                className="w-full rounded-md border border-slate-300 px-3 py-2 text-sm focus:border-indigo-500 focus:ring-2 focus:ring-indigo-100 focus:outline-none"
                            />
                        </div>
                        <div>
                            <label className="mb-1 block text-xs font-medium text-slate-500">
                                Max Fee Cap
                            </label>
                            <input
                                type="number"
                                step="0.01"
                                value={form.maxFeeCap ?? ''}
                                onChange={(e) =>
                                    update(
                                        'maxFeeCap',
                                        e.target.value === ''
                                            ? null
                                            : Number(e.target.value),
                                    )
                                }
                                className="w-full rounded-md border border-slate-300 px-3 py-2 text-sm focus:border-indigo-500 focus:ring-2 focus:ring-indigo-100 focus:outline-none"
                            />
                        </div>
                    </div>
                    <label className="flex items-center gap-2 rounded-lg border border-slate-100 bg-slate-50 px-3 py-2 text-sm text-slate-600">
                        <input
                            type="checkbox"
                            checked={form.active}
                            onChange={(e) => update('active', e.target.checked)}
                        />
                        Active
                    </label>
                </div>

                <div className="mt-4 flex gap-2">
                    <button
                        type="submit"
                        disabled={submitting}
                        className="flex-1 rounded-md bg-indigo-600 px-4 py-2 text-sm font-semibold text-white transition hover:bg-indigo-700 disabled:opacity-50"
                    >
                        {submitting
                            ? 'Saving…'
                            : editingId != null
                                ? 'Update Rule'
                                : 'Create Rule'}
                    </button>
                    {editingId != null && (
                        <button
                            type="button"
                            onClick={cancelEdit}
                            className="rounded-md border border-slate-300 px-4 py-2 text-sm font-semibold text-slate-600 hover:bg-slate-50"
                        >
                            Cancel
                        </button>
                    )}
                </div>
            </form>
        </div>
    );
}
