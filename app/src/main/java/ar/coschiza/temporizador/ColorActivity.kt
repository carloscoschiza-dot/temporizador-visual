package ar.coschiza.temporizador

import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import kotlinx.coroutines.delay

/**
 * Pantalla de color a pantalla completa con el tiempo transcurrido (grande) y el restante (chico).
 * Enciende la pantalla, aparece sobre el bloqueo y cambia sola de color al llegar al próximo hito.
 * Se cierra al tocarla.
 */
class ColorActivity : ComponentActivity() {

    companion object {
        @Volatile var visible = false
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        encenderPantallaSobreBloqueo()
        ocultarBarrasDelSistema()
        setContent { PantallaColor(onTocar = { finish() }) }
    }

    override fun onResume() {
        super.onResume()
        visible = true
        Avisos.quitarHito(this)
    }

    override fun onPause() {
        super.onPause()
        visible = false
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

    @Composable
    private fun PantallaColor(onTocar: () -> Unit) {
        var ahora by remember { mutableLongStateOf(Temporizador.transcurrido(this)) }
        LaunchedEffect(Unit) {
            while (true) {
                ahora = Temporizador.transcurrido(this@ColorActivity)
                delay(100)
            }
        }
        val hitos = remember { Temporizador.hitos(this) }
        val indice = hitos.indexOfLast { it.ms <= ahora }
        val colorHito = Color(if (indice >= 0) hitos[indice].color else 0xFF2E7D32.toInt())
        // El último hito parpadea (color / negro cada medio segundo), salvo en pausa.
        val parpadea = indice >= 0 && indice == hitos.lastIndex && !Temporizador.pausado(this)
        val fondo = if (parpadea && (System.currentTimeMillis() / 500) % 2 == 1L) Color.Black else colorHito
        val texto = if (fondo.luminance() > 0.5f) Color.Black else Color.White
        val total = hitos.maxOfOrNull { it.ms } ?: 0L
        val restante = total - ahora

        Column(
            modifier = Modifier.fillMaxSize().background(fondo).clickable { onTocar() },
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            val titulo = if (indice >= 0) hitos[indice].titulo else ""
            if (titulo.isNotBlank()) {
                Text(
                    titulo, color = texto, fontSize = 40.sp, fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center, lineHeight = 46.sp,
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 16.dp),
                )
            }
            Text(formatear(ahora), color = texto, fontSize = 110.sp, fontWeight = FontWeight.Bold)
            Text(
                if (restante > 0) "Restan " + formatear(restante)
                else "Tiempo cumplido +" + formatear(-restante),
                color = texto, fontSize = 32.sp,
            )
            if (Temporizador.pausado(this@ColorActivity)) {
                Text("EN PAUSA", color = texto, fontSize = 28.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}
