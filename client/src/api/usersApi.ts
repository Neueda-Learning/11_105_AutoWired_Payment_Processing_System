import { apiClient } from './client';
import type {
    BankAccount,
    CreateBankAccountRequest,
    CreatePaymentMethodRequest,
    CreateUserRequest,
    PaymentMethodEntity,
    UpdatePaymentMethodRequest,
    UpdatePinRequest,
    User,
} from '../types/banking';

export const usersApi = {
    async getAll(): Promise<User[]> {
        const { data } = await apiClient.get<User[]>('/users');
        return data;
    },

    async getAllBankAccounts(): Promise<BankAccount[]> {
        const { data } = await apiClient.get<BankAccount[]>(
            '/users/bank-accounts',
        );
        return data;
    },

    async getBankAccounts(userId: number): Promise<BankAccount[]> {
        const { data } = await apiClient.get<BankAccount[]>(
            `/users/${userId}/bank-accounts`,
        );
        return data;
    },

    async getPaymentMethods(userId: number): Promise<PaymentMethodEntity[]> {
        const { data } = await apiClient.get<PaymentMethodEntity[]>(
            `/users/${userId}/payment-methods`,
        );
        return data;
    },

    async register(payload: CreateUserRequest): Promise<User> {
        const { data } = await apiClient.post<User>('/users', payload);
        return data;
    },

    async addBankAccount(
        userId: number,
        payload: CreateBankAccountRequest,
    ): Promise<BankAccount> {
        const { data } = await apiClient.post<BankAccount>(
            `/users/${userId}/bank-accounts`,
            payload,
        );
        return data;
    },

    async addPaymentMethod(
        userId: number,
        payload: CreatePaymentMethodRequest,
    ): Promise<PaymentMethodEntity> {
        const { data } = await apiClient.post<PaymentMethodEntity>(
            `/users/${userId}/payment-methods`,
            payload,
        );
        return data;
    },

    async updatePaymentMethod(
        userId: number,
        methodId: number,
        payload: UpdatePaymentMethodRequest,
    ): Promise<PaymentMethodEntity> {
        const { data } = await apiClient.put<PaymentMethodEntity>(
            `/users/${userId}/payment-methods/${methodId}`,
            payload,
        );
        return data;
    },

    async deletePaymentMethod(userId: number, methodId: number): Promise<void> {
        await apiClient.delete(`/users/${userId}/payment-methods/${methodId}`);
    },

    async updatePin(
        userId: number,
        payload: UpdatePinRequest,
    ): Promise<User> {
        const { data } = await apiClient.put<User>(
            `/users/${userId}/pin`,
            payload,
        );
        return data;
    },
};
