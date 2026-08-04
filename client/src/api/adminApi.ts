import { apiClient } from './client';
import type { AdminStats } from '../types/admin';
import type { Payment, PaymentStatus } from '../types/payment';

export const adminApi = {
    async getAllPayments(status?: PaymentStatus | 'ALL'): Promise<Payment[]> {
        const params = status && status !== 'ALL' ? { status } : undefined;
        const { data } = await apiClient.get<Payment[]>('/admin/payments', { params });
        return data;
    },

    async getStats(): Promise<AdminStats> {
        const { data } = await apiClient.get<AdminStats>('/admin/stats');
        return data;
    },
};
