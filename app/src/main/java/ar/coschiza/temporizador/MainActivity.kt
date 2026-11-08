package ar.coschiza.temporizador

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import kotlinx.coroutines.delay

class MainActivity : ComponentActivity() {

    companion object {
        @Volatile var visible = false
    }

    /** Secuencia de prueba (etapa 3): hitos acumulados desde el inicio. */
    private val secuenciaPrueba = listOf(
        Hito(30_000, Color(0xFF2E7D32).toArgb()),  // 0:30 verde
        Hito(60_000, Color(0xFFFBC02D).toArgb()),  // 1:00 amarillo
        Hito(90_000, Color(0xFFC62828).toArgb()),  // 1:30 rojo
    )

    private var refrescar by mutableIntStateOf(0)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Avisos.crearCanales(this)
        pedirPermisoNotificaciones()
        setContent { MaterialTheme { Surface(Modifier.fillMaxSize()) { Pantalla() } } }
    }

    override fun onResume() {
        super.onResume()
        visible = true
        refrescar++
    }

    override fun onPause() {
        super.onPause()
        visible = false
    }

    private fun pedirPermisoNotificaciones() {
        if (Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
            != PackageManager.PERMISSION_GRANTED
        ) {
            requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 1)
        }
    }

    private fun tieneNotificaciones() =
        Build.VERSION.SDK_INT < 33 ||
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED

    @Composable
    private fun Pantalla() {
        var enMarcha by remember { mutableStateOf(Temporizador.enMarcha(this)) }
        var ahora by remember { mutableLongStateOf(0L) }
        var ultimoMostrado by remember { mutableIntStateOf(Temporizador.indiceActual(this)) }

        LaunchedEffect(Unit) {
            while (true) {
                enMarcha = Temporizador.enMarcha(this@MainActivity)
                ahora = Temporizador.transcurrido(this@MainActivity)
                val indice = Temporizador.indiceActual(this@MainActivity, ahora)
                // Con la app abierta, mostramos la pantalla de color nosotros mismos.
                if (enMarcha && indice > ultimoMostrado && visible) {
                    ultimoMostrado = indice
                    startActivity(Intent(this@MainActivity, ColorActivity::class.java))
                } else if (indice > ultimoMostrado) {
                    ultimoMostrado = indice
                }
                delay(250)
            }
        }

        Column(
            modifier = Modifier.fillMaxSize().padding(24.dp).verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Spacer(Modifier.height(24.dp))
            Text("Temporizador Visual", fontSize = 28.sp, fontWeight = FontWeight.Bold)
            Text("Versión ${versionInstalada()}", fontSize = 14.sp)
            Spacer(Modifier.height(8.dp))

            Text(formatear(ahora), fontSize = 72.sp, fontWeight = FontWeight.Bold)
            Text("Prueba: verde 0:30 · amarillo 1:00 · rojo 1:30", fontSize = 14.sp)

            if (!enMarcha) {
                Button(
                    onClick = {
                        Temporizador.iniciar(this@MainActivity, secuenciaPrueba)
                        ultimoMostrado = -1
                        enMarcha = true
                    },
                    modifier = Modifier.fillMaxWidth().height(64.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32)),
                ) { Text("Iniciar", fontSize = 22.sp) }
            } else {
                Button(
                    onClick = {
                        Temporizador.detener(this@MainActivity)
                        enMarcha = false
                    },
                    modifier = Modifier.fillMaxWidth().height(64.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFC62828)),
                ) { Text("Detener", fontSize = 22.sp) }
            }

            Spacer(Modifier.height(16.dp))
            PanelPermisos()
        }
    }

    @Composable
    private fun PanelPermisos() {
        @Suppress("UNUSED_VARIABLE") val r = refrescar // vuelve a revisar al regresar de Ajustes
        Text("Permisos", fontSize = 20.sp, fontWeight = FontWeight.Bold)
        EstadoPermiso("Notificaciones", tieneNotificaciones()) {
            abrir(Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                .putExtra(Settings.EXTRA_APP_PACKAGE, packageName))
        }
        EstadoPermiso("Pantalla completa", Avisos.puedePantallaCompleta(this)) {
            if (Build.VERSION.SDK_INT >= 34) {
                abrir(Intent(Settings.ACTION_MANAGE_APP_USE_FULL_SCREEN_INTENT,
                    Uri.parse("package:$packageName")))
            }
        }
        EstadoPermiso("Alarmas exactas", Alarmas.puedeProgramar(this)) {
            if (Build.VERSION.SDK_INT >= 31) {
                abrir(Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM,
                    Uri.parse("package:$packageName")))
            }
        }
        OutlinedButton(onClick = { abrirPermisosXiaomi() }, modifier = Modifier.fillMaxWidth()) {
            Text("Otros permisos (pantalla de bloqueo, ventanas emergentes)")
        }
        OutlinedButton(onClick = {
            abrir(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:$packageName")))
        }, modifier = Modifier.fillMaxWidth()) {
            Text("Información de la app (inicio automático, batería)")
        }
    }

    @Composable
    private fun EstadoPermiso(nombre: String, ok: Boolean, alActivar: () -> Unit) {
        if (ok) {
            Text("✅ $nombre", fontSize = 16.sp, modifier = Modifier.fillMaxWidth())
        } else {
            Button(onClick = alActivar, modifier = Modifier.fillMaxWidth()) {
                Text("⚠️ Activar: $nombre")
            }
        }
    }

    private fun abrirPermisosXiaomi() {
        val miui = Intent("miui.intent.action.APP_PERM_EDITOR")
            .setClassName("com.miui.securitycenter",
                "com.miui.permcenter.permissions.PermissionsEditorActivity")
            .putExtra("extra_pkgname", packageName)
        try {
            startActivity(miui)
        } catch (e: Exception) {
            abrir(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:$packageName")))
        }
    }

    private fun abrir(intent: Intent) {
        try {
            startActivity(intent)
        } catch (e: Exception) {
            startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:$packageName")))
        }
    }

    private fun versionInstalada(): String =
        try { packageManager.getPackageInfo(packageName, 0).versionName ?: "?" } catch (e: Exception) { "?" }
}
