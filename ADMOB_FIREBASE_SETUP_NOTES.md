# DoPalette AdMob + Firebase Setup Notes

This build connects the app structure to AdMob and Firebase Premium state.

## What is already wired

- Rewarded Ad for locked Special categories.
- 24-hour Special unlock after a rewarded ad is earned.
- Interstitial Ad after the artwork is saved and Done flow finishes.
- Banner Ad only in Me/Profile areas.
- Premium removes ads and opens Specials forever.
- Premium state saves to Firestore under `users/{uid}`.
- Guest users cannot buy Premium; they are sent to the account/sign-in page first.
- Signing out resets local premium state back to guest mode.

## AdMob IDs

The code currently uses Google test ad IDs:

- App ID in `AndroidManifest.xml`
- Banner ID in `AdMobManager.kt`
- Interstitial ID in `AdMobManager.kt`
- Rewarded ID in `AdMobManager.kt`

Before Play Store release, replace them with your real AdMob IDs.

## Firebase user fields

Premium writes these fields to Firestore:

```text
users/{uid}/isPremium = true
users/{uid}/premiumArtist = true
users/{uid}/adsRemoved = true
users/{uid}/specialsForever = true
users/{uid}/premiumBorderId = "rainbow_border"
users/{uid}/borderId = "rainbow_border"
users/{uid}/badgeId = "premium_artist"
users/{uid}/premiumSince = server timestamp
```

## Next step

Google Play Billing is still not connected. The current Premium button activates Premium through the existing app flow so the Firebase/AdMob behavior can be tested first. When ready, connect the purchase success callback to:

```kotlin
MonetizationStore.activatePremiumForAccount(account)
```
