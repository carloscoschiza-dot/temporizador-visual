package ar.coschiza.temporizador

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

/** Un hito: momento (en milisegundos desde el inicio), color (ARGB) y si vibra. */
data class Hito(val ms: Long, val color: Int, val vibrar: Boolean = false, val titulo: String = "")

fun hitosAJson(hitos: List<Hito>): JSONArray = JSONArray().apply {
    hitos.forEach {
        put(JSONObject().put("ms", it.ms).put("color", it.color.toLong() and 0xFFFFFFFFL)
            .put("vibrar", it.vibrar).put("titulo", it.titulo))
    }
}

fun hitosDeJson(arr: JSONArray): List<Hito> = (0 until arr.length()).map { j ->
    val x = arr.getJSONObject(j)
    Hito(x.getLong("ms"), x.getLong("color").toInt(), x.optBoolean("vibrar", false), x.optString("titulo", ""))
}

/** Estado del temporizador, guardado en el teléfono para sobrevivir si el sistema cierra la app. */
object Temporizador {
    private const val PREFS = "temporizador"

    fun enMarcha(ctx: Context) = prefs(ctx).getBoolean("enMarcha", false)
    fun pausado(ctx: Context) = enMarcha(ctx) && prefs(ctx).getBoolean("pausado", false)
    fun inicio(ctx: Context) = prefs(ctx).getLong("inicio", 0L)
    fun nombre(ctx: Context) = prefs(ctx).getString("nombre", "") ?: ""

    fun hitos(ctx: Context): List<Hito> {
        val json = prefs(ctx).getString("hitosJson", null)
        if (json != null) return try { hitosDeJson(JSONArray(json)).sortedBy { it.ms } } catch (e: Exception) { emptyList() }
        return (prefs(ctx).getString("hitos", "") ?: "")
            .split(";")
            .filter { it.contains(":") }
            .map {
                val p = it.split(":")
                Hito(p[0].toLong(), p[1].toLong().toInt(), p.getOrNull(2) == "1")
            }
            .sortedBy { it.ms }
    }

    fun iniciar(ctx: Context, nombre: String, hitos: List<Hito>) {
        prefs(ctx).edit()
            .putBoolean("enMarcha", true)
            .putBoolean("pausado", false)
            .putString("nombre", nombre)
            .putLong("inicio", System.currentTimeMillis())
            .putString("hitosJson", hitosAJson(hitos).toString())
            .commit()
        Alarmas.programar(ctx)
        Avisos.mostrarEnMarcha(ctx)
    }

    /** Congela el tiempo y cancela las alarmas pendientes. */
    fun pausar(ctx: Context) {
        if (!enMarcha(ctx) || pausado(ctx)) return
        val t = transcurrido(ctx)
        Alarmas.cancelar(ctx)
        prefs(ctx).edit().putBoolean("pausado", true).putLong("transcurridoEnPausa", t).commit()
        Avisos.mostrarEnMarcha(ctx)
    }

    /** Sigue desde donde quedó y reprograma las alarmas de los hitos que faltan. */
    fun reanudar(ctx: Context) {
        if (!pausado(ctx)) return
        val t = prefs(ctx).getLong("transcurridoEnPausa", 0L)
        prefs(ctx).edit()
            .putBoolean("pausado", false)
            .putLong("inicio", System.currentTimeMillis() - t)
            .commit()
        Alarmas.programar(ctx)
        Avisos.mostrarEnMarcha(ctx)
    }

    fun detener(ctx: Context) {
        Alarmas.cancelar(ctx)
        prefs(ctx).edit().putBoolean("enMarcha", false).putBoolean("pausado", false).commit()
        Avisos.quitarTodo(ctx)
    }

    fun transcurrido(ctx: Context): Long = when {
        !enMarcha(ctx) -> 0L
        pausado(ctx) -> prefs(ctx).getLong("transcurridoEnPausa", 0L)
        else -> System.currentTimeMillis() - inicio(ctx)
    }

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
