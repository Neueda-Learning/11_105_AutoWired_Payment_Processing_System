import {
    createContext,
    useCallback,
    useContext,
    useEffect,
    useMemo,
    useState,
    type ReactNode,
} from 'react';
import { usersApi } from '../api/usersApi';
import type {
    BankAccount,
    PaymentMethodEntity,
    User,
} from '../types/banking';

const STORAGE_KEY = 'payments.currentUserId.v1';

interface UserContextValue {
    user: User | null;
    bankAccounts: BankAccount[];
    paymentMethods: PaymentMethodEntity[];
    loading: boolean;
    selectUser: (userId: number) => Promise<void>;
    refresh: () => Promise<void>;
    clearSession: () => void;
}

const UserContext = createContext<UserContextValue | undefined>(undefined);

export function UserProvider({ children }: { children: ReactNode }) {
    const [user, setUser] = useState<User | null>(null);
    const [bankAccounts, setBankAccounts] = useState<BankAccount[]>([]);
    const [paymentMethods, setPaymentMethods] = useState<
        PaymentMethodEntity[]
    >([]);
    const [loading, setLoading] = useState(true);

    const loadForUser = useCallback(async (userId: number) => {
        setLoading(true);
        try {
            const [allUsers, accounts, methods] = await Promise.all([
                usersApi.getAll(),
                usersApi.getBankAccounts(userId),
                usersApi.getPaymentMethods(userId),
            ]);
            const freshUser = allUsers.find((u) => u.id === userId) ?? null;
            setUser(freshUser);
            setBankAccounts(accounts);
            setPaymentMethods(methods);
            if (freshUser) {
                localStorage.setItem(STORAGE_KEY, String(userId));
            } else {
                localStorage.removeItem(STORAGE_KEY);
            }
        } finally {
            setLoading(false);
        }
    }, []);

    useEffect(() => {
        const stored = localStorage.getItem(STORAGE_KEY);
        if (stored) {
            loadForUser(Number(stored));
        } else {
            setLoading(false);
        }
    }, [loadForUser]);

    const selectUser = useCallback(
        async (userId: number) => {
            await loadForUser(userId);
        },
        [loadForUser],
    );

    const refresh = useCallback(async () => {
        if (user) {
            await loadForUser(user.id);
        }
    }, [user, loadForUser]);

    const clearSession = useCallback(() => {
        localStorage.removeItem(STORAGE_KEY);
        setUser(null);
        setBankAccounts([]);
        setPaymentMethods([]);
    }, []);

    const value = useMemo(
        () => ({
            user,
            bankAccounts,
            paymentMethods,
            loading,
            selectUser,
            refresh,
            clearSession,
        }),
        [
            user,
            bankAccounts,
            paymentMethods,
            loading,
            selectUser,
            refresh,
            clearSession,
        ],
    );

    return <UserContext.Provider value={value}>{children}</UserContext.Provider>;
}

export function useUserSession() {
    const ctx = useContext(UserContext);
    if (!ctx) {
        throw new Error('useUserSession must be used within a UserProvider');
    }
    return ctx;
}

