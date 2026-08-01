import type { PaymentStatus } from '../types/payment';

const STYLES: Record<PaymentStatus, string> = {
    CREATED: 'bg-slate-200 text-slate-700',
    VALIDATED: 'bg-blue-100 text-blue-700',
    SENT: 'bg-amber-100 text-amber-700',
    COMPLETED: 'bg-emerald-100 text-emerald-700',
    FAILED: 'bg-red-100 text-red-700',
};

const DOT_STYLES: Record<PaymentStatus, string> = {
    CREATED: 'bg-slate-500',
    VALIDATED: 'bg-blue-500',
    SENT: 'bg-amber-500',
    COMPLETED: 'bg-emerald-500',
    FAILED: 'bg-red-500',
};

export default function StatusBadge({ status }: { status: PaymentStatus }) {
    return (
        <span
            className={`inline-flex items-center gap-1.5 rounded-full px-2.5 py-0.5 text-xs font-semibold ${STYLES[status]}`}
        >
            <span
                className={`h-1.5 w-1.5 rounded-full ${DOT_STYLES[status]} ${status === 'SENT' ? 'animate-pulse' : ''
                    }`}
            />
            {status}
        </span>
    );
}