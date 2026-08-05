import { Link } from 'react-router-dom';

export default function Landing() {
    return (
        <div className="flex min-h-screen items-center justify-center bg-linear-to-b from-slate-50 to-slate-100 px-4">
            <div className="w-full max-w-2xl text-center">
                <div className="mx-auto flex h-14 w-14 items-center justify-center rounded-2xl bg-linear-to-br from-indigo-600 to-emerald-500 text-2xl font-bold text-white shadow-lg shadow-indigo-900/20">
                    ₿
                </div>
                <p className="mt-4 text-xs font-semibold uppercase tracking-widest text-indigo-500">
                    Payments Platform
                </p>
                <h1 className="mt-2 text-3xl font-bold tracking-tight text-slate-900">
                    Welcome — choose a portal
                </h1>
                <p className="mt-2 text-sm text-slate-500">
                    Customers make and authenticate payments. Bank admins manage
                    fee rules and monitor the platform.
                </p>

                <div className="mt-8 grid grid-cols-1 gap-6 sm:grid-cols-2">
                    <Link
                        to="/user"
                        className="group rounded-2xl border border-slate-200 bg-white p-8 text-left shadow-sm transition hover:-translate-y-0.5 hover:border-emerald-300 hover:shadow-lg"
                    >
                        <span className="flex h-12 w-12 items-center justify-center rounded-xl bg-emerald-100 text-2xl">
                            🙋
                        </span>
                        <h2 className="mt-4 text-lg font-semibold text-slate-800 group-hover:text-emerald-600">
                            Customer Portal
                        </h2>
                        <p className="mt-1 text-sm text-slate-500">
                            Register, link bank accounts, add payment methods,
                            and make PIN/OTP-authenticated payments.
                        </p>
                        <span className="mt-4 inline-flex items-center gap-1 text-sm font-semibold text-emerald-600">
                            Enter portal →
                        </span>
                    </Link>

                    <Link
                        to="/bank"
                        className="group rounded-2xl border border-slate-200 bg-white p-8 text-left shadow-sm transition hover:-translate-y-0.5 hover:border-indigo-300 hover:shadow-lg"
                    >
                        <span className="flex h-12 w-12 items-center justify-center rounded-xl bg-indigo-100 text-2xl">
                            🏦
                        </span>
                        <h2 className="mt-4 text-lg font-semibold text-slate-800 group-hover:text-indigo-600">
                            Bank Admin Dashboard
                        </h2>
                        <p className="mt-1 text-sm text-slate-500">
                            Monitor payments, review fraud alerts, manage
                            transaction fee rules, and onboard new users.
                        </p>
                        <span className="mt-4 inline-flex items-center gap-1 text-sm font-semibold text-indigo-600">
                            Enter dashboard →
                        </span>
                    </Link>
                </div>

                <p className="mt-8 text-xs text-slate-400">
                    Secure · Real-time fraud monitoring · Built for scale
                </p>
            </div>
        </div>
    );
}

