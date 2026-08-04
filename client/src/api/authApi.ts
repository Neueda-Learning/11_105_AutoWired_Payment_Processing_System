import { apiClient } from './client';
import type { CurrentUser } from '../types/user';

export const authApi = {
    async listUsers(): Promise<CurrentUser[]> {
        const { data } = await apiClient.get<CurrentUser[]>('/auth/users');
        return data;
    },

    async login(accountNumber: string): Promise<CurrentUser> {
        const { data } = await apiClient.post<CurrentUser>('/auth/login', {
            accountNumber,
        });
        return data;
    },
};
