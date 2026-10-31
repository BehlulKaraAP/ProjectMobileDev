package com.example.grabeat.data

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

class FirebaseRepository {

    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()

    suspend fun register(name: String, email: String, password: String, role: String): Result<String> {
        return try {
            val result = auth.createUserWithEmailAndPassword(
                email.trim(),
                password
            ).await()

            val uid = result.user?.uid
                ?: throw Exception("User ID not found")

            val userData = hashMapOf(
                "name" to name.trim(),
                "email" to email.trim(),
                "role" to role
            )

            db.collection("users")
                .document(uid)
                .set(userData)
                .await()

            Result.success(uid)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun login( email: String, password: String): Result<String> {
        return try {
            val result = auth.signInWithEmailAndPassword(
                email.trim(),
                password
            ).await()

            val uid = result.user?.uid
                ?: throw Exception("User ID not found")

            Result.success(uid)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun logout() {
        auth.signOut()
    }

    fun currentUser() = auth.currentUser
}