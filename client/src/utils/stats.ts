import type { Payment, PaymentStats } from '../types/payment';

export function computeLocalStats(payments: Payment[]): PaymentStats {
    const totalCount = payments.length;
    const totalVolume = payments.reduce((sum, p) => sum + p.amount, 0);
    const completed = payments.filter((p) => p.status === 'COMPLETED').length;
    const failed = payments.filter((p) => p.status === 'FAILED').length;
    const finished = completed + failed;
    const successRate = finished > 0 ? (completed / finished) * 100 : 0;
    const avgRiskScore =
        totalCount > 0
            ? payments.reduce((sum, p) => sum + p.riskScore, 0) / totalCount
            : 0;

    const statusCounts = payments.reduce<PaymentStats['statusCounts']>(
        (acc, p) => {
            acc[p.status] = (acc[p.status] ?? 0) + 1;
            return acc;
        },
        {},
    );

    return { totalCount, totalVolume, successRate, avgRiskScore, statusCounts };
}
