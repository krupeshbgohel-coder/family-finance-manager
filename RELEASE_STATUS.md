# Release Status — updated source package

**Status: Feature-development candidate; NOT production-ready and NOT build-verified.**

## Implemented in this package
- Red-accented light/dark UI with bottom navigation for Transactions, Stats, Accounts and More.
- Local Room records for transactions, categories/subcategories, events, loans, budgets, accounts and family members.
- Transaction date picker; default date uses the last saved transaction date (today before the first entry).
- Active events appear in the Expense category dropdown and are linked to transactions by event ID.
- Event screen calculates actual spend from linked expense transactions and lists those transactions separately.
- Account picker only lists active accounts; Accounts screen can add, deactivate/reactivate and delete accounts.
- Category and subcategory management; a subcategory can be assigned to a parent category.
- Dashboard monthly Budget vs Actual indicators and over-budget warnings.
- Loan EMI due day, monthly “mark paid” action, and a local WorkManager notification worker.
- Room migrations are defined through schema version 6.
- GitHub Actions workflow requests a debug APK build.

## Still required before production
1. Run a real Android Gradle build and fix any build errors; this environment had no Gradle installation/wrapper JAR and no network access to download Gradle.
2. Test on a physical Android phone, including notification permission and WorkManager scheduling. WorkManager timing is approximate, not exact.
3. Implement and test bidirectional Room ↔ Firestore sync, offline queue/retry, conflict handling and deletion propagation. Current Firebase repository helpers are not wired to the Room database.
4. Complete the in-app Gujarati/Hindi/English language selector and translate all Compose labels; current language resources are only a starter set.
5. Configure and test Firebase Authentication providers, invitations, membership, and Firestore rules against the user's own Firebase project.
6. Add proper account-to-account transfer records and validate account balances, loan repayment schedules, exports, backup/restore, and privacy/security before release.

Do not use this as the only copy of financial records. The package is source code, not a verified production APK.
