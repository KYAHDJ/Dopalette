# DoPalette Firebase Storage + Google Play Readiness Notes

This build keeps DoPalette on Firebase only:

- Firebase Authentication: Google/Gmail account login.
- Cloud Firestore: profile name, XP, level, badgeId, borderId, legal/community metadata later.
- Firebase Storage: compressed profile images now, community artwork images later. This build also deletes orphaned Community artwork files when posts are removed.

## Profile image upload

Profile images are intentionally tiny before upload:

- max side: 128 px
- format: JPEG
- quality: 58
- storage path: `profileImages/{uid}/avatar.jpg`
- Firestore field: `users/{uid}.avatarUrl`

This keeps storage and bandwidth low.

## Recommended Firebase Storage rules for this build

Paste these into Firebase Console > Storage > Rules, then publish:

```javascript
rules_version = '2';

service firebase.storage {
  match /b/{bucket}/o {

    match /profileImages/{userId}/{allPaths=**} {
      allow read: if true;
      allow create, update: if request.auth != null
                            && request.auth.uid == userId
                            && request.resource.size < 200 * 1024
                            && request.resource.contentType.matches('image/.*');
      allow delete: if request.auth != null
                    && request.auth.uid == userId;
    }

    match /sharedArtworks/{userId}/{postId}/{allPaths=**} {
      allow read: if true;
      allow create, update: if request.auth != null
                            && request.auth.uid == userId
                            && request.resource.size < 2 * 1024 * 1024
                            && request.resource.contentType.matches('image/.*');
      allow delete: if request.auth != null
                    && request.auth.uid == userId;
    }

    match /communityArtworks/{postId}/{allPaths=**} {
      allow read: if true;
      allow create, update: if request.auth != null
                            && request.resource.size < 2 * 1024 * 1024
                            && request.resource.contentType.matches('image/.*');
      allow delete: if request.auth != null;
    }
  }
}
```

## Recommended Firestore rules for account/profile phase

```javascript
rules_version = '2';

service cloud.firestore {
  match /databases/{database}/documents {

    match /users/{userId} {
      allow read: if request.auth != null;
      allow create, update, delete: if request.auth != null
                                    && request.auth.uid == userId;
    }

    match /communityPosts/{postId} {
      allow read: if true;
      allow create: if request.auth != null
                    && request.resource.data.ownerId == request.auth.uid;
      allow update, delete: if request.auth != null
                            && resource.data.ownerId == request.auth.uid;
    }

    match /reports/{reportId} {
      allow create: if request.auth != null;
      allow read, update, delete: if false;
    }

    match /blocks/{blockId} {
      allow read, write: if request.auth != null
                         && blockId.matches(request.auth.uid + '_.*');
    }
  }
}
```

## Google Play readiness features added to Profile tab

- Privacy Policy screen
- Terms of Service screen
- Community Guidelines screen
- Blocked Users placeholder screen
- Report History placeholder screen
- Delete Account dialog
- Local Erase App Data dialog

Community image publishing is still not fully active in this build. The screens and structure are prepared so the app has the required user-facing safety/data controls before Google Play review.
