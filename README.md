# Family Finance Manager — Android Project Package

This repository is an Android Kotlin + Jetpack Compose project for a household finance manager.

## Included in this update
- Branded splash/logo and Compose UI.
- Local Room database for transactions, categories, family events, loans, budgets, accounts, and family members.
- Add/edit/delete transactions; category dropdowns; active family event dropdown; active/inactive category and event controls.
- Dashboard cash flow, monthly/year-to-date summaries, budget progress, and daily/weekly/monthly transaction summaries.
- Loan estimator with flat/reducing balance and borrowed/lent direction.
- System/Light/Dark theme.
- Firebase Email/Password sign-in, password reset, phone OTP sign-in code path, family creation and admin invitation records.
- Firestore repository and restrictive starter security rules.
- English/Hindi/Gujarati string resource starter files.
- GitHub Actions debug APK workflow.

## Important status
This package has **not been verified by a full Android Gradle build or device test in this environment**. Firebase is not configured by default. Automatic local Room ↔ Firestore data synchronization, invitation email delivery, complete screen-by-screen translation, repayment ledger, push reminders, and full chart visualizations are not complete. Do not call this production-ready until these are completed and tested.

## Build an APK on GitHub
1. Upload the contents of this project folder to the root of your repository.
2. Open Actions → Android Build APK.
3. Run workflow, then download the `family-finance-manager-debug-apk` artifact.

## Firebase setup
Follow `FIREBASE_SETUP_NEXT.md`. Do not commit private service-account credentials. The app-level Firebase values in `app/src/main/res/values/firebase.xml` are placeholders until replaced with your Firebase project's app configuration.
