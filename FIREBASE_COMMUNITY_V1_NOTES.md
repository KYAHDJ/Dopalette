# DoPalette Community v1 Notes

Admin account: `kyaiko.dev@gmail.com`

## Community behavior

- Community is likes-only.
- No comments, follows, messages, replies, or random photo uploads.
- Users can share only finished DoPalette artworks.
- Shared posts store compact recipe data, not full exported images.
- Limit: 2 community uploads per user per week.
- Limit: 10 active public community posts per user.
- If a user reaches 10 active public posts, they must delete/hide an older public post before posting another.

## Feed design

- New Today loads only the latest 20 active posts.
- Featured / Trending loads only the top 20 active posts by likes.
- This avoids loading the full community feed and helps keep Firestore reads low.

## Reports

Reports are stored in Firestore under:

```text
reports/{reportId}
```

Report fields include:

```text
targetType: artwork | user
targetId: artworkId or userId
targetOwnerId: userId
reporterId: userId
reason: Spam / Offensive Content / Harassment / Inappropriate Profile / Other
status: open / dismissed / resolved_hidden
createdAt
createdAtMillis
```

The Admin Review Reports button appears only when the signed-in email equals:

```text
kyaiko.dev@gmail.com
```

Admin can currently:

- Dismiss a report.
- Hide a reported artwork from Community.

## Blocking

Blocked users are stored under:

```text
users/{myUid}/blockedUsers/{blockedUid}
```

Blocked users are filtered out of the local Community feed for the blocker.

## Firestore collections used

```text
communityArtworks
communityArtworks/{postId}/likes/{uid}
reports
users/{uid}/blockedUsers/{blockedUid}
```
