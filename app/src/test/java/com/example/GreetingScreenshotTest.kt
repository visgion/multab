package com.example

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Laptop
import androidx.compose.material.icons.filled.Launch
import androidx.compose.material.icons.filled.VpnLock
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.ui.theme.MyApplicationTheme
import com.github.takahirom.roborazzi.RobolectricDeviceQualifiers
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = RobolectricDeviceQualifiers.Pixel8, sdk = [36])
class GreetingScreenshotTest {

  @get:Rule val composeTestRule = createComposeRule()

  @Test
  fun greeting_screenshot() {
    composeTestRule.setContent { 
      MyApplicationTheme {
        Box(
          modifier = Modifier
              .fillMaxSize()
              .background(MaterialTheme.colorScheme.background)
              .padding(16.dp),
          contentAlignment = Alignment.Center
        ) {
          // Render a preview Card matching the dashboard design
          Card(
              elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
              shape = RoundedCornerShape(16.dp),
              modifier = Modifier.fillMaxWidth(0.9f)
          ) {
              Column(modifier = Modifier.padding(16.dp)) {
                  Row(
                      verticalAlignment = Alignment.CenterVertically,
                      modifier = Modifier.fillMaxWidth()
                  ) {
                      Box(modifier = Modifier.size(12.dp).background(Color(0xFF4CAF50), CircleShape))
                      Spacer(modifier = Modifier.width(12.dp))
                      Text(
                          text = "Windows Ad Campaign 1",
                          style = MaterialTheme.typography.titleLarge,
                          fontWeight = FontWeight.Bold,
                          modifier = Modifier.weight(1f)
                      )
                      Row(
                          modifier = Modifier
                              .clip(RoundedCornerShape(8.dp))
                              .background(MaterialTheme.colorScheme.surfaceVariant)
                              .padding(horizontal = 8.dp, vertical = 4.dp),
                          verticalAlignment = Alignment.CenterVertically
                      ) {
                          Icon(Icons.Default.Laptop, contentDescription = null, modifier = Modifier.size(14.dp))
                          Spacer(modifier = Modifier.width(4.dp))
                          Text("Windows", style = MaterialTheme.typography.labelSmall)
                      }
                  }
                  Spacer(modifier = Modifier.height(12.dp))
                  Row(verticalAlignment = Alignment.CenterVertically) {
                      Icon(Icons.Default.VpnLock, contentDescription = null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary)
                      Spacer(modifier = Modifier.width(8.dp))
                      Text("192.168.1.100:8080", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.primary)
                  }
                  Spacer(modifier = Modifier.height(16.dp))
                  Button(onClick = {}, shape = RoundedCornerShape(8.dp), modifier = Modifier.align(Alignment.End)) {
                      Icon(Icons.Default.Launch, contentDescription = null, modifier = Modifier.size(16.dp))
                      Spacer(modifier = Modifier.width(6.dp))
                      Text("Launch Profile")
                  }
              }
          }
        }
      }
    }

    composeTestRule.onRoot().captureRoboImage(filePath = "src/test/screenshots/greeting.png")
  }
}
