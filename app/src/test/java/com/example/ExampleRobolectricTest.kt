package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("Chrome Multi-Tab Opener", appName)
  }

  @Test
  fun `test batch proxy import functionality`() {
    val application = ApplicationProvider.getApplicationContext<android.app.Application>()
    val viewModel = com.example.ui.BrowserViewModel(application)
    
    val rawProxyList = """
        http://142.93.202.130:3128
        socks4://130.49.187.63:1082
        socks5://95.140.154.156:1080
        socks5://user:pass@184.181.217.213:4145
        185.235.16.12:80
    """.trimIndent()
    
    var importFinished = false
    var importCount = -1
    viewModel.importProxies(rawProxyList, "Windows Chrome") { count ->
        importCount = count
        importFinished = true
    }
    
    val startTime = System.currentTimeMillis()
    while (!importFinished && System.currentTimeMillis() - startTime < 5000) {
        org.robolectric.shadows.ShadowLooper.idleMainLooper()
        Thread.sleep(10)
    }
    
    assertEquals(5, importCount)
  }
}
