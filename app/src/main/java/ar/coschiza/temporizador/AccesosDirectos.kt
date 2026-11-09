package ar.coschiza.temporizador

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.ShortcutInfo
import android.content.pm.ShortcutManager
import android.graphics.drawable.Icon

/** Accesos directos: al mantener apretado el ícono aparecen las secuencias para iniciarlas. */
object AccesosDirectos {
    const val ACCION = "ar.coschiza.temporizador.INICIAR"
    const val EXTRA_ID = "id"

    fun actualizar(ctx: Context) {
        try {
            val sm = ctx.getSystemService(ShortcutManager::class.java) ?: return
            val max = minOf(sm.maxShortcutCountPerActivity, 4)
            val lista = Secuencias.todas(ctx).filter { it.hitos.isNotEmpty() }.take(max).map { s ->
                ShortcutInfo.Builder(ctx, "secuencia_${s.id}")
                    .setShortLabel(s.nombre.take(24))
                    .setLongLabel("Iniciar " + s.nombre)
                    .setIcon(Icon.createWithResource(ctx, R.drawable.ic_acceso_directo))
                    .setIntent(
                        Intent(ctx, MainActivity::class.java)
                            .setAction(ACCION)
                            .putExtra(EXTRA_ID, s.id)
                            .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
                    )
                    .build()
            }
            sm.dynamicShortcuts = lista
        } catch (e: Exception) {
            // Algunos lanzadores no admiten accesos directos: no pasa nada.
        }
    }
}

/** Botones de la notificación fija: pausar/reanudar y detener sin abrir la app. */
class ReceptorAcciones : BroadcastReceiver() {
    companion object {
        const val PAUSAR_REANUDAR = "ar.coschiza.temporizador.PAUSAR_REANUDAR"
        const val DETENER = "ar.coschiza.temporizador.DETENER"
    }

    override fun onReceive(ctx: Context, intent: Intent) {
        when (intent.action) {
            PAUSAR_REANUDAR ->
                if (Temporizador.pausado(ctx)) Temporizador.reanudar(ctx) else Temporizador.pausar(ctx)
            DETENER -> Temporizador.detener(ctx)
        }
    }
}
