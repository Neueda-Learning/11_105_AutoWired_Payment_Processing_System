import { useEffect, useState } from 'react';
import { NavLink, Outlet, useLocation } from 'react-router-dom';
import { paymentsApi } from '../api/paymentsApi';

const PAGE_TITLES: Record<string, { title: string; subtitle: string }> = {
    '/bank': {
        title: 'Overview',
        subtitle: 'A real-time snapshot of payment activity across the bank.',
    },
    '/bank/payments': {
        title: 'Payments',
        subtitle: 'Search, filter, and drill into every payment on the platform.',
    },
    '/bank/flagged': {
        title: 'Fraud Alerts',
        subtitle: 'Review and action payments flagged by the risk engine.',
    },
    '/bank/fee-rules': {
        title: 'Fee Rules',
        subtitle: 'Configure how fees are calculated per payment method.',
    },
    '/bank/users': {
        title: 'Register User',
        subtitle: 'Onboard new customers and link their bank accounts.',
    },
};

const NAV_ITEMS = [
    { to: '/bank', label: 'Overview', icon: '📊', end: true },
    { to: '/bank/payments', label: 'Payments', icon: '💳' },
    { to: '/bank/flagged', label: 'Fraud Alerts', icon: '🚨' },
    { to: '/bank/fee-rules', label: 'Fee Rules', icon: '⚙️' },
    { to: '/bank/users', label: 'Register User', icon: '🧑‍💼' },
];

export default function BankLayout() {
    const location = useLocation();
    const [flaggedCount, setFlaggedCount] = useState(0);

    useEffect(() => {
        let cancelled = false;
        async function loadFlaggedCount() {
            try {
                const data = await paymentsApi.getFlagged();
                if (!cancelled) {
                    setFlaggedCount(
                        data.filter((p) => p.status === 'SENT').length,
                    );
                }
            } catch {
                // Silently ignore - badge is a non-critical enhancement.
            }
        }
        loadFlaggedCount();
        const interval = setInterval(loadFlaggedCount, 10000);
        return () => {
            cancelled = true;
            clearInterval(interval);
        };
    }, []);

    const page = PAGE_TITLES[location.pathname] ?? PAGE_TITLES['/bank'];

    return (
        <div className="flex min-h-screen bg-slate-100">
            <aside className="flex w-64 shrink-0 flex-col border-r border-slate-800 bg-linear-to-b from-slate-900 to-slate-950 text-slate-100">
                <div className="border-b border-slate-800/80 px-5 py-6">
                    <div className="flex items-center gap-2">
                        <span className="flex h-9 w-9 items-center justify-center rounded-lg bg-indigo-600 text-base font-bold text-white shadow-lg shadow-indigo-900/40">
                            ₿
                        </span>
                        <div>
                            <p className="text-[11px] font-semibold uppercase tracking-widest text-slate-400">
                               AutoWired Bank Admin
                            </p>
                            <h1 className="text-base font-bold text-white">
                                Control Center
                            </h1>
                        </div>
                    </div>
                </div>
                <nav className="flex-1 space-y-1 px-3 py-4">
                    <p className="px-3 pb-1 text-[10px] font-semibold uppercase tracking-widest text-slate-500">
                        Menu
                    </p>
                    {NAV_ITEMS.map((item) => (
                        <NavLink
                            key={item.to}
                            to={item.to}
                            end={item.end}
                            className={({ isActive }) =>
                                `flex items-center justify-between gap-3 rounded-lg px-3 py-2.5 text-sm font-medium transition ${isActive
                                    ? 'bg-indigo-600 text-white shadow-sm shadow-indigo-900/40'
                                    : 'text-slate-300 hover:bg-slate-800/80 hover:text-white'
                                }`
                            }
                        >
                            <span className="flex items-center gap-3">
                                <span aria-hidden>{item.icon}</span>
                                {item.label}
                            </span>
                            {item.to === '/bank/flagged' && flaggedCount > 0 && (
                                <span className="flex h-5 min-w-5 items-center justify-center rounded-full bg-red-500 px-1 text-[10px] font-bold text-white">
                                    {flaggedCount}
                                </span>
                            )}
                        </NavLink>
                    ))}
                </nav>
                <div className="border-t border-slate-800/80 px-5 py-4">
                    <NavLink
                        to="/"
                        className="flex items-center gap-1.5 text-xs font-medium text-slate-400 transition hover:text-white"
                    >
                        ← Back to home
                    </NavLink>
                </div>
            </aside>

            <div className="flex-1">
                <header className="flex items-center justify-between border-b border-slate-200 bg-white px-8 py-5 shadow-sm">
                    <div>
                        <h2 className="text-xl font-semibold text-slate-800">
                            {page.title}
                        </h2>
                        <p className="text-sm text-slate-500">{page.subtitle}</p>
                    </div>
                    {flaggedCount > 0 && (
                        <NavLink
                            to="/bank/flagged"
                            className="flex items-center gap-2 rounded-full border border-red-200 bg-red-50 px-3 py-1.5 text-xs font-semibold text-red-700 transition hover:bg-red-100"
                        >
                            🚨 {flaggedCount} awaiting review
                        </NavLink>
                    )}
                </header>
                <main className="px-8 py-8">
                    <Outlet />
                </main>
            </div>
        </div>
    );
}
