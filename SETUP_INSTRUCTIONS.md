# Setup Instructions for Song Upload Script

## Step 1: Create a Folder on Desktop

1. Go to your Desktop
2. Create a new folder named `song_uploader` (or any name you prefer)
3. This is where you'll save all the script files

## Step 2: Save the Files

Save these files in the folder you just created:

1. **upload_songs.py** - The main script
2. **requirements.txt** - Python dependencies
3. **README_UPLOAD.md** - Documentation (optional but helpful)

## Step 3: Install Python Dependencies

1. Open Command Prompt or Terminal
2. Navigate to your folder:
   ```bash
   cd Desktop\song_uploader
   ```
   (On Windows, use backslashes; on Mac/Linux, use forward slashes)

3. Install the required packages:
   ```bash
   pip install -r requirements.txt
   ```

## Step 4: Get Your Credentials Ready

Before running the script, make sure you have:

1. **Cloudinary Credentials:**
   - Cloud Name
   - API Key
   - API Secret
   - Get these from: https://cloudinary.com/console

2. **Firebase Service Account JSON:**
   - Go to Firebase Console > Project Settings > Service Accounts
   - Click "Generate new private key"
   - Save the JSON file in your `song_uploader` folder
   - Remember the filename (e.g., `harmoniq-firebase-adminsdk.json`)

3. **Genius API Token:**
   - Go to: https://genius.com/api-clients
   - Create a new API client
   - Copy the "Client Access Token"

## Step 5: Prepare Your MP3 Files

1. Put all your MP3 files in a folder (can be anywhere on your computer)
2. Remember the full path to this folder
   - Example: `C:\Users\YourName\Music\Songs`
   - Or: `C:\Users\YourName\Desktop\MySongs`

## Step 6: Run the Script

1. Open Command Prompt or Terminal
2. Navigate to your script folder:
   ```bash
   cd Desktop\song_uploader
   ```

3. Run the script:
   ```bash
   python upload_songs.py
   ```

4. Follow the prompts:
   - Enter the folder path with your MP3 files
   - Enter Cloudinary credentials
   - Enter the path to your Firebase JSON file (or just the filename if it's in the same folder)
   - Enter your Genius API token

## Example Folder Structure

```
Desktop/
└── song_uploader/
    ├── upload_songs.py
    ├── requirements.txt
    ├── README_UPLOAD.md
    └── harmoniq-firebase-adminsdk.json  (your Firebase credentials)
```

## Troubleshooting

- **If Python is not found:** Make sure Python 3.7+ is installed and added to PATH
- **If pip is not found:** Try `python -m pip install -r requirements.txt`
- **If encoding errors:** Make sure your terminal supports UTF-8 (Windows: use Windows Terminal or PowerShell)

## Notes

- The script processes songs one at a time
- It will skip songs that already exist in Firebase
- If lyrics aren't found, the song will still be uploaded without lyrics
- Album art is optional - songs will upload even without it

