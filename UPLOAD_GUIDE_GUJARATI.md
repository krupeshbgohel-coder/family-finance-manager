# GitHub પર Upload કરવાની ગુજરાતી માર્ગદર્શિકા

1. `Family_Finance_Manager_Complete_Package.zip` ડાઉનલોડ કરીને Extract કરો.
2. Extract થયેલા project folderની અંદરની બધી files અને folders તમારી GitHub repositoryના rootમાં upload કરો. માત્ર ZIP file upload ન કરશો.
3. `.github/workflows/build.yml` ફાઇલ પણ upload થયેલી હોવી જોઈએ.
4. GitHub → Actions → Android Build APK → Run workflow પસંદ કરો.
5. Build સફળ થાય તો Artifactsમાંથી `family-finance-manager-debug-apk` ડાઉનલોડ કરો.
6. Build fail થાય તો Actions logનો છેલ્લો error screenshot અથવા text મોકલો.

## Firebase પહેલાં ધ્યાનમાં રાખો
- `app/src/main/res/values/firebase.xml`માં તમારા Firebase projectની app configuration નાખ્યા વગર cloud login શરૂ નહીં થાય.
- Firebase Consoleમાં Email/Password અને Phone sign-in enable કરો અને SHA fingerprints ઉમેરો.
- Firestore rules deploy કરતાં પહેલાં test accounts સાથે Rules Playground/Emulatorમાં ચકાસો.
- આ packageમાં automatic multi-phone data sync હજી સંપૂર્ણ રીતે જોડાયેલું નથી. તેથી cloud login ચાલે તો પણ transactions બધા ફોનમાં દેખાશે એવું માનશો નહીં.
- હાલનો build full Gradle build અથવા device testingથી verify થયેલો નથી.
