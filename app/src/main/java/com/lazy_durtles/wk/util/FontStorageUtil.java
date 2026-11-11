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

package com.lazy_durtles.wk.util;

import android.content.Context;
import android.graphics.Typeface;

import androidx.core.content.ContextCompat;
import androidx.core.content.res.ResourcesCompat;

import com.lazy_durtles.wk.R;
import com.lazy_durtles.wk.WkApplication;
import com.lazy_durtles.wk.model.TypefaceConfiguration;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.Nullable;

import static com.lazy_durtles.wk.Constants.FONTS_DIRECTORY_NAME;

/**
 * Utility class for handling font files stored in local storage.
 */
public final class FontStorageUtil {
    private static final Logger LOGGER = Logger.get(FontStorageUtil.class);

    private static final Map<String, TypefaceConfiguration> CACHE = new HashMap<>();

    private FontStorageUtil() {
        //
    }

    /**
     * Get the file where a font with the specified name is or would be stored.
     *
     * @param fileName the name of the font file
     * @return the File instance for this font, may or may not exist yet
     */
    private static File getFontFile(final String fileName) {
        @Nullable File baseDirectory = ContextCompat.getNoBackupFilesDir(WkApplication.getInstance());
        if (baseDirectory == null) {
            baseDirectory = WkApplication.getInstance().getFilesDir();
        }
        baseDirectory = new File(baseDirectory, FONTS_DIRECTORY_NAME);
        return new File(baseDirectory, fileName);
    }

    /**
     * Make sure the base directory for font files exists in local storage.
     */
    private static void assertBaseDirectoryExists() {
        @Nullable File baseDirectory = ContextCompat.getNoBackupFilesDir(WkApplication.getInstance());
        if (baseDirectory == null) {
            baseDirectory = WkApplication.getInstance().getFilesDir();
        }
        baseDirectory = new File(baseDirectory, FONTS_DIRECTORY_NAME);
        if (!baseDirectory.exists()) {
            //noinspection ResultOfMethodCallIgnored
            baseDirectory.mkdirs();
        }
    }

    /**
     * Determine if a font with a specific name exists.
     *
     * @param fileName the font file name
     * @return true if it exists
     */
    public static boolean hasFontFile(final String fileName) {
        return getFontFile(fileName).exists();
    }

    /**
     * Import a font file from an input stream. Depending on the source
     * chosen by the user, this may be connected to a local file or a network socket.
     *
     * @param source the input source
     * @param fileName the name to store the file as
     * @throws IOException if anything fails during the import
     */
    public static void importFontFile(final InputStream source, final String fileName) throws IOException {
        assertBaseDirectoryExists();
        try (final OutputStream os = new FileOutputStream(getFontFile(fileName))) {
            StreamUtil.pump(source, os);
        }
    }

    /**
     * Remove a font ID from the cache to prevent stale data from hanging around.
     *
     * @param name the name to flush
     */
    public static void flushCache(final String name) {
        CACHE.remove(name);
    }

    /**
     * Load the typeface configuration specified by name, either a filename or a number 1-8.
     *
     * @param name font ID
     * @return the Typeface loaded or null if failed
     */
    public static @Nullable TypefaceConfiguration getTypefaceConfiguration(final String name) {
        final Context context = WkApplication.getInstance();
        if (!CACHE.containsKey(name)) {
            try {
                switch (name) {
                    case "1": {
                        CACHE.put(name, TypefaceConfiguration.DEFAULT);
                        break;
                    }
                    case "2": {
                        final @Nullable Typeface typeface = ResourcesCompat.getFont(context, R.font.sawarabi_mincho_medium);
                        if (typeface != null) {
                            CACHE.put(name, new TypefaceConfiguration(typeface));
                        }
                        break;
                    }
                    case "3": {
                        final @Nullable Typeface typeface = ResourcesCompat.getFont(context, R.font.sawarabi_gothic_medium);
                        if (typeface != null) {
                            CACHE.put(name, new TypefaceConfiguration(typeface));
                        }
                        break;
                    }
                    case "4": {
                        final @Nullable Typeface typeface = ResourcesCompat.getFont(context, R.font.mplus_1p_regular);
                        if (typeface != null) {
                            CACHE.put(name, new TypefaceConfiguration(typeface));
                        }
                        break;
                    }
                    case "5": {
                        final @Nullable Typeface typeface = ResourcesCompat.getFont(context, R.font.kosugi_regular);
                        if (typeface != null) {
                            CACHE.put(name, new TypefaceConfiguration(typeface));
                        }
                        break;
                    }
                    case "6": {
                        final @Nullable Typeface typeface = ResourcesCompat.getFont(context, R.font.kosugi_maru_regular);
                        if (typeface != null) {
                            CACHE.put(name, new TypefaceConfiguration(typeface));
                        }
                        break;
                    }
                    case "7": {
                        final @Nullable Typeface typeface = ResourcesCompat.getFont(context, R.font.otsutomefont_ver3);
                        if (typeface != null) {
                            CACHE.put(name, new TypefaceConfiguration(typeface, 15, 0, 0, 0));
                        }
                        break;
                    }
                    case "8": {
                        final @Nullable Typeface typeface = ResourcesCompat.getFont(context, R.font.gochikakutto);
                        if (typeface != null) {
                            CACHE.put(name, new TypefaceConfiguration(typeface));
                        }
                        break;
                    }
                    default: {
                        final File fontFile = getFontFile(name);
                        @Nullable Typeface typeface = null;
                        if (fontFile.exists()) {
                            typeface = Typeface.createFromFile(fontFile);
                        }
                        if (typeface != null) {
                            CACHE.put(name, new TypefaceConfiguration(typeface));
                        }
                        break;
                    }
                }
            }
            catch (final Exception e) {
                LOGGER.error(e, "Exception loading font file %s", name);
            }
        }
        return CACHE.get(name);
    }

    /**
     * Get the names of all imported font files.
     *
     * @return the list of names
     */
    public static List<String> getNames() {
        final List<String> result = new ArrayList<>();

        try {
            @Nullable File baseDirectory = ContextCompat.getNoBackupFilesDir(WkApplication.getInstance());
            if (baseDirectory == null) {
                baseDirectory = WkApplication.getInstance().getFilesDir();
            }
            baseDirectory = new File(baseDirectory, FONTS_DIRECTORY_NAME);
            @androidx.annotation.Nullable
            final @Nullable String[] names = baseDirectory.list();
            if (names != null) {
                for (final String name: names) {
                    final File fontFile = new File(baseDirectory, name);
                    if (!fontFile.exists() || !fontFile.isFile()) {
                        continue;
                    }
                    result.add(name);
                }
            }
        }
        catch (final Exception e) {
            //
        }

        Collections.sort(result);
        return result;
    }

    /**
     * Remove a font from local storage.
     *
     * @param name the font ID
     */
    public static void deleteFont(final String name) {
        final File file = getFontFile(name);
        if (file.exists()) {
            //noinspection ResultOfMethodCallIgnored
            file.delete();
        }
    }
}
