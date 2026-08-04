# Feature Plan: User/Admin Separation & Simplified Payment Flow

**Branch:** `feature/new-tpay`
**Status:** Planning — Phase 1 (User side) to be implemented first. Phase 2 (Admin) is later.

---

## 1. Requirement Summary (as understood)

1. Separate **User** and **Admin** experiences (both UI and backend).
2. Introduce a lightweight **"login"** concept — not full security auth, just knowing
   *which user* is currently using the app (can be hardened with real auth later).
3. Once a user is "logged in", they pick a payment method to **send** a payment.
4. Sender-side details should no longer be typed into the form — they should come
   from the logged-in user's own stored profile in the database:
   - **UPI:** no "source UPI ID" field — it's the logged-in user's own UPI ID.
   - **Account Transfer / Net Banking:** no "source account" field — it's the
     logged-in user's own account, already on file.
   - **Credit Card:** unchanged — user re-enters card number, expiry, and holder
     name on every payment. Card details are never stored on the user profile.
5. The **recipient/destination** side (destination account, payee UPI, payee bank,
   card, etc.) is still explicitly entered per payment.
6. **Admin dashboard** (Phase 2, later): admin can see the full payment flow across
   all users, statistics, audit trail, etc. Specifically, the admin must be able to see:
   - **Total payment volume** processed across the whole platform (all users, not
     just their own) — e.g., total amount moved, total number of payments, likely
     sliceable by status/payment method/time range.
   - **Admin's earnings from the processing fee** — the running total of processing
     fee revenue collected across all payments (see point 7).
7. **Processing fee**: every payment made on the platform incurs a **0.2% processing
   fee** on the payment amount. This fee is how the platform (admin) earns revenue,
   and must be visible to the paying user at the time of payment (so they know the
   total cost), and aggregated for the admin as "earnings" on the admin dashboard.
8. Phase 1 priority: get the **user-facing** endpoint/UI/database changes done first,
   including the processing fee plumbing (calculate + store + show fee), since the
   admin's earnings view in Phase 2 depends on this data already being captured.

---

## 2. Assumptions & Open Questions

These are assumptions made to proceed. Please confirm/correct before/while implementing:

| # | Assumption | Alternative if wrong |
|---|------------|----------------------|
| 1 | "Login" = pick/enter an existing user identity (e.g., account number or username), no password check in Phase 1. Client stores the logged-in user in `localStorage` and sends it with each request (e.g., `X-User-Id` header or bearer-style token). | Add a real password field + hashed check now instead of later. |
| 2 | Each user has **one profile per payment method** they can send from (for UPI/net banking): one own account number, one own UPI ID. Not multiple saved UPIs/accounts per user. Credit card is exempt — always re-entered, never stored. | Support multiple saved payment instruments per user (a `payment_profiles` table, user picks which one to pay from). |
| 3 | `Customer` (existing table/model) becomes the `User` entity — extended with `role` (`USER` / `ADMIN`) and the user's own `upiId`/`bankName`/account. No card fields added to the user profile. | Keep `Customer` (=payer registry) separate from a new `User`/login concept entirely. |
| 4 | Registration of new users is out of scope for Phase 1 — users are pre-seeded (like today's `DataSeeder` customers), just extended with role + own payment details. | Need a self-service "sign up" screen in Phase 1. |
| 5 | The recipient in a payment might be another user in the system, or an arbitrary external account/UPI/bank — recipient fields stay free text as today. | Recipient must also be a registered user (closed-loop only). |
| 6 | Processing fee is a flat **0.2% of the payment amount**, calculated and stored (snapshotted) on the payment at creation time, so historical payments keep the fee rate that applied when they were made even if the rate changes later. | Recalculate fee dynamically from a config value at read time instead of storing it. |
| 7 | The fee is charged **on top of** the payment amount (sender's total debit = amount + fee; recipient still receives the full stated amount) — this is closer to how a processing/convenience fee typically works. | Fee is deducted from the amount instead (recipient receives amount − fee). |
| 8 | The fee/"earnings" only counts for payments that reach **COMPLETED** status (a FAILED payment doesn't generate real revenue). | Count fee revenue on every created payment regardless of final status. |

---

## 3. Data Model Changes

### 3.1 `users` table (evolve `customers`)
Add to existing `customers` table (or introduce `users` as the new name):
- `role` VARCHAR — `USER` or `ADMIN` (default `USER`)
- `username` / login identifier (could reuse `account_number` as the login handle)
- `own_upi_id` VARCHAR — the user's own UPI address, used as implicit sender for UPI payments
- `own_bank_name` VARCHAR — user's own bank, used as implicit sender for net banking
- No card fields added here — credit card payments always require the user to
  re-enter card number/expiry/holder name per payment, exactly as today. Nothing
  new to store for cards.

### 3.2 `payments` table
- Add `user_id` INT (FK to users) — the sender, replacing free-text `source_account` as the *source of truth* (we can still denormalize `source_account` from the user record for display/history, but it's no longer client-supplied).
- `destination_*` fields (account/UPI/bank/card) remain as-is — always explicit per-payment input.
- Add `processing_fee` DECIMAL — the 0.2% fee amount calculated and snapshotted at
  payment creation time (`amount * 0.002`, rounded to 2 decimal places). Stored so it
  never has to be recomputed later even if the fee rate changes, and so admin
  earnings reports are stable/historical.

### 3.3 Processing fee constant
- Introduce a single constant/config value for the fee rate, e.g.
  `PROCESSING_FEE_RATE = 0.002` (0.2%), defined once in `PaymentService` (or a small
  `FeeCalculator`/`PricingService` helper) so it's easy to change/tune later without
  touching every call site.
- Fee is charged **on top of** the payment amount (see Assumption #7): the user's
  total debit is `amount + processing_fee`; the recipient still receives the full
  `amount`.

### 3.4 Migration
- Extend `schema.sql` with new columns (same `ALTER TABLE` pattern already used in `SchemaMigrationRunner` for backward-compatible column additions on existing DBs), including `payments.processing_fee`.
- Update `DataSeeder` to seed users with `role`, own UPI/bank/card info, so Phase 1 can be demoed without a registration flow.

---

## 4. Backend/API Changes

### 4.1 New lightweight "auth" endpoints
- `POST /api/auth/login` — body `{ username }` (or account number), returns the user profile (id, name, role, masked payment details). No password check in Phase 1 (can add later without breaking the contract).
- `GET /api/auth/me` — optional convenience if we pass a token/id back.

### 4.2 `PaymentController` / `CreatePaymentRequest` changes
- Request no longer includes sender-side fields for UPI (`sourceAccount`, sender `upiId`) or net banking (sender `bankName`). These are resolved server-side from the authenticated/current user.
- Credit card fields (`cardNumber`, `cardExpiry`, `cardHolderName`) stay in the request exactly as today — always re-entered per payment, never sourced from the user profile, never stored raw.
- The controller needs to know "who is calling" — simplest Phase 1 approach: require a header (e.g. `X-User-Id`) resolved to a `User`/`Customer` record; reject with 401/400 if missing/unknown user.
- `destinationAccount` / recipient UPI / recipient bank remain required, validated as today.

### 4.3 `PaymentService` / `PaymentValidationService`
- Sender account validation changes from "look up by free-text input" to "look up the current user's own profile" (always valid since it's already in DB) — removes the "source account invalid" failure mode, but keep "source == destination" check.
- Everything else (amount, currency, recipient checks, Luhn, UPI regex, bank whitelist) stays the same, just applied to the recipient side (and for UPI, only `destinationUpiId` needs the `name@bank` regex check now — no sender UPI to validate).

### 4.4 Payment listing/history scoping
- `GET /api/payments` should scope to the current user by default (a user only sees their own payments) when called from the user UI.
- Admin-wide listing (all users) is Phase 2 — but worth keeping the repository method generic (`findAll`) so Phase 2 can reuse it with a role check instead of a `userId` filter.

### 4.5 Processing fee calculation (Phase 1 — plumbing needed now)
- In `PaymentService`, when a payment is created, compute
  `processingFee = amount.multiply(0.002)` (rounded to 2 decimals, `HALF_UP`) and
  store it on the `Payment` alongside the rest of the fields — before validation/save.
- Include `processingFee` (and maybe `totalDebit = amount + processingFee`) in the
  `Payment` response so the client can display it without recalculating.
- No new validation errors introduced by the fee itself — it's derived, not user-input.

### 4.6 Admin endpoints (Phase 2 — for the admin dashboard)
- `GET /api/admin/payments` — cross-user payment listing (all users), same filters
  as the user-facing endpoint (status, etc.) but without the `X-User-Id` scoping;
  requires the caller's resolved user to have `role == ADMIN` (403 otherwise).
- `GET /api/admin/stats` — aggregate figures for the dashboard:
  - `totalVolume` — sum of `amount` across all payments (optionally filterable by
    status/date range — e.g., only `COMPLETED`, or all regardless of status).
  - `totalPaymentCount` — count of payments across all payments/users.
  - `totalFeeEarnings` — sum of `processing_fee` across payments with
    status = `COMPLETED` (see Assumption #8 — failed payments don't count as earned
    revenue).
  - Optionally a breakdown by payment method (UPI/NETBANKING/CREDIT_CARD) and/or by
    day, for simple charting.
- These endpoints live behind the same admin role check as `GET /api/admin/payments`.

---

## 5. Frontend/UI Changes

### Phase 1 (User)
- **Login screen**: simple form/select to "log in" as a user (e.g., pick account number from a list, or type it) — stored in `localStorage`/context, sent with API calls.
- **Simplified `PaymentForm`**: remove sender-side inputs for UPI and net banking:
  - No "Source Account" field.
  - No sender UPI ID field.
  - No sender bank selection.
  - Credit card section stays unchanged — user still types card number, expiry, holder name each time.
  - Form asks for: payment method, **recipient** details (destination account / recipient UPI / recipient bank), card details (if paying by card), amount, currency, reference.
- Show a "Paying as: {user name} ({account})" banner so it's clear whose funds are being used.
- **Processing fee preview**: as soon as the user enters an amount, show a small line
  like "Processing fee (0.2%): {fee} — Total debit: {amount + fee}" so the user knows
  the real cost before submitting. Reuses the value returned from the create-payment
  response for the confirmation/details view (no separate fee-preview endpoint needed
  — it's a pure client-side calculation of `amount * 0.002` for the live preview).
- `PaymentList` / `PaymentDetails` scoped to the logged-in user's own payments only;
  `PaymentDetails` also shows the `processingFee` that was charged for that payment.
- Add a simple "switch user / logout" control (clears local session).

### Phase 2 (Admin) — not building yet, noting for context
- Separate `/admin` route/view (shown today only as a placeholder once a user with
  `role == ADMIN` logs in).
- Full cross-user payment list, flow/status visualization, stats dashboard (reuses/extends existing `StatsBar`).
- **Platform volume card(s)**: total payment volume (sum of amounts) and total
  payment count across *all* users, likely with filters for status/payment
  method/date range — powered by `GET /api/admin/stats`.
- **Admin earnings card**: total processing fee revenue collected so far
  (`totalFeeEarnings` from `GET /api/admin/stats`), and ideally broken down by day/
  payment method so the admin can see trends, not just a single running total.
- Needs client-side routing (currently no `react-router` — will need to add it, or a simple state-based view switch, when Phase 2 starts).

---

## 6. Rollout Checklist (Phase 1 only)

1. [ ] Extend `schema.sql` + `SchemaMigrationRunner` with `role`, `own_upi_id`, `own_bank_name` on `customers`, and `user_id` on `payments`.
2. [ ] Update `Customer` model + repository to include role + own UPI/bank fields (no card fields).
3. [ ] Update `DataSeeder` with seeded users (role, own UPI/bank info).
4. [ ] Add simple login endpoint(s) (`/api/auth/login`) + a way for the controller to resolve "current user" from the request.
5. [ ] Update `CreatePaymentRequest` DTO — drop sender-side UPI/bank fields, keep card fields as-is, keep/rename destination fields clearly (e.g., `destinationUpiId`, `destinationBankName` if not already distinct from sender ones).
6. [ ] Update `PaymentService`/`PaymentValidationService` to source sender info from the current user instead of the request body.
7. [ ] Scope `GET /api/payments` (and history) to the current user.
8. [ ] Client: add login/user-switch screen + persist current user.
9. [ ] Client: strip sender fields from `PaymentForm`; wire "current user" banner.
10. [ ] Client: scope payment list/details fetches to current user.
11. [ ] Add `processing_fee` column + calculate/store 0.2% fee on payment creation (`PaymentService`); include it in the `Payment` response.
12. [ ] Client: show processing fee preview (amount × 0.2%) and total debit in `PaymentForm`, and display the charged fee in `PaymentDetails`.
13. [ ] Update seed/demo data and manually verify full lifecycle (create → validate → send → complete) still works end-to-end for a UPI, net banking, and credit card payment, including correct fee calculation.

---

## 6a. Rollout Checklist (Phase 2 — Admin, for later)

1. [ ] Add `role == ADMIN` authorization check (403 for non-admins) reusable across admin endpoints.
2. [ ] `GET /api/admin/payments` — cross-user payment listing with existing filters (status, etc.), no `X-User-Id` scoping.
3. [ ] `GET /api/admin/stats` — `totalVolume`, `totalPaymentCount`, `totalFeeEarnings` (fee revenue from `COMPLETED` payments), optionally broken down by payment method/date.
4. [ ] Add client-side view switching (or `react-router`) for an `/admin` route, replacing today's "coming in a later phase" placeholder.
5. [ ] Build admin dashboard UI: platform volume card(s), earnings-from-fees card, cross-user payment table/list reusing `PaymentList`/`StatusBadge`/`RiskMeter` where possible.
6. [ ] Manually verify: admin sees totals that match a manual sum across all seeded users' payments, and a `USER`-role login is blocked from the admin endpoints/UI.

---

## 7. Explicitly Out of Scope for Phase 1

- Admin dashboard UI/endpoints (see Phase 2 checklist above — planned, but not built until Phase 2 starts).
- Real authentication (passwords, hashing, JWT/session security, authorization middleware beyond basic user resolution).
- Multiple saved payment instruments per user.
- Self-service user registration.
