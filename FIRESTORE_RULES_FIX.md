# Firestore Security Rules Fix - PERMISSION_DENIED Error

You're getting a `PERMISSION_DENIED` error because your Firestore security rules are blocking access.

## Steps to Fix:

1. Go to Firebase Console: https://console.firebase.google.com/
2. Select your **Harmoniq** project
3. Click **Build** → **Firestore Database**
4. Click the **Rules** tab
5. **IMPORTANT**: Replace ALL existing rules with the following:

```javascript
rules_version = '2';
service cloud.firestore {
  match /databases/{database}/documents {
    // Helper function to check if user is authenticated
    function isAuthenticated() {
      return request.auth != null;
    }
    
    // Helper function to check if user owns the document
    function isOwner(userId) {
      return request.auth != null && request.auth.uid == userId;
    }
    
    // Users - allow authenticated users to read their own document
    // Allow create/update/delete only for their own document
    match /users/{userId} {
      // Allow read if authenticated (needed for getCurrentUser flow)
      allow read: if isAuthenticated();
      // Allow create if authenticated and creating their own document
      allow create: if isAuthenticated() && request.auth.uid == userId;
      // Allow update if authenticated and updating their own document
      allow update: if isAuthenticated() && request.auth.uid == userId;
      // Allow delete if authenticated and deleting their own document
      allow delete: if isAuthenticated() && request.auth.uid == userId;
    }
    
    // Songs - readable by all, writable by authenticated users
    match /songs/{songId} {
      allow read: if true;
      allow create, update: if isAuthenticated();
      allow delete: if false;
    }
    
    // Artists - readable by all
    match /artists/{artistId} {
      allow read: if true;
      allow write: if isAuthenticated();
    }
    
    // Albums - readable by all
    match /albums/{albumId} {
      allow read: if true;
      allow write: if isAuthenticated();
    }
    
    // Playlists - public readable, owner writable
    match /playlists/{playlistId} {
      allow read: if true;
      allow create: if isAuthenticated();
      allow update, delete: if isAuthenticated() && 
        resource.data.userId == request.auth.uid;
    }
  }
}
```

6. Click **Publish** to save the rules
7. Wait a few seconds for the rules to propagate

## Key Points:

- **`allow read: if isAuthenticated()`** - This allows any authenticated user to read user documents (needed for the snapshot listener)
- **`allow create: if isAuthenticated() && request.auth.uid == userId`** - Users can only create their own document
- **`allow update: if isAuthenticated() && request.auth.uid == userId`** - Users can only update their own document

## Testing:

After updating the rules:
1. **Restart your app** (close and reopen)
2. Try signing up or logging in again
3. Check Logcat - the PERMISSION_DENIED error should be gone
4. Check Firebase Console → Firestore Database → Data tab to see if user documents appear

## If Still Not Working:

If you still get permission errors, temporarily use these **TEST MODE** rules (ONLY FOR DEVELOPMENT):

```javascript
rules_version = '2';
service cloud.firestore {
  match /databases/{database}/documents {
    match /{document=**} {
      allow read, write: if request.time < timestamp.date(2025, 12, 31);
    }
  }
}
```

⚠️ **WARNING**: The test mode rules above allow anyone to read/write. Only use for testing, then switch back to the secure rules above!
