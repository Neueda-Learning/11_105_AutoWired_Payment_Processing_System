import { useMemo, useState } from 'react';
import type { Payment, PaymentStatus } from '../types/payment';
import StatusBadge from './StatusBadge';
import { formatWithInrEquivalent } from '../utils/currency';

const STATUS_OPTIONS: (PaymentStatus | 'ALL')[] = [
    'ALL',
    'CREATED',
    'VALIDATED',
    'SENT',
    'COMPLETED',
    'FAILED',
];

type SortKey = 'id' | 'amount' | 'status' | 'createdAt';
type SortDirection = 'asc' | 'desc';

interface SortOption {
    key: SortKey;
    label: string;
}

const SORT_OPTIONS: SortOption[] = [
    { key: 'createdAt', label: 'Created' },
    { key: 'id', label: 'ID' },
    { key: 'amount', label: 'Amount' },
    { key: 'status', label: 'Status' },
];

interface Props {
    payments: Payment[];
    statusFilter: PaymentStatus | 'ALL';
    onStatusFilterChange: (status: PaymentStatus | 'ALL') => void;
    search: string;
    onSearchChange: (value: string) => void;
    selectedId: number | null;
    onSelect: (id: number) => void;
    loading: boolean;
}

export default function PaymentList({
    payments,
    statusFilter,
    onStatusFilterChange,
    search,
    onSearchChange,
    selectedId,
    onSelect,
    loading,
}: Props) {
    const [sortKey, setSortKey] = useState<SortKey>('createdAt');
    const [sortDirection, setSortDirection] = useState<SortDirection>('desc');

    function toggleSort(key: SortKey) {
        if (key === sortKey) {
            setSortDirection((d) => (d === 'asc' ? 'desc' : 'asc'));
        } else {
            setSortKey(key);
            setSortDirection('asc');
        }
    }

    const filtered = payments.filter((p) => {
        if (!search.trim()) return true;
        const term = search.trim().toLowerCase();
        return (
            String(p.id).includes(term) ||
            (p.reference ?? '').toLowerCase().includes(term)
        );
    });

    const sorted = useMemo(() => {
        const list = [...filtered];
        list.sort((a, b) => {
            let cmp = 0;
            switch (sortKey) {
                case 'id':
                    cmp = a.id - b.id;
                    break;
                case 'amount':
                    cmp = (a.netAmount ?? a.amount) - (b.netAmount ?? b.amount);
                    break;
                case 'status':
                    cmp = a.status.localeCompare(b.status);
                    break;
                case 'createdAt':
                    cmp =
                        new Date(a.createdAt).getTime() -
                        new Date(b.createdAt).getTime();
                    break;
            }
            return sortDirection === 'asc' ? cmp : -cmp;
        });
        return list;
        // eslint-disable-next-line react-hooks/exhaustive-deps
    }, [filtered, sortKey, sortDirection]);

    return (
        <div className="rounded-xl border border-slate-200 bg-white shadow-sm">
            <div className="flex flex-col gap-3 border-b border-slate-200 p-4 sm:flex-row sm:items-center sm:justify-between">
                <h2 className="text-lg font-semibold text-slate-800">
                    💳 Payments
                </h2>
                <div className="flex flex-wrap gap-2">
                    <input
                        value={search}
                        onChange={(e) => onSearchChange(e.target.value)}
                        placeholder="Search by ID or reference"
                        className="rounded-md border border-slate-300 px-3 py-1.5 text-sm transition focus:border-indigo-500 focus:ring-2 focus:ring-indigo-100 focus:outline-none"
                    />
                    <select
                        value={statusFilter}
                        onChange={(e) =>
                            onStatusFilterChange(e.target.value as PaymentStatus | 'ALL')
                        }
                        className="rounded-md border border-slate-300 px-3 py-1.5 text-sm transition focus:border-indigo-500 focus:ring-2 focus:ring-indigo-100 focus:outline-none"
                    >
                        {STATUS_OPTIONS.map((s) => (
                            <option key={s} value={s}>
                                {s}
                            </option>
                        ))}
                    </select>
                    <select
                        value={sortKey}
                        onChange={(e) => setSortKey(e.target.value as SortKey)}
                        title="Sort by"
                        className="rounded-md border border-slate-300 px-3 py-1.5 text-sm transition focus:border-indigo-500 focus:ring-2 focus:ring-indigo-100 focus:outline-none"
                    >
                        {SORT_OPTIONS.map((opt) => (
                            <option key={opt.key} value={opt.key}>
                                Sort: {opt.label}
                            </option>
                        ))}
                    </select>
                    <button
                        type="button"
                        onClick={() =>
                            setSortDirection((d) => (d === 'asc' ? 'desc' : 'asc'))
                        }
                        title={
                            sortDirection === 'asc'
                                ? 'Ascending (click for descending)'
                                : 'Descending (click for ascending)'
                        }
                        className="flex items-center gap-1 rounded-md border border-slate-300 px-3 py-1.5 text-sm font-medium text-slate-600 transition hover:bg-slate-50"
                    >
                        {sortDirection === 'asc' ? '⬆️ Asc' : '⬇️ Desc'}
                    </button>
                </div>
            </div>

            <div className="max-h-130 overflow-y-auto">
                <table className="w-full text-left text-sm">
                    <thead className="sticky top-0 bg-slate-50 text-xs uppercase tracking-wide text-slate-500">
                        <tr>
                            <th
                                className="cursor-pointer select-none px-4 py-2.5 hover:text-slate-700"
                                onClick={() => toggleSort('id')}
                            >
                                ID{sortKey === 'id' ? (sortDirection === 'asc' ? ' ▲' : ' ▼') : ''}
                            </th>
                            <th
                                className="cursor-pointer select-none px-4 py-2.5 hover:text-slate-700"
                                onClick={() => toggleSort('amount')}
                            >
                                Total Debited{sortKey === 'amount' ? (sortDirection === 'asc' ? ' ▲' : ' ▼') : ''}
                            </th>
                            <th
                                className="cursor-pointer select-none px-4 py-2.5 hover:text-slate-700"
                                onClick={() => toggleSort('status')}
                            >
                                Status{sortKey === 'status' ? (sortDirection === 'asc' ? ' ▲' : ' ▼') : ''}
                            </th>
                            <th
                                className="cursor-pointer select-none px-4 py-2.5 hover:text-slate-700"
                                onClick={() => toggleSort('createdAt')}
                            >
                                Created{sortKey === 'createdAt' ? (sortDirection === 'asc' ? ' ▲' : ' ▼') : ''}
                            </th>
                        </tr>
                    </thead>
                    <tbody>
                        {sorted.map((p) => (
                            <tr
                                key={p.id}
                                onClick={() => onSelect(p.id)}
                                className={`cursor-pointer border-t border-slate-100 transition hover:bg-indigo-50/60 ${selectedId === p.id ? 'bg-indigo-50' : ''
                                    }`}
                            >
                                <td className="px-4 py-2.5 font-medium text-slate-700">
                                    #{p.id}
                                </td>
                                <td className="px-4 py-2.5 text-slate-600">
                                    {formatWithInrEquivalent(
                                        p.netAmount ?? p.amount,
                                        p.currency,
                                    )}
                                </td>
                                <td className="px-4 py-2.5">
                                    <StatusBadge status={p.status} />
                                </td>
                                <td className="px-4 py-2.5 text-slate-500">
                                    {new Date(p.createdAt).toLocaleString()}
                                </td>
                            </tr>
                        ))}
                        {sorted.length === 0 && !loading && (
                            <tr>
                                <td
                                    colSpan={4}
                                    className="px-4 py-10 text-center text-slate-400"
                                >
                                    No payments found
                                </td>
                            </tr>
                        )}
                    </tbody>
                </table>
            </div>
        </div>
    );
}
