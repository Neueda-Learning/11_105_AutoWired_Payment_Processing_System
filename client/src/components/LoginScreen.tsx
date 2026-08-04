import { useEffect, useState } from 'react';
import { authApi } from '../api/authApi';
import type { CurrentUser } from '../types/user';

interface Props {
    onLoggedIn: (user: CurrentUser) => void;
}

export default function LoginScreen({ onLoggedIn }: Props) {
    const [users, setUsers] = useState<CurrentUser[]>([]);
    const [selectedAccount, setSelectedAccount] = useState('');
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState<string | null>(null);
    const [submitting, setSubmitting] = useState(false);

    useEffect(() => {
        let cancelled = false;
        authApi
            .listUsers()
            .then((data) => {
                if (cancelled) return;
                setUsers(data);
                if (data.length > 0) setSelectedAccount(data[0].accountNumber);
            })
            .catch(() => {
                if (!cancelled) setError('Unable to reach the payments API.');
            })
            .finally(() => {
                if (!cancelled) setLoading(false);
            });
        return () => {
            cancelled = true;
        };
    }, []);

    async function handleSubmit(e: React.FormEvent) {
        e.preventDefault();
        if (!selectedAccount) return;
        setSubmitting(true);
        setError(null);
        try {
            const user = await authApi.login(selectedAccount);
            onLoggedIn(user);
        } catch {
            setError('Login failed — user not found.');
        } finally {
            setSubmitting(false);
        }
    }

    return (
        <div className="flex min-h-screen items-center justify-center bg-linear-to-b from-slate-50 to-slate-100 px-4">
            <div className="w-full max-w-sm rounded-xl border border-slate-200 bg-white p-6 shadow-sm">
                <p className="text-xs font-semibold uppercase tracking-widest text-indigo-500">
                    Payments Platform
                </p>
                <h1 className="mt-1 text-xl font-bold text-slate-800">Sign in</h1>
                <p className="mt-1 text-sm text-slate-500">
                    Pick your account to continue. No password required for this
                    demo.
                </p>

                {error && (
                    <p className="mt-4 rounded-md border border-red-200 bg-red-50 px-3 py-2 text-sm text-red-700">
                        {error}
                    </p>
                )}

                {loading ? (
                    <p className="mt-6 text-sm text-slate-400">Loading users…</p>
                ) : (
                    <form onSubmit={handleSubmit} className="mt-6">
                        <label className="mb-1 block text-sm font-medium text-slate-600">
                            Account
                        </label>
                        <select
                            value={selectedAccount}
                            onChange={(e) => setSelectedAccount(e.target.value)}
                            className="w-full rounded-md border border-slate-300 px-3 py-2 text-sm transition focus:border-indigo-500 focus:ring-2 focus:ring-indigo-100 focus:outline-none"
                        >
                            {users.map((u) => (
                                <option key={u.id} value={u.accountNumber}>
                                    {u.name} ({u.accountNumber})
                                    {u.role === 'ADMIN' ? ' — Admin' : ''}
                                </option>
                            ))}
                        </select>

                        <button
                            type="submit"
                            disabled={submitting || !selectedAccount}
                            className="mt-5 w-full rounded-md bg-indigo-600 px-4 py-2.5 text-sm font-semibold text-white shadow-sm transition hover:bg-indigo-700 disabled:cursor-not-allowed disabled:opacity-60"
                        >
                            {submitting ? 'Signing in…' : 'Sign in'}
                        </button>
                    </form>
                )}
            </div>
        </div>
    );
}
