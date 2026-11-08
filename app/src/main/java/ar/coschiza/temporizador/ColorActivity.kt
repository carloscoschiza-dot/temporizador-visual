package ar.coschiza.temporizador

import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat

/**
 * Muestra un color a pantalla completa. Enciende la pantalla, se muestra sobre la
 * pantalla de bloqueo y la mantiene encendida. Se cierra al tocarla.
 */
class ColorActivity : ComponentActivity() {

    companion object {
        const val EXTRA_COLOR = "color"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        encenderPantallaSobreBloqueo()
        ocultarBarrasDelSistema()

        val colorArgb = intent.getIntExtra(EXTRA_COLOR, 0xFF2E7D32.toInt())
        setContent {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(colorArgb))
                    .clickable { finish() }
            )
        }
    }

    private fun encenderPantallaSobreBloqueo() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        } else {
            @Suppress("DEPRECATION")
            window.addFlags(
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                    WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
            )
        }
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    }

    private fun ocultarBarrasDelSistema() {
        WindowCompat.setDecorFitsSystemWindows(window, false)
        WindowInsetsControllerCompat(window, window.decorView).apply {
            hide(WindowInsetsCompat.Type.systemBars())
            systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }
    }
}
