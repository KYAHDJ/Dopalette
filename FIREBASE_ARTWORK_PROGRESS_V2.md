# DoPalette Firebase Artwork Progress V2

This build keeps the working local V2 save system and adds Firebase as a sync layer.

## What syncs

For each signed-in Firebase user:

```text
users/{uid}/artworkDraftsV2/{artworkTitle}
users/{uid}/artworkFinishedV2/{finishedLayerId}
```

Each document stores the same lightweight V2 JSON already used locally.

## What does NOT sync

- PNG exports
- thumbnails
- preview cache
- undo history
- redo history
- temporary editor state

## Expected behavior

- Local save still works instantly.
- If signed in, each Back/Done/save uploads the tiny V2 JSON to Firestore.
- On sign in, cloud V2 progress downloads and merges with local V2 progress.
- If Firebase is offline or denied by rules, local progress remains safe.

## Firestore rules needed

Use rules like this for testing authenticated users:

```js
rules_version = '2';
service cloud.firestore {
  match /databases/{database}/documents {
    match /users/{userId} {
      allow read, write: if request.auth != null && request.auth.uid == userId;

      match /artworkDraftsV2/{docId} {
        allow read, write, delete: if request.auth != null && request.auth.uid == userId;
      }

      match /artworkFinishedV2/{docId} {
        allow read, write, delete: if request.auth != null && request.auth.uid == userId;
      }
    }
  }
}
```

