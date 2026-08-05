import { apiClient } from './client';
import type { FeeRuleRequest, TransactionFeeRule } from '../types/banking';

export const feeRulesApi = {
    async getAll(): Promise<TransactionFeeRule[]> {
        const { data } = await apiClient.get<TransactionFeeRule[]>('/fee-rules');
        return data;
    },

    async create(payload: FeeRuleRequest): Promise<TransactionFeeRule> {
        const { data } = await apiClient.post<TransactionFeeRule>(
            '/fee-rules',
            payload,
        );
        return data;
    },

    async update(
        id: number,
        payload: FeeRuleRequest,
    ): Promise<TransactionFeeRule> {
        const { data } = await apiClient.put<TransactionFeeRule>(
            `/fee-rules/${id}`,
            payload,
        );
        return data;
    },
};
