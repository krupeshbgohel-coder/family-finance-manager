# Firebase configuration plan — Family Finance Manager

Package: `com.familyfinance.manager`

This repository is intentionally prepared for the next Firebase setup step. Do not add a fake `google-services.json`; Firebase generates it only for your real project.

## Authentication choices agreed
- Email + password sign-in
- Phone number + SMS OTP sign-in
- Password reset for email accounts
- Language options: Gujarati, Hindi, English

## Before enabling Firebase
1. In Firebase Console, create/select the project.
2. Add an Android app with package name `com.familyfinance.manager`.
3. Download the actual `google-services.json` and put it in `app/google-services.json`.
4. Enable Email/Password and Phone providers in Authentication.
5. Create Firestore in production mode.
6. Review and deploy `firestore.rules` only after confirming the final schema and family membership flow.

## Data model proposed (not yet connected)
- `users/{uid}`: displayName, email, phoneNumber, preferredLanguage, createdAt
- `families/{familyId}`: name, ownerUid, createdAt
- `families/{familyId}/members/{uid}`: role (`owner`, `admin`, `member`), status, joinedAt
- `families/{familyId}/transactions/{transactionId}`: type, amountPaise, category, accountId, note, occurredAt, createdBy
- `families/{familyId}/budgets/{budgetId}`: category, monthKey, limitPaise
- `families/{familyId}/accounts/{accountId}`: name, type, openingBalancePaise
- `families/{familyId}/events/{eventId}`: title, eventDate, budgetPaise, actualPaise
- `families/{familyId}/loans/{loanId}`: name, principalPaise, annualRateBps, tenureMonths, startDate

Store money as integer paise rather than floating-point rupees in cloud documents.

## Security requirements
- Never use public read/write rules.
- A signed-in user must be an active member of the family before reading or writing that family's records.
- Only owners/admins may invite/remove members or change roles.
- A user's client-supplied `role` field must never grant privileges by itself.
- Test rules with two separate family IDs and users before storing real financial data.

## Current implementation status
The current Android package includes the local finance UI and Room database. Firebase Authentication, OTP delivery, Firestore sync, and server-side family membership enforcement are **not active yet**. These are the next implementation/configuration step after the real Firebase project details are available.
