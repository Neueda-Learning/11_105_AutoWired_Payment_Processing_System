import type { CurrentUser } from '../types/user';

const STORAGE_KEY = 'tpay.currentUser';

export function getStoredUser(): CurrentUser | null {
    try {
        const raw = localStorage.getItem(STORAGE_KEY);
        return raw ? (JSON.parse(raw) as CurrentUser) : null;
    } catch {
        return null;
    }
}

export function setStoredUser(user: CurrentUser): void {
    localStorage.setItem(STORAGE_KEY, JSON.stringify(user));
}

export function clearStoredUser(): void {
    localStorage.removeItem(STORAGE_KEY);
}
