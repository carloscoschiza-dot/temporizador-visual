package ar.coschiza.temporizador

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

/** Una secuencia guardada: nombre + hitos (tiempos acumulados desde el inicio). */
data class Secuencia(val id: Long, val nombre: String, val hitos: List<Hito>)

/** Colores disponibles para los hitos. */
object Paleta {
    val colores: List<Pair<String, Int>> = listOf(
        "Verde" to 0xFF2E7D32.toInt(),
        "Verde claro" to 0xFF9CCC65.toInt(),
        "Amarillo" to 0xFFFBC02D.toInt(),
        "Naranja" to 0xFFFB8C00.toInt(),
        "Rojo" to 0xFFC62828.toInt(),
        "Rosa" to 0xFFE91E63.toInt(),
        "Violeta" to 0xFF7B1FA2.toInt(),
        "Azul" to 0xFF1565C0.toInt(),
        "Celeste" to 0xFF29B6F6.toInt(),
        "Blanco" to 0xFFFFFFFF.toInt(),
    )

    fun nombre(color: Int): String = colores.firstOrNull { it.second == color }?.first ?: "Color"

    val VERDE get() = colores[0].second
    val AMARILLO get() = colores[2].second
    val ROJO get() = colores[4].second
}

/** Guarda y lee las secuencias en el teléfono. */
object Secuencias {
    private const val PREFS = "secuencias"
    private const val CLAVE = "lista"

    fun todas(ctx: Context): List<Secuencia> {
        val p = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val json = p.getString(CLAVE, null) ?: return ejemplos().also { guardarTodas(ctx, it) }
        return try {
            val arr = JSONArray(json)
            (0 until arr.length()).map { i ->
                val o = arr.getJSONObject(i)
                val h = o.getJSONArray("hitos")
                Secuencia(
                    id = o.getLong("id"),
                    nombre = o.getString("nombre"),
                    hitos = (0 until h.length()).map { j ->
                        val x = h.getJSONObject(j)
                        Hito(x.getLong("ms"), x.getLong("color").toInt())
                    },
                )
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun obtener(ctx: Context, id: Long): Secuencia? = todas(ctx).firstOrNull { it.id == id }

    fun guardar(ctx: Context, s: Secuencia) {
        val lista = todas(ctx).toMutableList()
        val i = lista.indexOfFirst { it.id == s.id }
        if (i >= 0) lista[i] = s else lista.add(s)
        guardarTodas(ctx, lista)
    }

    fun borrar(ctx: Context, id: Long) = guardarTodas(ctx, todas(ctx).filter { it.id != id })

    fun nuevoId(): Long = System.currentTimeMillis()

    private fun guardarTodas(ctx: Context, lista: List<Secuencia>) {
        val arr = JSONArray()
        lista.forEach { s ->
            val h = JSONArray()
            s.hitos.forEach { h.put(JSONObject().put("ms", it.ms).put("color", it.color.toLong() and 0xFFFFFFFFL)) }
            arr.put(JSONObject().put("id", s.id).put("nombre", s.nombre).put("hitos", h))
        }
        ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putString(CLAVE, arr.toString()).apply()
    }

    private fun ejemplos(): List<Secuencia> = listOf(
        Secuencia(1, "Prueba rápida", listOf(
            Hito(30_000, Paleta.VERDE), Hito(60_000, Paleta.AMARILLO), Hito(90_000, Paleta.ROJO))),
        Secuencia(2, "Exposición 10 min", listOf(
            Hito(5 * 60_000L, Paleta.VERDE), Hito(8 * 60_000L, Paleta.AMARILLO), Hito(10 * 60_000L, Paleta.ROJO))),
        Secuencia(3, "Clase 80 min", listOf(
            Hito(60 * 60_000L, Paleta.VERDE), Hito(70 * 60_000L, Paleta.AMARILLO), Hito(80 * 60_000L, Paleta.ROJO))),
    )
}
