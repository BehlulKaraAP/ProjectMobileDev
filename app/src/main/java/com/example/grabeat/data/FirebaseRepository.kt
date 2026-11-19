package com.example.grabeat.data

import android.R
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import com.google.firebase.firestore.GeoPoint
import kotlin.text.get
import kotlin.text.toInt

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

    data class MerchantPackage(
        val id: String,
        val storeId: String,
        val storeName: String,
        val title: String,
        val description: String,
        val price: Double,
        val originalValue: Double,
        val quantity: Int,
        val pickupWindow: String,
        val imageBase64: String,
        val createdAt: Long
    )

    suspend fun getPackagesForOwner(ownerId: String) : Result<List<MerchantPackage>>{
        return try {
            val snapshot = db.collection("bags")
                .whereEqualTo("ownerId", ownerId)
                .get()
                .await()

            val packages = snapshot.documents.map { doc ->
                MerchantPackage(
                    id = doc.id,
                    storeId = doc.getString("storeId") ?: "",
                    storeName = doc.getString("storeName") ?: "Onbekende winkel",
                    title = doc.getString("title") ?: "",
                    description = doc.getString("description") ?: "",
                    price = doc.getDouble("price") ?: 0.0,
                    originalValue = doc.getDouble("originalValue") ?: 0.0,
                    quantity = doc.getLong("quantity")?.toInt() ?: 0,
                    pickupWindow = doc.getString("pickupWindow") ?: "",
                    imageBase64 = doc.getString("imageBase64") ?: "",
                    createdAt = doc.getLong("createdAt") ?: 0L
                )
            }.sortedByDescending { it.createdAt }

            Result.success(packages)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    data class MerchantReservation(
        val id: String,
        val bagId: String,
        val bagTitle: String,
        val storeId: String,
        val storeName: String,
        val customerId: String,
        val customerName: String,
        val pickupCode: String,
        val quantity: Int,
        val totalPrice: Double,
        val pickupWindow: String,
        val status: String,
        val createdAt: Long
    )

    suspend fun getReservationsForOwner(ownerId: String): Result<List<MerchantReservation>> {
        return try {
            val storesSnapshot = db.collection("stores")
                .whereEqualTo("ownerId", ownerId)
                .get()
                .await()

            val storeMap = storesSnapshot.documents.associate { doc ->
                doc.id to (doc.getString("name") ?: "Onbekende winkel")
            }

            if (storeMap.isEmpty()) {
                return Result.success(emptyList())
            }

            val reservationsSnapshot = db.collection("reservations")
                .whereIn("storeId", storeMap.keys.take(30))
                .get()
                .await()

            if (reservationsSnapshot.isEmpty) {
                return Result.success(emptyList())
            }

            val bagsSnapshot = db.collection("bags")
                .whereEqualTo("ownerId", ownerId)
                .get()
                .await()

            val bagMap = bagsSnapshot.documents.associateBy { it.id }

            val customerCache = mutableMapOf<String, String>()

            val reservations = reservationsSnapshot.documents.map { doc ->
                val storeId = doc.getString("storeId") ?: ""
                val bagId = doc.getString("bagId") ?: ""
                val customerId = doc.getString("customerId") ?: ""
                val quantity = doc.getLong("quantity")?.toInt() ?: 1

                val bagDoc = bagMap[bagId]
                val bagTitle = bagDoc?.getString("title") ?: "Verrassingspakket"
                val unitPrice = bagDoc?.getDouble("price") ?: 0.0
                val pickupWindow = bagDoc?.getString("pickupWindow") ?: "Niet opgegeven"

                val customerName = if (customerId.isNotBlank()) {
                    customerCache.getOrPut(customerId) {
                        try {
                            val userDoc = db.collection("users").document(customerId).get().await()
                            userDoc.getString("name")
                                ?: userDoc.getString("email")
                                ?: "Klant"
                        } catch (e: Exception) {
                            "Klant"
                        }
                    }
                } else {
                    "Klant"
                }

                val createdAtMillis = doc.getTimestamp("createdAt")?.toDate()?.time
                    ?: doc.getLong("createdAt")
                    ?: 0L

                MerchantReservation(
                    id = doc.id,
                    bagId = bagId,
                    bagTitle = bagTitle,
                    storeId = storeId,
                    storeName = storeMap[storeId] ?: "Onbekende winkel",
                    customerId = customerId,
                    customerName = customerName,
                    pickupCode = doc.getString("pickupCode") ?: "-",
                    quantity = quantity,
                    totalPrice = unitPrice * quantity,
                    pickupWindow = pickupWindow,
                    status = doc.getString("status") ?: "reserved",
                    createdAt = createdAtMillis
                )
            }.sortedByDescending { it.createdAt }

            Result.success(reservations)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

