package com.example.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.ProfileEntity
import android.webkit.WebView
import java.net.URI

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainBrowserScreen(
    viewModel: BrowserViewModel,
    modifier: Modifier = Modifier
) {
    val profiles by viewModel.profiles.collectAsState()
    val activeTabs by viewModel.activeTabs.collectAsState()
    val selectedTabId by viewModel.selectedTabId.collectAsState()
    val isSplitScreen by viewModel.isSplitScreen.collectAsState()
    val secondaryTabId by viewModel.secondaryTabId.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val editingProfile by viewModel.editingProfile.collectAsState()

    var showCreateDialog by remember { mutableStateOf(false) }
    var showImportDialog by remember { mutableStateOf(false) }

    val activeTabSession = activeTabs.find { it.id == selectedTabId }
    val secondaryTabSession = activeTabs.find { it.id == secondaryTabId }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            if (activeTabs.isNotEmpty()) {
                // Browser top panel including Tab Selector
                BrowserTopBar(
                    activeTabs = activeTabs,
                    selectedTabId = selectedTabId,
                    secondaryTabId = secondaryTabId,
                    isSplitScreen = isSplitScreen,
                    onTabSelected = { viewModel.selectTab(it) },
                    onSecondaryTabSelected = { viewModel.setSecondaryTab(it) },
                    onTabClose = { viewModel.closeTab(it) },
                    onToggleSplit = { viewModel.toggleSplitScreen() },
                    onReturnHome = { 
                        // To return home, we can just clear selection or let users view list
                        // In our structure, if activeTabs is empty we show home.
                        // So "Home" button closes all active tabs. Let's provide a way to close active tabs
                        // or toggle a dashboard overlay!
                    }
                )
            } else {
                // Standard app dashboard top bar
                TopAppBar(
                    title = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Web,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(32.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = "Chrome TabOpener",
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.SansSerif,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(2.dp)
                    ),
                    actions = {
                        IconButton(
                            onClick = { showImportDialog = true },
                            modifier = Modifier.testTag("import_proxies_top_button")
                        ) {
                            Icon(Icons.Default.CloudUpload, contentDescription = "Batch Import Proxies")
                        }
                        IconButton(
                            onClick = { 
                                viewModel.startEditingProfile(ProfileEntity(name = ""))
                                showCreateDialog = true 
                            },
                            modifier = Modifier.testTag("add_profile_top_button")
                        ) {
                            Icon(Icons.Default.Add, contentDescription = "Create Profile")
                        }
                    }
                )
            }
        },
        floatingActionButton = {
            if (activeTabs.isEmpty()) {
                ExtendedFloatingActionButton(
                    text = { Text("Create Profile") },
                    icon = { Icon(Icons.Default.Add, contentDescription = null) },
                    onClick = {
                        viewModel.startEditingProfile(ProfileEntity(name = ""))
                        showCreateDialog = true
                    },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.testTag("add_profile_fab")
                )
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            if (activeTabs.isEmpty()) {
                // Show Profile Dashboard list
                ProfileDashboard(
                    profiles = profiles,
                    searchQuery = searchQuery,
                    onSearchQueryChange = { viewModel.setSearchQuery(it) },
                    onOpenProfile = { viewModel.openProfileInTab(it) },
                    onEditProfile = { profile ->
                        viewModel.startEditingProfile(profile)
                        showCreateDialog = true
                    },
                    onDeleteProfile = { viewModel.deleteProfile(it) }
                )
            } else {
                // Show Multi-tab WebView workspace (supports split-screen!)
                BrowserWorkspace(
                    activeTabs = activeTabs,
                    selectedTabId = selectedTabId,
                    secondaryTabId = secondaryTabId,
                    isSplitScreen = isSplitScreen,
                    viewModel = viewModel,
                    onBackToDashboard = {
                        // Close all tabs to go back to dashboard
                        activeTabs.toList().forEach { viewModel.closeTab(it.id) }
                    }
                )
            }

            // Dialog for editing/creating profiles
            if (showCreateDialog && editingProfile != null) {
                ProfileCreatorDialog(
                    profile = editingProfile!!,
                    onDismiss = { showCreateDialog = false },
                    onSave = {
                        viewModel.saveProfile(it)
                        showCreateDialog = false
                    }
                )
            }

            val context = LocalContext.current
            if (showImportDialog) {
                BatchProxyImportDialog(
                    onDismiss = { showImportDialog = false },
                    onImport = { rawText, preset ->
                        viewModel.importProxies(rawText, preset) { count ->
                            android.widget.Toast.makeText(context, "Successfully imported $count profiles!", android.widget.Toast.LENGTH_LONG).show()
                            showImportDialog = false
                        }
                    }
                )
            }
        }
    }
}

@Composable
fun BrowserTopBar(
    activeTabs: List<TabSession>,
    selectedTabId: String?,
    secondaryTabId: String?,
    isSplitScreen: Boolean,
    onTabSelected: (String) -> Unit,
    onSecondaryTabSelected: (String) -> Unit,
    onTabClose: (String) -> Unit,
    onToggleSplit: () -> Unit,
    onReturnHome: () -> Unit
) {
    Surface(
        tonalElevation = 4.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column {
            // Tab Strip
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                    .padding(horizontal = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Layers,
                    contentDescription = null,
                    modifier = Modifier.padding(8.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                // Tabs List
                LazyRow(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    items(activeTabs) { tab ->
                        val isSelected = tab.id == selectedTabId
                        val isSecondary = tab.id == secondaryTabId && isSplitScreen
                        val indicatorColor = Color(android.graphics.Color.parseColor(tab.profile.colorAccentHex))

                        Row(
                            modifier = Modifier
                                .fillMaxHeight()
                                .widthIn(max = 180.dp)
                                .padding(top = 4.dp, end = 4.dp)
                                .clip(RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp))
                                .background(
                                    when {
                                        isSelected -> MaterialTheme.colorScheme.surface
                                        isSecondary -> MaterialTheme.colorScheme.surface.copy(alpha = 0.5f)
                                        else -> Color.Transparent
                                    }
                                )
                                .clickable {
                                    if (isSplitScreen && !isSelected) {
                                        onSecondaryTabSelected(tab.id)
                                    } else {
                                        onTabSelected(tab.id)
                                    }
                                }
                                .padding(horizontal = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Profile Color indicator
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .background(indicatorColor, CircleShape)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            
                            // Tab Name
                            Text(
                                text = tab.title.ifEmpty { tab.profile.name },
                                style = MaterialTheme.typography.bodyMedium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f),
                                fontWeight = if (isSelected || isSecondary) FontWeight.Bold else FontWeight.Normal,
                                color = if (isSelected || isSecondary) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            
                            // Close Button
                            IconButton(
                                onClick = { onTabClose(tab.id) },
                                modifier = Modifier.size(16.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Close Tab",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                    modifier = Modifier.size(12.dp)
                                )
                            }
                        }
                    }
                }

                // Split Screen Button
                IconButton(onClick = onToggleSplit) {
                    Icon(
                        imageVector = if (isSplitScreen) Icons.Default.VerticalSplit else Icons.Default.Window,
                        contentDescription = "Toggle Split Screen",
                        tint = if (isSplitScreen) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
fun ProfileDashboard(
    profiles: List<ProfileEntity>,
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    onOpenProfile: (ProfileEntity) -> Unit,
    onEditProfile: (ProfileEntity) -> Unit,
    onDeleteProfile: (ProfileEntity) -> Unit
) {
    val filteredProfiles = remember(profiles, searchQuery) {
        if (searchQuery.isEmpty()) {
            profiles
        } else {
            profiles.filter {
                it.name.contains(searchQuery, ignoreCase = true) ||
                it.proxyHost.contains(searchQuery, ignoreCase = true)
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Search bar & Header
        Text(
            text = "Secure Browser Profiles",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        
        Text(
            text = "Each profile below runs with its own completely isolated browser environment, custom proxy IP, and digital fingerprints.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        OutlinedTextField(
            value = searchQuery,
            onValueChange = onSearchQueryChange,
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp)
                .testTag("profile_search"),
            placeholder = { Text("Search profiles by name or proxy...") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
            singleLine = true,
            shape = RoundedCornerShape(12.dp)
        )

        if (filteredProfiles.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.SearchOff,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "No Profiles Found",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Click the '+' button to add a new fingerprint-isolated profile.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                        modifier = Modifier.align(Alignment.CenterHorizontally)
                    )
                }
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = 300.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.weight(1f)
            ) {
                items(filteredProfiles, key = { it.id }) { profile ->
                    ProfileCard(
                        profile = profile,
                        onOpen = { onOpenProfile(profile) },
                        onEdit = { onEditProfile(profile) },
                        onDelete = { onDeleteProfile(profile) }
                    )
                }
            }
        }
    }
}

@Composable
fun ProfileCard(
    profile: ProfileEntity,
    onOpen: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val accentColor = Color(android.graphics.Color.parseColor(profile.colorAccentHex))
    
    // Determine OS Badge
    val (osIcon, osName) = when {
        profile.userAgent.contains("Windows") -> Pair(Icons.Default.Laptop, "Windows")
        profile.userAgent.contains("Macintosh") || profile.userAgent.contains("Mac OS") -> Pair(Icons.Default.DesktopMac, "macOS")
        profile.userAgent.contains("iPhone") || profile.userAgent.contains("iPad") -> Pair(Icons.Default.PhoneIphone, "iOS")
        profile.userAgent.contains("Android") -> Pair(Icons.Default.Android, "Android")
        else -> Pair(Icons.Default.Computer, "Generic Linux")
    }

    Card(
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onOpen() }
            .testTag("profile_card_${profile.id}")
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            // Card Header
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                // Color bar
                Box(
                    modifier = Modifier
                        .size(12.dp)
                        .background(accentColor, CircleShape)
                )
                Spacer(modifier = Modifier.width(12.dp))
                
                Text(
                    text = profile.name,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )

                // OS Badge
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = osIcon,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = osName,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Proxy IP details
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = if (profile.useProxy) Icons.Default.VpnLock else Icons.Default.Security,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = if (profile.useProxy) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (profile.useProxy) {
                        "${profile.proxyHost}:${profile.proxyPort}"
                    } else {
                        "Direct IP (Local ISP Connection)"
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = if (profile.useProxy) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Fingerprint indicators
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // UA/Platform Spoof
                AssistChip(
                    onClick = {},
                    label = { Text("Canvas Spoof: ${if (profile.spoofCanvas) "ON" else "OFF"}") },
                    leadingIcon = { Icon(Icons.Default.Brush, contentDescription = null, modifier = Modifier.size(12.dp)) },
                    colors = AssistChipDefaults.assistChipColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                )
                AssistChip(
                    onClick = {},
                    label = { Text("${profile.hardwareConcurrency} Cores") },
                    leadingIcon = { Icon(Icons.Default.Memory, contentDescription = null, modifier = Modifier.size(12.dp)) },
                    colors = AssistChipDefaults.assistChipColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                )
            }

            if (profile.customNotes.isNotEmpty()) {
                Text(
                    text = profile.customNotes,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                IconButton(onClick = onEdit) {
                    Icon(Icons.Default.Edit, contentDescription = "Edit Profile", tint = MaterialTheme.colorScheme.primary)
                }
                IconButton(onClick = onDelete) {
                    Icon(Icons.Default.Delete, contentDescription = "Delete Profile", tint = MaterialTheme.colorScheme.error)
                }
                Button(
                    onClick = onOpen,
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.testTag("launch_button_${profile.id}")
                ) {
                    Icon(Icons.Default.Launch, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Launch")
                }
            }
        }
    }
}

@Composable
fun BrowserWorkspace(
    activeTabs: List<TabSession>,
    selectedTabId: String?,
    secondaryTabId: String?,
    isSplitScreen: Boolean,
    viewModel: BrowserViewModel,
    onBackToDashboard: () -> Unit
) {
    val activeTabSession = activeTabs.find { it.id == selectedTabId }
    val secondaryTabSession = activeTabs.find { it.id == secondaryTabId }

    Column(modifier = Modifier.fillMaxSize()) {
        if (!isSplitScreen) {
            // Single Browser Pane
            if (activeTabSession != null) {
                BrowserPane(
                    tabSession = activeTabSession,
                    viewModel = viewModel,
                    isActive = true,
                    onBackToDashboard = onBackToDashboard
                )
            }
        } else {
            // Split-Screen Browser Panes
            val isLandscape = LocalContext.current.resources.configuration.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE
            
            if (isLandscape) {
                Row(modifier = Modifier.fillMaxSize()) {
                    Box(modifier = Modifier.weight(1f)) {
                        if (activeTabSession != null) {
                            BrowserPane(
                                tabSession = activeTabSession,
                                viewModel = viewModel,
                                isActive = true,
                                onBackToDashboard = onBackToDashboard
                            )
                        } else {
                            PlaceholderPane(activeTabs, onSelect = { viewModel.selectTab(it) })
                        }
                    }
                    VerticalDivider(modifier = Modifier.fillMaxHeight().width(4.dp), color = MaterialTheme.colorScheme.outline)
                    Box(modifier = Modifier.weight(1f)) {
                        if (secondaryTabSession != null) {
                            BrowserPane(
                                tabSession = secondaryTabSession,
                                viewModel = viewModel,
                                isActive = false, // Process global routes primary tab, but second is active
                                onBackToDashboard = onBackToDashboard
                            )
                        } else {
                            PlaceholderPane(activeTabs.filter { it.id != selectedTabId }, onSelect = { viewModel.setSecondaryTab(it) })
                        }
                    }
                }
            } else {
                Column(modifier = Modifier.fillMaxSize()) {
                    Box(modifier = Modifier.weight(1f)) {
                        if (activeTabSession != null) {
                            BrowserPane(
                                tabSession = activeTabSession,
                                viewModel = viewModel,
                                isActive = true,
                                onBackToDashboard = onBackToDashboard
                            )
                        } else {
                            PlaceholderPane(activeTabs, onSelect = { viewModel.selectTab(it) })
                        }
                    }
                    HorizontalDivider(modifier = Modifier.fillMaxWidth().height(4.dp), color = MaterialTheme.colorScheme.outline)
                    Box(modifier = Modifier.weight(1f)) {
                        if (secondaryTabSession != null) {
                            BrowserPane(
                                tabSession = secondaryTabSession,
                                viewModel = viewModel,
                                isActive = false,
                                onBackToDashboard = onBackToDashboard
                            )
                        } else {
                            PlaceholderPane(activeTabs.filter { it.id != selectedTabId }, onSelect = { viewModel.setSecondaryTab(it) })
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun PlaceholderPane(
    availableTabs: List<TabSession>,
    onSelect: (String) -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(24.dp)
        ) {
            Icon(Icons.Default.AddToHomeScreen, contentDescription = null, modifier = Modifier.size(48.dp))
            Spacer(modifier = Modifier.height(12.dp))
            Text("Select Tab for this Pane", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(16.dp))
            if (availableTabs.isEmpty()) {
                Text("No other tabs open.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            } else {
                availableTabs.forEach { tab ->
                    Button(
                        onClick = { onSelect(tab.id) },
                        modifier = Modifier.padding(vertical = 4.dp).fillMaxWidth(0.8f)
                    ) {
                        Text(tab.profile.name)
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BrowserPane(
    tabSession: TabSession,
    viewModel: BrowserViewModel,
    isActive: Boolean,
    onBackToDashboard: () -> Unit
) {
    var webViewRef by remember { mutableStateOf<WebView?>(null) }
    var inputUrl by remember(tabSession.currentUrl) { mutableStateOf(tabSession.currentUrl) }
    val keyboardController = LocalSoftwareKeyboardController.current

    Column(modifier = Modifier.fillMaxSize()) {
        // Toolbar controls
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surfaceColorAtElevation(1.dp))
                .padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Dashboard Exit
            IconButton(onClick = onBackToDashboard) {
                Icon(Icons.Default.Dashboard, contentDescription = "Dashboard", tint = MaterialTheme.colorScheme.onSurface)
            }

            // Navigation controls
            IconButton(
                onClick = { webViewRef?.goBack() },
                enabled = tabSession.canGoBack
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = if (tabSession.canGoBack) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                )
            }

            IconButton(
                onClick = { webViewRef?.goForward() },
                enabled = tabSession.canGoForward
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = "Forward",
                    tint = if (tabSession.canGoForward) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                )
            }

            IconButton(onClick = { webViewRef?.reload() }) {
                Icon(Icons.Default.Refresh, contentDescription = "Refresh")
            }

            // Address Bar
            OutlinedTextField(
                value = inputUrl,
                onValueChange = { inputUrl = it },
                modifier = Modifier
                    .weight(1f)
                    .height(52.dp)
                    .testTag("address_bar_${tabSession.id}"),
                textStyle = MaterialTheme.typography.bodyMedium.copy(fontSize = 13.sp),
                keyboardOptions = KeyboardOptions(
                    imeAction = ImeAction.Go
                ),
                keyboardActions = KeyboardActions(
                    onGo = {
                        keyboardController?.hide()
                        var destination = inputUrl.trim()
                        if (destination.isNotEmpty()) {
                            if (!destination.startsWith("http://") && !destination.startsWith("https://")) {
                                destination = "https://$destination"
                            }
                            webViewRef?.loadUrl(destination)
                        }
                    }
                ),
                singleLine = true,
                shape = RoundedCornerShape(26.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                )
            )

            Spacer(modifier = Modifier.width(6.dp))

            // IP Status / Geolocation Badge
            val badgeColor = when (tabSession.ipStatus) {
                IpStatus.CHECKING -> MaterialTheme.colorScheme.outline
                IpStatus.ACTIVE -> MaterialTheme.colorScheme.primary
                IpStatus.DIRECT -> MaterialTheme.colorScheme.secondary
                IpStatus.FAILED -> MaterialTheme.colorScheme.error
            }

            var showIpDetailDialog by remember { mutableStateOf(false) }

            Surface(
                color = badgeColor.copy(alpha = 0.15f),
                contentColor = badgeColor,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .clickable { showIpDetailDialog = true }
                    .padding(horizontal = 4.dp, vertical = 2.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = if (tabSession.ipStatus == IpStatus.ACTIVE) Icons.Default.VpnLock else Icons.Default.Public,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = tabSession.ipAddress.take(12) + (if (tabSession.ipAddress.length > 12) ".." else ""),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            if (showIpDetailDialog) {
                Dialog(onDismissRequest = { showIpDetailDialog = false }) {
                    Card(
                        modifier = Modifier.padding(16.dp),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Default.NetworkCheck,
                                contentDescription = null,
                                modifier = Modifier.size(48.dp),
                                tint = badgeColor
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "Tab Network Identity",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text("Profile Name: ${tabSession.profile.name}")
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("Current Proxy IP: ${tabSession.ipAddress}")
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("Geolocation Country: ${tabSession.ipCountry.ifEmpty { "N/A" }}")
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("Proxy Settings: ${if (tabSession.profile.useProxy) "${tabSession.profile.proxyHost}:${tabSession.profile.proxyPort}" else "Direct ISP Mode"}")
                            Spacer(modifier = Modifier.height(24.dp))
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                OutlinedButton(
                                    onClick = {
                                        viewModel.verifyTabIp(tabSession.id, tabSession.profile)
                                        showIpDetailDialog = false
                                    }
                                ) {
                                    Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Recheck")
                                }
                                Button(onClick = { showIpDetailDialog = false }) {
                                    Text("OK")
                                }
                            }
                        }
                    }
                }
            }
        }

        // Progress bar
        if (tabSession.loading) {
            LinearProgressIndicator(
                progress = { tabSession.progress / 100f },
                modifier = Modifier.fillMaxWidth().height(2.dp),
                color = MaterialTheme.colorScheme.primary,
                trackColor = Color.Transparent
            )
        }

        // WebView itself
        BrowserWebView(
            tabSession = tabSession,
            viewModel = viewModel,
            isActive = isActive,
            modifier = Modifier.weight(1f),
            onWebViewCreated = { webViewRef = it }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileCreatorDialog(
    profile: ProfileEntity,
    onDismiss: () -> Unit,
    onSave: (ProfileEntity) -> Unit
) {
    var name by remember { mutableStateOf(profile.name) }
    var useProxy by remember { mutableStateOf(profile.useProxy) }
    var proxyHost by remember { mutableStateOf(profile.proxyHost) }
    var proxyPort by remember { mutableStateOf(profile.proxyPort.toString()) }
    var proxyUsername by remember { mutableStateOf(profile.proxyUsername) }
    var proxyPassword by remember { mutableStateOf(profile.proxyPassword) }
    
    // Fingerprints
    var userAgent by remember { mutableStateOf(profile.userAgent) }
    var platform by remember { mutableStateOf(profile.platform) }
    var webglVendor by remember { mutableStateOf(profile.webglVendor) }
    var webglRenderer by remember { mutableStateOf(profile.webglRenderer) }
    var concurrency by remember { mutableStateOf(profile.hardwareConcurrency.toString()) }
    var memory by remember { mutableStateOf(profile.deviceMemory.toString()) }
    var spoofCanvas by remember { mutableStateOf(profile.spoofCanvas) }
    var customNotes by remember { mutableStateOf(profile.customNotes) }

    var selectedPreset by remember { mutableStateOf("Custom") }
    var advancedExpanded by remember { mutableStateOf(false) }

    val presetOptions = listOf("Custom", "Windows Chrome", "MacOS Safari", "Android Chrome Mobile", "iOS iPhone Safari")

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.92f)
                .clip(RoundedCornerShape(16.dp)),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp)
            ) {
                // Header
                Text(
                    text = if (profile.id == 0L) "New Browser Profile" else "Edit Browser Profile",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(16.dp))

                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Profile Name
                    item {
                        OutlinedTextField(
                            value = name,
                            onValueChange = { name = it },
                            label = { Text("Profile Name (e.g. Account 1)") },
                            modifier = Modifier.fillMaxWidth().testTag("profile_name_input"),
                            singleLine = true
                        )
                    }

                    // 1-Click Fingerprint Presets
                    item {
                        Column {
                            Text("1-Click Fingerprint Preset Template", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
                            Spacer(modifier = Modifier.height(6.dp))
                            
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp)
                                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f), RoundedCornerShape(8.dp)),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                LazyRow(
                                    modifier = Modifier.weight(1f),
                                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    items(presetOptions) { preset ->
                                        val isSelected = selectedPreset == preset
                                        SuggestionChip(
                                            onClick = {
                                                selectedPreset = preset
                                                if (preset != "Custom") {
                                                    val template = ProfileEntity.createDefaultPreset("", preset)
                                                    userAgent = template.userAgent
                                                    platform = template.platform
                                                    webglVendor = template.webglVendor
                                                    webglRenderer = template.webglRenderer
                                                    concurrency = template.hardwareConcurrency.toString()
                                                    memory = template.deviceMemory.toString()
                                                }
                                            },
                                            label = { Text(preset) },
                                            modifier = Modifier.padding(horizontal = 4.dp),
                                            colors = if (isSelected) {
                                                SuggestionChipDefaults.suggestionChipColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                                            } else SuggestionChipDefaults.suggestionChipColors()
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Proxy details
                    item {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Icon(Icons.Default.VpnLock, contentDescription = null, modifier = Modifier.size(20.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Dynamic Routing (Proxy Server)", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                    Spacer(modifier = Modifier.weight(1f))
                                    Switch(
                                        checked = useProxy,
                                        onCheckedChange = { useProxy = it },
                                        modifier = Modifier.testTag("proxy_toggle")
                                    )
                                }

                                if (useProxy) {
                                    Spacer(modifier = Modifier.height(12.dp))
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        OutlinedTextField(
                                            value = proxyHost,
                                            onValueChange = { proxyHost = it },
                                            label = { Text("Proxy Host/IP") },
                                            modifier = Modifier.weight(0.7f).testTag("proxy_host_input"),
                                            singleLine = true
                                        )
                                        OutlinedTextField(
                                            value = proxyPort,
                                            onValueChange = { proxyPort = it },
                                            label = { Text("Port") },
                                            modifier = Modifier.weight(0.3f).testTag("proxy_port_input"),
                                            singleLine = true
                                        )
                                    }

                                    Spacer(modifier = Modifier.height(12.dp))
                                    Text("Authentication (Optional)", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Spacer(modifier = Modifier.height(6.dp))
                                    
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        OutlinedTextField(
                                            value = proxyUsername,
                                            onValueChange = { proxyUsername = it },
                                            label = { Text("Username") },
                                            modifier = Modifier.weight(1f).testTag("proxy_username_input"),
                                            singleLine = true
                                        )
                                        
                                        var passwordVisible by remember { mutableStateOf(false) }
                                        OutlinedTextField(
                                            value = proxyPassword,
                                            onValueChange = { proxyPassword = it },
                                            label = { Text("Password") },
                                            modifier = Modifier.weight(1f).testTag("proxy_password_input"),
                                            singleLine = true,
                                            visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                                            trailingIcon = {
                                                IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                                    Icon(
                                                        imageVector = if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                                        contentDescription = null
                                                    )
                                                }
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Advanced browser fingerprint settings (Accordion)
                    item {
                        Card(
                            onClick = { advancedExpanded = !advancedExpanded },
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.Fingerprint, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Advanced Privacy Fingerprints", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                Spacer(modifier = Modifier.weight(1f))
                                Icon(
                                    imageVector = if (advancedExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                    contentDescription = null
                                )
                            }
                        }
                    }

                    if (advancedExpanded) {
                        item {
                            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                // User Agent
                                OutlinedTextField(
                                    value = userAgent,
                                    onValueChange = { userAgent = it },
                                    label = { Text("Custom User-Agent String") },
                                    modifier = Modifier.fillMaxWidth(),
                                    maxLines = 3
                                )

                                // Platform
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    OutlinedTextField(
                                        value = platform,
                                        onValueChange = { platform = it },
                                        label = { Text("navigator.platform") },
                                        modifier = Modifier.weight(1f),
                                        singleLine = true
                                    )
                                    
                                    // Concurrency
                                    OutlinedTextField(
                                        value = concurrency,
                                        onValueChange = { concurrency = it },
                                        label = { Text("concurrency (cores)") },
                                        modifier = Modifier.weight(1f),
                                        singleLine = true
                                    )
                                }

                                // WebGL Spoof Vendor
                                OutlinedTextField(
                                    value = webglVendor,
                                    onValueChange = { webglVendor = it },
                                    label = { Text("WebGL GPU Vendor") },
                                    modifier = Modifier.fillMaxWidth(),
                                    singleLine = true
                                )

                                // WebGL Spoof Renderer
                                OutlinedTextField(
                                    value = webglRenderer,
                                    onValueChange = { webglRenderer = it },
                                    label = { Text("WebGL GPU Renderer") },
                                    modifier = Modifier.fillMaxWidth(),
                                    singleLine = true
                                )

                                // Canvas spoofs
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                                ) {
                                    Checkbox(
                                        checked = spoofCanvas,
                                        onCheckedChange = { spoofCanvas = it }
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Column {
                                        Text("Active Canvas Noise Injection", fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.bodyMedium)
                                        Text("Micro-alters canvas pixels to block cookie-less device fingerprint tracking.", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                }
                            }
                        }
                    }

                    // Notes
                    item {
                        OutlinedTextField(
                            value = customNotes,
                            onValueChange = { customNotes = it },
                            label = { Text("Custom Notes / Account Credentials") },
                            modifier = Modifier.fillMaxWidth(),
                            maxLines = 4
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Footer Actions
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            if (name.trim().isNotEmpty()) {
                                onSave(
                                    profile.copy(
                                        name = name.trim(),
                                        useProxy = useProxy,
                                        proxyHost = proxyHost.trim(),
                                        proxyPort = proxyPort.toIntOrNull() ?: 8080,
                                        proxyUsername = proxyUsername.trim(),
                                        proxyPassword = proxyPassword.trim(),
                                        userAgent = userAgent.trim(),
                                        platform = platform.trim(),
                                        webglVendor = webglVendor.trim(),
                                        webglRenderer = webglRenderer.trim(),
                                        hardwareConcurrency = concurrency.toIntOrNull() ?: 8,
                                        deviceMemory = memory.toIntOrNull() ?: 8,
                                        spoofCanvas = spoofCanvas,
                                        customNotes = customNotes.trim()
                                    )
                                )
                            }
                        },
                        enabled = name.trim().isNotEmpty(),
                        modifier = Modifier.testTag("save_profile_button")
                    ) {
                        Text("Save Profile")
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BatchProxyImportDialog(
    onDismiss: () -> Unit,
    onImport: (String, String) -> Unit
) {
    var rawText by remember { mutableStateOf("") }
    var selectedPreset by remember { mutableStateOf("Windows Chrome") }
    val presetOptions = listOf("Windows Chrome", "MacOS Safari", "Android Chrome Mobile", "iOS iPhone Safari")

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.90f)
                .clip(RoundedCornerShape(16.dp)),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp)
            ) {
                // Title and Icon
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.CloudUpload,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(28.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "Batch Import Proxy List",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "Import proxies instantly in formats such as protocol://ip:port or ip:port (SOCKS4, SOCKS5, HTTP supported). Each valid proxy will generate a dedicated secure browser profile automatically.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Selector for Fingerprint Template
                Text(
                    text = "Apply Fingerprint Template to Imported Profiles",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(bottom = 6.dp)
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp)
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f), RoundedCornerShape(8.dp)),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    LazyRow(
                        modifier = Modifier.weight(1f),
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp)
                    ) {
                        items(presetOptions) { preset ->
                            val isSelected = selectedPreset == preset
                            SuggestionChip(
                                onClick = { selectedPreset = preset },
                                label = { Text(preset, style = MaterialTheme.typography.labelMedium) },
                                colors = if (isSelected) {
                                    SuggestionChipDefaults.suggestionChipColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                                } else SuggestionChipDefaults.suggestionChipColors(),
                                modifier = Modifier.testTag("import_preset_$preset")
                            )
                        }
                    }
                }

                // Raw input Text Field
                OutlinedTextField(
                    value = rawText,
                    onValueChange = { rawText = it },
                    label = { Text("Paste proxy lines here...") },
                    placeholder = {
                        Text(
                            "socks5://134.122.1.61:11679\nhttp://185.235.16.12:80\nsocks4://130.49.187.63:1082"
                        )
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .testTag("raw_proxy_import_input"),
                    minLines = 6,
                    maxLines = 15,
                    textStyle = MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.Monospace, fontSize = 12.sp)
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Format Guidance / Counter
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val lineCount = rawText.lines().count { it.trim().isNotEmpty() }
                    Text(
                        text = "Detected $lineCount potential proxies",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.SemiBold,
                        color = if (lineCount > 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                    )
                    
                    Text(
                        text = "Format: host:port[:user:pass]",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Actions
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(
                        onClick = onDismiss,
                        modifier = Modifier.testTag("cancel_import_button")
                    ) {
                        Text("Cancel")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            if (rawText.trim().isNotEmpty()) {
                                onImport(rawText, selectedPreset)
                            }
                        },
                        enabled = rawText.trim().isNotEmpty(),
                        modifier = Modifier.testTag("submit_import_button")
                    ) {
                        Icon(Icons.Default.CloudDownload, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Import Profiles")
                    }
                }
            }
        }
    }
}
