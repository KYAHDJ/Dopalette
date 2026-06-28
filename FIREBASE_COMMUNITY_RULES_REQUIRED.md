# Required Firestore Rules For DoPalette Community v1

Admin email:

```text
kyaiko.dev@gmail.com
```

Paste this in Firebase Console → Firestore Database → Rules → Publish.

These rules are required for upload, likes, reports, blocking/unblocking, admin review, and hard cleanup when posts/reports are removed.

```js
rules_version = '2';
service cloud.firestore {
  match /databases/{database}/documents {

    function signedIn() {
      return request.auth != null;
    }

    function isOwner(uid) {
      return signedIn() && request.auth.uid == uid;
    }

    function isAdmin() {
      return signedIn() && request.auth.token.email == 'kyaiko.dev@gmail.com';
    }

    function onlyLikesCountChanged() {
      return signedIn()
        && request.resource.data.diff(resource.data).affectedKeys().hasOnly(['likesCount']);
    }

    match /communityArtworks/{postId} {
      allow read: if true;
      allow create: if signedIn()
        && request.resource.data.ownerId == request.auth.uid;

      // Post owner/admin can edit/delete. Any signed-in user can only change likesCount.
      allow update: if isAdmin()
        || (signedIn() && resource.data.ownerId == request.auth.uid)
        || onlyLikesCountChanged();

      allow delete: if isAdmin()
        || (signedIn() && resource.data.ownerId == request.auth.uid);

      match /likes/{userId} {
        allow read: if true;
        allow create, delete: if signedIn() && userId == request.auth.uid;
      }
    }

    // Kept for older test builds if any old Community code still points here.
    match /communityPosts/{postId} {
      allow read: if true;
      allow create: if signedIn()
        && request.resource.data.ownerId == request.auth.uid;
      allow update: if isAdmin()
        || (signedIn() && resource.data.ownerId == request.auth.uid)
        || onlyLikesCountChanged();
      allow delete: if isAdmin()
        || (signedIn() && resource.data.ownerId == request.auth.uid);

      match /likes/{userId} {
        allow read: if true;
        allow create, delete: if signedIn() && userId == request.auth.uid;
      }
    }

    match /reports/{reportId} {
      // The app uses deterministic report IDs so the same user cannot spam the same target.
      // Admin delete is required so dismissed/resolved reports do not remain in Firestore.
      allow create, update: if signedIn()
        && request.resource.data.reporterId == request.auth.uid;
      allow read, delete: if isAdmin();
    }

    match /users/{userId} {
      allow read: if true;
      allow write, delete: if isOwner(userId) || isAdmin();

      match /blockedUsers/{blockedUserId} {
        allow read, write, delete: if isOwner(userId);
      }

      match /{document=**} {
        allow read, write, delete: if isOwner(userId) || isAdmin();
      }
    }

    // Kept only in case older builds used this root-level block path.
    match /blockedUsers/{userId}/users/{blockedUserId} {
      allow read, write, delete: if isOwner(userId);
    }
  }
}
```


## Cleanup behavior in this build

This build no longer only marks Community posts/reports as hidden or dismissed. It deletes the related data so Firebase does not fill with old unused records.

When a user deletes their own Community post, the app removes:

- `communityArtworks/{postId}`
- `communityArtworks/{postId}/likes/*`
- matching `reports/*` where `targetId` or `artworkId` equals the post id
- matching Storage images in `sharedArtworks/{ownerId}/{postId}/...` or `communityArtworks/{postId}/...` if those files exist

When an admin dismisses a report, the app deletes the report document.

When an admin removes reported artwork, the app deletes the artwork document, likes, related reports, and matching Storage files.
