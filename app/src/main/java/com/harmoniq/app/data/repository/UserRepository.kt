package com.harmoniq.app.data.repository

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.harmoniq.app.data.model.User
import com.harmoniq.app.data.model.toMap
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UserRepository @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth
) {
    private val usersCollection = firestore.collection("users")

    val currentUserId: String?
        get() = auth.currentUser?.uid

    val isLoggedIn: Boolean
        get() = auth.currentUser != null

    fun getCurrentUser(): Flow<User?> = callbackFlow {
        val userId = currentUserId
        if (userId == null) {
            trySend(null)
            close()
            return@callbackFlow
        }

        val listener = usersCollection.document(userId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                val user = snapshot?.toObject(User::class.java)
                trySend(user)
            }
        awaitClose { listener.remove() }
    }

    suspend fun createOrUpdateUser(user: User): Boolean {
        return try {
            val userId = user.id.ifEmpty { currentUserId ?: return false }
            val userData = user.toMap().toMutableMap()
            
            android.util.Log.d("UserRepository", "Creating/updating user document: $userId with data: $userData")
            
            // Check if user already exists
            val snapshot = usersCollection.document(userId).get().await()
            if (snapshot.exists()) {
                android.util.Log.d("UserRepository", "User document already exists, updating...")
                userData.remove("createdAt") // Keep existing createdAt
                usersCollection.document(userId).update(userData).await()
            } else {
                android.util.Log.d("UserRepository", "User document does not exist, creating new...")
                // For new documents, use set() without merge to ensure all fields are written
                usersCollection.document(userId).set(userData).await()
            }
            
            android.util.Log.d("UserRepository", "User document created/updated successfully: $userId")
            true
        } catch (e: com.google.firebase.firestore.FirebaseFirestoreException) {
            android.util.Log.e("UserRepository", "Firestore error creating/updating user: ${e.code} - ${e.message}", e)
            e.printStackTrace()
            false
        } catch (e: Exception) {
            android.util.Log.e("UserRepository", "Error creating/updating user: ${e.message}", e)
            e.printStackTrace()
            false
        }
    }
    
    suspend fun updateUserProfile(
        displayName: String? = null,
        email: String? = null,
        photoUrl: String? = null
    ): Boolean {
        val userId = currentUserId ?: return false
        return try {
            val updates = mutableMapOf<String, Any>()
            displayName?.let { updates["displayName"] = it }
            email?.let { updates["email"] = it }
            photoUrl?.let { updates["photoUrl"] = it }
            
            if (updates.isNotEmpty()) {
                usersCollection.document(userId).update(updates).await()
            }
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
    
    suspend fun getUserPlaylists(): Flow<List<com.harmoniq.app.data.model.Playlist>> = callbackFlow {
        val userId = currentUserId
        if (userId == null) {
            trySend(emptyList())
            close()
            return@callbackFlow
        }
        
        val listener = firestore.collection("playlists")
            .whereEqualTo("userId", userId)
            .orderBy("updatedAt", com.google.firebase.firestore.Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                val playlists = snapshot?.documents?.mapNotNull { doc ->
                    doc.toObject(com.harmoniq.app.data.model.Playlist::class.java)?.copy(id = doc.id)
                } ?: emptyList()
                trySend(playlists)
            }
        awaitClose { listener.remove() }
    }

    suspend fun addToLikedSongs(songId: String) {
        val userId = currentUserId ?: return
        try {
            usersCollection.document(userId)
                .update("likedSongs", FieldValue.arrayUnion(songId))
                .await()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    suspend fun removeFromLikedSongs(songId: String) {
        val userId = currentUserId ?: return
        try {
            usersCollection.document(userId)
                .update("likedSongs", FieldValue.arrayRemove(songId))
                .await()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    suspend fun addToRecentlyPlayed(songId: String) {
        val userId = currentUserId ?: return
        try {
            firestore.runTransaction { transaction ->
                val userRef = usersCollection.document(userId)
                val snapshot = transaction.get(userRef)
                val recentlyPlayed = (snapshot.get("recentlyPlayed") as? List<String>)?.toMutableList() ?: mutableListOf()
                
                // Remove if already exists and add to beginning (newest song at front)
                recentlyPlayed.remove(songId)
                recentlyPlayed.add(0, songId)
                
                // Keep only last 10 songs (newest at index 0, oldest at index 9)
                val trimmed = recentlyPlayed.take(10)
                transaction.update(userRef, "recentlyPlayed", trimmed)
            }.await()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    suspend fun removeFromRecentlyPlayed(songId: String) {
        val userId = currentUserId ?: return
        try {
            firestore.runTransaction { transaction ->
                val userRef = usersCollection.document(userId)
                val snapshot = transaction.get(userRef)
                val recentlyPlayed = (snapshot.get("recentlyPlayed") as? List<String>)?.toMutableList() ?: mutableListOf()
                
                // Remove the song from recently played
                recentlyPlayed.remove(songId)
                
                transaction.update(userRef, "recentlyPlayed", recentlyPlayed)
            }.await()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    suspend fun signInAnonymously(): Boolean {
        return try {
            val result = auth.signInAnonymously().await()
            val user = result.user
            if (user != null) {
                val newUser = User(
                    id = user.uid,
                    displayName = "User",
                    email = ""
                )
                createOrUpdateUser(newUser)
            }
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
    
    suspend fun signInWithEmail(email: String, password: String): Result<String> {
        return try {
            val result = auth.signInWithEmailAndPassword(email, password).await()
            val user = result.user
            if (user != null) {
                // Load or create user document
                val userDoc = usersCollection.document(user.uid).get().await()
                if (!userDoc.exists()) {
                    val newUser = User(
                        id = user.uid,
                        displayName = user.displayName ?: "User",
                        email = user.email ?: email
                    )
                    createOrUpdateUser(newUser)
                }
            }
            Result.success("Signed in successfully")
        } catch (e: com.google.firebase.auth.FirebaseAuthException) {
            val errorMessage = when (e.errorCode) {
                "ERROR_USER_NOT_FOUND" -> "No account found with this email address"
                "ERROR_WRONG_PASSWORD" -> "Incorrect password. Please try again"
                "ERROR_INVALID_EMAIL" -> "Invalid email format. Please check your email address"
                "ERROR_NETWORK_REQUEST_FAILED" -> "Network error. Please check your internet connection"
                "ERROR_OPERATION_NOT_ALLOWED" -> "Email/Password sign-in is not enabled. Please enable it in Firebase Console under Authentication > Sign-in method"
                else -> {
                    // Check if the error message contains the operation not allowed text
                    if (e.message?.contains("operation is not allowed", ignoreCase = true) == true ||
                        e.message?.contains("sign-in provider is disabled", ignoreCase = true) == true) {
                        "Email/Password sign-in is not enabled. Please enable it in Firebase Console under Authentication > Sign-in method"
                    } else {
                        e.message ?: "Failed to sign in. Please try again."
                    }
                }
            }
            Result.failure(Exception(errorMessage))
        } catch (e: Exception) {
            e.printStackTrace()
            val errorMessage = when {
                e.message?.contains("user not found", ignoreCase = true) == true -> 
                    "No account found with this email address"
                e.message?.contains("wrong password", ignoreCase = true) == true || 
                e.message?.contains("invalid", ignoreCase = true) == true -> 
                    "Invalid email or password"
                e.message?.contains("network", ignoreCase = true) == true -> 
                    "Network error. Please check your internet connection"
                else -> "Failed to sign in: ${e.message ?: "Unknown error"}"
            }
            Result.failure(Exception(errorMessage))
        }
    }
    
    suspend fun createAccountWithEmail(email: String, password: String, displayName: String): Result<String> {
        return try {
            android.util.Log.d("UserRepository", "Starting account creation for: $email")
            
            val result = auth.createUserWithEmailAndPassword(email, password).await()
            val user = result.user
            android.util.Log.d("UserRepository", "Firebase Auth user created: ${user?.uid}")
            
            if (user != null) {
                // Update Firebase Auth profile
                val profileUpdates = com.google.firebase.auth.UserProfileChangeRequest.Builder()
                    .setDisplayName(displayName)
                    .build()
                user.updateProfile(profileUpdates).await()
                android.util.Log.d("UserRepository", "Firebase Auth profile updated")
                
                // Ensure we're authenticated before creating Firestore document
                val currentAuthUser = auth.currentUser
                android.util.Log.d("UserRepository", "Current auth user: ${currentAuthUser?.uid}, Email: ${currentAuthUser?.email}")
                
                if (currentAuthUser == null) {
                    android.util.Log.e("UserRepository", "No authenticated user found after signup")
                    return Result.failure(Exception("Authentication failed. Please try again."))
                }
                
                // Create user document in Firestore directly
                val userData = mapOf(
                    "displayName" to displayName,
                    "email" to email,
                    "photoUrl" to "",
                    "likedSongs" to emptyList<String>(),
                    "recentlyPlayed" to emptyList<String>(),
                    "playlistIds" to emptyList<String>(),
                    "createdAt" to com.google.firebase.Timestamp.now()
                )
                
                android.util.Log.d("UserRepository", "Attempting to create Firestore document for user: ${user.uid}")
                android.util.Log.d("UserRepository", "User data: $userData")
                
                // Use set() to create the document
                usersCollection.document(user.uid).set(userData).await()
                android.util.Log.d("UserRepository", "Firestore document created successfully")
                
                // Wait a moment to ensure document is written
                kotlinx.coroutines.delay(300)
                
                // Verify the document was created
                val verifyDoc = usersCollection.document(user.uid).get().await()
                if (!verifyDoc.exists()) {
                    android.util.Log.e("UserRepository", "User document verification failed - document does not exist")
                    return Result.failure(Exception("User profile was not created. Please try again."))
                }
                
                android.util.Log.d("UserRepository", "User document verified successfully")
                val verifiedData = verifyDoc.data
                android.util.Log.d("UserRepository", "Verified document data: $verifiedData")
            }
            Result.success("Account created successfully")
        } catch (e: com.google.firebase.auth.FirebaseAuthException) {
            val errorMessage = when (e.errorCode) {
                "ERROR_EMAIL_ALREADY_IN_USE" -> "An account with this email already exists"
                "ERROR_WEAK_PASSWORD" -> "Password is too weak. Please use at least 6 characters"
                "ERROR_INVALID_EMAIL" -> "Invalid email format. Please check your email address"
                "ERROR_NETWORK_REQUEST_FAILED" -> "Network error. Please check your internet connection"
                "ERROR_OPERATION_NOT_ALLOWED" -> "Email/Password sign-in is not enabled. Please enable it in Firebase Console under Authentication > Sign-in method"
                else -> {
                    // Check if the error message contains the operation not allowed text
                    if (e.message?.contains("operation is not allowed", ignoreCase = true) == true ||
                        e.message?.contains("sign-in provider is disabled", ignoreCase = true) == true) {
                        "Email/Password sign-in is not enabled. Please enable it in Firebase Console under Authentication > Sign-in method"
                    } else {
                        e.message ?: "Failed to create account. Please try again."
                    }
                }
            }
            Result.failure(Exception(errorMessage))
        } catch (e: Exception) {
            e.printStackTrace()
            // Check error message for common Firebase errors
            val errorMessage = when {
                e.message?.contains("email address is already in use", ignoreCase = true) == true -> 
                    "An account with this email already exists"
                e.message?.contains("password", ignoreCase = true) == true && 
                e.message?.contains("weak", ignoreCase = true) == true -> 
                    "Password is too weak. Please use at least 6 characters"
                e.message?.contains("invalid", ignoreCase = true) == true && 
                e.message?.contains("email", ignoreCase = true) == true -> 
                    "Invalid email format. Please check your email address"
                e.message?.contains("network", ignoreCase = true) == true -> 
                    "Network error. Please check your internet connection"
                else -> "Failed to create account: ${e.message ?: "Unknown error"}"
            }
            Result.failure(Exception(errorMessage))
        }
    }

    fun signOut() {
        auth.signOut()
    }
}

