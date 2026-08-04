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
   all users, statistics, audit trail, etc. — not being built yet.
7. Phase 1 priority: get the **user-facing** endpoint/UI/database changes done first.

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

### 3.3 Migration
- Extend `schema.sql` with new columns (same `ALTER TABLE` pattern already used in `SchemaMigrationRunner` for backward-compatible column additions on existing DBs).
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
- `PaymentList` / `PaymentDetails` scoped to the logged-in user's own payments only.
- Add a simple "switch user / logout" control (clears local session).

### Phase 2 (Admin) — not building yet, noting for context
- Separate `/admin` route/view.
- Full cross-user payment list, flow/status visualization, stats dashboard (reuses/extends existing `StatsBar`).
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
11. [ ] Update seed/demo data and manually verify full lifecycle (create → validate → send → complete) still works end-to-end for a UPI, net banking, and credit card payment.

---

## 7. Explicitly Out of Scope for Phase 1

- Admin dashboard UI/endpoints.
- Real authentication (passwords, hashing, JWT/session security, authorization middleware beyond basic user resolution).
- Multiple saved payment instruments per user.
- Self-service user registration.
