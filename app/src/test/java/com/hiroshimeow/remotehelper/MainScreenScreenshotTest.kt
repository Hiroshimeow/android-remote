package com.hiroshimeow.remotehelper

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import com.hiroshimeow.remotehelper.session.SessionManager
import com.hiroshimeow.remotehelper.ui.theme.MyApplicationTheme
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
class MainScreenScreenshotTest {

    @get:Rule val composeTestRule = createComposeRule()

    @Test
    fun mainScreen_screenshot() {
        val sessionManager = SessionManager()
        
        composeTestRule.setContent { 
            MyApplicationTheme { 
                MainScreen(
                    sessionManager = sessionManager,
                    onStartSession = {},
                    onStopSession = {},
                    ipAddress = "192.168.1.100"
                )
            } 
        }

        composeTestRule.onRoot().captureRoboImage(filePath = "src/test/screenshots/mainscreen.png")
    }

    @Test
    fun mainScreen_activeSession_screenshot() {
        val sessionManager = SessionManager()
        sessionManager.startSession()
        sessionManager.onScreenConsentGranted()
        sessionManager.onControllerConnected()

        composeTestRule.setContent { 
            MyApplicationTheme { 
                MainScreen(
                    sessionManager = sessionManager,
                    onStartSession = {},
                    onStopSession = {},
                    ipAddress = "100.82.14.92"
                )
            } 
        }

        composeTestRule.onRoot().captureRoboImage(filePath = "src/test/screenshots/mainscreen_active.png")
    }
}
