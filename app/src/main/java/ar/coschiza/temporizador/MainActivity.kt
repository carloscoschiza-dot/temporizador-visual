package ar.coschiza.temporizador

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** Pantalla principal. Etapa 1-2: botones de prueba que muestran un color a pantalla completa. */
class MainActivity : ComponentActivity() {

    private val coloresDePrueba = listOf(
        "Verde" to Color(0xFF2E7D32),
        "Amarillo" to Color(0xFFFBC02D),
        "Rojo" to Color(0xFFC62828),
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    Column(
                        modifier = Modifier.fillMaxSize().padding(24.dp),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Text("Temporizador Visual", fontSize = 28.sp, fontWeight = FontWeight.Bold)
                        Text("Versión ${BuildConfigVersion.nombre(this@MainActivity)}", fontSize = 14.sp)
                        Spacer(Modifier.height(32.dp))
                        Text("Probar pantalla de color:", fontSize = 18.sp)
                        Spacer(Modifier.height(16.dp))
                        coloresDePrueba.forEach { (nombre, color) ->
                            Button(
                                onClick = { mostrarColor(color) },
                                colors = ButtonDefaults.buttonColors(containerColor = color),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp).height(56.dp),
                            ) {
                                Text(nombre, fontSize = 18.sp, color = Color.Black)
                            }
                        }
                        Spacer(Modifier.height(16.dp))
                        Text("Tocá la pantalla de color para cerrarla.", fontSize = 14.sp)
                    }
                }
            }
        }
    }

    private fun mostrarColor(color: Color) {
        startActivity(
            Intent(this, ColorActivity::class.java)
                .putExtra(ColorActivity.EXTRA_COLOR, color.toArgb())
        )
    }
}

/** Lee el nombre de versión instalado (para confirmar qué versión tiene el celular). */
object BuildConfigVersion {
    fun nombre(activity: ComponentActivity): String =
        try {
            activity.packageManager.getPackageInfo(activity.packageName, 0).versionName ?: "?"
        } catch (e: Exception) {
            "?"
        }
}
