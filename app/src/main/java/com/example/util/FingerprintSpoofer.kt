package com.example.util

import com.example.data.ProfileEntity

object FingerprintSpoofer {

    fun generateSpoofScript(profile: ProfileEntity): String {
        val escapedUA = escapeJsString(profile.userAgent)
        val escapedPlatform = escapeJsString(profile.platform)
        val escapedVendor = escapeJsString(profile.webglVendor)
        val escapedRenderer = escapeJsString(profile.webglRenderer)
        val concurrency = profile.hardwareConcurrency
        val memory = profile.deviceMemory
        val languagesList = profile.languages.split(",").joinToString(prefix = "[", postfix = "]") { "'${escapeJsString(it.trim())}'" }
        val spoofCanvas = profile.spoofCanvas

        return """
            (function() {
                try {
                    // 1. Spoof Navigator Properties
                    const customUserAgent = '$escapedUA';
                    const customPlatform = '$escapedPlatform';
                    const customLanguages = $languagesList;
                    const customConcurrency = $concurrency;
                    const customMemory = $memory;

                    Object.defineProperty(navigator, 'userAgent', {
                        get: () => customUserAgent,
                        configurable: true
                    });
                    
                    Object.defineProperty(navigator, 'platform', {
                        get: () => customPlatform,
                        configurable: true
                    });
                    
                    Object.defineProperty(navigator, 'languages', {
                        get: () => customLanguages,
                        configurable: true
                    });

                    Object.defineProperty(navigator, 'language', {
                        get: () => customLanguages[0] || 'en-US',
                        configurable: true
                    });

                    Object.defineProperty(navigator, 'hardwareConcurrency', {
                        get: () => customConcurrency,
                        configurable: true
                    });

                    Object.defineProperty(navigator, 'deviceMemory', {
                        get: () => customMemory,
                        configurable: true
                    });

                    // Disable WebDriver automated flag
                    Object.defineProperty(navigator, 'webdriver', {
                        get: () => false,
                        configurable: true
                    });

                    // 2. Spoof WebGL GPU Fingerprint
                    const spoofWebGL = (glContextProto) => {
                        if (!glContextProto) return;
                        const originalGetParameter = glContextProto.getParameter;
                        glContextProto.getParameter = function(parameter) {
                            // 37445 is UNMASKED_VENDOR_WEBGL (0x9245)
                            if (parameter === 37445) {
                                return '$escapedVendor';
                            }
                            // 37446 is UNMASKED_RENDERER_WEBGL (0x9246)
                            if (parameter === 37446) {
                                return '$escapedRenderer';
                            }
                            // Spoof standard vendor and renderer values as well
                            if (parameter === 7936) { // VENDOR
                                return '$escapedVendor';
                            }
                            if (parameter === 7937) { // RENDERER
                                return '$escapedRenderer';
                            }
                            return originalGetParameter.call(this, parameter);
                        };
                    };

                    if (window.WebGLRenderingContext) {
                        spoofWebGL(WebGLRenderingContext.prototype);
                    }
                    if (window.WebGL2RenderingContext) {
                        spoofWebGL(WebGL2RenderingContext.prototype);
                    }

                    // 3. Spoof Canvas Pixel Output to break canvas tracking hash
                    if ($spoofCanvas && window.HTMLCanvasElement) {
                        const originalToDataURL = HTMLCanvasElement.prototype.toDataURL;
                        HTMLCanvasElement.prototype.toDataURL = function(...args) {
                            try {
                                const ctx = this.getContext('2d');
                                if (ctx) {
                                    // Make a microscopic, non-visual alteration to the pixel data
                                    const width = this.width;
                                    const height = this.height;
                                    if (width > 0 && height > 0) {
                                        // Read the first pixel (0, 0)
                                        const imgData = ctx.getImageData(0, 0, 1, 1);
                                        // Add extremely subtle noise (+1 channel value)
                                        imgData.data[0] = (imgData.data[0] + 1) % 256;
                                        ctx.putImageData(imgData, 0, 0);
                                    }
                                }
                            } catch (err) {
                                // Ignore context errors
                            }
                            return originalToDataURL.apply(this, args);
                        };
                    }

                    console.log('Anti-detect browser fingerprint spoofing activated for profile: ' + '$escapedPlatform');
                } catch(e) {
                    console.error('Failed to apply anti-detect fingerprint: ' + e);
                }
            })();
        """.trimIndent()
    }

    private fun escapeJsString(str: String): String {
        return str.replace("\\", "\\\\")
            .replace("'", "\\'")
            .replace("\"", "\\\"")
            .replace("\n", "\\n")
            .replace("\r", "\\r")
    }
}
