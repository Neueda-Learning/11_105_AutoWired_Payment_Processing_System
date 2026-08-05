// Types mirroring the v2 banking domain (User, BankAccount, PaymentMethod,
// TransactionFeeRule, and the auth-gated payment flow).
// See new-docs/payment-system-v2-design.md sections 3, 5, 8.

export type KycStatus = 'PENDING' | 'VERIFIED';

export interface User {
    id: number;
    fullName: string;
    email: string;
    phone?: string;
    kycStatus: KycStatus;
    createdAt: string;
    dailyLimit?: number;
    country?: string;
}

export type BankAccountStatus = 'ACTIVE' | 'FROZEN' | 'CLOSED';

export interface BankAccount {
    id: number;
    userId: number;
    accountNumber: string;
    ifscCode?: string;
    bankName: string;
    balance: number;
    primary: boolean;
    status: BankAccountStatus;
}

export type PaymentMethodType = 'UPI' | 'CARD' | 'NETBANKING';

export interface PaymentMethodEntity {
    id: number;
    userId: number;
    bankAccountId: number;
    type: PaymentMethodType;
    upiId?: string;
    cardLast4?: string;
    cardToken?: string;
    linkedBankName?: string;
    default: boolean;
}

export type FeeType = 'FLAT' | 'PERCENTAGE';

export interface TransactionFeeRule {
    id: number;
    paymentMethod: string; // UPI / NETBANKING / CREDIT_CARD / ALL
    minAmount: number;
    maxAmount?: number | null;
    feeType: FeeType;
    feeValue: number;
    minFeeCap?: number | null;
    maxFeeCap?: number | null;
    active: boolean;
}

export interface CreateUserRequest {
    fullName: string;
    email: string;
    phone?: string;
    pin: string;
}

export interface CreateBankAccountRequest {
    accountNumber: string;
    ifscCode?: string;
    bankName: string;
    balance?: number;
    isPrimary?: boolean;
}

export interface CreatePaymentMethodRequest {
    bankAccountId: number;
    type: PaymentMethodType;
    upiId?: string;
    cardNumber?: string;
    linkedBankName?: string;
    isDefault?: boolean;
}

export type AuthMethod = 'PIN' | 'OTP';

export interface InitiatePaymentRequest {
    payerUserId: number;
    payeeUserId?: number;
    sourceAccount: string;
    destinationAccount: string;
    sourcePaymentMethodId?: number;
    amount: number;
    currency: string;
    paymentMethod: 'UPI' | 'NETBANKING' | 'CREDIT_CARD';
    reference?: string;
    idempotencyKey?: string;
    authMethod: AuthMethod;
    cardNumber?: string;
    cardExpiry?: string;
    cardHolderName?: string;
    upiId?: string;
    bankName?: string;
}

export interface AuthenticatePaymentRequest {
    pin?: string;
    otp?: string;
    method: AuthMethod;
}

export interface FeeRuleRequest {
    paymentMethod: string;
    minAmount: number;
    maxAmount?: number | null;
    feeType: FeeType;
    feeValue: number;
    minFeeCap?: number | null;
    maxFeeCap?: number | null;
    active: boolean;
}

export interface UpdatePinRequest {
    currentPin: string;
    newPin: string;
}
