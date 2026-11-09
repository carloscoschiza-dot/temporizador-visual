package ar.coschiza.temporizador

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.PowerManager

/** Pasos de configuración según la marca del celular, para que el temporizador no sea frenado. */
object Guia {
    private val marca = Build.MANUFACTURER.lowercase()

    fun esXiaomi() = marca in listOf("xiaomi", "redmi", "poco")

    fun marcaVisible(): String = "${Build.MANUFACTURER} ${Build.MODEL} (Android ${Build.VERSION.RELEASE})"

    fun sinRestriccionBateria(ctx: Context): Boolean =
        ctx.getSystemService(PowerManager::class.java).isIgnoringBatteryOptimizations(ctx.packageName)

    fun pasos(): List<String> = when {
        esXiaomi() -> listOf(
            "En \"Abrir otros permisos (Xiaomi)\": activá \"Mostrar en pantalla de bloqueo\" y \"Mostrar ventanas emergentes mientras se ejecuta en segundo plano\".",
            "En \"Abrir información de la app\": activá \"Inicio automático\".",
            "En la misma pantalla, entrá a \"Ahorro de batería\" y elegí \"Sin restricciones\".",
            "Opcional: en Recientes, mantené apretada la app y tocá el candado para que no se cierre.",
        )
        marca == "samsung" -> listOf(
            "En \"Abrir información de la app\" → Batería: elegí \"Sin restricciones\".",
            "Ajustes → Mantenimiento del dispositivo → Batería → Límites de uso en segundo plano: sacá la app de \"Aplicaciones en suspensión\" y \"en suspensión profunda\".",
            "Si no se enciende la pantalla: en información de la app → Notificaciones, verificá que estén permitidas y con estilo \"Ventana emergente\".",
        )
        marca == "motorola" -> listOf(
            "En \"Abrir información de la app\" → Batería: elegí \"Sin restricciones\".",
            "Verificá que las notificaciones de la app estén permitidas.",
        )
        marca in listOf("huawei", "honor") -> listOf(
            "Ajustes → Batería → Inicio de aplicaciones: desactivá \"Gestionar automáticamente\" para esta app y activá las tres opciones.",
            "En información de la app → Notificaciones: permitir notificaciones en pantalla de bloqueo y \"banners\".",
        )
        marca in listOf("oppo", "realme", "oneplus", "vivo") -> listOf(
            "En \"Abrir información de la app\" → Uso de batería: activá \"Permitir actividad en segundo plano\" y \"Permitir inicio automático\".",
            "En Notificaciones: permitir en pantalla de bloqueo y ventanas flotantes.",
        )
        else -> listOf(
            "En \"Abrir información de la app\" → Batería: elegí \"Sin restricciones\" (o \"No optimizar\").",
            "Verificá que las notificaciones estén permitidas, incluso en la pantalla de bloqueo.",
        )
    } + "Hacé la \"Prueba de 10 segundos\" con el celular bloqueado para confirmar que funciona."
}

/** Al reiniciar el celular (o actualizar la app), vuelve a programar las alarmas pendientes. */
class ReceptorReinicio : BroadcastReceiver() {
    override fun onReceive(ctx: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED &&
            intent.action != Intent.ACTION_MY_PACKAGE_REPLACED) return
        if (!Temporizador.enMarcha(ctx)) return
        if (!Temporizador.pausado(ctx)) Alarmas.programar(ctx)
        Avisos.mostrarEnMarcha(ctx)
    }
}
