import { Link } from 'react-router-dom';

export default function Landing() {
    return (
        <div className="relative min-h-screen overflow-hidden bg-gradient-to-br from-slate-50 via-white to-slate-100">
            {/* Background decorative elements */}
            <div className="absolute inset-0 overflow-hidden">
                <div className="absolute -top-40 -right-40 h-80 w-80 rounded-full bg-indigo-100/40 blur-3xl"></div>
                <div className="absolute -bottom-40 -left-40 h-80 w-80 rounded-full bg-emerald-100/40 blur-3xl"></div>
            </div>

            <div className="relative flex min-h-screen flex-col">
                {/* Header */}
                <header className="border-b border-slate-200/60 bg-white/80 backdrop-blur-sm">
                    <div className="mx-auto max-w-7xl px-4 py-4 sm:px-6 lg:px-8">
                        <div className="flex items-center justify-between">
                            <div className="flex items-center gap-3">
                                <div className="flex h-10 w-10 items-center justify-center rounded-xl bg-gradient-to-br from-indigo-600 to-indigo-500 shadow-lg shadow-indigo-500/30">
                                    <svg className="h-6 w-6 text-white" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                                        <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 8c-1.657 0-3 .895-3 2s1.343 2 3 2 3 .895 3 2-1.343 2-3 2m0-8c1.11 0 2.08.402 2.599 1M12 8V7m0 1v8m0 0v1m0-1c-1.11 0-2.08-.402-2.599-1M21 12a9 9 0 11-18 0 9 9 0 0118 0z" />
                                    </svg>
                                </div>
                                <div>
                                    <h1 className="text-lg font-bold text-slate-900">AutoWired</h1>
                                    <p className="text-xs text-slate-500">Payment Processing</p>
                                </div>
                            </div>
                            <div className="flex items-center gap-2 text-xs text-slate-600">
                                <div className="flex items-center gap-1.5">
                                    <div className="h-2 w-2 rounded-full bg-emerald-500"></div>
                                    <span>All systems operational</span>
                                </div>
                            </div>
                        </div>
                    </div>
                </header>

                {/* Main content */}
                <main className="flex flex-1 items-center justify-center px-4 py-12 sm:px-6 lg:px-8">
                    <div className="w-full max-w-5xl">
                        <div className="text-center">
                            <div className="inline-flex items-center gap-2 rounded-full border border-indigo-200 bg-indigo-50 px-4 py-1.5 text-sm font-medium text-indigo-700">
                                <svg className="h-4 w-4" fill="currentColor" viewBox="0 0 20 20">
                                    <path fillRule="evenodd" d="M2.166 4.999A11.954 11.954 0 0010 1.944 11.954 11.954 0 0017.834 5c.11.65.166 1.32.166 2.001 0 5.225-3.34 9.67-8 11.317C5.34 16.67 2 12.225 2 7c0-.682.057-1.35.166-2.001zm11.541 3.708a1 1 0 00-1.414-1.414L9 10.586 7.707 9.293a1 1 0 00-1.414 1.414l2 2a1 1 0 001.414 0l4-4z" clipRule="evenodd" />
                                </svg>
                                Enterprise-grade payment infrastructure
                            </div>

                            <h1 className="mt-6 text-4xl font-bold tracking-tight text-slate-900 sm:text-5xl lg:text-6xl">
                                Secure Payment Processing
                            </h1>
                            <p className="mx-auto mt-6 max-w-2xl text-lg leading-relaxed text-slate-600">
                                Advanced authentication, real-time fraud detection, and intelligent fee management.
                                Choose your portal to access powerful payment capabilities.
                            </p>
                        </div>

                        {/* Portal cards */}
                        <div className="mt-12 grid grid-cols-1 gap-8 lg:grid-cols-2">
                            {/* Customer Portal Card */}
                            <Link
                                to="/user"
                                className="group relative overflow-hidden rounded-2xl border border-slate-200 bg-white p-8 shadow-sm transition-all duration-300 hover:shadow-xl hover:shadow-emerald-500/10 hover:-translate-y-1"
                            >
                                <div className="absolute right-0 top-0 h-32 w-32 translate-x-8 -translate-y-8 rounded-full bg-emerald-500/5 transition-transform duration-500 group-hover:scale-150"></div>

                                <div className="relative">
                                    <div className="flex items-start justify-between">
                                        <div className="flex h-14 w-14 items-center justify-center rounded-xl bg-gradient-to-br from-emerald-500 to-emerald-600 shadow-lg shadow-emerald-500/30">
                                            <svg className="h-7 w-7 text-white" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                                                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M16 7a4 4 0 11-8 0 4 4 0 018 0zM12 14a7 7 0 00-7 7h14a7 7 0 00-7-7z" />
                                            </svg>
                                        </div>
                                        <span className="rounded-full bg-emerald-100 px-3 py-1 text-xs font-semibold text-emerald-700">
                                            Customer
                                        </span>
                                    </div>

                                    <h2 className="mt-6 text-2xl font-bold text-slate-900 group-hover:text-emerald-600 transition-colors">
                                        Customer Portal
                                    </h2>
                                    <p className="mt-3 text-sm leading-relaxed text-slate-600">
                                        Complete payment lifecycle management with multi-factor authentication,
                                        account linking, and transaction tracking.
                                    </p>

                                    <div className="mt-6 space-y-2">
                                        <div className="flex items-center gap-2 text-sm text-slate-600">
                                            <svg className="h-4 w-4 text-emerald-500" fill="currentColor" viewBox="0 0 20 20">
                                                <path fillRule="evenodd" d="M10 18a8 8 0 100-16 8 8 0 000 16zm3.707-9.293a1 1 0 00-1.414-1.414L9 10.586 7.707 9.293a1 1 0 00-1.414 1.414l2 2a1 1 0 001.414 0l4-4z" clipRule="evenodd" />
                                            </svg>
                                            PIN & OTP authentication
                                        </div>
                                        <div className="flex items-center gap-2 text-sm text-slate-600">
                                            <svg className="h-4 w-4 text-emerald-500" fill="currentColor" viewBox="0 0 20 20">
                                                <path fillRule="evenodd" d="M10 18a8 8 0 100-16 8 8 0 000 16zm3.707-9.293a1 1 0 00-1.414-1.414L9 10.586 7.707 9.293a1 1 0 00-1.414 1.414l2 2a1 1 0 001.414 0l4-4z" clipRule="evenodd" />
                                            </svg>
                                            Multi-currency support
                                        </div>
                                        <div className="flex items-center gap-2 text-sm text-slate-600">
                                            <svg className="h-4 w-4 text-emerald-500" fill="currentColor" viewBox="0 0 20 20">
                                                <path fillRule="evenodd" d="M10 18a8 8 0 100-16 8 8 0 000 16zm3.707-9.293a1 1 0 00-1.414-1.414L9 10.586 7.707 9.293a1 1 0 00-1.414 1.414l2 2a1 1 0 001.414 0l4-4z" clipRule="evenodd" />
                                            </svg>
                                            Real-time transaction status
                                        </div>
                                    </div>

                                    <div className="mt-8 flex items-center gap-2 text-sm font-semibold text-emerald-600 transition-all group-hover:gap-3">
                                        Access portal
                                        <svg className="h-4 w-4" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                                            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M13 7l5 5m0 0l-5 5m5-5H6" />
                                        </svg>
                                    </div>
                                </div>
                            </Link>

                            {/* Bank Admin Card */}
                            <Link
                                to="/bank"
                                className="group relative overflow-hidden rounded-2xl border border-slate-200 bg-white p-8 shadow-sm transition-all duration-300 hover:shadow-xl hover:shadow-indigo-500/10 hover:-translate-y-1"
                            >
                                <div className="absolute right-0 top-0 h-32 w-32 translate-x-8 -translate-y-8 rounded-full bg-indigo-500/5 transition-transform duration-500 group-hover:scale-150"></div>

                                <div className="relative">
                                    <div className="flex items-start justify-between">
                                        <div className="flex h-14 w-14 items-center justify-center rounded-xl bg-gradient-to-br from-indigo-600 to-indigo-700 shadow-lg shadow-indigo-500/30">
                                            <svg className="h-7 w-7 text-white" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                                                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M19 21V5a2 2 0 00-2-2H7a2 2 0 00-2 2v16m14 0h2m-2 0h-5m-9 0H3m2 0h5M9 7h1m-1 4h1m4-4h1m-1 4h1m-5 10v-5a1 1 0 011-1h2a1 1 0 011 1v5m-4 0h4" />
                                            </svg>
                                        </div>
                                        <span className="rounded-full bg-indigo-100 px-3 py-1 text-xs font-semibold text-indigo-700">
                                            Admin
                                        </span>
                                    </div>

                                    <h2 className="mt-6 text-2xl font-bold text-slate-900 group-hover:text-indigo-600 transition-colors">
                                        Bank Admin Dashboard
                                    </h2>
                                    <p className="mt-3 text-sm leading-relaxed text-slate-600">
                                        Comprehensive oversight with advanced analytics, fraud monitoring,
                                        and dynamic fee configuration capabilities.
                                    </p>

                                    <div className="mt-6 space-y-2">
                                        <div className="flex items-center gap-2 text-sm text-slate-600">
                                            <svg className="h-4 w-4 text-indigo-500" fill="currentColor" viewBox="0 0 20 20">
                                                <path fillRule="evenodd" d="M10 18a8 8 0 100-16 8 8 0 000 16zm3.707-9.293a1 1 0 00-1.414-1.414L9 10.586 7.707 9.293a1 1 0 00-1.414 1.414l2 2a1 1 0 001.414 0l4-4z" clipRule="evenodd" />
                                            </svg>
                                            Risk scoring & fraud detection
                                        </div>
                                        <div className="flex items-center gap-2 text-sm text-slate-600">
                                            <svg className="h-4 w-4 text-indigo-500" fill="currentColor" viewBox="0 0 20 20">
                                                <path fillRule="evenodd" d="M10 18a8 8 0 100-16 8 8 0 000 16zm3.707-9.293a1 1 0 00-1.414-1.414L9 10.586 7.707 9.293a1 1 0 00-1.414 1.414l2 2a1 1 0 001.414 0l4-4z" clipRule="evenodd" />
                                            </svg>
                                            Dynamic fee rule management
                                        </div>
                                        <div className="flex items-center gap-2 text-sm text-slate-600">
                                            <svg className="h-4 w-4 text-indigo-500" fill="currentColor" viewBox="0 0 20 20">
                                                <path fillRule="evenodd" d="M10 18a8 8 0 100-16 8 8 0 000 16zm3.707-9.293a1 1 0 00-1.414-1.414L9 10.586 7.707 9.293a1 1 0 00-1.414 1.414l2 2a1 1 0 001.414 0l4-4z" clipRule="evenodd" />
                                            </svg>
                                            Platform-wide analytics
                                        </div>
                                    </div>

                                    <div className="mt-8 flex items-center gap-2 text-sm font-semibold text-indigo-600 transition-all group-hover:gap-3">
                                        Access dashboard
                                        <svg className="h-4 w-4" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                                            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M13 7l5 5m0 0l-5 5m5-5H6" />
                                        </svg>
                                    </div>
                                </div>
                            </Link>
                        </div>

                        {/* Trust indicators */}
                        <div className="mt-16 border-t border-slate-200 pt-8">
                            <div className="grid grid-cols-2 gap-8 md:grid-cols-4">
                                <div className="text-center">
                                    <div className="text-2xl font-bold text-slate-900">256-bit</div>
                                    <div className="mt-1 text-xs text-slate-500">Encryption</div>
                                </div>
                                <div className="text-center">
                                    <div className="text-2xl font-bold text-slate-900">99.9%</div>
                                    <div className="mt-1 text-xs text-slate-500">Uptime SLA</div>
                                </div>
                                <div className="text-center">
                                    <div className="text-2xl font-bold text-slate-900">Real-time</div>
                                    <div className="mt-1 text-xs text-slate-500">Fraud Detection</div>
                                </div>
                                <div className="text-center">
                                    <div className="text-2xl font-bold text-slate-900">Multi-factor</div>
                                    <div className="mt-1 text-xs text-slate-500">Authentication</div>
                                </div>
                            </div>
                        </div>
                    </div>
                </main>

                {/* Footer */}
                <footer className="border-t border-slate-200 bg-white/50 backdrop-blur-sm">
                    <div className="mx-auto max-w-7xl px-4 py-6 sm:px-6 lg:px-8">
                        <div className="flex flex-col items-center justify-between gap-4 sm:flex-row">
                            <p className="text-xs text-slate-500">
                                © 2026 AutoWired Payment Processing. Secured by industry-leading protocols.
                            </p>
                            <div className="flex items-center gap-6 text-xs text-slate-500">
                                <span className="flex items-center gap-1.5">
                                    <svg className="h-3.5 w-3.5 text-emerald-500" fill="currentColor" viewBox="0 0 20 20">
                                        <path fillRule="evenodd" d="M2.166 4.999A11.954 11.954 0 0010 1.944 11.954 11.954 0 0017.834 5c.11.65.166 1.32.166 2.001 0 5.225-3.34 9.67-8 11.317C5.34 16.67 2 12.225 2 7c0-.682.057-1.35.166-2.001zm11.541 3.708a1 1 0 00-1.414-1.414L9 10.586 7.707 9.293a1 1 0 00-1.414 1.414l2 2a1 1 0 001.414 0l4-4z" clipRule="evenodd" />
                                    </svg>
                                    PCI DSS Compliant
                                </span>
                                <span className="flex items-center gap-1.5">
                                    <svg className="h-3.5 w-3.5 text-indigo-500" fill="currentColor" viewBox="0 0 20 20">
                                        <path fillRule="evenodd" d="M5 9V7a5 5 0 0110 0v2a2 2 0 012 2v5a2 2 0 01-2 2H5a2 2 0 01-2-2v-5a2 2 0 012-2zm8-2v2H7V7a3 3 0 016 0z" clipRule="evenodd" />
                                    </svg>
                                    ISO 27001 Certified
                                </span>
                            </div>
                        </div>
                    </div>
                </footer>
            </div>
        </div>
    );
}

