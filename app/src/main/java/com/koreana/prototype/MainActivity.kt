package com.koreana.prototype

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Color as AndroidColor
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import com.koreana.prototype.home.HomeScreen
import com.koreana.prototype.home.NotificationMock
import com.koreana.prototype.onboarding.OnboardingFlow
import com.koreana.prototype.talk.TalkScreen
import com.koreana.prototype.ui.K
import com.koreana.prototype.ui.LocalBottomExtra

class MainActivity : ComponentActivity() {
    private val model = AppModel()
    private lateinit var voice: Voice
    private var pendingMicResult: ((Boolean) -> Unit)? = null

    private val micLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        model.micPermission = granted
        pendingMicResult?.invoke(granted)
        pendingMicResult = null
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(AndroidColor.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(AndroidColor.TRANSPARENT),
        )
        super.onCreate(savedInstanceState)
        voice = Voice(this)
        model.micPermission = hasMicPermission()
        model.requestMic = { onResult ->
            if (hasMicPermission()) {
                model.micPermission = true
                onResult(true)
            } else {
                pendingMicResult = onResult
                micLauncher.launch(Manifest.permission.RECORD_AUDIO)
            }
        }
        setContent { DesignScaled { App(model, voice) } }
    }

    override fun onResume() {
        super.onResume()
        model.micPermission = hasMicPermission()
    }

    override fun onStop() {
        super.onStop()
        voice.stop()
    }

    override fun onDestroy() {
        voice.shutdown()
        super.onDestroy()
    }

    private fun hasMicPermission() =
        checkSelfPermission(Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
}

/**
 * Figma 프레임(390 x 844)의 숫자를 그대로 dp로 쓸 수 있게, 화면 너비가 390dp가 되도록 밀도를 맞춘다.
 * 태블릿처럼 넓은 화면에서는 높이에 맞추고 가운데에 둔다.
 */
@Composable
private fun DesignScaled(content: @Composable () -> Unit) {
    BoxWithConstraints(Modifier.fillMaxSize().background(Color.Black), contentAlignment = Alignment.Center) {
        val base = LocalDensity.current
        val wide = maxWidth.value / maxHeight.value > 0.62f
        val scale = if (wide) maxHeight.value / 844f else maxWidth.value / 390f
        val navBottomPx = WindowInsets.navigationBars.getBottom(base)
        val navBottom = navBottomPx / (base.density * scale)
        val extra = (navBottom - 24f).coerceAtLeast(0f)
        CompositionLocalProvider(
            LocalDensity provides Density(base.density * scale, fontScale = 1f),
            LocalBottomExtra provides extra.dp,
        ) {
            Box(
                (if (wide) Modifier.width(390.dp).fillMaxHeight() else Modifier.fillMaxSize())
                    .clipToBounds()
                    .background(K.BrandStrong),
            ) { content() }
        }
    }
}

@Composable
private fun App(model: AppModel, voice: Voice) {
    var debugOpen by remember { mutableStateOf(false) }
    CompositionLocalProvider(LocalVoice provides voice) {
        Box(Modifier.fillMaxSize()) {
            Crossfade(targetState = model.screen, animationSpec = tween(350), label = "screen") { screen ->
                when (screen) {
                    Screen.Onboarding -> key(model.onboardingKey) {
                        OnboardingFlow(model, onOpenDebug = { debugOpen = true })
                    }
                    Screen.Home -> HomeScreen(model, onOpenDebug = { debugOpen = true })
                    Screen.Notification -> NotificationMock(model)
                    is Screen.Talk -> key(screen.config) {
                        TalkScreen(
                            model = model,
                            config = screen.config,
                            onClose = { turn ->
                                when (screen.config.mode) {
                                    TalkMode.Main -> model.leaveTalk(turn)
                                    else -> model.screen = Screen.Home
                                }
                            },
                            onFinished = { model.finishTalk(more = false) },
                        )
                    }
                }
            }
            if (debugOpen) {
                DebugPanel(model, onClose = { debugOpen = false })
            }
        }
    }
}
