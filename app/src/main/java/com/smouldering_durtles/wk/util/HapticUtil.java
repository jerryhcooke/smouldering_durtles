package com.smouldering_durtles.wk.util;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Build;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.view.HapticFeedbackConstants;
import android.view.View;

public final class HapticUtil {
    private HapticUtil() {}

    @SuppressLint("NewApi")
    public static void onCorrect(final View view, final Context context) {
        feedback(view, context, HapticFeedbackConstants.CONFIRM, 100);
    }

    @SuppressLint("NewApi")
    public static void onWrong(final View view, final Context context) {
        feedback(view, context, HapticFeedbackConstants.REJECT, 300);
    }

    @SuppressLint("NewApi")
    public static void onRetry(final View view, final Context context) {
        feedback(view, context, HapticFeedbackConstants.REJECT, 200);
    }

    @SuppressWarnings("deprecation")
    private static void feedback(final View view, final Context context, final int constant, final long fallbackMs) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            view.performHapticFeedback(constant);
        } else {
            final Vibrator vibrator = (Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE);
            if (vibrator != null && vibrator.hasVibrator()) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    vibrator.vibrate(VibrationEffect.createOneShot(fallbackMs, VibrationEffect.DEFAULT_AMPLITUDE));
                } else {
                    vibrator.vibrate(fallbackMs);
                }
            }
        }
    }
}
