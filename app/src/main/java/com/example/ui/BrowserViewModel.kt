package com.example.ui

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.AppDatabase
import com.example.data.ProfileEntity
import com.example.data.ProfileRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.Credentials
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.net.InetSocketAddress
import java.net.Proxy
import java.util.concurrent.TimeUnit

data class TabSession(
    val id: String, // Unique tab ID
    val profile: ProfileEntity,
    val currentUrl: String = "https://whoer.net",
    val title: String = "",
    val loading: Boolean = false,
    val progress: Int = 0,
    val canGoBack: Boolean = false,
    val canGoForward: Boolean = false,
    val ipAddress: String = "Checking...",
    val ipCountry: String = "",
    val ipStatus: IpStatus = IpStatus.CHECKING
)

enum class IpStatus {
    CHECKING,
    ACTIVE,
    FAILED,
    DIRECT
}

class BrowserViewModel(application: Application) : AndroidViewModel(application) {
    private val database = AppDatabase.getDatabase(application)
    private val repository = ProfileRepository(database.profileDao())

    // All available profiles in DB
    val profiles: StateFlow<List<ProfileEntity>> = repository.allProfiles
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // Current open browser tabs
    private val _activeTabs = MutableStateFlow<List<TabSession>>(emptyList())
    val activeTabs: StateFlow<List<TabSession>> = _activeTabs.asStateFlow()

    // Currently focused tab ID
    private val _selectedTabId = MutableStateFlow<String?>(null)
    val selectedTabId: StateFlow<String?> = _selectedTabId.asStateFlow()

    // Split-screen mode configuration
    private val _isSplitScreen = MutableStateFlow(false)
    val isSplitScreen: StateFlow<Boolean> = _isSplitScreen.asStateFlow()

    private val _secondaryTabId = MutableStateFlow<String?>(null)
    val secondaryTabId: StateFlow<String?> = _secondaryTabId.asStateFlow()

    // Search query for dashboard profiles
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    // Profile being edited or created
    private val _editingProfile = MutableStateFlow<ProfileEntity?>(null)
    val editingProfile: StateFlow<ProfileEntity?> = _editingProfile.asStateFlow()

    // IP Cache to avoid redundant network calls
    private val ipCache = mutableMapOf<String, Pair<String, String>>()

    init {
        // Create an initial placeholder profile if database is empty on first boot
        viewModelScope.launch {
            repository.allProfiles.first().let { list ->
                if (list.isEmpty()) {
                    val defaultProfiles = listOf(
                        ProfileEntity.createDefaultPreset("Default Direct Window", "Windows Chrome").copy(useProxy = false),
                        ProfileEntity.createDefaultPreset("Mobile Safari Session", "iOS iPhone Safari").copy(useProxy = false)
                    )
                    defaultProfiles.forEach { repository.insert(it) }
                }
            }
        }
    }

    // Profile actions
    fun saveProfile(profile: ProfileEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            if (profile.id == 0L) {
                repository.insert(profile)
            } else {
                repository.update(profile)
                // If this profile is currently open in active tabs, update those tab sessions
                _activeTabs.update { tabs ->
                    tabs.map { tab ->
                        if (tab.profile.id == profile.id) {
                            tab.copy(profile = profile)
                        } else tab
                    }
                }
            }
        }
    }

    fun deleteProfile(profile: ProfileEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.delete(profile)
            // Close any tabs using this profile
            _activeTabs.update { list -> list.filter { it.profile.id != profile.id } }
            if (_selectedTabId.value == profile.id.toString()) {
                _selectedTabId.value = _activeTabs.value.firstOrNull()?.id
            }
        }
    }

    fun importProxies(rawText: String, presetType: String, onComplete: (successCount: Int) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            val lines = rawText.lines()
            var successCount = 0
            
            for (line in lines) {
                val trimmedLine = line.trim()
                if (trimmedLine.isEmpty()) continue
                
                try {
                    // Parser for: protocol://host:port or protocol://username:password@host:port or host:port
                    var protocol = "http"
                    var hostPortAuthPart = trimmedLine
                    
                    if (trimmedLine.contains("://")) {
                        val parts = trimmedLine.split("://", limit = 2)
                        protocol = parts[0].lowercase()
                        hostPortAuthPart = parts[1]
                    }
                    
                    var username = ""
                    var password = ""
                    var host = ""
                    var port = 8080
                    
                    if (hostPortAuthPart.contains("@")) {
                        val atParts = hostPortAuthPart.split("@", limit = 2)
                        val auth = atParts[0]
                        val hostPort = atParts[1]
                        
                        if (auth.contains(":")) {
                            val authParts = auth.split(":", limit = 2)
                            username = authParts[0]
                            password = authParts[1]
                        } else {
                            username = auth
                        }
                        
                        val hostPortParts = hostPort.split(":", limit = 2)
                        host = hostPortParts[0]
                        if (hostPortParts.size > 1) {
                            port = hostPortParts[1].toIntOrNull() ?: 8080
                        }
                    } else {
                        val colonParts = hostPortAuthPart.split(":")
                        if (colonParts.size >= 4) {
                            host = colonParts[0]
                            port = colonParts[1].toIntOrNull() ?: 8080
                            username = colonParts[2]
                            password = colonParts[3]
                        } else if (colonParts.size == 3) {
                            host = colonParts[0]
                            port = colonParts[1].toIntOrNull() ?: 8080
                            username = colonParts[2]
                        } else if (colonParts.size == 2) {
                            host = colonParts[0]
                            port = colonParts[1].toIntOrNull() ?: 8080
                        } else {
                            continue
                        }
                    }
                    
                    if (host.isNotEmpty()) {
                        val badge = when (protocol) {
                            "socks5" -> "⚡ SOCKS5"
                            "socks4" -> "🔒 SOCKS4"
                            else -> "🌐 HTTP"
                        }
                        val profileName = "Proxy $badge (${host}:${port})"
                        val baseProfile = ProfileEntity.createDefaultPreset(profileName, presetType)
                        val importedProfile = baseProfile.copy(
                            useProxy = true,
                            proxyHost = host,
                            proxyPort = port,
                            proxyUsername = username,
                            proxyPassword = password,
                            customNotes = "Batch imported from proxy list.\nProtocol: $protocol\nAdded on ${java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault()).format(java.util.Date())}"
                        )
                        repository.insert(importedProfile)
                        successCount++
                    }
                } catch (e: Exception) {
                    Log.e("ProxyImport", "Failed to parse proxy line: $trimmedLine", e)
                }
            }
            
            withContext(Dispatchers.Main) {
                onComplete(successCount)
            }
        }
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun startEditingProfile(profile: ProfileEntity?) {
        _editingProfile.value = profile
    }

    // Tab actions
    fun openProfileInTab(profile: ProfileEntity) {
        val existingTab = _activeTabs.value.find { it.profile.id == profile.id }
        if (existingTab != null) {
            // Already open, select it
            _selectedTabId.value = existingTab.id
            return
        }

        // Create new tab session
        val tabId = "tab_${profile.id}_${System.currentTimeMillis()}"
        val newTab = TabSession(
            id = tabId,
            profile = profile,
            currentUrl = profile.lastUsedUrl,
            title = profile.name
        )

        _activeTabs.update { it + newTab }
        
        if (_isSplitScreen.value && _selectedTabId.value != null && _secondaryTabId.value == null) {
            _secondaryTabId.value = tabId
        } else {
            _selectedTabId.value = tabId
        }

        // Asynchronously check connection IP
        verifyTabIp(tabId, profile)
    }

    fun closeTab(tabId: String) {
        _activeTabs.update { list -> list.filter { it.id != tabId } }
        
        if (_selectedTabId.value == tabId) {
            _selectedTabId.value = _activeTabs.value.firstOrNull()?.id
        }
        if (_secondaryTabId.value == tabId) {
            _secondaryTabId.value = null
            _isSplitScreen.value = false
        }
    }

    fun selectTab(tabId: String) {
        _selectedTabId.value = tabId
    }

    fun toggleSplitScreen() {
        if (_isSplitScreen.value) {
            _isSplitScreen.value = false
            _secondaryTabId.value = null
        } else {
            _isSplitScreen.value = true
            // Find another tab to fill the secondary pane if available
            val otherTab = _activeTabs.value.find { it.id != _selectedTabId.value }
            _secondaryTabId.value = otherTab?.id
        }
    }

    fun setSecondaryTab(tabId: String) {
        _secondaryTabId.value = tabId
    }

    fun updateTabUrl(tabId: String, url: String) {
        _activeTabs.update { list ->
            list.map { tab ->
                if (tab.id == tabId) {
                    // Update database with last used URL
                    viewModelScope.launch(Dispatchers.IO) {
                        repository.update(tab.profile.copy(lastUsedUrl = url))
                    }
                    tab.copy(currentUrl = url)
                } else tab
            }
        }
    }

    fun updateTabLoadingState(tabId: String, loading: Boolean, progress: Int = 0) {
        _activeTabs.update { list ->
            list.map { tab ->
                if (tab.id == tabId) {
                    tab.copy(loading = loading, progress = progress)
                } else tab
            }
        }
    }

    fun updateTabHistoryState(tabId: String, canBack: Boolean, canForward: Boolean) {
        _activeTabs.update { list ->
            list.map { tab ->
                if (tab.id == tabId) {
                    tab.copy(canGoBack = canBack, canGoForward = canForward)
                } else tab
            }
        }
    }

    fun updateTabTitle(tabId: String, title: String) {
        _activeTabs.update { list ->
            list.map { tab ->
                if (tab.id == tabId) {
                    tab.copy(title = title)
                } else tab
            }
        }
    }

    // IP checking using proxy
    fun verifyTabIp(tabId: String, profile: ProfileEntity) {
        val cacheKey = if (profile.useProxy) "${profile.proxyHost}:${profile.proxyPort}" else "direct"
        
        // If cached and it's not a recheck, use cache
        if (ipCache.containsKey(cacheKey)) {
            val (ip, country) = ipCache[cacheKey]!!
            _activeTabs.update { list ->
                list.map { tab ->
                    if (tab.id == tabId) {
                        tab.copy(
                            ipAddress = ip,
                            ipCountry = country,
                            ipStatus = if (profile.useProxy) IpStatus.ACTIVE else IpStatus.DIRECT
                        )
                    } else tab
                }
            }
            return
        }

        viewModelScope.launch(Dispatchers.IO) {
            _activeTabs.update { list ->
                list.map { tab ->
                    if (tab.id == tabId) tab.copy(ipAddress = "Checking...", ipStatus = IpStatus.CHECKING) else tab
                }
            }

            try {
                val client = buildOkHttpClientForProfile(profile)
                val request = Request.Builder()
                    .url("https://ipapi.co/json/")
                    .header("User-Agent", profile.userAgent)
                    .build()

                client.newCall(request).execute().use { response ->
                    if (response.isSuccessful) {
                        val body = response.body?.string() ?: ""
                        val json = JSONObject(body)
                        val ip = json.optString("ip", "Unknown")
                        val country = json.optString("country_name", "")
                        val countryCode = json.optString("country_code", "")
                        val locationString = if (countryCode.isNotEmpty()) "$country ($countryCode)" else country

                        ipCache[cacheKey] = Pair(ip, locationString)

                        _activeTabs.update { list ->
                            list.map { tab ->
                                if (tab.id == tabId) {
                                    tab.copy(
                                        ipAddress = ip,
                                        ipCountry = locationString,
                                        ipStatus = if (profile.useProxy) IpStatus.ACTIVE else IpStatus.DIRECT
                                    )
                                } else tab
                            }
                        }
                    } else {
                        throw Exception("HTTP Error: ${response.code}")
                    }
                }
            } catch (e: Exception) {
                Log.e("IPChecker", "Failed to check IP for tab $tabId: ${e.message}")
                _activeTabs.update { list ->
                    list.map { tab ->
                        if (tab.id == tabId) {
                            tab.copy(
                                ipAddress = "Connection Error",
                                ipCountry = "Failed to establish proxy tunnel",
                                ipStatus = IpStatus.FAILED
                            )
                        } else tab
                    }
                }
            }
        }
    }

    private fun buildOkHttpClientForProfile(profile: ProfileEntity): OkHttpClient {
        val builder = OkHttpClient.Builder()
            .connectTimeout(8, TimeUnit.SECONDS)
            .readTimeout(8, TimeUnit.SECONDS)

        if (profile.useProxy && profile.proxyHost.isNotEmpty()) {
            val proxy = Proxy(
                Proxy.Type.HTTP,
                InetSocketAddress(profile.proxyHost, profile.proxyPort)
            )
            builder.proxy(proxy)

            if (profile.proxyUsername.isNotEmpty()) {
                builder.proxyAuthenticator { _, response ->
                    val credential = Credentials.basic(profile.proxyUsername, profile.proxyPassword)
                    response.request.newBuilder()
                        .header("Proxy-Authorization", credential)
                        .build()
                }
            }
        }
        return builder.build()
    }
}
