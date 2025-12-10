# Harmoniq 🎵

A beautiful Spotify clone built with Jetpack Compose, Kotlin, and Firebase. Features a dark theme with cyan accents, mood-based music filtering, and synced lyrics support.

![Kotlin](https://img.shields.io/badge/Kotlin-1.9.20-blue)
![Jetpack Compose](https://img.shields.io/badge/Jetpack%20Compose-Material3-green)
![Firebase](https://img.shields.io/badge/Firebase-Backend-orange)

## ✨ Features

- 🎨 **Beautiful Dark Theme** - Minimal, modern UI with cyan accents
- 🎭 **Mood-Based Filtering** - Filter songs by mood (Happy, Sad, Energetic, Calm, etc.)
- 📝 **Synced Lyrics** - Real-time lyrics that scroll with the music
- 🎵 **Full Music Player** - Play, pause, skip, shuffle, repeat
- 💿 **Album Art Colors** - Dynamic gradient backgrounds based on album art
- 📚 **Library Management** - Playlists, liked songs, recently played
- 🔍 **Search** - Find songs, artists, and albums
- ☁️ **Firebase Backend** - Store music and sync across devices

## 📱 Screenshots

The UI is inspired by modern music players with:
- Home screen with suggestions and quick actions
- Full-screen player with large album art
- Synced lyrics view
- Library with playlists and liked songs

## 🚀 Getting Started

### Prerequisites

- Android Studio Hedgehog (2023.1.1) or later
- JDK 17
- Firebase account

### Firebase Setup

1. **Create a Firebase Project**
   - Go to [Firebase Console](https://console.firebase.google.com/)
   - Click "Add project"
   - Enter project name: "Harmoniq" (or your choice)
   - Disable Google Analytics (optional) and create project

2. **Add Android App to Firebase**
   - In Firebase console, click the Android icon
   - Package name: `com.harmoniq.app`
   - App nickname: "Harmoniq"
   - Download `google-services.json`
   - Place it in `app/` directory

3. **Enable Firebase Services**

   **Firestore Database:**
   - Go to Build → Firestore Database
   - Click "Create database"
   - Start in test mode (for development)
   - Choose a location close to your users

   **Firebase Storage:**
   - Go to Build → Storage
   - Click "Get started"
   - Start in test mode
   - Choose same location as Firestore

   **Authentication:**
   - Go to Build → Authentication
   - Click "Get started"
   - Enable "Anonymous" sign-in method

4. **Set Firestore Rules (Production)**
   ```javascript
   rules_version = '2';
   service cloud.firestore {
     match /databases/{database}/documents {
       // Songs collection - read for all, write for admins
       match /songs/{songId} {
         allow read: if true;
         allow write: if request.auth != null;
       }
       
       // Artists collection
       match /artists/{artistId} {
         allow read: if true;
         allow write: if request.auth != null;
       }
       
       // Albums collection
       match /albums/{albumId} {
         allow read: if true;
         allow write: if request.auth != null;
       }
       
       // User-specific data
       match /users/{userId} {
         allow read, write: if request.auth != null && request.auth.uid == userId;
       }
       
       // Playlists
       match /playlists/{playlistId} {
         allow read: if true;
         allow write: if request.auth != null;
       }
     }
   }
   ```

5. **Set Storage Rules**
   ```javascript
   rules_version = '2';
   service firebase.storage {
     match /b/{bucket}/o {
       match /music/{allPaths=**} {
         allow read: if true;
         allow write: if request.auth != null;
       }
       match /album-art/{allPaths=**} {
         allow read: if true;
         allow write: if request.auth != null;
       }
       match /artist-images/{allPaths=**} {
         allow read: if true;
         allow write: if request.auth != null;
       }
     }
   }
   ```

### Firestore Data Structure

The app expects the following collections:

#### `songs` Collection
```json
{
  "id": "auto-generated",
  "title": "Heat Waves",
  "artistId": "artist_id",
  "artistName": "Glass Animals",
  "albumId": "album_id",
  "albumName": "Dreamland",
  "albumArtUrl": "https://firebase-storage-url/album-art/dreamland.jpg",
  "audioUrl": "https://firebase-storage-url/music/heat-waves.mp3",
  "duration": 238000,
  "mood": "melancholic",
  "genre": "indie",
  "playCount": 0,
  "releaseDate": 1597708800000,
  "hasLyrics": true,
  "lyrics": [
    { "timestamp": 0, "text": "Sometimes, all I think about is you" },
    { "timestamp": 4000, "text": "Late nights in the middle of June" },
    { "timestamp": 8000, "text": "Heat waves been fakin' me out" }
  ],
  "createdAt": 1700000000000
}
```

#### `artists` Collection
```json
{
  "id": "auto-generated",
  "name": "Glass Animals",
  "imageUrl": "https://firebase-storage-url/artist-images/glass-animals.jpg",
  "bio": "English indie rock band from Oxford...",
  "genres": ["indie", "psychedelic"],
  "monthlyListeners": 25000000,
  "songCount": 45,
  "albumCount": 3,
  "createdAt": 1700000000000
}
```

#### `albums` Collection
```json
{
  "id": "auto-generated",
  "title": "Dreamland",
  "artistId": "artist_id",
  "artistName": "Glass Animals",
  "coverUrl": "https://firebase-storage-url/album-art/dreamland.jpg",
  "releaseDate": 1597708800000,
  "songCount": 16,
  "totalDuration": 2700000,
  "genre": "indie",
  "createdAt": 1700000000000
}
```

#### `users` Collection
```json
{
  "id": "firebase-auth-uid",
  "displayName": "User Name",
  "email": "user@email.com",
  "photoUrl": "",
  "likedSongs": ["song_id_1", "song_id_2"],
  "recentlyPlayed": ["song_id_3", "song_id_1"],
  "playlistIds": ["playlist_id_1"],
  "createdAt": 1700000000000
}
```

#### `playlists` Collection
```json
{
  "id": "auto-generated",
  "name": "My Playlist",
  "description": "My favorite songs",
  "coverUrl": "",
  "userId": "user_id",
  "songIds": ["song_id_1", "song_id_2"],
  "songCount": 2,
  "isPublic": false,
  "mood": "",
  "createdAt": 1700000000000,
  "updatedAt": 1700000000000
}
```

### Mood Values

The following mood values are supported:
- `happy`
- `sad`
- `energetic`
- `calm`
- `romantic`
- `melancholic`
- `focused`
- `party`
- `chill`
- `workout`

### Upload Music to Firebase Storage

1. Go to Firebase Console → Storage
2. Create folders:
   - `music/` - for audio files (MP3, M4A)
   - `album-art/` - for album cover images
   - `artist-images/` - for artist photos

3. Upload your music files and note the download URLs

4. Add song documents to Firestore with the storage URLs

### Building the App

1. Clone the repository
2. Add `google-services.json` to `app/` directory
3. Open in Android Studio
4. Sync Gradle
5. Run on device/emulator

```bash
# Build debug APK
./gradlew assembleDebug

# Build release APK
./gradlew assembleRelease
```

## 🏗️ Architecture

```
app/
├── src/main/java/com/harmoniq/app/
│   ├── data/
│   │   ├── model/          # Data classes (Song, Artist, Album, etc.)
│   │   └── repository/     # Firebase repositories
│   ├── di/                 # Hilt dependency injection
│   ├── navigation/         # Navigation graph
│   ├── service/            # Media playback service
│   └── ui/
│       ├── components/     # Reusable UI components
│       ├── screens/        # App screens
│       ├── theme/          # Colors, typography, theme
│       └── viewmodel/      # ViewModels
├── MainActivity.kt
└── HarmoniqApplication.kt
```

## 🛠️ Tech Stack

- **UI**: Jetpack Compose with Material 3
- **Architecture**: MVVM with Hilt DI
- **Backend**: Firebase (Firestore, Storage, Auth)
- **Media**: Media3 ExoPlayer
- **Image Loading**: Coil
- **Dynamic Colors**: Palette API

## 📄 License

This project is for educational purposes. Music files are not included.

## 🤝 Contributing

Contributions are welcome! Please feel free to submit a Pull Request.

---

Made with ❤️ using Jetpack Compose

