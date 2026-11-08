package ar.coschiza.temporizador

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import kotlinx.coroutines.delay

class MainActivity : ComponentActivity() {

    companion object {
        @Volatile var visible = false
    }

    private var refrescar by mutableIntStateOf(0)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Avisos.crearCanales(this)
        pedirPermisoNotificaciones()
        setContent { MaterialTheme { Surface(Modifier.fillMaxSize()) { App() } } }
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

    // ---------- Navegación ----------

    /** null = lista; -1 = nueva secuencia; otro = editar esa secuencia. */
    private var editando by mutableStateOf<Long?>(null)

    @Composable
    private fun App() {
        val id = editando
        if (id == null) PantallaPrincipal()
        else {
            BackHandler { editando = null }
            Editor(id)
        }
    }

    // ---------- Pantalla principal ----------

    @Composable
    private fun PantallaPrincipal() {
        var enMarcha by remember { mutableStateOf(Temporizador.enMarcha(this)) }
        var ahora by remember { mutableLongStateOf(0L) }
        var ultimoMostrado by remember { mutableIntStateOf(Temporizador.indiceActual(this)) }
        var secuencias by remember { mutableStateOf(Secuencias.todas(this)) }
        LaunchedEffect(refrescar) { secuencias = Secuencias.todas(this@MainActivity) }

        LaunchedEffect(Unit) {
            while (true) {
                enMarcha = Temporizador.enMarcha(this@MainActivity)
                ahora = Temporizador.transcurrido(this@MainActivity)
                val indice = Temporizador.indiceActual(this@MainActivity, ahora)
                // Con la app abierta, mostramos la pantalla de color nosotros mismos.
                if (enMarcha && indice > ultimoMostrado && visible) {
                    startActivity(Intent(this@MainActivity, ColorActivity::class.java))
                }
                ultimoMostrado = maxOf(ultimoMostrado, indice)
                delay(250)
            }
        }

        Column(
            modifier = Modifier.fillMaxSize().padding(20.dp).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Spacer(Modifier.height(16.dp))
            Text("Temporizador Visual", fontSize = 26.sp, fontWeight = FontWeight.Bold)
            Text("Versión ${versionInstalada()}", fontSize = 13.sp, color = Color.Gray)

            if (enMarcha) {
                EnMarcha(ahora) {
                    Temporizador.detener(this@MainActivity)
                    enMarcha = false
                }
            } else {
                Text("Mis secuencias", fontSize = 20.sp, fontWeight = FontWeight.Bold)
                secuencias.forEach { s ->
                    TarjetaSecuencia(
                        s,
                        alIniciar = {
                            Temporizador.iniciar(this@MainActivity, s.nombre, s.hitos)
                            ultimoMostrado = -1
                            enMarcha = true
                        },
                        alEditar = { editando = s.id },
                    )
                }
                OutlinedButton(onClick = { editando = -1L }, modifier = Modifier.fillMaxWidth().height(52.dp)) {
                    Text("+ Nueva secuencia", fontSize = 18.sp)
                }
            }

            Spacer(Modifier.height(12.dp))
            PanelPermisos()
            Spacer(Modifier.height(24.dp))
        }
    }

    @Composable
    private fun EnMarcha(ahora: Long, alDetener: () -> Unit) {
        val hitos = Temporizador.hitos(this)
        val proximo = hitos.firstOrNull { it.ms > ahora }
        val total = hitos.maxOfOrNull { it.ms } ?: 0L
        Card(Modifier.fillMaxWidth()) {
            Column(
                Modifier.fillMaxWidth().padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(Temporizador.nombre(this@MainActivity), fontSize = 20.sp, fontWeight = FontWeight.Bold)
                Text(formatear(ahora), fontSize = 72.sp, fontWeight = FontWeight.Bold)
                Text(
                    if (total - ahora > 0) "Restan " + formatear(total - ahora)
                    else "Tiempo cumplido +" + formatear(ahora - total),
                    fontSize = 18.sp,
                )
                if (proximo != null) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Próximo: ", fontSize = 16.sp)
                        Muestra(proximo.color, 20)
                        Text(" ${Paleta.nombre(proximo.color)} a ${formatearMinSeg(proximo.ms)}", fontSize = 16.sp)
                    }
                }
                Button(
                    onClick = alDetener,
                    modifier = Modifier.fillMaxWidth().height(60.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFC62828)),
                ) { Text("Detener", fontSize = 22.sp) }
            }
        }
    }

    @Composable
    private fun TarjetaSecuencia(s: Secuencia, alIniciar: () -> Unit, alEditar: () -> Unit) {
        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(s.nombre, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                s.hitos.sortedBy { it.ms }.forEach { h ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Muestra(h.color, 18)
                        Text("  ${formatearMinSeg(h.ms)}  ${Paleta.nombre(h.color)}", fontSize = 15.sp)
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = alIniciar,
                        enabled = s.hitos.isNotEmpty(),
                        modifier = Modifier.weight(1f).height(52.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32)),
                    ) { Text("Iniciar", fontSize = 18.sp) }
                    OutlinedButton(onClick = alEditar, modifier = Modifier.height(52.dp)) { Text("Editar") }
                }
            }
        }
    }

    @Composable
    private fun Muestra(color: Int, tam: Int) {
        Box(
            Modifier.size(tam.dp).background(Color(color), CircleShape).border(1.dp, Color.Gray, CircleShape)
        )
    }

    // ---------- Editor ----------

    /** Fila editable: minutos y segundos como texto para poder escribir libremente. */
    private class FilaHito(min: String, seg: String, color: Int) {
        var min by mutableStateOf(min)
        var seg by mutableStateOf(seg)
        var color by mutableIntStateOf(color)
        fun ms(): Long? {
            val m = min.trim().ifEmpty { "0" }.toLongOrNull() ?: return null
            val s = seg.trim().ifEmpty { "0" }.toLongOrNull() ?: return null
            if (m < 0 || s < 0 || s > 59) return null
            return (m * 60 + s) * 1000
        }
    }

    @OptIn(ExperimentalLayoutApi::class)
    @Composable
    private fun Editor(id: Long) {
        val original = remember { if (id == -1L) null else Secuencias.obtener(this, id) }
        var nombre by remember { mutableStateOf(original?.nombre ?: "") }
        val filas = remember {
            mutableStateListOf<FilaHito>().apply {
                (original?.hitos ?: listOf(Hito(5 * 60_000L, Paleta.VERDE))).sortedBy { it.ms }.forEach {
                    val s = it.ms / 1000
                    add(FilaHito((s / 60).toString(), "%02d".format(s % 60), it.color))
                }
            }
        }
        var eligiendoColor by remember { mutableStateOf<FilaHito?>(null) }
        var error by remember { mutableStateOf("") }
        var confirmarBorrar by remember { mutableStateOf(false) }

        Column(
            modifier = Modifier.fillMaxSize().padding(20.dp).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Spacer(Modifier.height(16.dp))
            Text(if (original == null) "Nueva secuencia" else "Editar secuencia",
                fontSize = 24.sp, fontWeight = FontWeight.Bold)
            OutlinedTextField(
                value = nombre, onValueChange = { nombre = it },
                label = { Text("Nombre (ej.: Clase 80 min)") }, singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            Text("Hitos (tiempo desde el inicio)", fontSize = 18.sp, fontWeight = FontWeight.Bold)

            filas.forEachIndexed { i, f ->
                Card(Modifier.fillMaxWidth()) {
                    Row(
                        Modifier.padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Box(
                            Modifier.size(44.dp).background(Color(f.color), RoundedCornerShape(8.dp))
                                .border(1.dp, Color.Gray, RoundedCornerShape(8.dp))
                                .clickable { eligiendoColor = f }
                        )
                        OutlinedTextField(
                            value = f.min, onValueChange = { f.min = it.filter(Char::isDigit).take(3) },
                            label = { Text("min") }, singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.width(72.dp),
                        )
                        Text(":", fontSize = 22.sp)
                        OutlinedTextField(
                            value = f.seg, onValueChange = { f.seg = it.filter(Char::isDigit).take(2) },
                            label = { Text("seg") }, singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.width(68.dp),
                        )
                        Spacer(Modifier.weight(1f))
                        TextButton(onClick = { filas.removeAt(i) }) { Text("✕", fontSize = 20.sp) }
                    }
                }
            }

            OutlinedButton(onClick = {
                val ultimo = filas.lastOrNull()?.ms() ?: 0L
                val s = (ultimo / 1000) + 60
                val siguienteColor = Paleta.colores.getOrNull(
                    (Paleta.colores.indexOfFirst { it.second == filas.lastOrNull()?.color } + 2)
                        .coerceIn(0, Paleta.colores.lastIndex)
                )?.second ?: Paleta.VERDE
                filas.add(FilaHito((s / 60).toString(), "%02d".format(s % 60), siguienteColor))
            }, modifier = Modifier.fillMaxWidth()) { Text("+ Agregar hito") }

            if (error.isNotEmpty()) Text(error, color = Color(0xFFC62828))

            Button(
                onClick = {
                    val hitos = filas.map { f -> f.ms()?.let { Hito(it, f.color) } }
                    error = when {
                        nombre.isBlank() -> "Poné un nombre a la secuencia."
                        filas.isEmpty() -> "Agregá al menos un hito."
                        hitos.any { it == null } -> "Revisá los tiempos (segundos entre 0 y 59)."
                        hitos.any { it!!.ms == 0L } -> "Ningún hito puede ser en 0:00."
                        hitos.map { it!!.ms }.toSet().size != hitos.size -> "Hay dos hitos con el mismo tiempo."
                        else -> ""
                    }
                    if (error.isEmpty()) {
                        Secuencias.guardar(
                            this@MainActivity,
                            Secuencia(original?.id ?: Secuencias.nuevoId(), nombre.trim(),
                                hitos.filterNotNull().sortedBy { it.ms }),
                        )
                        editando = null
                        refrescar++
                    }
                },
                modifier = Modifier.fillMaxWidth().height(56.dp),
            ) { Text("Guardar", fontSize = 18.sp) }

            OutlinedButton(onClick = { editando = null }, modifier = Modifier.fillMaxWidth()) { Text("Cancelar") }

            if (original != null) {
                TextButton(onClick = { confirmarBorrar = true }, modifier = Modifier.fillMaxWidth()) {
                    Text("Borrar esta secuencia", color = Color(0xFFC62828))
                }
            }
            Text("Al guardar, los hitos se ordenan por tiempo.", fontSize = 13.sp, color = Color.Gray)
            Spacer(Modifier.height(24.dp))
        }

        eligiendoColor?.let { fila ->
            AlertDialog(
                onDismissRequest = { eligiendoColor = null },
                confirmButton = { TextButton(onClick = { eligiendoColor = null }) { Text("Cerrar") } },
                title = { Text("Elegí un color") },
                text = {
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Paleta.colores.forEach { (n, c) ->
                            Column(horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier.clickable { fila.color = c; eligiendoColor = null }) {
                                Box(Modifier.size(52.dp).background(Color(c), RoundedCornerShape(10.dp))
                                    .border(if (fila.color == c) 3.dp else 1.dp,
                                        if (fila.color == c) Color.Black else Color.Gray, RoundedCornerShape(10.dp)))
                                Text(n, fontSize = 11.sp)
                            }
                        }
                    }
                },
            )
        }

        if (confirmarBorrar && original != null) {
            AlertDialog(
                onDismissRequest = { confirmarBorrar = false },
                title = { Text("¿Borrar \"${original.nombre}\"?") },
                text = { Text("No se puede deshacer.") },
                confirmButton = {
                    TextButton(onClick = {
                        Secuencias.borrar(this@MainActivity, original.id)
                        confirmarBorrar = false
                        editando = null
                        refrescar++
                    }) { Text("Borrar", color = Color(0xFFC62828)) }
                },
                dismissButton = { TextButton(onClick = { confirmarBorrar = false }) { Text("Cancelar") } },
            )
        }
    }

    // ---------- Permisos ----------

    private fun pedirPermisoNotificaciones() {
        if (Build.VERSION.SDK_INT >= 33 && !tieneNotificaciones()) {
            requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 1)
        }
    }

    private fun tieneNotificaciones() =
        Build.VERSION.SDK_INT < 33 ||
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED

    @Composable
    private fun PanelPermisos() {
        @Suppress("UNUSED_VARIABLE") val r = refrescar // vuelve a revisar al regresar de Ajustes
        val notif = tieneNotificaciones()
        val pantalla = Avisos.puedePantallaCompleta(this)
        val alarmas = Alarmas.puedeProgramar(this)
        var verAjustes by remember { mutableStateOf(false) }

        if (!notif || !pantalla || !alarmas) {
            Text("⚠️ Faltan permisos", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color(0xFFC62828))
        }
        if (!notif) BotonPermiso("Activar notificaciones") {
            abrir(Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(Settings.EXTRA_APP_PACKAGE, packageName))
        }
        if (!pantalla && Build.VERSION.SDK_INT >= 34) BotonPermiso("Activar pantalla completa") {
            abrir(Intent(Settings.ACTION_MANAGE_APP_USE_FULL_SCREEN_INTENT, Uri.parse("package:$packageName")))
        }
        if (!alarmas && Build.VERSION.SDK_INT >= 31) BotonPermiso("Activar alarmas exactas") {
            abrir(Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM, Uri.parse("package:$packageName")))
        }

        TextButton(onClick = { verAjustes = !verAjustes }) {
            Text(if (verAjustes) "Ocultar ajustes del sistema" else "Ajustes del sistema (pantalla de bloqueo, batería)")
        }
        if (verAjustes) {
            OutlinedButton(onClick = { abrirPermisosXiaomi() }, modifier = Modifier.fillMaxWidth()) {
                Text("Otros permisos (pantalla de bloqueo, ventanas emergentes)")
            }
            OutlinedButton(onClick = {
                abrir(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:$packageName")))
            }, modifier = Modifier.fillMaxWidth()) {
                Text("Información de la app (inicio automático, batería)")
            }
        }
    }

    @Composable
    private fun BotonPermiso(texto: String, alTocar: () -> Unit) {
        Button(onClick = alTocar, modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFB8C00))) { Text(texto) }
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
