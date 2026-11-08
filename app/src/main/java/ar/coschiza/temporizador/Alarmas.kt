package ar.coschiza.temporizador

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.os.Build
import android.os.VibrationAttributes
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager

/** Programa una alarma exacta del sistema por cada hito (funciona con la pantalla apagada). */
object Alarmas {
    private const val MAX_HITOS = 50

    fun puedeProgramar(ctx: Context): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return true
        return ctx.getSystemService(AlarmManager::class.java).canScheduleExactAlarms()
    }

    fun programar(ctx: Context) {
        val am = ctx.getSystemService(AlarmManager::class.java)
        val inicio = Temporizador.inicio(ctx)
        val abrirApp = PendingIntent.getActivity(
            ctx, 0, Intent(ctx, MainActivity::class.java), PendingIntent.FLAG_IMMUTABLE
        )
        Temporizador.hitos(ctx).forEachIndexed { i, hito ->
            val momento = inicio + hito.ms
            if (momento <= System.currentTimeMillis()) return@forEachIndexed
            try {
                am.setAlarmClock(AlarmManager.AlarmClockInfo(momento, abrirApp), intentoAlarma(ctx, i))
            } catch (e: SecurityException) {
                am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, momento, intentoAlarma(ctx, i))
            }
        }
    }

    fun cancelar(ctx: Context) {
        val am = ctx.getSystemService(AlarmManager::class.java)
        for (i in 0 until MAX_HITOS) am.cancel(intentoAlarma(ctx, i))
    }

    private fun intentoAlarma(ctx: Context, indice: Int): PendingIntent =
        PendingIntent.getBroadcast(
            ctx, indice,
            Intent(ctx, ReceptorHito::class.java).putExtra("indice", indice),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

    /** Dos pulsos de vibración, como alarma (para que funcione aun en modo silencio). */
    fun vibrar(ctx: Context) {
        val vibrador: Vibrator = if (Build.VERSION.SDK_INT >= 31)
            ctx.getSystemService(VibratorManager::class.java).defaultVibrator
        else @Suppress("DEPRECATION") ctx.getSystemService(Vibrator::class.java)
        val patron = longArrayOf(0, 500, 250, 500)
        try {
            when {
                Build.VERSION.SDK_INT >= 33 -> vibrador.vibrate(
                    VibrationEffect.createWaveform(patron, -1),
                    VibrationAttributes.createForUsage(VibrationAttributes.USAGE_ALARM),
                )
                else -> @Suppress("DEPRECATION") vibrador.vibrate(
                    VibrationEffect.createWaveform(patron, -1),
                    AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_ALARM).build(),
                )
            }
        } catch (e: Exception) {
            // Si el celular no tiene vibrador, no pasa nada.
        }
    }
}

/** Se ejecuta cuando llega un hito: vibra (si corresponde) y enciende la pantalla con el color. */
class ReceptorHito : BroadcastReceiver() {
    override fun onReceive(ctx: Context, intent: Intent) {
        if (!Temporizador.enMarcha(ctx) || Temporizador.pausado(ctx)) return
        val hito = Temporizador.hitos(ctx).getOrNull(intent.getIntExtra("indice", -1))
        if (hito?.vibrar == true) Alarmas.vibrar(ctx)
        // Si la pantalla de color ya está visible, ella misma cambia de color.
        // Si la app está abierta, la pantalla principal la muestra sola.
        if (ColorActivity.visible || MainActivity.visible) return
        Avisos.mostrarHito(ctx)
    }
}
