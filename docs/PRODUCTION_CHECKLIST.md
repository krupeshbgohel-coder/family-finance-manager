# Production Readiness Checklist

This checklist is intentionally explicit: a successful APK build alone does not make a financial app production-ready.

## Identity and access
- [ ] Firebase project registered for `com.familyfinance.manager`
- [ ] Email/password sign-in enabled and account recovery tested
- [ ] Family invitation flow defined; no public self-joining to a family
- [ ] Roles and permissions documented (owner/admin/member/read-only)
- [ ] Firestore rules tested with Firebase Emulator Suite for cross-family isolation
- [ ] Never use `allow read, write: if true`

## Data integrity
- [ ] Stable IDs and familyId/createdBy/updatedAt fields on every cloud record
- [ ] Duplicate prevention and idempotent sync
- [ ] Conflict resolution for offline edits on multiple devices
- [ ] Database migrations tested
- [ ] Delete/export/restore flow tested
- [ ] Currency/date/time-zone behavior tested

## Finance calculations
- [ ] EMI formula checked against lender schedules for reducing and flat-rate products
- [ ] Part-payment, prepayment, overdue fees and rate changes modelled
- [ ] Account transfers represented as paired ledger entries without double counting
- [ ] Monthly/yearly reports reconcile against raw transactions
- [ ] No claims of live bank/UPI integration unless a licensed integration is actually implemented

## Security and privacy
- [ ] App lock / biometric gate and secure session expiry
- [ ] Sensitive fields minimized; no secrets committed to GitHub
- [ ] Cloud data access logs and deletion process reviewed
- [ ] Privacy notice and user consent prepared
- [ ] Backup encryption and restore drill tested

## Release
- [ ] Unit tests and UI tests pass
- [ ] Test on multiple Android versions and screen sizes
- [ ] Crash reporting and monitoring configured with privacy review
- [ ] Release signing key stored outside source control
- [ ] Release APK/AAB built and tested; debug APK not distributed as production
