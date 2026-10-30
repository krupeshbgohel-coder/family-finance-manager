# Firebase setup (required for cloud login)

1. Open Firebase Console and create/select your project.
2. Register an Android app with package name `com.familyfinance.manager`.
3. Copy the Web API key, App ID, Project ID, and Sender ID into `app/src/main/res/values/firebase.xml` by replacing each `PASTE_...` placeholder. These are app configuration values, not service-account private keys.
4. In Firebase Authentication, enable Email/Password and Phone providers.
5. Add SHA-1 and SHA-256 fingerprints for your Android signing key to the Firebase Android app. Phone verification may require Play Integrity/reCAPTCHA configuration and a real-device test.
6. Create a Firestore database and publish `firestore.rules` after reviewing it in the Firebase Emulator/Rules Playground.
7. Build and test sign-in. With placeholder values left in place, the app remains in local-only mode.

## Family invitations
- Sign in with the account that will be the Admin.
- Use the cloud icon in the top bar to create a Family ID.
- Invite a member using the email address associated with their Firebase account.
- The invite record is stored in Firestore; this package does not send an email automatically. Share the Family ID with the invitee securely.
- The invited member signs in, opens the cloud setup dialog, enters the same Family ID, and selects Accept invitation.
- Validate all of this using test accounts before real financial data is entered.

## Critical limitation
The Firebase repository and login/invitation paths are included, but automatic synchronization of the local Room database with Firestore is not yet wired end-to-end. A successful cloud login does not by itself mean that transactions entered on one phone appear on another. Do not rely on cloud backup until that sync feature has been implemented and tested.
