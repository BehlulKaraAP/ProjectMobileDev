package com.example.grabeat.screens.merchant

import android.content.Context
import android.location.Geocoder
import android.os.Build
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.grabeat.data.FirebaseRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.osmdroid.events.MapEventsReceiver
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.MapEventsOverlay
import org.osmdroid.views.overlay.Marker
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddShopScreen(repository: FirebaseRepository) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var shopName by remember { mutableStateOf("") }
    var address by remember { mutableStateOf("") }
    var city by remember { mutableStateOf("") }

    var categoryExpanded by remember { mutableStateOf(false) }
    var selectedCategory by remember { mutableStateOf("") }
    val categories = listOf("Bakker", "Supermarkt", "Restaurant", "Slager")

    var selectedPoint by remember { mutableStateOf<GeoPoint?>(null) }
    var isGeocoding by remember { mutableStateOf(false) }

    var isSaving by remember { mutableStateOf(false) }
    var saveMessage by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        Text(
            text = "Nieuwe Winkel Toevoegen",
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        OutlinedTextField(
            value = shopName,
            onValueChange = { shopName = it },
            label = { Text("Naam van de winkel") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        Spacer(modifier = Modifier.height(16.dp))

        ExposedDropdownMenuBox(
            expanded = categoryExpanded,
            onExpandedChange = { categoryExpanded = !categoryExpanded }
        ) {
            OutlinedTextField(
                value = selectedCategory,
                onValueChange = {},
                readOnly = true,
                label = { Text("Categorie") },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = categoryExpanded) },
                colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(),
                modifier = Modifier
                    .menuAnchor()
                    .fillMaxWidth()
            )
            ExposedDropdownMenu(
                expanded = categoryExpanded,
                onDismissRequest = { categoryExpanded = false }
            ) {
                categories.forEach { category ->
                    DropdownMenuItem(
                        text = { Text(category) },
                        onClick = {
                            selectedCategory = category
                            categoryExpanded = false
                        }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
        Text("Selecteer de locatie op de kaart:")
        Spacer(modifier = Modifier.height(8.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(300.dp)
        ) {
            AndroidView(
                factory = { ctx ->
                    // Basisconfiguratie voor osmdroid vereist door het beleid van OpenStreetMap
//                    Configuration.getInstance().load(ctx, PreferenceManager.getDefaultSharedPreferences(ctx))
//                    Configuration.getInstance().userAgentValue = ctx.packageName

                    MapView(ctx).apply {
//                        setTileSource(org.osmdroid.tileprovider.tilesource.TileSourceFactory.MAPNIK)
                        setTileSource(org.osmdroid.tileprovider.tilesource.TileSourceFactory.OpenTopo)
                        setMultiTouchControls(true)
                        controller.setZoom(15.0)
                        // Standaard startpunt (bijv. Antwerpen)
                        val startPoint = GeoPoint(51.2194, 4.4025)
                        controller.setCenter(startPoint)

                        setOnTouchListener { view, _ ->
                            view.parent.requestDisallowInterceptTouchEvent(true)
                            false
                        }

                        val marker = Marker(this)

                        val mapEventsReceiver = object : MapEventsReceiver {
                            override fun singleTapConfirmedHelper(p: GeoPoint?): Boolean {
                                p?.let { geoPoint ->
                                    selectedPoint = geoPoint

                                    marker.position = geoPoint
                                    marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                                    if (!overlays.contains(marker)) {
                                        overlays.add(marker)
                                    }
                                    invalidate() // Herteken de kaart

                                    isGeocoding = true
                                    coroutineScope.launch {
                                        val locationData = getAddressAndCity(ctx, geoPoint.latitude, geoPoint.longitude)
                                        address = locationData.first
                                        city = locationData.second
                                        isGeocoding = false
                                    }
                                }
                                return true
                            }

                            override fun longPressHelper(p: GeoPoint?): Boolean = false
                        }

                        overlays.add(MapEventsOverlay(mapEventsReceiver))
                    }
                },
                modifier = Modifier.fillMaxSize()
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = address,
            onValueChange = { address = it },
            label = { Text(if (isGeocoding) "Adres aan het ophalen..." else "Adres (automatisch ingevuld)") },
            modifier = Modifier.fillMaxWidth(),
            maxLines = 3
        )

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = city,
            onValueChange = { city = it },
            label = { Text(if (isGeocoding) "Stad aan het ophalen..." else "Stad/Gemeente") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = {
                val uid = repository.currentUser()?.uid
                if (uid != null && selectedPoint != null) {
                    isSaving = true
                    coroutineScope.launch {
                        val result = repository.addShop(
                            ownerId = uid,
                            name = shopName,
                            category = selectedCategory,
                            address = address,
                            city = city,
                            latitude = selectedPoint!!.latitude,
                            longitude = selectedPoint!!.longitude
                        )
                        isSaving = false
                        result.onSuccess {
                            saveMessage = "Winkel succesvol opgeslagen!"
                            shopName = ""
                            address = ""
                            city = ""
                            selectedCategory = ""
                        }.onFailure { error ->
                            saveMessage = "Fout bij opslaan: ${error.localizedMessage}"
                        }
                    }
                }
            },
            modifier = Modifier.fillMaxWidth(),
            enabled = shopName.isNotBlank() && selectedCategory.isNotBlank() && address.isNotBlank() && city.isNotBlank() && selectedPoint != null && !isSaving
        ) {
            if (isSaving) {
                CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
            } else {
                Text("Winkel Opslaan")
            }
        }
    }
}

suspend fun getAddressAndCity(context: Context, lat: Double, lon: Double): Pair<String, String> {
    return withContext(Dispatchers.IO) {
        try {
            val geocoder = Geocoder(context, Locale.getDefault())

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                val addresses = geocoder.getFromLocation(lat, lon, 1)
                val addressLine = addresses?.firstOrNull()?.getAddressLine(0) ?: "Adres niet gevonden"
                val city = addresses?.firstOrNull()?.locality ?: addresses?.firstOrNull()?.subAdminArea ?: ""
                Pair(addressLine, city)
            } else {
                @Suppress("DEPRECATION")
                val addresses = geocoder.getFromLocation(lat, lon, 1)
                val addressLine = addresses?.firstOrNull()?.getAddressLine(0) ?: "Adres niet gevonden"
                val city = addresses?.firstOrNull()?.locality ?: addresses?.firstOrNull()?.subAdminArea ?: ""
                Pair(addressLine, city)
            }
        } catch (e: Exception) {
            Pair("Kon adres niet ophalen", "")
        }
    }
}