#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
Script to upload MP3 songs to Firebase Firestore and Cloudinary.
For each song, it extracts metadata, uploads to Cloudinary, fetches lyrics from Genius,
and uploads everything to Firebase.
Supports Hindi and other Unicode languages.
"""

import os
import sys
import json
import re
from pathlib import Path
from datetime import datetime
from typing import List, Dict, Optional, Tuple

# Ensure UTF-8 encoding for proper Hindi/Unicode support
if sys.version_info[0] < 3:
    reload(sys)
    sys.setdefaultencoding('utf-8')

try:
    from mutagen import File
    from mutagen.id3 import ID3NoHeaderError
except ImportError:
    print("Error: mutagen not installed. Install it with: pip install mutagen")
    sys.exit(1)

try:
    import cloudinary
    import cloudinary.uploader
    import cloudinary.api
except ImportError:
    print("Error: cloudinary not installed. Install it with: pip install cloudinary")
    sys.exit(1)

try:
    import firebase_admin
    from firebase_admin import credentials, firestore
except ImportError:
    print("Error: firebase-admin not installed. Install it with: pip install firebase-admin")
    sys.exit(1)

try:
    import lyricsgenius
except ImportError:
    print("Error: lyricsgenius not installed. Install it with: pip install lyricsgenius")
    sys.exit(1)

try:
    import requests
    from bs4 import BeautifulSoup
except ImportError:
    print("Error: requests or beautifulsoup4 not installed. Install with: pip install requests beautifulsoup4")
    sys.exit(1)


class SongUploader:
    def __init__(self, cloudinary_config: Dict, firebase_cred_path: str, genius_token: str):
        """Initialize the uploader with configuration."""
        # Configure Cloudinary
        cloudinary.config(
            cloud_name=cloudinary_config['cloud_name'],
            api_key=cloudinary_config['api_key'],
            api_secret=cloudinary_config['api_secret']
        )
        
        # Initialize Firebase
        if not firebase_admin._apps:
            cred = credentials.Certificate(firebase_cred_path)
            firebase_admin.initialize_app(cred)
        self.db = firestore.client()
        
        # Initialize Genius
        self.genius = lyricsgenius.Genius(genius_token)
        self.genius.verbose = False
        self.genius.remove_section_headers = True
        self.genius.skip_non_songs = True
    
    def extract_metadata(self, mp3_path: Path) -> Dict:
        """Extract metadata from MP3 file."""
        try:
            audio_file = File(str(mp3_path))
            if audio_file is None:
                return {}
            
            metadata = {}
            
            # Get title and remove "_spotdown.org" suffix
            title = audio_file.get('TIT2', [None])[0] or audio_file.get('TITLE', [None])[0]
            if not title:
                # Fallback to filename without extension
                title = mp3_path.stem
            title = title.replace('_spotdown.org', '').strip()
            metadata['title'] = title
            
            # Get artist
            artist = audio_file.get('TPE1', [None])[0] or audio_file.get('ARTIST', [None])[0]
            if artist:
                metadata['artistName'] = artist.strip()
            
            # Get album
            album = audio_file.get('TALB', [None])[0] or audio_file.get('ALBUM', [None])[0]
            if album:
                metadata['albumName'] = album.strip()
            
            # Get duration in milliseconds
            if hasattr(audio_file, 'info') and audio_file.info:
                duration_ms = int(audio_file.info.length * 1000)
                metadata['duration'] = duration_ms
            
            # Try to extract album art
            try:
                # Try different tag formats for album art
                artwork = None
                for tag in ['APIC:', 'APIC', 'covr', 'COVER ART (FRONT)']:
                    if tag in audio_file:
                        artwork = audio_file[tag]
                        break
                
                if artwork:
                    if hasattr(artwork, 'data'):
                        metadata['album_art_data'] = artwork.data
                    elif isinstance(artwork, bytes):
                        metadata['album_art_data'] = artwork
                    elif isinstance(artwork, list) and len(artwork) > 0:
                        if hasattr(artwork[0], 'data'):
                            metadata['album_art_data'] = artwork[0].data
                        elif isinstance(artwork[0], bytes):
                            metadata['album_art_data'] = artwork[0]
            except Exception as e:
                # Album art extraction failed, continue without it
                pass
            
            return metadata
        except Exception as e:
            print(f"Error extracting metadata from {mp3_path}: {e}")
            return {}
    
    def upload_to_cloudinary(self, file_path: Path, resource_type: str = 'video', 
                            folder: str = 'harmoniq') -> Optional[str]:
        """Upload file to Cloudinary."""
        try:
            if resource_type == 'image' and 'album_art_data' in str(file_path):
                # Upload image from bytes
                result = cloudinary.uploader.upload(
                    file_path,
                    resource_type='image',
                    folder=folder,
                    overwrite=False
                )
            else:
                # Upload file
                result = cloudinary.uploader.upload(
                    str(file_path),
                    resource_type=resource_type,
                    folder=folder,
                    overwrite=False
                )
            return result.get('secure_url')
        except Exception as e:
            print(f"Error uploading to Cloudinary: {e}")
            return None
    
    def upload_album_art(self, art_data: bytes, folder: str = 'harmoniq') -> Optional[str]:
        """Upload album art to Cloudinary."""
        try:
            import io
            result = cloudinary.uploader.upload(
                io.BytesIO(art_data),
                resource_type='image',
                folder=folder,
                overwrite=False
            )
            return result.get('secure_url')
        except Exception as e:
            print(f"Error uploading album art: {e}")
            return None
    
    def fetch_lyrics_from_genius(self, title: str, artist: str) -> Optional[List[Dict]]:
        """Fetch lyrics from Genius and parse them."""
        try:
            # Search for the song
            song = self.genius.search_song(title, artist)
            if not song:
                print(f"  Could not find lyrics for {title} by {artist}")
                return None
            
            # Get lyrics and ensure proper Unicode/UTF-8 encoding for Hindi and other languages
            lyrics_text = song.lyrics
            if isinstance(lyrics_text, bytes):
                lyrics_text = lyrics_text.decode('utf-8')
            
            # Parse lyrics - remove intro/outro labels and verse/chorus labels
            # Keep only the actual lyrics with timestamps if available
            lines = lyrics_text.split('\n')
            parsed_lyrics = []
            in_intro = True
            outro_started = False
            
            for line in lines:
                original_line = line
                line = line.strip()
                
                # Skip empty lines
                if not line:
                    continue
                
                # Check if line is a label in brackets (e.g., [Verse 1], [Chorus], [Intro], etc.)
                bracket_match = re.match(r'^\[([^\]]+)\]$', line)
                if bracket_match:
                    label = bracket_match.group(1).lower()
                    
                    # Check for intro
                    if 'intro' in label:
                        in_intro = True
                        continue
                    # Check for outro
                    elif 'outro' in label or 'out' in label:
                        outro_started = True
                        break
                    # Skip all other labels (verse, chorus, bridge, pre-chorus, hook, etc.)
                    else:
                        in_intro = False  # If we see verse/chorus, we're past intro
                        continue
                
                # If we're still in intro, skip until we see actual lyrics
                if in_intro:
                    # Check if this line has actual content (not just brackets)
                    if not re.match(r'^\[.*\]', line):
                        in_intro = False
                    else:
                        continue
                
                # Stop if outro started
                if outro_started:
                    break
                
                # Now process the actual lyric line
                # Extract timestamp if present (format: [mm:ss] or [hh:mm:ss] or [m:ss])
                timestamp_match = re.search(r'\[(\d{1,2}):(\d{2})(?::(\d{2}))?\]', line)
                timestamp_ms = None
                
                if timestamp_match:
                    if timestamp_match.group(3):  # Has hours [hh:mm:ss]
                        hours = int(timestamp_match.group(1))
                        minutes = int(timestamp_match.group(2))
                        seconds = int(timestamp_match.group(3))
                        timestamp_ms = (hours * 3600 + minutes * 60 + seconds) * 1000
                    else:  # Just minutes:seconds [mm:ss] or [m:ss]
                        minutes = int(timestamp_match.group(1))
                        seconds = int(timestamp_match.group(2))
                        timestamp_ms = (minutes * 60 + seconds) * 1000
                    
                    # Remove timestamp from line
                    line = re.sub(r'\[.*?\]', '', line).strip()
                
                # Remove any remaining bracket content (like [Produced by...], [Explicit], etc.)
                line = re.sub(r'\[.*?\]', '', line).strip()
                
                # Only add non-empty lines
                # Ensure proper Unicode encoding for Hindi and other languages
                if line:
                    # Clean the line and ensure it's a proper Unicode string
                    line = line.strip()
                    # Remove any zero-width characters or formatting issues
                    line = re.sub(r'[\u200b-\u200f\ufeff]', '', line)  # Remove zero-width chars
                    
                    if line:  # Double check after cleaning
                        parsed_lyrics.append({
                            'text': line,
                            'timestamp': timestamp_ms if timestamp_ms is not None else 0
                        })
            
            return parsed_lyrics if parsed_lyrics else None
            
        except Exception as e:
            print(f"  Error fetching lyrics from Genius: {e}")
            import traceback
            traceback.print_exc()
            return None
    
    def check_song_exists(self, title: str, artist: str) -> bool:
        """Check if song already exists in Firebase."""
        try:
            songs_ref = self.db.collection('songs')
            query = songs_ref.where('title', '==', title).where('artistName', '==', artist).limit(1)
            docs = query.get()
            return len(docs) > 0
        except Exception as e:
            print(f"  ⚠ Error checking for duplicates: {e}")
            return False
    
    def upload_song_to_firebase(self, song_data: Dict):
        """Upload song data to Firebase Firestore."""
        try:
            # Check if song already exists
            if self.check_song_exists(song_data['title'], song_data['artistName']):
                print(f"  ⚠ Song already exists in Firebase, skipping...")
                return None
            
            # Prepare document data
            # Ensure all text fields are properly encoded as Unicode strings
            lyrics_list = []
            for lyric_item in song_data.get('lyrics', []):
                # Ensure text is a proper Unicode string
                text = lyric_item.get('text', '')
                if isinstance(text, bytes):
                    text = text.decode('utf-8')
                lyrics_list.append({
                    'text': text,
                    'timestamp': lyric_item.get('timestamp', 0)
                })
            
            doc_data = {
                'title': str(song_data['title']),  # Ensure string type
                'artistName': str(song_data['artistName']),  # Ensure string type
                'albumName': str(song_data.get('albumName', '')),  # Ensure string type
                'audioUrl': str(song_data['audioUrl']),
                'albumArtUrl': str(song_data.get('albumArtUrl', '')),
                'duration': song_data.get('duration', 0),
                'hasLyrics': song_data.get('hasLyrics', False),
                'lyrics': lyrics_list,  # Use processed lyrics with proper encoding
                'mood': song_data.get('mood', []),
                'playCount': 0,
                'releaseDate': 0,
                'createdAt': firestore.SERVER_TIMESTAMP,
                'albumId': '',
                'artistId': '',
                'genre': ''
            }
            
            # Add document to Firestore
            doc_ref = self.db.collection('songs').add(doc_data)
            print(f"  ✓ Uploaded to Firebase with ID: {doc_ref[1].id}")
            return doc_ref[1].id
            
        except Exception as e:
            print(f"  ✗ Error uploading to Firebase: {e}")
            import traceback
            traceback.print_exc()
            return None
    
    def process_song(self, mp3_path: Path) -> bool:
        """Process a single song: extract metadata, upload files, fetch lyrics, upload to Firebase."""
        print(f"\nProcessing: {mp3_path.name}")
        
        # Extract metadata
        metadata = self.extract_metadata(mp3_path)
        if not metadata:
            print("  ✗ Could not extract metadata")
            return False
        
        title = metadata.get('title', mp3_path.stem.replace('_spotdown.org', ''))
        artist = metadata.get('artistName', 'Unknown Artist')
        
        print(f"  Title: {title}")
        print(f"  Artist: {artist}")
        print(f"  Album: {metadata.get('albumName', 'Unknown Album')}")
        
        # Upload audio to Cloudinary
        print("  Uploading audio to Cloudinary...")
        audio_url = self.upload_to_cloudinary(mp3_path, resource_type='video')
        if not audio_url:
            print("  ✗ Failed to upload audio")
            return False
        print(f"  ✓ Audio uploaded: {audio_url[:50]}...")
        
        # Upload album art if available
        album_art_url = None
        if 'album_art_data' in metadata and metadata['album_art_data']:
            print("  Uploading album art to Cloudinary...")
            album_art_url = self.upload_album_art(metadata['album_art_data'])
            if album_art_url:
                print(f"  ✓ Album art uploaded: {album_art_url[:50]}...")
            else:
                print("  ⚠ Failed to upload album art")
        else:
            print("  ⚠ No album art found in metadata")
        
        # Fetch lyrics from Genius
        print(f"  Fetching lyrics from Genius...")
        lyrics = self.fetch_lyrics_from_genius(title, artist)
        has_lyrics = lyrics is not None and len(lyrics) > 0
        
        if has_lyrics:
            print(f"  ✓ Found {len(lyrics)} lyric lines")
        else:
            print("  ⚠ No lyrics found")
            lyrics = []
        
        # Prepare song data
        song_data = {
            'title': title,
            'artistName': artist,
            'albumName': metadata.get('albumName', ''),
            'audioUrl': audio_url,
            'albumArtUrl': album_art_url or '',
            'duration': metadata.get('duration', 0),
            'hasLyrics': has_lyrics,
            'lyrics': lyrics,
            'mood': []  # Can be populated later or manually
        }
        
        # Upload to Firebase
        print("  Uploading to Firebase...")
        doc_id = self.upload_song_to_firebase(song_data)
        
        if doc_id:
            print(f"  ✓ Successfully processed {title}")
            return True
        else:
            print(f"  ✗ Failed to upload {title} to Firebase")
            return False


def main():
    """Main function to run the script."""
    # Ensure UTF-8 encoding for console output (important for Hindi/Unicode)
    if sys.stdout.encoding != 'utf-8':
        import codecs
        sys.stdout = codecs.getwriter('utf-8')(sys.stdout.buffer, 'strict')
        sys.stderr = codecs.getwriter('utf-8')(sys.stderr.buffer, 'strict')
    
    print("=" * 60)
    print("Song Uploader to Firebase and Cloudinary")
    print("Supports Hindi and other Unicode languages")
    print("=" * 60)
    
    # Get folder path from user
    folder_path = input("\nEnter the folder path containing MP3 files: ").strip()
    folder_path = Path(folder_path).expanduser().resolve()
    
    if not folder_path.exists() or not folder_path.is_dir():
        print(f"Error: Folder not found: {folder_path}")
        sys.exit(1)
    
    # Get Cloudinary credentials
    print("\n--- Cloudinary Configuration ---")
    cloud_name = input("Cloudinary Cloud Name: ").strip()
    api_key = input("Cloudinary API Key: ").strip()
    api_secret = input("Cloudinary API Secret: ").strip()
    
    cloudinary_config = {
        'cloud_name': cloud_name,
        'api_key': api_key,
        'api_secret': api_secret
    }
    
    # Get Firebase credentials path
    print("\n--- Firebase Configuration ---")
    firebase_cred_path = input("Path to Firebase service account JSON file: ").strip()
    firebase_cred_path = Path(firebase_cred_path).expanduser().resolve()
    
    if not firebase_cred_path.exists():
        print(f"Error: Firebase credentials file not found: {firebase_cred_path}")
        sys.exit(1)
    
    # Get Genius API token
    print("\n--- Genius API Configuration ---")
    genius_token = input("Genius API Access Token: ").strip()
    
    if not genius_token:
        print("Error: Genius API token is required")
        sys.exit(1)
    
    # Initialize uploader
    try:
        uploader = SongUploader(cloudinary_config, str(firebase_cred_path), genius_token)
    except Exception as e:
        print(f"Error initializing uploader: {e}")
        sys.exit(1)
    
    # Find all MP3 files
    mp3_files = list(folder_path.glob("*.mp3"))
    if not mp3_files:
        print(f"No MP3 files found in {folder_path}")
        sys.exit(1)
    
    print(f"\nFound {len(mp3_files)} MP3 file(s)")
    confirm = input("Proceed with upload? (yes/no): ").strip().lower()
    if confirm != 'yes':
        print("Upload cancelled")
        sys.exit(0)
    
    # Process each song
    successful = 0
    failed = 0
    
    for i, mp3_file in enumerate(mp3_files, 1):
        print(f"\n[{i}/{len(mp3_files)}]")
        if uploader.process_song(mp3_file):
            successful += 1
        else:
            failed += 1
    
    # Summary
    print("\n" + "=" * 60)
    print("Upload Summary")
    print("=" * 60)
    print(f"Total songs: {len(mp3_files)}")
    print(f"Successful: {successful}")
    print(f"Failed: {failed}")
    print("=" * 60)


if __name__ == "__main__":
    main()

