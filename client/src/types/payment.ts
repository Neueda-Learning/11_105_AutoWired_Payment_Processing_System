export type PaymentStatus =
    | 'CREATED'
    | 'VALIDATED'
    | 'SENT'
    | 'COMPLETED'
    | 'FAILED';

export type PaymentMethod = 'UPI' | 'NETBANKING' | 'CREDIT_CARD';

export interface Payment {
    id: number;
    sourceAccount: string;
    destinationAccount: string;
    amount: number;
    currency: string;
    paymentMethod: PaymentMethod;
    status: PaymentStatus;
    riskScore: number;
    reference?: string;
    cardLast4?: string;
    cardExpiry?: string;
    upiId?: string;
    bankName?: string;
    feeAmount?: number;
    feePercentage?: number;
    netAmount?: number;
    createdAt: string;
    updatedAt: string;
}

export interface PaymentStatusHistory {
    id: number;
    paymentId: number;
    status: PaymentStatus;
    previousStatus: PaymentStatus | null;
    timestamp: string;
    notes?: string;
}

export interface CreatePaymentRequest {
    sourceAccount: string;
    destinationAccount: string;
    amount: number;
    currency: string;
    paymentMethod: PaymentMethod;
    reference?: string;
    idempotencyKey?: string;
    cardNumber?: string;
    cardExpiry?: string;
    cardHolderName?: string;
    upiId?: string;
    bankName?: string;
}

export interface UpdateStatusRequest {
    status: PaymentStatus;
    notes?: string;
}

export interface ApiErrorResponse {
    errorCode?: string;
    message: string;
    details?: string[];
    timestamp?: string;
    existingPaymentId?: number;
}

export interface PaymentStats {
    totalCount: number;
    totalVolume: number;
    successRate: number;
    avgRiskScore: number;
    statusCounts: Partial<Record<PaymentStatus, number>>;
    totalFeesCollected?: number;
}
