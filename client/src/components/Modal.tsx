import { useEffect } from 'react';
import type { ReactNode } from 'react';

interface Props {
    open: boolean;
    onClose: () => void;
    title: string;
    icon?: string;
    children: ReactNode;
}

export default function Modal({ open, onClose, title, icon, children }: Props) {
    useEffect(() => {
        if (!open) return;
        function onKeyDown(e: KeyboardEvent) {
            if (e.key === 'Escape') onClose();
        }
        document.addEventListener('keydown', onKeyDown);
        return () => document.removeEventListener('keydown', onKeyDown);
    }, [open, onClose]);

    if (!open) return null;

    return (
        <div className="fixed inset-0 z-50 flex items-center justify-center p-4">
            <div
                className="absolute inset-0 bg-slate-900/50 backdrop-blur-sm"
                onClick={onClose}
                aria-hidden
            />
            <div className="relative w-full max-w-md rounded-2xl border border-slate-200 bg-white shadow-xl">
                <div className="flex items-center justify-between border-b border-slate-100 px-6 py-4">
                    <div className="flex items-center gap-2">
                        {icon && (
                            <span className="flex h-8 w-8 items-center justify-center rounded-lg bg-emerald-100 text-lg">
                                {icon}
                            </span>
                        )}
                        <h2 className="text-lg font-semibold text-slate-800">
                            {title}
                        </h2>
                    </div>
                    <button
                        onClick={onClose}
                        aria-label="Close"
                        className="flex h-8 w-8 items-center justify-center rounded-full text-slate-400 transition hover:bg-slate-100 hover:text-slate-600"
                    >
                        ✕
                    </button>
                </div>
                <div className="max-h-[75vh] overflow-y-auto px-6 py-5">
                    {children}
                </div>
            </div>
        </div>
    );
}
