import type { TransactionFeeRule } from '../types/banking';

// Client-side preview of the fee the server will charge, mirroring
// FeeCalculationService on the backend. This is ONLY used to show the user
// an estimate before submitting - the server always recalculates and
// enforces the authoritative fee.
const DEFAULT_FEE_PERCENTAGE = 1.0; // 1% fallback, matches server default
const DEFAULT_MAX_FEE_CAP = 500.0;

export interface FeePreview {
    feeAmount: number;
    feePercentage: number;
    /** Total amount debited from the sender: amount + feeAmount. */
    totalDebit: number;
}

function round2(value: number): number {
    return Math.round((value + Number.EPSILON) * 100) / 100;
}

function applyCaps(
    feeAmount: number,
    minCap?: number | null,
    maxCap?: number | null,
): number {
    let result = feeAmount;
    if (minCap != null && result < minCap) result = minCap;
    if (maxCap != null && result > maxCap) result = maxCap;
    return result;
}

function findMatchingRule(
    rules: TransactionFeeRule[],
    paymentMethod: string,
    amount: number,
): TransactionFeeRule | undefined {
    const candidates = rules.filter(
        (r) =>
            r.active &&
            r.paymentMethod &&
            (r.paymentMethod.toUpperCase() === paymentMethod.toUpperCase() ||
                r.paymentMethod.toUpperCase() === 'ALL') &&
            (r.minAmount == null || amount >= r.minAmount) &&
            (r.maxAmount == null || amount <= r.maxAmount),
    );
    // Prefer a method-specific rule over an "ALL" catch-all.
    return [...candidates].sort(
        (a, b) =>
            (a.paymentMethod.toUpperCase() === 'ALL' ? 1 : 0) -
            (b.paymentMethod.toUpperCase() === 'ALL' ? 1 : 0),
    )[0];
}

/**
 * Estimates the transaction fee and resulting total debit for a given
 * amount/payment method, based on the currently configured fee rules.
 * Returns null when the amount isn't a positive number yet.
 */
export function calculateFeePreview(
    rules: TransactionFeeRule[],
    paymentMethod: string,
    amount: number,
): FeePreview | null {
    if (!Number.isFinite(amount) || amount <= 0) return null;

    const rule = findMatchingRule(rules, paymentMethod, amount);
    let feeAmount: number;
    let feePercentage: number;

    if (rule) {
        if (rule.feeType === 'FLAT') {
            feeAmount = rule.feeValue;
            feePercentage = amount > 0 ? (feeAmount / amount) * 100 : 0;
        } else {
            feePercentage = rule.feeValue;
            feeAmount = round2((amount * feePercentage) / 100);
            feeAmount = applyCaps(feeAmount, rule.minFeeCap, rule.maxFeeCap);
        }
    } else {
        feePercentage = DEFAULT_FEE_PERCENTAGE;
        feeAmount = round2((amount * feePercentage) / 100);
        feeAmount = applyCaps(feeAmount, null, DEFAULT_MAX_FEE_CAP);
    }

    feeAmount = round2(feeAmount);
    const totalDebit = round2(amount + feeAmount);
    return { feeAmount, feePercentage, totalDebit };
}
