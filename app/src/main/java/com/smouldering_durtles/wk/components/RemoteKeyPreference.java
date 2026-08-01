/*
 * Copyright 2019-2020 Ernst Jan Plugge <rmc@dds.nl>
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.smouldering_durtles.wk.components;

import android.content.Context;
import android.util.AttributeSet;
import android.view.KeyEvent;

import androidx.appcompat.app.AlertDialog;
import androidx.preference.Preference;

import javax.annotation.Nullable;

import static com.smouldering_durtles.wk.util.ObjectSupport.safe;

/**
 * A preference that captures a single key press from an external input device, so that any
 * remote control can be configured without having to know its keycodes in advance.
 *
 * <p>
 *     Clicking the preference opens a dialog that grabs the next key event it sees and stores
 *     the keycode as an int preference.
 * </p>
 */
public final class RemoteKeyPreference extends Preference {
    /**
     * The constructor.
     *
     * @param context Android context
     * @param attrs attributes from XML
     * @param defStyleAttr default style
     * @param defStyleRes default style resource
     */
    @SuppressWarnings({"WeakerAccess", "RedundantSuppression", "unused"})
    public RemoteKeyPreference(final Context context, final AttributeSet attrs, final int defStyleAttr, final int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
    }

    /**
     * The constructor.
     *
     * @param context Android context
     * @param attrs attributes from XML
     * @param defStyleAttr default style
     */
    @SuppressWarnings({"WeakerAccess", "RedundantSuppression", "unused"})
    public RemoteKeyPreference(final Context context, final AttributeSet attrs, final int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    /**
     * The constructor.
     *
     * @param context Android context
     * @param attrs attributes from XML
     */
    @SuppressWarnings({"WeakerAccess", "RedundantSuppression", "unused"})
    public RemoteKeyPreference(final Context context, final AttributeSet attrs) {
        super(context, attrs);
    }

    /**
     * The constructor.
     *
     * @param context Android context
     */
    @SuppressWarnings({"WeakerAccess", "RedundantSuppression", "unused"})
    public RemoteKeyPreference(final Context context) {
        super(context);
    }

    @Override
    public @Nullable CharSequence getSummary() {
        return safe("", () -> {
            final int keyCode = getPersistedKeyCode();
            if (keyCode == KeyEvent.KEYCODE_UNKNOWN) {
                return "Not assigned - tap to record a button press";
            }
            return describeKeyCode(keyCode) + " - tap to change";
        });
    }

    @Override
    protected void onClick() {
        safe(this::showCaptureDialog);
    }

    private void showCaptureDialog() {
        final AlertDialog dialog = new AlertDialog.Builder(getContext())
                .setTitle(getTitle())
                .setMessage("Press the button on your remote that you want to use for this action.")
                .setNegativeButton("Cancel", null)
                .setNeutralButton("Clear", (d, which) -> safe(() -> storeKeyCode(KeyEvent.KEYCODE_UNKNOWN)))
                .create();

        dialog.setOnKeyListener((d, keyCode, event) -> safe(false, () -> {
            // Leave the user a way out that doesn't get recorded as a binding.
            if (keyCode == KeyEvent.KEYCODE_BACK) {
                return false;
            }
            if (event.getAction() != KeyEvent.ACTION_DOWN || event.getRepeatCount() > 0) {
                return true;
            }
            storeKeyCode(keyCode);
            d.dismiss();
            return true;
        }));

        dialog.show();
    }

    private void storeKeyCode(final int keyCode) {
        if (callChangeListener(keyCode)) {
            persistInt(keyCode);
            notifyChanged();
        }
    }

    private int getPersistedKeyCode() {
        return getPersistedInt(defaultKeyCode());
    }

    /**
     * The factory default keycode for this button, matching the defaults in GlobalSettings.
     * These are the keys sent by the common cheap BLE "anki ring" remotes.
     *
     * @return the default keycode
     */
    private int defaultKeyCode() {
        final @Nullable String key = getKey();
        if (key == null) {
            return KeyEvent.KEYCODE_UNKNOWN;
        }
        switch (key) {
            case "remote_control_keycode_1":
                return KeyEvent.KEYCODE_1;
            case "remote_control_keycode_2":
                return KeyEvent.KEYCODE_2;
            case "remote_control_keycode_3":
                return KeyEvent.KEYCODE_4;
            case "remote_control_keycode_4":
                return KeyEvent.KEYCODE_SPACE;
            default:
                return KeyEvent.KEYCODE_UNKNOWN;
        }
    }

    /**
     * Produce a human readable description of a keycode.
     *
     * @param keyCode the keycode
     * @return the description
     */
    private static String describeKeyCode(final int keyCode) {
        final String name = KeyEvent.keyCodeToString(keyCode);
        final String stripped = name.startsWith("KEYCODE_") ? name.substring("KEYCODE_".length()) : name;
        return stripped.replace('_', ' ') + " (" + keyCode + ')';
    }
}
