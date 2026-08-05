import { useState } from 'react';
import type { AxiosError } from 'axios';
import { usersApi } from '../../api/usersApi';
import type { ApiErrorResponse } from '../../types/payment';
import type { CreateUserRequest, User } from '../../types/banking';

export default function RegisterUserPage() {
    const [form, setForm] = useState<CreateUserRequest>({
        fullName: '',
        email: '',
        phone: '',
        pin: '',
    });
    const [errors, setErrors] = useState<string[]>([]);
    const [submitting, setSubmitting] = useState(false);
    const [created, setCreated] = useState<User | null>(null);

    function update<K extends keyof CreateUserRequest>(
        key: K,
        value: CreateUserRequest[K],
    ) {
        setForm((f) => ({ ...f, [key]: value }));
    }

    async function handleSubmit(e: React.FormEvent) {
        e.preventDefault();
        setErrors([]);
        setSubmitting(true);
        try {
            const user = await usersApi.register(form);
            setCreated(user);
            setForm({ fullName: '', email: '', phone: '', pin: '' });
        } catch (err) {
            const data = (err as AxiosError<ApiErrorResponse>).response?.data;
            setErrors(
                data?.details && data.details.length > 0
                    ? data.details
                    : [data?.message ?? 'Failed to register user'],
            );
        } finally {
            setSubmitting(false);
        }
    }

    return (
        <div className="mx-auto max-w-md">
            <form
                onSubmit={handleSubmit}
                className="rounded-xl border border-slate-200 bg-white p-6 shadow-sm"
            >
                <div className="flex items-center gap-2">
                    <span className="flex h-8 w-8 items-center justify-center rounded-lg bg-indigo-100 text-lg">
                        🧑‍💼
                    </span>
                    <h2 className="text-lg font-semibold text-slate-800">
                        Register a New Customer
                    </h2>
                </div>

                {created && (
                    <p className="mt-3 flex items-center gap-2 rounded-md border border-emerald-200 bg-emerald-50 p-2 text-sm text-emerald-700">
                        <span>✅</span> Created user #{created.id} — {created.fullName}
                    </p>
                )}
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
                            Full Name
                        </label>
                        <input
                            required
                            value={form.fullName}
                            onChange={(e) => update('fullName', e.target.value)}
                            className="w-full rounded-md border border-slate-300 px-3 py-2 text-sm focus:border-indigo-500 focus:ring-2 focus:ring-indigo-100 focus:outline-none"
                        />
                    </div>
                    <div>
                        <label className="mb-1 block text-sm font-medium text-slate-600">
                            Email
                        </label>
                        <input
                            required
                            type="email"
                            value={form.email}
                            onChange={(e) => update('email', e.target.value)}
                            className="w-full rounded-md border border-slate-300 px-3 py-2 text-sm focus:border-indigo-500 focus:ring-2 focus:ring-indigo-100 focus:outline-none"
                        />
                    </div>
                    <div>
                        <label className="mb-1 block text-sm font-medium text-slate-600">
                            Phone
                        </label>
                        <input
                            value={form.phone}
                            onChange={(e) => update('phone', e.target.value)}
                            className="w-full rounded-md border border-slate-300 px-3 py-2 text-sm focus:border-indigo-500 focus:ring-2 focus:ring-indigo-100 focus:outline-none"
                        />
                    </div>
                    <div>
                        <label className="mb-1 block text-sm font-medium text-slate-600">
                            PIN (4-6 digits)
                        </label>
                        <input
                            required
                            pattern="\d{4,6}"
                            value={form.pin}
                            onChange={(e) => update('pin', e.target.value)}
                            className="w-full rounded-md border border-slate-300 px-3 py-2 text-sm focus:border-indigo-500 focus:ring-2 focus:ring-indigo-100 focus:outline-none"
                        />
                    </div>
                </div>

                <button
                    type="submit"
                    disabled={submitting}
                    className="mt-5 w-full rounded-md bg-indigo-600 px-4 py-2 text-sm font-semibold text-white transition hover:bg-indigo-700 disabled:opacity-50"
                >
                    {submitting ? 'Registering…' : 'Register User'}
                </button>
            </form>
        </div>
    );
}
