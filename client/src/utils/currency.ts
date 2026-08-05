// Static exchange rates to INR (base currency). In a production system these
// would be fetched from a live FX-rate provider and cached/refreshed
// periodically. Values are illustrative approximations for demo purposes.
const RATES_TO_INR: Record<string, number> = {
    INR: 1,
    USD: 83.5,
    EUR: 90.5,
    GBP: 105.8,
};

export const DEFAULT_CURRENCY = 'INR';

export function getSupportedCurrencies(): string[] {
    return Object.keys(RATES_TO_INR);
}

/** Converts an amount in `currency` to its INR equivalent. */
export function toINR(amount: number, currency: string): number {
    const rate = RATES_TO_INR[currency?.toUpperCase()] ?? 1;
    return amount * rate;
}

export function formatINR(amount: number): string {
    return new Intl.NumberFormat('en-IN', {
        style: 'currency',
        currency: 'INR',
        maximumFractionDigits: 2,
    }).format(amount);
}

/** Formats an amount in its own currency, e.g. "1,250.00 USD". */
export function formatAmount(amount: number, currency: string): string {
    return `${amount.toFixed(2)} ${currency}`;
}

/**
 * Renders "1,250.00 USD (≈ ₹1,04,375.00)" for non-INR currencies, or just
 * the INR amount when the payment is already in INR.
 */
export function formatWithInrEquivalent(
    amount: number,
    currency: string,
): string {
    if (currency?.toUpperCase() === 'INR') {
        return formatINR(amount);
    }
    return `${formatAmount(amount, currency)} (\u2248 ${formatINR(toINR(amount, currency))})`;
}
