import { apiClient } from './client';
import type {
    CreatePaymentRequest,
    Payment,
    PaymentStats,
    PaymentStatus,
    PaymentStatusHistory,
    UpdateStatusRequest,
} from '../types/payment';

export const paymentsApi = {
    async getAll(status?: PaymentStatus | 'ALL'): Promise<Payment[]> {
        const params = status && status !== 'ALL' ? { status } : undefined;
        const { data } = await apiClient.get<Payment[]>('/payments', { params });
        return data;
    },

    async getById(id: number): Promise<Payment> {
        const { data } = await apiClient.get<Payment>(`/payments/${id}`);
        return data;
    },

    async getHistory(id: number): Promise<PaymentStatusHistory[]> {
        const { data } = await apiClient.get<PaymentStatusHistory[]>(
            `/payments/${id}/history`,
        );
        return data;
    },

    async create(payload: CreatePaymentRequest): Promise<Payment> {
        const { data } = await apiClient.post<Payment>('/payments', payload);
        return data;
    },

    async updateStatus(
        id: number,
        payload: UpdateStatusRequest,
    ): Promise<Payment> {
        const { data } = await apiClient.put<Payment>(
            `/payments/${id}/status`,
            payload,
        );
        return data;
    },

    async getStats(): Promise<PaymentStats> {
        const { data } = await apiClient.get<PaymentStats>('/payments/stats');
        return data;
    },
};
