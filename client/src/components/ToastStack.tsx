import { useEffect } from 'react';

export interface ToastMessage {
    id: number;
    type: 'success' | 'error' | 'info';
    text: string;
}

const STYLES: Record<ToastMessage['type'], string> = {
    success: 'border-emerald-200 bg-emerald-50 text-emerald-800',
    error: 'border-red-200 bg-red-50 text-red-800',
    info: 'border-blue-200 bg-blue-50 text-blue-800',
};

interface Props {
    toasts: ToastMessage[];
    onDismiss: (id: number) => void;
}

function ToastItem({
    toast,
    onDismiss,
}: {
    toast: ToastMessage;
    onDismiss: (id: number) => void;
}) {
    useEffect(() => {
        const timer = setTimeout(() => onDismiss(toast.id), 4500);
        return () => clearTimeout(timer);
    }, [toast.id, onDismiss]);

    return (
        <div
            role="status"
            className={`pointer-events-auto flex items-start gap-2 rounded-lg border px-4 py-3 text-sm shadow-lg ${STYLES[toast.type]}`}
        >
            <span className="flex-1">{toast.text}</span>
            <button
                type="button"
                onClick={() => onDismiss(toast.id)}
                className="text-xs font-semibold opacity-60 hover:opacity-100"
                aria-label="Dismiss"
            >
                ✕
            </button>
        </div>
    );
}

export default function ToastStack({ toasts, onDismiss }: Props) {
    return (
        <div className="pointer-events-none fixed right-4 top-4 z-50 flex w-80 flex-col gap-2">
            {toasts.map((t) => (
                <ToastItem key={t.id} toast={t} onDismiss={onDismiss} />
            ))}
        </div>
    );
}
