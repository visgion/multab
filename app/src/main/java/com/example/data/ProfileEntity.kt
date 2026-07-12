package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "browser_profiles")
data class ProfileEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    
    // Proxy configurations
    val useProxy: Boolean = false,
    val proxyHost: String = "",
    val proxyPort: Int = 8080,
    val proxyUsername: String = "",
    val proxyPassword: String = "",
    
    // Fingerprint parameters (User Agent, Platform, WebGL, etc.)
    val userAgent: String = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36",
    val platform: String = "Win32",
    val languages: String = "en-US,en;q=0.9",
    val webglVendor: String = "Google Inc. (NVIDIA)",
    val webglRenderer: String = "ANGLE (NVIDIA, NVIDIA GeForce RTX 4070 Ti Direct3D11 vs_5_0 ps_5_0, D3D11)",
    val hardwareConcurrency: Int = 8,
    val deviceMemory: Int = 8,
    val spoofCanvas: Boolean = true,
    
    // Customization
    val colorAccentHex: String = "#2196F3", // Color category indicator
    val customNotes: String = "",
    val lastUsedUrl: String = "https://whoer.net", // High-quality IP & fingerprint leak checking site
    val timestamp: Long = System.currentTimeMillis()
) {
    companion object {
        fun createDefaultPreset(name: String, presetType: String): ProfileEntity {
            return when (presetType) {
                "Windows Chrome" -> ProfileEntity(
                    name = name,
                    userAgent = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36",
                    platform = "Win32",
                    webglVendor = "Google Inc. (NVIDIA)",
                    webglRenderer = "ANGLE (NVIDIA, NVIDIA GeForce RTX 4070 Ti Direct3D11 vs_5_0 ps_5_0)",
                    colorAccentHex = "#4CAF50"
                )
                "MacOS Safari" -> ProfileEntity(
                    name = name,
                    userAgent = "Mozilla/5.0 (Macintosh; Intel Mac OS X 14_2_1) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/17.2 Safari/605.1.15",
                    platform = "MacIntel",
                    webglVendor = "Apple Inc.",
                    webglRenderer = "Apple M3 GPU",
                    colorAccentHex = "#9C27B0"
                )
                "Android Chrome Mobile" -> ProfileEntity(
                    name = name,
                    userAgent = "Mozilla/5.0 (Linux; Android 10; K) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36",
                    platform = "Linux armv8l",
                    webglVendor = "Google Inc. (Qualcomm)",
                    webglRenderer = "Adreno (TM) 740",
                    colorAccentHex = "#FF5722"
                )
                "iOS iPhone Safari" -> ProfileEntity(
                    name = name,
                    userAgent = "Mozilla/5.0 (iPhone; CPU iPhone OS 17_2 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/17.2 Mobile/15E148 Safari/605.1.15",
                    platform = "iPhone",
                    webglVendor = "Apple Inc.",
                    webglRenderer = "Apple GPU",
                    colorAccentHex = "#E91E63"
                )
                else -> ProfileEntity(name = name)
            }
        }
    }
}
