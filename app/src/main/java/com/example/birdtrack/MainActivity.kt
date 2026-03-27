package com.example.birdtrack

import android.Manifest
import android.app.Activity
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.Sailing
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeFloatingActionButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import coil.compose.AsyncImage
import com.example.birdtrack.data.BirdDatabase
import com.example.birdtrack.data.BirdRepository
import com.example.birdtrack.data.Sighting
import com.example.birdtrack.data.Trip
import com.example.birdtrack.sync.SyncWorker
import com.example.birdtrack.ui.BirdViewModel
import com.example.birdtrack.ui.BirdViewModelFactory
import com.example.birdtrack.ui.theme.BirdTrackTheme
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.Scope
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.Circle
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.rememberCameraPositionState
import kotlinx.coroutines.delay
import java.util.Locale

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            BirdTrackTheme {
                val context = LocalContext.current
                val database = BirdDatabase.getDatabase(context)
                val repository = BirdRepository(database.birdDao())
                val viewModel: BirdViewModel = viewModel(factory = BirdViewModelFactory(repository, context))
                BirdApp(viewModel)
            }
        }
    }
}

@Composable
fun BirdApp(viewModel: BirdViewModel) {
    val navController = rememberNavController()
    NavHost(navController = navController, startDestination = "home") {
        composable("home") {
            HomeScreen(
                viewModel = viewModel,
                onTripClick = { tripId -> navController.navigate("tripDetails/$tripId") },
                onSearchClick = { navController.navigate("search") },
                onHotspotsClick = { navController.navigate("hotspots") },
                onAddTripClick = { navController.navigate("addTrip") }
            )
        }
        composable("addTrip") {
            AddTripScreen(
                viewModel = viewModel,
                onNavigateBack = { navController.popBackStack() },
                onTripAdded = { tripId ->
                    navController.popBackStack()
                    navController.navigate("tripDetails/$tripId")
                }
            )
        }
        composable("search") { SearchScreen(viewModel) }
        composable("hotspots") { HotspotMapScreen(viewModel) }
        composable(
            "tripDetails/{tripId}",
            arguments = listOf(navArgument("tripId") { type = NavType.LongType })
        ) { entry ->
            val tripId = entry.arguments?.getLong("tripId") ?: return@composable
            TripDetailsScreen(viewModel = viewModel, tripId = tripId)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: BirdViewModel,
    onTripClick: (Long) -> Unit,
    onSearchClick: () -> Unit,
    onHotspotsClick: () -> Unit,
    onAddTripClick: () -> Unit
) {
    val trips by viewModel.allTrips.collectAsState()
    val badges by viewModel.badges.collectAsState()
    val lifeListCount by viewModel.lifeListCount.collectAsState()
    val context = LocalContext.current

    val googleSignInLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val syncRequest = OneTimeWorkRequestBuilder<SyncWorker>().build()
            WorkManager.getInstance(context).enqueue(syncRequest)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("BirdTrack Pro", color = MaterialTheme.colorScheme.primary) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent,
                    titleContentColor = MaterialTheme.colorScheme.primary,
                    actionIconContentColor = Color.White
                ),
                actions = {
                    IconButton(onClick = {
                        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                            .requestScopes(Scope("https://www.googleapis.com/auth/drive.file"))
                            .requestEmail()
                            .build()
                        val client = GoogleSignIn.getClient(context, gso)
                        googleSignInLauncher.launch(client.signInIntent)
                    }) { Icon(Icons.Default.CloudSync, contentDescription = "Sync Drive") }
                    IconButton(onClick = onSearchClick) { Icon(Icons.Default.Search, contentDescription = "Smart Search") }
                    IconButton(onClick = onHotspotsClick) { Icon(Icons.Default.Map, contentDescription = "Hotspots") }
                }
            )
        },
        floatingActionButton = {
            LargeFloatingActionButton(
                onClick = onAddTripClick,
                modifier = Modifier.size(56.dp),
                shape = RoundedCornerShape(16.dp),
                containerColor = MaterialTheme.colorScheme.secondary
            ) { Icon(Icons.Default.Add, contentDescription = "Add Trip") }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                ElevatedCard(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.elevatedCardColors(
                        containerColor = MaterialTheme.colorScheme.surface,
                        contentColor = MaterialTheme.colorScheme.onSurface
                    )
                ) {
                    Column(Modifier.padding(16.dp)) {
                        Text("Life List", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onSurface)
                        Text("$lifeListCount species documented", color = MaterialTheme.colorScheme.onSurface)
                        if (badges.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(8.dp))
                            badges.forEach { badge -> Text("• ${badge.title}", color = MaterialTheme.colorScheme.onSurface) }
                        }
                    }
                }
            }
            items(trips) { trip ->
                TripItem(trip = trip, onClick = { onTripClick(trip.id) }, onDelete = { viewModel.deleteTrip(trip) })
            }
        }
    }
}

@Composable
fun TripItem(trip: Trip, onClick: () -> Unit, onDelete: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.onSurface
        )
    ) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(trip.name, style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onSurface)
                Text("${trip.location} • ${trip.date} • ${trip.time}", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f))
                Text("Duration: ${trip.duration}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f))
            }
            IconButton(onClick = onDelete) { Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.onSurface) }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddTripScreen(
    viewModel: BirdViewModel,
    onNavigateBack: () -> Unit,
    onTripAdded: (Long) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var date by remember { mutableStateOf("") }
    var time by remember { mutableStateOf("") }
    var location by remember { mutableStateOf("") }
    var duration by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var weatherCondition by remember { mutableStateOf("") }

    var error by remember { mutableStateOf<String?>(null) }
    var showConfirmation by remember { mutableStateOf(false) }

    if (showConfirmation) {
        AlertDialog(
            onDismissRequest = { showConfirmation = false },
            title = { Text("Confirm Trip Details", color = MaterialTheme.colorScheme.onSurface) },
            text = {
                Column {
                    Text("Name: $name", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                    Text("Date: $date", color = MaterialTheme.colorScheme.onSurface)
                    Text("Time: $time", color = MaterialTheme.colorScheme.onSurface)
                    Text("Location: $location", color = MaterialTheme.colorScheme.onSurface)
                    Text("Duration: $duration", color = MaterialTheme.colorScheme.onSurface)
                    if (description.isNotBlank()) Text("Description: $description", color = MaterialTheme.colorScheme.onSurface)
                    if (weatherCondition.isNotBlank()) Text("Weather: $weatherCondition", color = MaterialTheme.colorScheme.onSurface)
                }
            },
            confirmButton = {
                Button(onClick = {
                    viewModel.addTrip(name, date, time, location, duration, description, weatherCondition) { id ->
                        onTripAdded(id)
                    }
                }) { Text("Confirm") }
            },
            dismissButton = {
                TextButton(onClick = { showConfirmation = false }) { Text("Edit") }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("New Expedition") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back") }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .padding(16.dp)
                .fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (error != null) {
                Text(error!!, color = MaterialTheme.colorScheme.error)
            }
            OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Trip Name *") }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(value = date, onValueChange = { date = it }, label = { Text("Date (e.g. 2024-05-20) *") }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(value = time, onValueChange = { time = it }, label = { Text("Time (e.g. 10:00) *") }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(value = location, onValueChange = { location = it }, label = { Text("Location *") }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(value = duration, onValueChange = { duration = it }, label = { Text("Duration (e.g. 2 hours) *") }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(value = description, onValueChange = { description = it }, label = { Text("Description (Optional)") }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(value = weatherCondition, onValueChange = { weatherCondition = it }, label = { Text("Weather Condition (Optional)") }, modifier = Modifier.fillMaxWidth())

            Button(
                onClick = {
                    if (name.isBlank() || date.isBlank() || time.isBlank() || location.isBlank() || duration.isBlank()) {
                        error = "Please fill in all required fields."
                    } else {
                        error = null
                        showConfirmation = true
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Next")
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TripDetailsScreen(viewModel: BirdViewModel, tripId: Long) {
    val sightings by viewModel.getSightingsForTrip(tripId).collectAsState(initial = emptyList())
    var showAddSightingDialog by remember { mutableStateOf(false) }
    var sightingToEdit by remember { mutableStateOf<Sighting?>(null) }
    var showSavedAnimation by remember { mutableStateOf(false) }
    val flightProgress by animateFloatAsState(
        targetValue = if (showSavedAnimation) 1f else 0f,
        animationSpec = tween(850, easing = LinearOutSlowInEasing),
        label = "flight"
    )

    LaunchedEffect(showSavedAnimation) {
        if (showSavedAnimation) {
            delay(950)
            showSavedAnimation = false
        }
    }

    Scaffold(
        topBar = { TopAppBar(title = { Text("Field Notes") }) },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { showAddSightingDialog = true },
                text = { Text("Log Sighting") },
                icon = { Icon(Icons.Default.Add, null) }
            )
        }
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            LazyColumn(
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(sightings) { sighting ->
                    SightingItem(
                        sighting = sighting,
                        onEdit = { sightingToEdit = sighting },
                        onDelete = { viewModel.deleteSighting(sighting) }
                    )
                }
            }
            AnimatedVisibility(visible = showSavedAnimation) {
                Icon(
                    imageVector = Icons.Outlined.Sailing,
                    contentDescription = null,
                    modifier = Modifier
                        .padding(16.dp)
                        .graphicsLayer { translationX = flightProgress * 620f },
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }
    }

    if (showAddSightingDialog) {
        AddSightingDialog(
            viewModel = viewModel,
            onDismiss = { showAddSightingDialog = false },
            onConfirm = { species, location, quantity, comments, imageUri ->
                viewModel.addSighting(tripId, species, location, quantity, comments, imageUri)
                showAddSightingDialog = false
                showSavedAnimation = true
            }
        )
    }

    if (sightingToEdit != null) {
        AddSightingDialog(
            viewModel = viewModel,
            initialSighting = sightingToEdit,
            onDismiss = { sightingToEdit = null },
            onConfirm = { species, location, quantity, comments, imageUri ->
                val updatedSighting = sightingToEdit!!.copy(
                    species = species,
                    location = location,
                    quantity = quantity,
                    comments = comments,
                    imageUri = imageUri
                )
                viewModel.updateSighting(updatedSighting)
                sightingToEdit = null
            }
        )
    }
}

@Composable
fun SightingItem(sighting: Sighting, onEdit: () -> Unit, onDelete: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.onSurface
        )
    ) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            if (sighting.imageUri != null) {
                AsyncImage(
                    model = sighting.imageUri,
                    contentDescription = sighting.species,
                    modifier = Modifier.size(64.dp).clip(RoundedCornerShape(8.dp)),
                    contentScale = ContentScale.Crop
                )
                Spacer(Modifier.width(12.dp))
            }
            Column(Modifier.weight(1f)) {
                Text(sighting.species, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
                Text("Location: ${sighting.location}", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f))
                Text("Quantity: ${sighting.quantity}", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f))
                if (!sighting.comments.isNullOrBlank()) {
                    Text("Notes: ${sighting.comments}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f))
                }
                if (sighting.temperatureC != null) {
                    Text(String.format(Locale.getDefault(), "%.1f°C • %.1f kph", sighting.temperatureC, sighting.windSpeedKph), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                }
            }
            IconButton(onClick = onEdit) { Icon(Icons.Default.Edit, contentDescription = "Edit", tint = MaterialTheme.colorScheme.onSurface) }
            IconButton(onClick = onDelete) { Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.onSurface) }
        }
    }
}

@Composable
fun AddSightingDialog(
    viewModel: BirdViewModel,
    initialSighting: Sighting? = null,
    onDismiss: () -> Unit,
    onConfirm: (String, String, Int, String?, String?) -> Unit
) {
    var species by remember { mutableStateOf(initialSighting?.species ?: "") }
    var location by remember { mutableStateOf(initialSighting?.location ?: "") }
    var quantity by remember { mutableStateOf(initialSighting?.quantity?.toString() ?: "1") }
    var comments by remember { mutableStateOf(initialSighting?.comments ?: "") }
    var imageUri by remember { mutableStateOf<Uri?>(initialSighting?.imageUri?.let { Uri.parse(it) }) }
    var aiSuggestion by remember { mutableStateOf<String?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            imageUri = uri
            aiSuggestion = viewModel.getSpeciesSuggestion(uri)
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initialSighting == null) "Log Bird" else "Edit Sighting", color = MaterialTheme.colorScheme.onSurface) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (error != null) {
                    Text(error!!, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                }
                OutlinedTextField(
                    value = species, 
                    onValueChange = { species = it }, 
                    label = { Text("Species Name *") },
                    colors = TextFieldDefaults.colors(
                        focusedTextColor = MaterialTheme.colorScheme.onSurface,
                        unfocusedTextColor = MaterialTheme.colorScheme.onSurface
                    )
                )
                if (aiSuggestion != null) {
                    Text("AI Suggestion: $aiSuggestion", 
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.clickable { species = aiSuggestion!! }.padding(vertical = 4.dp))
                }
                OutlinedTextField(value = location, onValueChange = { location = it }, label = { Text("Location *") })
                OutlinedTextField(value = quantity, onValueChange = { quantity = it }, label = { Text("Quantity *") })
                OutlinedTextField(value = comments, onValueChange = { comments = it }, label = { Text("Comments (Optional)") })
                
                Button(onClick = { launcher.launch("image/*") }) {
                    Text(if (imageUri == null) "Attach Photo" else "Photo Attached")
                }
            }
        },
        confirmButton = {
            Button(onClick = { 
                if (species.isBlank() || location.isBlank() || quantity.isBlank()) {
                    error = "Species, Location, and Quantity are required."
                } else {
                    onConfirm(species, location, quantity.toIntOrNull() ?: 1, comments, imageUri?.toString())
                }
            }) {
                Text(if (initialSighting == null) "Log" else "Update")
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(viewModel: BirdViewModel) {
    val query by viewModel.searchQuery.collectAsState()
    val results by viewModel.searchResults.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(title = {
                TextField(
                    value = query,
                    onValueChange = { viewModel.updateSearchQuery(it) },
                    placeholder = { Text("Search life list...") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    )
                )
            })
        }
    ) { padding ->
        LazyColumn(modifier = Modifier.padding(padding), contentPadding = PaddingValues(16.dp)) {
            items(results) { sighting ->
                Text(
                    text = sighting.species,
                    modifier = Modifier.padding(vertical = 8.dp),
                    style = MaterialTheme.typography.bodyLarge,
                    color = Color.White
                )
            }
        }
    }
}

@Composable
fun HotspotMapScreen(viewModel: BirdViewModel) {
    val hotspots by viewModel.hotspots.collectAsState()
    val context = LocalContext.current
    var userLocation by remember { mutableStateOf<LatLng?>(null) }
    val cameraPositionState = rememberCameraPositionState()

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { }

    LaunchedEffect(Unit) {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            val fusedLocationClient = com.google.android.gms.location.LocationServices.getFusedLocationProviderClient(context)
            fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                location?.let {
                    val latLng = LatLng(it.latitude, it.longitude)
                    userLocation = latLng
                    cameraPositionState.position = CameraPosition.fromLatLngZoom(latLng, 12f)
                }
            }
        } else {
            permissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
        }
    }

    GoogleMap(
        modifier = Modifier.fillMaxSize(),
        cameraPositionState = cameraPositionState
    ) {
        userLocation?.let {
            Marker(state = MarkerState(position = it), title = "You are here")
            Circle(
                center = it,
                radius = 5000.0,
                fillColor = Color.Blue.copy(alpha = 0.1f),
                strokeColor = Color.Blue
            )
        }
        hotspots.forEach { hotspot ->
            Marker(
                state = MarkerState(position = LatLng(hotspot.latitude, hotspot.longitude)),
                title = hotspot.species,
                snippet = "Sightings: ${hotspot.sightingCount}"
            )
        }
    }
}
