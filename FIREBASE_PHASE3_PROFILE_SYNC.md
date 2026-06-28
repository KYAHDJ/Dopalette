# DoPalette Phase 3 Profile Sync

This phase syncs only lightweight account profile data:

- displayName
- email
- Google photo URL
- DoPalette custom avatar image URL
- xp
- level
- badgeId
- borderId

It does NOT sync drafts, strokes, finished artwork, or community artwork.

## Firestore rules

Use:

```js
rules_version = '2';

service cloud.firestore {
  match /databases/{database}/documents {
    match /users/{userId} {
      allow read, write: if request.auth != null && request.auth.uid == userId;
    }
  }
}
```

## Storage setup for profile image sync

Enable Firebase Storage, then use:

```js
rules_version = '2';

service firebase.storage {
  match /b/{bucket}/o {
    match /profiles/{userId}/{fileName} {
      allow read, write: if request.auth != null && request.auth.uid == userId;
    }
  }
}
```

The app uploads only one compressed profile avatar file:

`profiles/{uid}/avatar.jpg`

This keeps profile image sync lightweight.
