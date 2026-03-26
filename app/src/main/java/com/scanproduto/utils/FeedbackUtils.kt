package com.scanproduto.utils

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager

/**
 * Utilitários para feedback sensorial (vibração).
 * Compatível com Android 24+.
 */
object FeedbackUtils {

    /**
     * Vibração curta de sucesso na leitura de barcode (~100ms).
     */
    fun vibrar(context: Context) {
        vibrar(context, 100L)
    }

    /**
     * Vibração dupla de erro (~200ms com pausa).
     */
    fun vibrarErro(context: Context) {
        vibrarPadrao(context, longArrayOf(0, 150, 100, 150))
    }

    /**
     * Vibração por duração específica em milissegundos.
     */
    fun vibrar(context: Context, duracaoMs: Long) {
        val vibrator = obterVibrator(context) ?: return

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator.vibrate(
                VibrationEffect.createOneShot(duracaoMs, VibrationEffect.DEFAULT_AMPLITUDE)
            )
        } else {
            @Suppress("DEPRECATION")
            vibrator.vibrate(duracaoMs)
        }
    }

    /**
     * Vibração com padrão personalizado [espera, vibra, espera, vibra, ...].
     */
    private fun vibrarPadrao(context: Context, pattern: LongArray) {
        val vibrator = obterVibrator(context) ?: return

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator.vibrate(VibrationEffect.createWaveform(pattern, -1))
        } else {
            @Suppress("DEPRECATION")
            vibrator.vibrate(pattern, -1)
        }
    }

    /**
     * Obtém o serviço de vibração compatível com a versão do Android.
     */
    private fun obterVibrator(context: Context): Vibrator? {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val manager =
                context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
            manager?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        }
    }
}
