# Song Upload Script

This script uploads MP3 songs from a local folder to Firebase Firestore and Cloudinary, automatically fetching lyrics from Genius.

## Features

- Extracts metadata from MP3 files (title, artist, album, duration, album art)
- Removes "_spotdown.org" suffix from song titles
- Uploads audio files to Cloudinary
- Uploads album art to Cloudinary
- Fetches lyrics from Genius.com
- Parses lyrics (skips intro/outro labels, removes verse/chorus labels, preserves timestamps)
- **Full Unicode support for Hindi and other languages** - properly handles Devanagari script
- Uploads complete song data to Firebase Firestore

## Prerequisites

1. Python 3.7 or higher
2. Cloudinary account (get credentials from https://cloudinary.com/)
3. Firebase project with Firestore enabled
4. Firebase service account JSON file (download from Firebase Console > Project Settings > Service Accounts)
5. Genius API access token (get from https://genius.com/api-clients)

## Installation

1. Install required packages:
```bash
pip install -r requirements.txt
```

## Setup

### 1. Get Genius API Token

1. Go to https://genius.com/api-clients
2. Create a new API client
3. Copy the "Client Access Token"

### 2. Get Firebase Service Account

1. Go to Firebase Console > Project Settings > Service Accounts
2. Click "Generate new private key"
3. Save the JSON file

### 3. Get Cloudinary Credentials

1. Go to Cloudinary Dashboard
2. Copy your Cloud Name, API Key, and API Secret

## Usage

Run the script:
```bash
python upload_songs.py
```

The script will prompt you for:
1. Folder path containing MP3 files
2. Cloudinary credentials (Cloud Name, API Key, API Secret)
3. Path to Firebase service account JSON file
4. Genius API access token

## How It Works

For each MP3 file:
1. Extracts metadata using mutagen library
2. Removes "_spotdown.org" from title
3. Uploads audio to Cloudinary (as video resource type)
4. Extracts and uploads album art if embedded in MP3
5. Searches Genius for lyrics
6. Parses lyrics:
   - Skips intro section
   - Removes verse/chorus/bridge labels (anything in square brackets)
   - Stops at outro
   - Preserves timestamps if available
7. Uploads all data to Firebase Firestore

## Firebase Document Structure

```json
{
  "title": "Song Title",
  "artistName": "Artist Name",
  "albumName": "Album Name",
  "audioUrl": "https://cloudinary.com/...",
  "albumArtUrl": "https://cloudinary.com/...",
  "duration": 312868,
  "hasLyrics": true,
  "lyrics": [
    {"text": "Lyric line 1", "timestamp": 0},
    {"text": "Lyric line 2", "timestamp": 5000}
  ],
  "mood": [],
  "playCount": 0,
  "releaseDate": 0,
  "createdAt": "timestamp",
  "albumId": "",
  "artistId": "",
  "genre": ""
}
```

## Notes

- The script processes songs one at a time
- If lyrics are not found, the song is still uploaded without lyrics
- Album art is optional (song will be uploaded even without it)
- All uploads go to the "harmoniq" folder in Cloudinary

