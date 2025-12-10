# Firestore Index Setup Guide

## Create Composite Index for Playlists

Your app needs a composite index for querying playlists by `userId` and ordering by `updatedAt`.

### Quick Method (Recommended):

1. **Click the link from the error message** - The error log contains a direct link to create the index:
   ```
   https://console.firebase.google.com/v1/r/project/harmoniq-67/firestore/indexes?create_composite=...
   ```
   Just click this link and it will auto-create the index!

### Manual Method:

1. Go to **Firebase Console**: https://console.firebase.google.com/
2. Select your **Harmoniq** project
3. Click **Build** → **Firestore Database**
4. Click the **Indexes** tab
5. Click **Create Index**
6. Fill in the following:
   - **Collection ID**: `playlists`
   - **Fields to index**:
     - Field: `userId` | Order: **Ascending**
     - Field: `updatedAt` | Order: **Descending**
   - **Query scope**: Collection
7. Click **Create**

### What This Index Does:

This composite index allows Firestore to efficiently query playlists:
- Filtered by `userId` (where userId == currentUserId)
- Ordered by `updatedAt` in descending order (newest first)

### After Creating the Index:

1. **Wait 2-5 minutes** for the index to build (you'll see a "Building" status)
2. Once it shows "Enabled", **restart your app**
3. The error should be gone and playlists will load sorted by most recently updated

### Note:

If you don't have any playlists yet, the index will still be created but won't be used until you have data. This is fine - the index will be ready when you need it!

