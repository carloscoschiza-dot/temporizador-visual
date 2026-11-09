package ar.coschiza.temporizador

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.drawable.Icon
import android.os.Build

/** Notificaciones: una fija mientras corre y otra a pantalla completa para cada hito. */
object Avisos {
    private const val CANAL_MARCHA = "en_marcha"
    private const val CANAL_HITO = "hitos"
    const val ID_MARCHA = 1
    const val ID_HITO = 2

    private fun nm(ctx: Context) = ctx.getSystemService(NotificationManager::class.java)

    fun crearCanales(ctx: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        nm(ctx).createNotificationChannel(
            NotificationChannel(CANAL_MARCHA, "Temporizador en marcha", NotificationManager.IMPORTANCE_LOW)
                .apply { setSound(null, null) }
        )
        nm(ctx).createNotificationChannel(
            NotificationChannel(CANAL_HITO, "Hitos (pantalla de color)", NotificationManager.IMPORTANCE_HIGH)
                .apply { setSound(null, null); enableVibration(false) }
        )
    }

    fun puedePantallaCompleta(ctx: Context): Boolean =
        if (Build.VERSION.SDK_INT >= 34) nm(ctx).canUseFullScreenIntent() else true

    fun mostrarEnMarcha(ctx: Context) {
        crearCanales(ctx)
        val abrir = PendingIntent.getActivity(
            ctx, 1, Intent(ctx, MainActivity::class.java), PendingIntent.FLAG_IMMUTABLE
        )
        val pausado = Temporizador.pausado(ctx)
        val n = constructor(ctx, CANAL_MARCHA)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle(if (pausado) "Temporizador en pausa" else "Temporizador en marcha")
            .setContentText(
                if (pausado) "Detenido en " + formatear(Temporizador.transcurrido(ctx)) + " · Tocá para abrir"
                else "Tocá para abrir"
            )
            .setOngoing(true)
            .setUsesChronometer(!pausado)
            .setShowWhen(!pausado)
            .setWhen(Temporizador.inicio(ctx))
            .setContentIntent(abrir)
            .addAction(accion(ctx, if (pausado) "Reanudar" else "Pausar", ReceptorAcciones.PAUSAR_REANUDAR, 10))
            .addAction(accion(ctx, "Detener", ReceptorAcciones.DETENER, 11))
            .build()
        publicar(ctx, ID_MARCHA, n)
    }

    fun mostrarHito(ctx: Context) {
        crearCanales(ctx)
        val pantalla = PendingIntent.getActivity(
            ctx, 2,
            Intent(ctx, ColorActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        val n = constructor(ctx, CANAL_HITO)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle("Hito alcanzado")
            .setContentText("Tiempo: " + formatear(Temporizador.transcurrido(ctx)))
            .setCategory(Notification.CATEGORY_ALARM)
            .setPriority(Notification.PRIORITY_MAX)
            .setFullScreenIntent(pantalla, true)
            .setContentIntent(pantalla)
            .setAutoCancel(true)
            .build()
        publicar(ctx, ID_HITO, n)
    }

    private fun accion(ctx: Context, titulo: String, accion: String, codigo: Int): Notification.Action {
        val pi = PendingIntent.getBroadcast(
            ctx, codigo, Intent(ctx, ReceptorAcciones::class.java).setAction(accion),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        return Notification.Action.Builder(
            Icon.createWithResource(ctx, android.R.drawable.ic_media_pause), titulo, pi
        ).build()
    }

    fun quitarHito(ctx: Context) = nm(ctx).cancel(ID_HITO)

    fun quitarTodo(ctx: Context) {
        nm(ctx).cancel(ID_MARCHA)
        nm(ctx).cancel(ID_HITO)
    }

    @Suppress("DEPRECATION")
    private fun constructor(ctx: Context, canal: String): Notification.Builder =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) Notification.Builder(ctx, canal)
        else Notification.Builder(ctx)

    private fun publicar(ctx: Context, id: Int, n: Notification) {
        try {
            nm(ctx).notify(id, n)
        } catch (e: SecurityException) {
            // Sin permiso de notificaciones: no se puede mostrar.
        }
    }
}
