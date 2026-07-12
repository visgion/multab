package com.example.ui

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import android.webkit.*
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.webkit.ProxyConfig
import androidx.webkit.ProxyController
import androidx.webkit.WebViewFeature
import com.example.data.ProfileEntity
import com.example.util.FingerprintSpoofer

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun BrowserWebView(
    tabSession: TabSession,
    viewModel: BrowserViewModel,
    isActive: Boolean,
    modifier: Modifier = Modifier,
    onWebViewCreated: (WebView) -> Unit = {}
) {
    val context = LocalContext.current
    val profile = tabSession.profile

    val webView = remember(tabSession.id) {
        WebView(context).apply {
            layoutParams = android.view.ViewGroup.LayoutParams(
                android.view.ViewGroup.LayoutParams.MATCH_PARENT,
                android.view.ViewGroup.LayoutParams.MATCH_PARENT
            )
            
            // Basic browser configurations
            settings.apply {
                javaScriptEnabled = true
                domStorageEnabled = true
                databaseEnabled = true
                useWideViewPort = true
                loadWithOverviewMode = true
                builtInZoomControls = true
                displayZoomControls = false
                setSupportZoom(true)
                
                // Fingerprinting setup (User-Agent string)
                userAgentString = profile.userAgent
                
                // Force secure modern parameters
                mediaPlaybackRequiresUserGesture = false
                mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
            }

            // Support third party cookies for secure authentication
            val cookieManager = CookieManager.getInstance()
            cookieManager.setAcceptCookie(true)
            cookieManager.setAcceptThirdPartyCookies(this, true)

            webViewClient = object : WebViewClient() {
                override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                    super.onPageStarted(view, url, favicon)
                    viewModel.updateTabLoadingState(tabSession.id, true, 5)
                    url?.let { viewModel.updateTabUrl(tabSession.id, it) }
                    
                    // Inject fingerprinter spoofer script at startup
                    val spoofScript = FingerprintSpoofer.generateSpoofScript(profile)
                    view?.evaluateJavascript(spoofScript, null)
                }

                override fun onPageFinished(view: WebView?, url: String?) {
                    super.onPageFinished(view, url)
                    viewModel.updateTabLoadingState(tabSession.id, false, 100)
                    url?.let { viewModel.updateTabUrl(tabSession.id, it) }
                    viewModel.updateTabHistoryState(
                        tabSession.id,
                        canBack = view?.canGoBack() ?: false,
                        canForward = view?.canGoForward() ?: false
                    )
                    
                    // Inject fingerprinter spoofer script again at page completion
                    val spoofScript = FingerprintSpoofer.generateSpoofScript(profile)
                    view?.evaluateJavascript(spoofScript, null)
                }

                override fun doUpdateVisitedHistory(view: WebView?, url: String?, isReload: Boolean) {
                    super.doUpdateVisitedHistory(view, url, isReload)
                    url?.let { viewModel.updateTabUrl(tabSession.id, it) }
                    viewModel.updateTabHistoryState(
                        tabSession.id,
                        canBack = view?.canGoBack() ?: false,
                        canForward = view?.canGoForward() ?: false
                    )
                }

                override fun onReceivedError(
                    view: WebView?,
                    request: WebResourceRequest?,
                    error: WebResourceError?
                ) {
                    super.onReceivedError(view, request, error)
                    // Only handle main frame loading failures
                    if (request?.isForMainFrame == true) {
                        viewModel.updateTabLoadingState(tabSession.id, false, 0)
                    }
                }

                // Proxy Authentication Credential Handlers
                override fun onReceivedHttpAuthRequest(
                    view: WebView?,
                    handler: HttpAuthHandler?,
                    host: String?,
                    realm: String?
                ) {
                    if (profile.useProxy && profile.proxyUsername.isNotEmpty()) {
                        Log.d("BrowserWebView", "Supplying proxy authentication credentials for: $host")
                        handler?.proceed(profile.proxyUsername, profile.proxyPassword)
                    } else {
                        super.onReceivedHttpAuthRequest(view, handler, host, realm)
                    }
                }
            }

            webChromeClient = object : WebChromeClient() {
                override fun onProgressChanged(view: WebView?, newProgress: Int) {
                    super.onProgressChanged(view, newProgress)
                    viewModel.updateTabLoadingState(tabSession.id, newProgress < 100, newProgress)
                    
                    // Inject early and often as the DOM is compiling to pre-empt detection scripts
                    if (newProgress > 10 && newProgress < 90) {
                        val spoofScript = FingerprintSpoofer.generateSpoofScript(profile)
                        view?.evaluateJavascript(spoofScript, null)
                    }
                }

                override fun onReceivedTitle(view: WebView?, title: String?) {
                    super.onReceivedTitle(view, title)
                    title?.let { viewModel.updateTabTitle(tabSession.id, it) }
                }
            }

            onWebViewCreated(this)
            loadUrl(tabSession.currentUrl)
        }
    }

    // Dynamic proxy routing when tab focus state changes
    LaunchedEffect(isActive, profile) {
        if (isActive) {
            setupProxyForWebView(context, profile)
        }
    }

    AndroidView(
        factory = { webView },
        modifier = modifier.fillMaxSize()
    )
}

/**
 * Configure the AndroidX Webkit Proxy Controller globally for this application process
 */
private fun setupProxyForWebView(context: Context, profile: ProfileEntity) {
    if (!WebViewFeature.isFeatureSupported(WebViewFeature.PROXY_OVERRIDE)) {
        Log.e("ProxySetup", "PROXY_OVERRIDE feature is not supported on this Android system WebView.")
        return
    }

    val proxyController = ProxyController.getInstance()

    if (profile.useProxy && profile.proxyHost.isNotEmpty()) {
        val proxyRule = "${profile.proxyHost}:${profile.proxyPort}"
        val proxyConfig = ProxyConfig.Builder()
            .addProxyRule(proxyRule)
            .addDirect() // fallback directly if proxy fails/local resource requested
            .build()

        try {
            proxyController.setProxyOverride(proxyConfig, { command ->
                // Run on main-thread executor or standard sync command
                command.run()
            }, {
                Log.d("ProxySetup", "Proxy override successfully applied process-wide: $proxyRule")
            })
        } catch (e: Exception) {
            Log.e("ProxySetup", "Failed to apply proxy override: ${e.message}")
        }
    } else {
        try {
            proxyController.clearProxyOverride({ command ->
                command.run()
            }, {
                Log.d("ProxySetup", "Proxy override cleared globally (direct mode active).")
            })
        } catch (e: Exception) {
            Log.e("ProxySetup", "Failed to clear proxy override: ${e.message}")
        }
    }
}
