# 🔥 Complete Firebase Setup Guide for Harmoniq

This guide walks you through setting up Firebase as the backend for your Harmoniq music streaming app.

## Table of Contents
1. [Create Firebase Project](#1-create-firebase-project)
2. [Configure Android App](#2-configure-android-app)
3. [Set Up Firestore Database](#3-set-up-firestore-database)
4. [Set Up Firebase Storage](#4-set-up-firebase-storage)
5. [Enable Authentication](#5-enable-authentication)
6. [Add Sample Data](#6-add-sample-data)
7. [Upload Music Files](#7-upload-music-files)
8. [Test Your Setup](#8-test-your-setup)

---

## 1. Create Firebase Project

### Step 1: Go to Firebase Console
1. Navigate to [https://console.firebase.google.com/](https://console.firebase.google.com/)
2. Sign in with your Google account

### Step 2: Create New Project
1. Click **"Add project"**
2. Enter project name: `Harmoniq` (or your preferred name)
3. Click **Continue**
4. (Optional) Disable Google Analytics for simpler setup
5. Click **Create project**
6. Wait for project creation, then click **Continue**

---

## 2. Configure Android App

### Step 1: Add Android App
1. In Firebase Console, click the **Android icon** (🤖)
2. Fill in the details:
   - **Package name**: `com.harmoniq.app`
   - **App nickname**: `Harmoniq` (optional)
   - **Debug signing certificate**: Leave blank for now
3. Click **Register app**

### Step 2: Download Configuration File
1. Download `google-services.json`
2. Place it in your project's `app/` directory:
   ```
   Harmoniq/
   ├── app/
   │   ├── google-services.json  ← Place here
   │   ├── build.gradle.kts
   │   └── src/
   └── ...
   ```

### Step 3: Verify Setup
The project is already configured with Firebase dependencies. Just ensure the `google-services.json` file is in place.

---

## 3. Set Up Firestore Database

### Step 1: Create Database
1. In Firebase Console sidebar, click **Build** → **Firestore Database**
2. Click **"Create database"**
3. Choose **"Start in test mode"** (for development)
4. Select a location (choose closest to your users)
5. Click **Enable**

### Step 2: Create Collections

#### Create `songs` Collection
1. Click **"Start collection"**
2. Collection ID: `songs`
3. Add a sample document:
   ```
   Document ID: (Auto-ID)
   
   Fields:
   - title (string): "Sample Song"
   - artistId (string): ""
   - artistName (string): "Sample Artist"
   - albumId (string): ""
   - albumName (string): "Sample Album"
   - albumArtUrl (string): "https://picsum.photos/400"
   - audioUrl (string): ""
   - duration (number): 180000
   - mood (string): "happy"
   - genre (string): "pop"
   - playCount (number): 0
   - releaseDate (number): 1700000000000
   - hasLyrics (boolean): false
   - createdAt (number): 1700000000000
   ```

#### Create `artists` Collection
1. Click **"Start collection"**
2. Collection ID: `artists`
3. Add a sample document with fields like name, imageUrl, bio, genres (array), etc.

#### Create `albums` Collection
1. Collection ID: `albums`
2. Add documents with title, artistId, artistName, coverUrl, releaseDate, songCount, etc.

#### Create `users` Collection
1. Collection ID: `users`
2. This will be auto-populated when users sign in

#### Create `playlists` Collection
1. Collection ID: `playlists`
2. Add documents with name, description, userId, songIds (array), etc.

### Step 3: Create Firestore Indexes

For the mood filter to work efficiently, create a composite index:
1. Go to **Firestore** → **Indexes**
2. Click **"Create index"**
3. Add:
   - Collection: `songs`
   - Fields: `mood` (Ascending), `playCount` (Descending)
   - Query scope: Collection

### Step 4: Security Rules (for Production)

Replace the default rules with:

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
      return request.auth.uid == userId;
    }
    
    // Songs - readable by all, writable by authenticated users
    match /songs/{songId} {
      allow read: if true;
      allow create, update: if isAuthenticated();
      allow delete: if false; // Only admin can delete
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
    
    // Users - only owner can read/write their data
    match /users/{userId} {
      allow read, write: if isOwner(userId);
    }
    
    // Playlists - public readable, owner writable
    match /playlists/{playlistId} {
      allow read: if resource.data.isPublic == true || 
                    (isAuthenticated() && resource.data.userId == request.auth.uid);
      allow create: if isAuthenticated();
      allow update, delete: if isAuthenticated() && resource.data.userId == request.auth.uid;
    }
  }
}
```

---

## 4. Set Up Firebase Storage

### Step 1: Enable Storage
1. In Firebase Console, click **Build** → **Storage**
2. Click **"Get started"**
3. Choose **"Start in test mode"**
4. Click **Next**
5. Select same location as Firestore
6. Click **Done**

### Step 2: Create Folder Structure
In Storage, create these folders:
```
/music/           - Audio files (.mp3, .m4a, .wav)
/album-art/       - Album cover images
/artist-images/   - Artist profile photos
/user-avatars/    - User profile pictures
```

### Step 3: Storage Security Rules

```javascript
rules_version = '2';
service firebase.storage {
  match /b/{bucket}/o {
    // Music files - readable by all, writable by authenticated
    match /music/{fileName} {
      allow read: if true;
      allow write: if request.auth != null
                   && request.resource.size < 50 * 1024 * 1024 // 50MB limit
                   && request.resource.contentType.matches('audio/.*');
    }
    
    // Album art - readable by all
    match /album-art/{fileName} {
      allow read: if true;
      allow write: if request.auth != null
                   && request.resource.size < 5 * 1024 * 1024 // 5MB limit
                   && request.resource.contentType.matches('image/.*');
    }
    
    // Artist images
    match /artist-images/{fileName} {
      allow read: if true;
      allow write: if request.auth != null
                   && request.resource.size < 5 * 1024 * 1024
                   && request.resource.contentType.matches('image/.*');
    }
    
    // User avatars - only owner can write
    match /user-avatars/{userId}/{fileName} {
      allow read: if true;
      allow write: if request.auth != null && request.auth.uid == userId;
    }
  }
}
```

---

## 5. Enable Authentication

### Step 1: Enable Auth Providers
1. In Firebase Console, click **Build** → **Authentication**
2. Click **"Get started"**
3. Go to **Sign-in method** tab
4. Enable:
   - **Anonymous** (click, toggle Enable, Save)
   - (Optional) **Email/Password**
   - (Optional) **Google**

The app uses Anonymous auth by default for easy onboarding.

---

## 6. Add Sample Data

### Using Firebase Console
You can manually add data through the Firebase Console's Firestore interface.

### Using a Script (Node.js)

Create a file `seed-data.js`:

```javascript
const admin = require('firebase-admin');
const serviceAccount = require('./serviceAccountKey.json');

admin.initializeApp({
  credential: admin.credential.cert(serviceAccount)
});

const db = admin.firestore();

async function seedData() {
  // Add sample artists
  const artistRef = await db.collection('artists').add({
    name: 'Glass Animals',
    imageUrl: 'https://your-storage-url/artist-images/glass-animals.jpg',
    bio: 'English indie rock band from Oxford',
    genres: ['indie', 'psychedelic pop'],
    monthlyListeners: 25000000,
    songCount: 45,
    albumCount: 3,
    createdAt: Date.now()
  });

  // Add sample album
  const albumRef = await db.collection('albums').add({
    title: 'Dreamland',
    artistId: artistRef.id,
    artistName: 'Glass Animals',
    coverUrl: 'https://your-storage-url/album-art/dreamland.jpg',
    releaseDate: new Date('2020-08-07').getTime(),
    songCount: 16,
    totalDuration: 2700000,
    genre: 'indie',
    createdAt: Date.now()
  });

  // Add sample songs
  const songs = [
    {
      title: 'Heat Waves',
      artistId: artistRef.id,
      artistName: 'Glass Animals',
      albumId: albumRef.id,
      albumName: 'Dreamland',
      albumArtUrl: 'https://your-storage-url/album-art/dreamland.jpg',
      audioUrl: 'https://your-storage-url/music/heat-waves.mp3',
      duration: 238000,
      mood: 'melancholic',
      genre: 'indie',
      playCount: 0,
      releaseDate: new Date('2020-08-07').getTime(),
      hasLyrics: true,
      lyrics: [
        { timestamp: 0, text: 'Sometimes, all I think about is you' },
        { timestamp: 4500, text: 'Late nights in the middle of June' },
        { timestamp: 8500, text: "Heat waves been fakin' me out" },
        { timestamp: 13000, text: "Can't make you happier now" }
      ],
      createdAt: Date.now()
    },
    {
      title: 'Tangerine',
      artistId: artistRef.id,
      artistName: 'Glass Animals',
      albumId: albumRef.id,
      albumName: 'Dreamland',
      albumArtUrl: 'https://your-storage-url/album-art/dreamland.jpg',
      audioUrl: 'https://your-storage-url/music/tangerine.mp3',
      duration: 240000,
      mood: 'happy',
      genre: 'indie',
      playCount: 0,
      releaseDate: new Date('2020-08-07').getTime(),
      hasLyrics: false,
      lyrics: [],
      createdAt: Date.now()
    }
  ];

  for (const song of songs) {
    await db.collection('songs').add(song);
    console.log(`Added song: ${song.title}`);
  }

  console.log('Seed data added successfully!');
}

seedData().catch(console.error);
```

---

## 7. Upload Music Files

### Step 1: Prepare Your Files
- Audio: MP3, M4A, WAV (preferably 128-320kbps MP3)
- Images: JPG, PNG (500x500 or 1000x1000 for album art)

### Step 2: Upload via Firebase Console
1. Go to **Storage**
2. Navigate to appropriate folder
3. Click **"Upload file"**
4. Select your files

### Step 3: Get Download URLs
1. Click on uploaded file
2. Copy the **"Token URL"** (includes access token)
3. Use this URL in your Firestore documents

### Step 4: Using Firebase CLI (Bulk Upload)
```bash
# Install Firebase CLI
npm install -g firebase-tools

# Login
firebase login

# Upload directory
firebase storage:upload ./local-music-folder gs://your-project.appspot.com/music/
```

---

## 8. Test Your Setup

### Verify Firestore
1. Go to Firestore Database
2. Check that collections exist and have documents
3. Test a query in the console

### Verify Storage
1. Go to Storage
2. Click on a file
3. Copy the URL and test in browser

### Verify Auth
1. Go to Authentication
2. Run the app
3. Check that a new anonymous user appears

### Test in App
1. Build and run the app
2. Check if songs load on home screen
3. Try playing a song
4. Test the mood filter

---

## Troubleshooting

### Songs not loading?
- Check Firestore rules allow read
- Verify collection name is `songs` (lowercase)
- Check console for errors

### Audio not playing?
- Verify audioUrl is a valid Firebase Storage URL
- Check Storage rules allow read
- Test URL directly in browser

### Images not loading?
- Similar to audio - check URLs and rules
- Ensure image URLs are publicly accessible

### Authentication issues?
- Verify Anonymous auth is enabled
- Check app's google-services.json is correct

---

## Next Steps

1. ✅ Set up Firebase project
2. ✅ Configure Android app
3. ✅ Create Firestore collections
4. ✅ Set up Storage
5. ✅ Enable Authentication
6. ✅ Add sample data
7. ✅ Upload music files
8. ✅ Test everything

Now you're ready to build and run your Harmoniq app! 🎉

