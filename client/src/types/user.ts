export type UserRole = 'USER' | 'ADMIN';

export interface CurrentUser {
    id: number;
    name: string;
    accountNumber: string;
    country?: string;
    role: UserRole;
    ownUpiId?: string;
    ownBankName?: string;
}
