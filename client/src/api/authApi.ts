import { apiClient } from './client';
import type {
    AuthenticatePaymentRequest,
    InitiatePaymentRequest,
} from '../types/banking';
import type { Payment } from '../types/payment';

export const authApi = {
    async initiate(payload: InitiatePaymentRequest): Promise<Payment> {
        const { data } = await apiClient.post<Payment>(
            '/payments/initiate',
            payload,
        );
        return data;
    },

    async authenticate(
        paymentId: number,
        payload: AuthenticatePaymentRequest,
    ): Promise<Payment> {
        const { data } = await apiClient.post<Payment>(
            `/payments/${paymentId}/authenticate`,
            payload,
        );
        return data;
    },

    async resendOtp(paymentId: number): Promise<Payment> {
        const { data } = await apiClient.post<Payment>(
            `/payments/${paymentId}/resend-otp`,
        );
        return data;
    },
};
