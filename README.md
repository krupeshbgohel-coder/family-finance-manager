# Family Finance Manager — Production-oriented Android Starter

A clean, modern Android application foundation for household finance management. It includes:
- Kotlin + Jetpack Compose UI
- Room local database for transactions, events, loans, budgets, accounts and family members
- Dashboard, transaction list, family events, loans/EMI estimates, budgets/accounts/member management
- GitHub Actions APK build workflow
- Firebase security rules starter and cloud rollout checklist

## Important truth about "production-ready"
This is a production-oriented starter package, **not a fully production-certified or deployed system**. A real family multi-login/cloud-sync app cannot be safely activated without creating a Firebase project, configuring Authentication, applying Firestore rules, testing on devices, and deciding how family invitations/roles work. No valid `google-services.json` can be generated without your Firebase project. This package intentionally does not pretend cloud sync or authentication is already active.

## Build from GitHub
1. Download and extract the ZIP on your phone or computer.
2. Create a new empty GitHub repository.
3. Upload the extracted *contents* to the repository root.
4. Open Actions → `Android Build APK`. A successful run produces the debug APK artifact.
5. For release use, configure signing and test the release build; do not distribute a debug APK as a production release.

## Firebase activation (required for multi-phone cloud sync)
1. Create a Firebase project in the Firebase console.
2. Register Android app package `com.familyfinance.manager`.
3. Download the real `google-services.json` and place it in `app/`.
4. Enable Email/Password authentication. Configure account recovery.
5. Create Cloud Firestore and apply `firestore.rules` (never use open/test rules).
6. Implement/verify family membership and invitation flow before enabling writes. Current rules are a restrictive starting point; they require each user to have a `familyId` claim/document strategy and should be reviewed against the eventual schema.
7. Test access isolation with at least two families and two user roles.

## Financial safety notes
- Loan EMI displayed is an estimate using standard monthly reducing-balance math, not a lender quote.
- This starter's account opening balances are records, not a live bank connection.
- It does not connect to UPI/banks or move money.
- Do not put passwords, API secrets, or real financial information in source control.
- Keep independent backups. Local app data can be lost if the app is uninstalled or device storage is cleared.

## Package
`com.familyfinance.manager`
Minimum Android: API 26 (Android 8)
JDK: 17


## Appearance settings
The app includes an Appearance section under **Manage** with three choices:
- **System**: follows the Android device's light/dark appearance.
- **Light**: forces the light theme.
- **Dark**: forces the dark theme.

The selected choice is saved on the device and restored the next time the app opens.
