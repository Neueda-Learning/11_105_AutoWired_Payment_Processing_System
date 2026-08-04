import { useCallback, useEffect, useRef, useState } from 'react';
import './App.css';
import { paymentsApi } from './api/paymentsApi';
import PaymentForm from './components/PaymentForm';
import PaymentList from './components/PaymentList';
import PaymentDetails from './components/PaymentDetails';
import StatsBar from './components/StatsBar';
import LoginScreen from './components/LoginScreen';
import AdminDashboard from './components/AdminDashboard';
import ToastStack, { type ToastMessage } from './components/ToastStack';
import { computeLocalStats } from './utils/stats';
import { clearStoredUser, getStoredUser, setStoredUser } from './utils/currentUser';
import type { Payment, PaymentStats, PaymentStatus } from './types/payment';
import type { CurrentUser } from './types/user';

const POLL_INTERVAL_MS = 8000;

function App() {
  const [currentUser, setCurrentUser] = useState<CurrentUser | null>(() =>
    getStoredUser(),
  );
  const [payments, setPayments] = useState<Payment[]>([]);
  const [stats, setStats] = useState<PaymentStats | null>(null);
  const [statusFilter, setStatusFilter] = useState<PaymentStatus | 'ALL'>(
    'ALL',
  );
  const [search, setSearch] = useState('');
  const [selectedId, setSelectedId] = useState<number | null>(null);
  const [loading, setLoading] = useState(true);
  const [loadError, setLoadError] = useState<string | null>(null);
  const [toasts, setToasts] = useState<ToastMessage[]>([]);
  const toastIdRef = useRef(0);

  const pushToast = useCallback(
    (type: ToastMessage['type'], text: string) => {
      const id = ++toastIdRef.current;
      setToasts((prev) => [...prev, { id, type, text }]);
    },
    [],
  );

  const dismissToast = useCallback((id: number) => {
    setToasts((prev) => prev.filter((t) => t.id !== id));
  }, []);

  const loadPayments = useCallback(async () => {
    try {
      const data = await paymentsApi.getAll(statusFilter);
      setPayments(data);
      setLoadError(null);

      try {
        const remoteStats = await paymentsApi.getStats();
        setStats(remoteStats);
      } catch {
        setStats(computeLocalStats(data));
      }
    } catch {
      setLoadError('Unable to reach the payments API.');
    } finally {
      setLoading(false);
    }
  }, [statusFilter]);

  useEffect(() => {
    if (!currentUser || currentUser.role !== 'USER') return;
    let cancelled = false;

    async function tick() {
      if (!cancelled) await loadPayments();
    }

    tick();
    const interval = setInterval(tick, POLL_INTERVAL_MS);
    return () => {
      cancelled = true;
      clearInterval(interval);
    };
  }, [loadPayments, currentUser]);

  function handleLoggedIn(user: CurrentUser) {
    setStoredUser(user);
    setCurrentUser(user);
  }

  function handleLogout() {
    clearStoredUser();
    setCurrentUser(null);
    setPayments([]);
    setStats(null);
    setSelectedId(null);
  }

  function handleCreated(payment: Payment, wasDuplicate: boolean) {
    setPayments((prev) => {
      const next = prev.some((p) => p.id === payment.id)
        ? prev.map((p) => (p.id === payment.id ? payment : p))
        : [payment, ...prev];
      setStats(computeLocalStats(next));
      return next;
    });
    setSelectedId(payment.id);
    pushToast(
      wasDuplicate ? 'info' : 'success',
      wasDuplicate
        ? `Duplicate detected — showing existing payment #${payment.id}.`
        : `Payment #${payment.id} created successfully.`,
    );
  }

  if (!currentUser) {
    return <LoginScreen onLoggedIn={handleLoggedIn} />;
  }

  return (
    <div className="min-h-screen bg-linear-to-b from-slate-50 to-slate-100">
      <ToastStack toasts={toasts} onDismiss={dismissToast} />

      <header className="border-b border-slate-200 bg-linear-to-r from-indigo-600 via-indigo-600 to-violet-600 text-white shadow-sm">
        <div className="mx-auto max-w-6xl px-4 py-6">
          <div className="flex flex-col gap-1 sm:flex-row sm:items-center sm:justify-between">
            <div>
              <p className="text-xs font-semibold uppercase tracking-widest text-indigo-200">
                Payments Platform
              </p>
              <h1 className="text-2xl font-bold tracking-tight">
                Payments Processing Dashboard
              </h1>
              <p className="mt-1 text-sm text-indigo-100">
                Create payments, track their lifecycle, and review the full
                audit trail — live-refreshed every 8 seconds.
              </p>
            </div>
            <div className="flex items-center gap-3 self-start sm:self-auto">
              <div className="flex items-center gap-2 rounded-full bg-white/10 px-3 py-1.5 text-xs font-medium text-indigo-50">
                <span className="h-2 w-2 animate-pulse rounded-full bg-emerald-400" />
                Live
              </div>
              <div className="flex items-center gap-2 rounded-full bg-white/10 px-3 py-1.5 text-xs font-medium text-indigo-50">
                <span>
                  {currentUser.name} ({currentUser.accountNumber})
                </span>
                <button
                  type="button"
                  onClick={handleLogout}
                  className="rounded-full bg-white/20 px-2 py-0.5 font-semibold transition hover:bg-white/30"
                >
                  Switch user
                </button>
              </div>
            </div>
          </div>
        </div>
      </header>

      <main className="mx-auto max-w-6xl px-4 py-8">
        {currentUser.role === 'ADMIN' ? (
          <AdminDashboard />
        ) : (
          <>
            {loadError && (
              <p className="mb-6 rounded-md border border-amber-200 bg-amber-50 px-3 py-2 text-sm text-amber-700">
                {loadError}
              </p>
            )}

            {stats && (
              <div className="mb-6">
                <StatsBar stats={stats} />
              </div>
            )}

            <div className="grid grid-cols-1 gap-6 lg:grid-cols-3">
              <div className="lg:col-span-1">
                <PaymentForm currentUser={currentUser} onCreated={handleCreated} />
              </div>

              <div className="lg:col-span-1">
                <PaymentList
                  payments={payments}
                  statusFilter={statusFilter}
                  onStatusFilterChange={setStatusFilter}
                  search={search}
                  onSearchChange={setSearch}
                  selectedId={selectedId}
                  onSelect={setSelectedId}
                  loading={loading}
                />
              </div>

              <div className="lg:col-span-1">
                {selectedId ? (
                  <PaymentDetails paymentId={selectedId} />
                ) : (
                  <div className="rounded-xl border border-dashed border-slate-300 bg-white/60 p-8 text-center text-sm text-slate-400 shadow-sm">
                    Select a payment to view its details and history.
                  </div>
                )}
              </div>
            </div>
          </>
        )}
      </main>
    </div>
  );
}

export default App;

