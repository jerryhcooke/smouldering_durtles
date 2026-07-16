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

import android.content.SharedPreferences;
import android.os.Build;
import android.util.Base64;

import androidx.preference.PreferenceDataStore;
import androidx.preference.PreferenceManager;
import androidx.security.crypto.EncryptedSharedPreferences;
import androidx.security.crypto.MasterKey;

import com.smouldering_durtles.wk.WkApplication;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.spec.AlgorithmParameterSpec;

import javax.annotation.Nullable;
import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import static com.smouldering_durtles.wk.util.ObjectSupport.safe;
import static com.smouldering_durtles.wk.util.ObjectSupport.safeNullable;

/**
 * Wrapper for preference storage that stores values in an encrypted form.
 * The encryption key is stored hardcoded in here, so this is not super-secure. It's mostly
 * intended so a casual inspection of the preferences file doesn't show cleartext values.
 * Otherwise, this just delegates to the system's default shared preferences.
 */
public final class EncryptedPreferenceDataStore extends PreferenceDataStore {
    private @Nullable SharedPreferences encryptedPrefs = null;

    private static final byte[] KEY = {
            20, 15, 42, (byte) 207, (byte) 154, 103, (byte) 247, (byte) 238, (byte) 188, (byte) 253, 6, (byte) 245, 70, 45, (byte) 178, (byte) 201,
            56, (byte) 206, 115, 51, (byte) 147, (byte) 239, (byte) 173, (byte) 131, (byte) 202, 55, (byte) 172, (byte) 133, (byte) 135, 22, 63, 32};

    private static SharedPreferences prefs() {
        return PreferenceManager.getDefaultSharedPreferences(WkApplication.getInstance());
    }

    private SharedPreferences encryptedPrefs() throws IOException, GeneralSecurityException {
        if (encryptedPrefs == null) {
            final MasterKey masterKey = new MasterKey.Builder(WkApplication.getInstance(), MasterKey.DEFAULT_MASTER_KEY_ALIAS)
                    .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                    .build();

            encryptedPrefs = EncryptedSharedPreferences.create(WkApplication.getInstance(),
                    "encrypted_shared_prefs",
                    masterKey,
                    EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                    EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM);
        }
        return encryptedPrefs;
    }

    private static String decrypt(final String encrypted) {
        return safe("", () -> {
            final byte[] iv = { 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0 };
            final AlgorithmParameterSpec ivspec = new IvParameterSpec(iv);
            final Cipher aes = Cipher.getInstance("AES/CBC/PKCS5Padding");
            aes.init(Cipher.DECRYPT_MODE, new SecretKeySpec(KEY, "AES"), ivspec);
            return new String(aes.doFinal(Base64.decode(encrypted, Base64.DEFAULT)), StandardCharsets.UTF_8);
        });
    }

    private @Nullable String getStringImpl(final String key, final @Nullable String defValue) throws Exception {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            final @Nullable String newStoredValue = encryptedPrefs().getString(key, null);
            if (newStoredValue != null) {
                return newStoredValue;
            }

            @Nullable String storedValue = prefs().getString(key, null);
            if (storedValue == null) {
                return defValue;
            }
            if (storedValue.startsWith("enc:")) {
                storedValue = decrypt(storedValue.substring(4));
            }

            final SharedPreferences.Editor editor = encryptedPrefs().edit();
            editor.putString(key, storedValue);
            editor.apply();

            return storedValue;
        }
        else {
            @Nullable String storedValue = prefs().getString(key, null);
            if (storedValue == null) {
                return defValue;
            }
            if (storedValue.startsWith("enc:")) {
                storedValue = decrypt(storedValue.substring(4));
                final SharedPreferences.Editor editor = prefs().edit();
                editor.putString(key, storedValue);
                editor.apply();
            }
            return storedValue;
        }
    }

    @Override
    public @Nullable String getString(final String key, final @Nullable String defValue) {
        return safeNullable(() -> getStringImpl(key, defValue));
    }

    @Override
    public void putString(final String key, final @Nullable String value) {
        safe(() -> {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                final SharedPreferences.Editor editor = encryptedPrefs().edit();
                editor.putString(key, value);
                editor.apply();
            }
            else {
                final SharedPreferences.Editor editor = prefs().edit();
                editor.putString(key, value);
                editor.apply();
            }
        });
    }
}
