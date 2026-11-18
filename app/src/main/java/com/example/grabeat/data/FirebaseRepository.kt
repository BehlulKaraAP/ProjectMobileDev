package com.example.grabeat.data

import android.R
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import com.google.firebase.firestore.GeoPoint

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

    suspend fun getUserRole(uid: String): Result<String> {
        return try{
            val document = db.collection("users").document(uid).get().await()
            val role = document.getString("role") ?: "customer"
            Result.success(role)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun addShop(
        ownerId: String,
        name: String,
        category: String,
        address: String,
        city: String,
        latitude: Double,
        longitude: Double
    ): Result<String> {
        return try {
            val shopData = hashMapOf(
                "ownerId" to ownerId,
                "name" to name,
                "category" to category,
                "address" to address,
                "city" to city,
                "location" to GeoPoint(latitude, longitude)
            )

            val document = db.collection("stores").add(shopData).await()
            Result.success(document.id)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    data class MerchantStore(
        val id: String,
        val name: String
    )

    suspend fun getStoresForOwner(ownerId: String): Result<List<MerchantStore>>{
        return try {
            val snapshot = db.collection("stores")
                .whereEqualTo("ownerId", ownerId)
                .get()
                .await()

            val stores = snapshot.documents.map { doc ->
                MerchantStore(
                    id = doc.id,
                    name = doc.getString("name") ?: "Onbekende winkel"
                )
            }
            Result.success(stores)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun addPackage(
        ownerId: String,
        storeId: String,
        storeName: String,
        title: String,
        description: String,
        price: Double,
        originalValue: Double,
        quantity: Int,
        pickupWindow: String,
        imageBase64: String
    ): Result<String> {
        return try {
            val packageData = hashMapOf(
                "ownerId" to ownerId,
                "storeId" to storeId,
                "storeName" to storeName,
                "title" to title.trim(),
                "description" to description.trim(),
                "price" to price,
                "originalValue" to originalValue,
                "quantity" to quantity,
                "pickupWindow" to pickupWindow.trim(),
                "imageBase64" to imageBase64,
                "createdAt" to System.currentTimeMillis()
            )

            val document = db.collection("bags").add(packageData).await()
            Result.success(document.id)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}