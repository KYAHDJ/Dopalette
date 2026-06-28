# DoPalette Phase 1: Gmail Login Setup

This build adds the Google/Gmail sign-in UI and code only. Sync is intentionally OFF.

## What is already added

- Me tab > Sign In With Google
- Firebase Auth dependency
- Google Sign-In dependency
- Conditional Google Services Gradle plugin
- `GoogleAuthController.kt`

## Important

The app still builds without Firebase config, but real sign-in will show:

`Firebase is not connected yet. Add app/google-services.json first.`

That is expected until you create the Firebase app.

## Steps to enable real Gmail login

1. Go to Firebase Console.
2. Create a project named `DoPalette`.
3. Add an Android app.
4. Use package name:

```text
com.dopalette.app
```

5. Download `google-services.json`.
6. Put it here:

```text
app/google-services.json
```

7. In Firebase Console > Authentication > Sign-in method, enable Google.
8. Add SHA-1 for your debug build.

Use this command in the project folder:

```powershell
.\gradlew signingReport
```

Copy the SHA-1 from the `debug` variant into Firebase project settings.

9. Re-download `google-services.json` after adding SHA-1 and replace the old one.
10. Build again:

```powershell
.\gradlew assembleDebug
```

## What Phase 1 does NOT do yet

- No cloud progress backup
- No artwork sync
- No community upload
- No Firestore writes
- No Storage upload

Phase 1 is only login/logout.
