package com.harmoniq.app.util

import android.util.Base64
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

/**
 * Utility for uploading images to Cloudinary
 * 
 * Note: For production, you should use a backend endpoint to handle uploads
 * to keep your Cloudinary credentials secure.
 */
object CloudinaryUploader {
    
    // TODO: Replace with your Cloudinary credentials from upload_music.py
    // Get these from: C:\Users\ayush\Desktop\upload_music.py
    // For security, these should ideally come from a backend endpoint
    private const val CLOUDINARY_CLOUD_NAME = "dhjfaiawv"  // From your upload_music.py
    private const val CLOUDINARY_API_KEY = "899363719515662"  // From your upload_music.py
    private const val CLOUDINARY_API_SECRET = "U9oUVhpfanzJ-E-msz8f_P_VpwI"  // From your upload_music.py
    
    suspend fun uploadImage(
        file: File,
        folder: String = "profile_pictures"
    ): String? = withContext(Dispatchers.IO) {
        try {
            Log.d("CloudinaryUploader", "Starting upload for file: ${file.name}, size: ${file.length()} bytes")
            
            // Create upload URL
            val uploadUrl = "https://api.cloudinary.com/v1_1/$CLOUDINARY_CLOUD_NAME/image/upload"
            
            // Generate timestamp and signature
            // Cloudinary signature: parameters sorted alphabetically, then API secret appended, then SHA1
            val timestamp = System.currentTimeMillis() / 1000
            val folderPath = "harmoniq/$folder"
            // Parameters must be sorted alphabetically for signature
            val paramsToSign = "folder=$folderPath&timestamp=$timestamp$CLOUDINARY_API_SECRET"
            val signature = generateSignature(paramsToSign)
            
            Log.d("CloudinaryUploader", "Signature generated for params: $paramsToSign")
            
            // Create multipart form data
            val boundary = "----WebKitFormBoundary${System.currentTimeMillis()}"
            val url = URL(uploadUrl)
            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "POST"
            connection.setRequestProperty("Content-Type", "multipart/form-data; boundary=$boundary")
            connection.doOutput = true
            
            connection.outputStream.use { output ->
                // Write file
                output.write("--$boundary\r\n".toByteArray())
                output.write("Content-Disposition: form-data; name=\"file\"; filename=\"${file.name}\"\r\n".toByteArray())
                output.write("Content-Type: image/jpeg\r\n\r\n".toByteArray())
                file.inputStream().use { input ->
                    input.copyTo(output)
                }
                output.write("\r\n".toByteArray())
                
                // Write folder
                output.write("--$boundary\r\n".toByteArray())
                output.write("Content-Disposition: form-data; name=\"folder\"\r\n\r\n".toByteArray())
                output.write("$folderPath\r\n".toByteArray())
                
                // Write api_key
                output.write("--$boundary\r\n".toByteArray())
                output.write("Content-Disposition: form-data; name=\"api_key\"\r\n\r\n".toByteArray())
                output.write("$CLOUDINARY_API_KEY\r\n".toByteArray())
                
                // Write timestamp
                output.write("--$boundary\r\n".toByteArray())
                output.write("Content-Disposition: form-data; name=\"timestamp\"\r\n\r\n".toByteArray())
                output.write("$timestamp\r\n".toByteArray())
                
                // Write signature
                output.write("--$boundary\r\n".toByteArray())
                output.write("Content-Disposition: form-data; name=\"signature\"\r\n\r\n".toByteArray())
                output.write("$signature\r\n".toByteArray())
                
                // Close boundary
                output.write("--$boundary--\r\n".toByteArray())
            }
            
            val responseCode = connection.responseCode
            Log.d("CloudinaryUploader", "Response code: $responseCode")
            
            if (responseCode == HttpURLConnection.HTTP_OK) {
                val response = connection.inputStream.bufferedReader().use { it.readText() }
                Log.d("CloudinaryUploader", "Upload response: $response")
                
                // Extract URL from response
                val urlPattern = "\"secure_url\":\"([^\"]+)\"".toRegex()
                val match = urlPattern.find(response)
                val imageUrl = match?.groupValues?.get(1)?.replace("\\/", "/")
                
                if (imageUrl != null) {
                    Log.d("CloudinaryUploader", "Upload successful! URL: $imageUrl")
                    return@withContext imageUrl
                } else {
                    Log.e("CloudinaryUploader", "Could not extract URL from response")
                    return@withContext null
                }
            } else {
                val errorResponse = connection.errorStream?.bufferedReader()?.use { it.readText() }
                Log.e("CloudinaryUploader", "Upload failed: $responseCode, Error: $errorResponse")
                return@withContext null
            }
        } catch (e: Exception) {
            Log.e("CloudinaryUploader", "Error uploading: ${e.message}", e)
            e.printStackTrace()
            return@withContext null
        }
    }
    
    private fun generateSignature(paramsToSign: String): String {
        try {
            // Cloudinary uses SHA-1 hash (not HMAC) of the string
            val md = java.security.MessageDigest.getInstance("SHA-1")
            val hash = md.digest(paramsToSign.toByteArray())
            return bytesToHex(hash)
        } catch (e: Exception) {
            Log.e("CloudinaryUploader", "Error generating signature: ${e.message}", e)
            throw e
        }
    }
    
    private fun bytesToHex(bytes: ByteArray): String {
        val hexChars = CharArray(bytes.size * 2)
        for (i in bytes.indices) {
            val v = bytes[i].toInt() and 0xFF
            hexChars[i * 2] = "0123456789abcdef"[v ushr 4]
            hexChars[i * 2 + 1] = "0123456789abcdef"[v and 0x0F]
        }
        return String(hexChars)
    }
    
}

