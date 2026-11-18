package com.example.grabeat.screens.merchant

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.grabeat.data.FirebaseRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.result.PickVisualMediaRequest
import androidx.core.content.ContextCompat

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddPackageScreen(repository: FirebaseRepository) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var stores by remember { mutableStateOf<List<FirebaseRepository.MerchantStore>>(emptyList()) }
    var selectedStore by remember { mutableStateOf<FirebaseRepository.MerchantStore?>(null) }
    var storeDropdownExpanded by remember { mutableStateOf(false) }
    var isLoadingStores by remember { mutableStateOf(true) }

    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var priceText by remember { mutableStateOf("") }
    var originalValueText by remember { mutableStateOf("") }
    var quantityText by remember { mutableStateOf("") }
    var pickupWindow by remember { mutableStateOf("") }

    var previewBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var imageBase64 by remember { mutableStateOf("") }
    var isProcessingImage by remember { mutableStateOf(false) }

    var isSaving by remember { mutableStateOf(false) }
    var statusMessage by remember { mutableStateOf("") }
    var isError by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        val uid = repository.currentUser()?.uid
        if (uid != null) {
            repository.getStoresForOwner(uid).onSuccess { fetchedStores ->
                stores = fetchedStores
                if (fetchedStores.isNotEmpty()) {
                    selectedStore = fetchedStores.first()
                }
            }
        }
        isLoadingStores = false
    }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        uri?.let {
            isProcessingImage = true
            coroutineScope.launch {
                val result = encodeUriToCompressedBase64(context, it)
                if (result != null) {
                    previewBitmap = result.first
                    imageBase64 = result.second
                } else {
                    isError = true
                    statusMessage = "Kon de gekozen afbeelding niet verwerken."
                }
                isProcessingImage = false
            }
        }
    }

    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicturePreview()
    ) { bitmap: Bitmap? ->
        bitmap?.let {
            isProcessingImage = true
            coroutineScope.launch {
                val result = encodeBitmapToCompressedBase64(it)
                previewBitmap = result.first
                imageBase64 = result.second
                isProcessingImage = false
            }
        }
    }
    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            cameraLauncher.launch(null)
        } else {
            isError = true
            statusMessage = "Cameratoestemming is geweigerd. Sta dit toe in je instellingen."
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        Text(
            text = "Nieuw Pakket Toevoegen",
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        if (isLoadingStores) {
            CircularProgressIndicator(modifier = Modifier.size(24.dp))
            Spacer(modifier = Modifier.height(16.dp))
        } else if (stores.isEmpty()) {
            Text(
                text = "Je hebt nog geen winkel toegevoegd. Voeg eerst een winkel toe voordat je een pakket plaatst.",
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(bottom = 16.dp)
            )
        } else {
            ExposedDropdownMenuBox(
                expanded = storeDropdownExpanded,
                onExpandedChange = { storeDropdownExpanded = !storeDropdownExpanded }
            ) {
                OutlinedTextField(
                    value = selectedStore?.name ?: "",
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Winkel") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = storeDropdownExpanded) },
                    colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(),
                    modifier = Modifier
                        .menuAnchor()
                        .fillMaxWidth()
                )
                ExposedDropdownMenu(
                    expanded = storeDropdownExpanded,
                    onDismissRequest = { storeDropdownExpanded = false }
                ) {
                    stores.forEach { store ->
                        DropdownMenuItem(
                            text = { Text(store.name) },
                            onClick = {
                                selectedStore = store
                                storeDropdownExpanded = false
                            }
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
        }

        OutlinedTextField(
            value = title,
            onValueChange = { title = it },
            label = { Text("Titel (bijv. Verrassingspakket Brood)") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(
            value = description,
            onValueChange = { description = it },
            label = { Text("Beschrijving") },
            modifier = Modifier.fillMaxWidth(),
            minLines = 2,
            maxLines = 4
        )

        Spacer(modifier = Modifier.height(12.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedTextField(
                value = priceText,
                onValueChange = { priceText = it },
                label = { Text("Prijs (€)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.weight(1f),
                singleLine = true
            )

            OutlinedTextField(
                value = originalValueText,
                onValueChange = { originalValueText = it },
                label = { Text("Originele waarde (€)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.weight(1f),
                singleLine = true
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(
            value = quantityText,
            onValueChange = { quantityText = it },
            label = { Text("Aantal pakketten") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(
            value = pickupWindow,
            onValueChange = { pickupWindow = it },
            label = { Text("Ophaalvenster (bijv. 17:30 - 18:30)") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        Spacer(modifier = Modifier.height(16.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedButton(
                onClick = {
                    photoPickerLauncher.launch(
                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                    )
                },
                modifier = Modifier.weight(1f),
                enabled = !isProcessingImage
            ) {
                Text("Galerij")
            }

            OutlinedButton(
                onClick = {
                    val hasPermission = ContextCompat.checkSelfPermission(
                        context,
                        Manifest.permission.CAMERA
                    ) == PackageManager.PERMISSION_GRANTED

                    if (hasPermission) {
                        cameraLauncher.launch(null)
                    } else {
                        cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                    }
                },
                modifier = Modifier.weight(1f),
                enabled = !isProcessingImage
            ) {
                Text("Camera")
            }
        }

        if (isProcessingImage) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(top = 8.dp)
            ) {
                CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Foto verwerken...")
            }
        }

        previewBitmap?.let { bitmap ->
            Spacer(modifier = Modifier.height(12.dp))
            Image(
                bitmap = bitmap.asImageBitmap(),
                contentDescription = "Gekozen pakketfoto",
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp)
                    .clip(RoundedCornerShape(12.dp))
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        if (statusMessage.isNotBlank()) {
            Text(
                text = statusMessage,
                color = if (isError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(bottom = 8.dp)
            )
        }

        Button(
            onClick = {
                statusMessage = ""
                isError = false

                val price = priceText.replace(",", ".").toDoubleOrNull()
                val originalValue = originalValueText.replace(",", ".").toDoubleOrNull()
                val quantity = quantityText.toIntOrNull()
                val uid = repository.currentUser()?.uid
                val store = selectedStore

                if (uid == null || store == null) {
                    isError = true
                    statusMessage = "Selecteer eerst een geldige winkel."
                } else if (price == null || originalValue == null || price <= 0 || originalValue <= 0) {
                    isError = true
                    statusMessage = "Vul geldige bedragen in voor prijs en originele waarde."
                } else if (quantity == null || quantity <= 0) {
                    isError = true
                    statusMessage = "Vul een geldig aantal pakketten in (minstens 1)."
                } else if (imageBase64.isBlank()) {
                    isError = true
                    statusMessage = "Kies eerst een foto voor het pakket."
                } else {
                    isSaving = true
                    coroutineScope.launch {
                        val result = repository.addPackage(
                            ownerId = uid,
                            storeId = store.id,
                            storeName = store.name,
                            title = title,
                            description = description,
                            price = price,
                            originalValue = originalValue,
                            quantity = quantity,
                            pickupWindow = pickupWindow,
                            imageBase64 = imageBase64
                        )
                        isSaving = false

                        result.onSuccess {
                            isError = false
                            statusMessage = "Pakket succesvol opgeslagen!"
                            title = ""
                            description = ""
                            priceText = ""
                            originalValueText = ""
                            quantityText = ""
                            pickupWindow = ""
                            previewBitmap = null
                            imageBase64 = ""
                        }.onFailure { error ->
                            isError = true
                            statusMessage = "Fout bij opslaan: ${error.localizedMessage}"
                        }
                    }
                }
            },
            modifier = Modifier.fillMaxWidth(),
            enabled = selectedStore != null &&
                    title.isNotBlank() &&
                    description.isNotBlank() &&
                    priceText.isNotBlank() &&
                    originalValueText.isNotBlank() &&
                    quantityText.isNotBlank() &&
                    pickupWindow.isNotBlank() &&
                    imageBase64.isNotBlank() &&
                    !isSaving &&
                    !isProcessingImage
        ) {
            if (isSaving) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    strokeWidth = 2.dp,
                    color = MaterialTheme.colorScheme.onPrimary
                )
            } else {
                Text("Pakket Opslaan")
            }
        }
    }
}

suspend fun encodeUriToCompressedBase64(context: Context, uri: Uri): Pair<Bitmap, String>? {
    return withContext(Dispatchers.IO) {
        try {
            val inputStream = context.contentResolver.openInputStream(uri) ?: return@withContext null
            val originalBitmap = BitmapFactory.decodeStream(inputStream)
            inputStream.close()

            if (originalBitmap == null) return@withContext null
            encodeBitmapToCompressedBase64(originalBitmap)
        } catch (e: Exception) {
            null
        }
    }
}

suspend fun encodeBitmapToCompressedBase64(originalBitmap: Bitmap): Pair<Bitmap, String> {
    return withContext(Dispatchers.IO) {
        val maxDimension = 600
        val ratio = minOf(
            maxDimension.toFloat() / originalBitmap.width,
            maxDimension.toFloat() / originalBitmap.height
        )
        val scaledBitmap = if (ratio < 1f) {
            Bitmap.createScaledBitmap(
                originalBitmap,
                (originalBitmap.width * ratio).toInt(),
                (originalBitmap.height * ratio).toInt(),
                true
            )
        } else {
            originalBitmap
        }

        val outputStream = ByteArrayOutputStream()
        scaledBitmap.compress(Bitmap.CompressFormat.JPEG, 70, outputStream)
        val byteArray = outputStream.toByteArray()
        val base64String = Base64.encodeToString(byteArray, Base64.NO_WRAP)

        Pair(scaledBitmap, base64String)
    }
}