import { NavLink, Outlet, useLocation } from 'react-router-dom';
import { useUserSession } from '../context/UserContext';

const NAV_ITEMS = [
    { to: '/user/pay', label: 'Make a Payment', icon: '📤' },
    { to: '/user/payments', label: 'My Payments', icon: '📜' },
    { to: '/user/methods', label: 'Payment Methods', icon: '💳' },
    { to: '/user/accounts', label: 'Bank Accounts', icon: '🏦' },
    { to: '/user', label: 'Profile', icon: '👤', end: true },

];

const PAGE_TITLES: Record<string, { title: string; subtitle: string }> = {
    '/user': {
        title: 'Profile',
        subtitle: 'Your account details at a glance.',
    },
    '/user/accounts': {
        title: 'Bank Accounts',
        subtitle: 'Link and manage the bank accounts you pay from.',
    },
    '/user/methods': {
        title: 'Payment Methods',
        subtitle: 'Add cards and UPI IDs for faster checkout.',
    },
    '/user/pay': {
        title: 'Make a Payment',
        subtitle: 'Send money securely with PIN/OTP verification.',
    },
    '/user/payments': {
        title: 'My Payments',
        subtitle: 'Track the status and history of everything you\u2019ve sent.',
    },
};

export default function UserLayout() {
    const { user, clearSession } = useUserSession();
    const location = useLocation();
    const page = PAGE_TITLES[location.pathname] ?? PAGE_TITLES['/user'];

    const initials = user
        ? user.fullName
            .split(' ')
            .map((p) => p[0])
            .slice(0, 2)
            .join('')
            .toUpperCase()
        : '?';

    return (
        <div className="flex min-h-screen bg-slate-100">
            <aside className="flex w-64 shrink-0 flex-col border-r border-emerald-900/40 bg-linear-to-b from-emerald-900 to-slate-950 text-emerald-50">
                <div className="border-b border-white/10 px-5 py-6">
                    <div className="flex items-center gap-2">
                        <span className="flex h-9 w-9 items-center justify-center rounded-lg bg-emerald-500 text-base font-bold text-white shadow-lg shadow-emerald-900/40">
                            ₿
                        </span>
                        <div>
                            <p className="text-[11px] font-semibold uppercase tracking-widest text-emerald-300/80">
                                Payments Platform
                            </p>
                            <h1 className="text-base font-bold text-white">
                                Customer Portal
                            </h1>
                        </div>
                    </div>
                </div>

                <nav className="flex-1 space-y-1 px-3 py-4">
                    <p className="px-3 pb-1 text-[10px] font-semibold uppercase tracking-widest text-emerald-300/60">
                        Menu
                    </p>
                    {NAV_ITEMS.map((item) => (
                        <NavLink
                            key={item.to}
                            to={item.to}
                            end={item.end}
                            className={({ isActive }) =>
                                `flex items-center gap-3 rounded-lg px-3 py-2.5 text-sm font-medium transition ${isActive
                                    ? 'bg-emerald-500 text-white shadow-sm shadow-emerald-900/40'
                                    : 'text-emerald-100/80 hover:bg-white/10 hover:text-white'
                                }`
                            }
                        >
                            <span aria-hidden>{item.icon}</span>
                            {item.label}
                        </NavLink>
                    ))}
                </nav>

                {user && (
                    <div className="border-t border-white/10 px-4 py-4">
                        <div className="flex items-center gap-3 rounded-lg bg-white/5 p-3">
                            <span className="flex h-9 w-9 shrink-0 items-center justify-center rounded-full bg-emerald-500/90 text-sm font-bold text-white">
                                {initials}
                            </span>
                            <div className="min-w-0 flex-1">
                                <p className="truncate text-sm font-semibold text-white">
                                    {user.fullName}
                                </p>
                                <p className="truncate text-xs text-emerald-200/70">
                                    #{user.id}
                                </p>
                            </div>
                        </div>
                        <button
                            onClick={clearSession}
                            className="mt-2 w-full rounded-md bg-white/10 px-3 py-1.5 text-xs font-semibold text-emerald-50 transition hover:bg-white/20"
                        >
                            Switch user
                        </button>
                    </div>
                )}

                <div className="border-t border-white/10 px-5 py-4">
                    <NavLink
                        to="/"
                        className="flex items-center gap-1.5 text-xs font-medium text-emerald-200/70 transition hover:text-white"
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
                </header>
                <main className="px-8 py-8">
                    <Outlet />
                </main>
            </div>
        </div>
    );
}

