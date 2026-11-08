package ar.coschiza.temporizador

import android.content.Context

/** Un hito: momento (en milisegundos desde el inicio) y color (ARGB). */
data class Hito(val ms: Long, val color: Int)

/** Estado del temporizador, guardado en el teléfono para sobrevivir si el sistema cierra la app. */
object Temporizador {
    private const val PREFS = "temporizador"

    fun enMarcha(ctx: Context) = prefs(ctx).getBoolean("enMarcha", false)
    fun inicio(ctx: Context) = prefs(ctx).getLong("inicio", 0L)

    fun hitos(ctx: Context): List<Hito> =
        (prefs(ctx).getString("hitos", "") ?: "")
            .split(";")
            .filter { it.contains(":") }
            .map { val (ms, c) = it.split(":"); Hito(ms.toLong(), c.toLong().toInt()) }
            .sortedBy { it.ms }

    fun nombre(ctx: Context) = prefs(ctx).getString("nombre", "") ?: ""

    fun iniciar(ctx: Context, nombre: String, hitos: List<Hito>) {
        prefs(ctx).edit()
            .putBoolean("enMarcha", true)
            .putString("nombre", nombre)
            .putLong("inicio", System.currentTimeMillis())
            .putString("hitos", hitos.joinToString(";") { "${it.ms}:${it.color.toLong() and 0xFFFFFFFFL}" })
            .apply()
        Alarmas.programar(ctx)
        Avisos.mostrarEnMarcha(ctx)
    }

    fun detener(ctx: Context) {
        Alarmas.cancelar(ctx)
        prefs(ctx).edit().putBoolean("enMarcha", false).apply()
        Avisos.quitarTodo(ctx)
    }

    fun transcurrido(ctx: Context): Long =
        if (enMarcha(ctx)) System.currentTimeMillis() - inicio(ctx) else 0L

    /** Índice del último hito alcanzado, o -1 si todavía no llegó ninguno. */
    fun indiceActual(ctx: Context, transcurrido: Long = transcurrido(ctx)): Int =
        hitos(ctx).indexOfLast { it.ms <= transcurrido }

    fun duracionTotal(ctx: Context): Long = hitos(ctx).maxOfOrNull { it.ms } ?: 0L

    private fun prefs(ctx: Context) = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
}

/** Formato 05:07 o 1:05:07. */
fun formatear(ms: Long): String {
    val s = (ms.coerceAtLeast(0) / 1000)
    val h = s / 3600
    val m = (s % 3600) / 60
    val seg = s % 60
    return if (h > 0) "%d:%02d:%02d".format(h, m, seg) else "%02d:%02d".format(m, seg)
}

/** Formato minutos:segundos sin horas (ej.: 80:00), para listas y editor. */
fun formatearMinSeg(ms: Long): String {
    val s = ms.coerceAtLeast(0) / 1000
    return "%d:%02d".format(s / 60, s % 60)
}
