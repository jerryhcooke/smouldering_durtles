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

package com.smouldering_durtles.wk.util;

import android.annotation.TargetApi;
import android.media.AudioAttributes;
import android.media.AudioFocusRequest;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;

import androidx.arch.core.util.Function;
import androidx.core.content.ContextCompat;
import androidx.core.os.EnvironmentCompat;

import com.smouldering_durtles.wk.BuildConfig;
import com.smouldering_durtles.wk.GlobalSettings;
import com.smouldering_durtles.wk.WkApplication;
import com.smouldering_durtles.wk.api.model.PronunciationAudio;
import com.smouldering_durtles.wk.db.AppDatabase;
import com.smouldering_durtles.wk.db.model.PronunciationAudioOwner;
import com.smouldering_durtles.wk.db.model.Subject;
import com.smouldering_durtles.wk.db.model.SubjectPronunciationAudio;
import com.smouldering_durtles.wk.enums.VoicePreference;
import com.smouldering_durtles.wk.livedata.LiveAudioDownloadStatus;
import com.smouldering_durtles.wk.model.GenderedFile;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

import javax.annotation.Nullable;

import static com.smouldering_durtles.wk.Constants.AUDIO_DIRECTORY_NAME;
import static com.smouldering_durtles.wk.enums.VoicePreference.ALTERNATE;
import static com.smouldering_durtles.wk.enums.VoicePreference.FEMALE;
import static com.smouldering_durtles.wk.enums.VoicePreference.MALE;
import static com.smouldering_durtles.wk.util.ObjectSupport.isEmpty;
import static com.smouldering_durtles.wk.util.ObjectSupport.isEqual;
import static com.smouldering_durtles.wk.util.ObjectSupport.safe;
import static com.smouldering_durtles.wk.util.ObjectSupport.shuffle;
import static java.util.Objects.requireNonNull;

/**
 * Utility methods related to pronunciation audio files.
 */
public final class AudioUtil {
    private static final Logger LOGGER = Logger.get(AudioUtil.class);

    /**
     * An unused reference to the most recent MediaPlayer instance. This is kept around so it
     * doesn't get recycled before it is done playing.
     */
    @SuppressWarnings({"unused", "FieldCanBeLocal", "RedundantSuppression"})
    private static @Nullable MediaPlayer savedMediaPlayer = null;

    private static boolean lastWasMale = false;

    private AudioUtil() {
        //
    }

    /**
     * Get the array of external directories available to the app (scoped storage).
     *
     * @return the array of absolute path names, never null, could be empty
     */
    @TargetApi(Build.VERSION_CODES.KITKAT)
    private static File[] getExternalFilesDirs() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            return WkApplication.getInstance().getExternalFilesDirs(null);
        }
        return new File[] {WkApplication.getInstance().getExternalFilesDir(null)};
    }


    /**
     * Get a list of possible audio download locations. The first entry is always "Internal",
     * the rest are filesystem paths for the various available external storage locations.
     *
     * @return the list
     */
    public static List<String> getLocationValues() {
        final List<String> result = new ArrayList<>();

        for (final File file: getExternalFilesDirs()) {
            try {
                result.add(file.getCanonicalPath());
            }
            catch (final Exception e) {
                //
            }
        }

        final String currentLocation = GlobalSettings.Api.getAudioLocation();
        if (!isEmpty(currentLocation) && !result.contains(currentLocation)) {
            result.add(currentLocation);
        }

        result.remove("Internal");
        Collections.sort(result);
        result.add(0, "Internal");

        return result;
    }

    /**
     * Get a list of human-readable audio download locations. Based on the list retrieved
     * from getLocationValues(), these strings have the package-specific part removed for
     * display.
     *
     * @param locationValues the result of getLocationValues()
     * @return the list
     */
    public static List<String> getLocations(final List<String> locationValues) {
        final List<String> result = new ArrayList<>(locationValues);

        for (int i=0; i<result.size(); i++) {
            String location = result.get(i);
            final int p = location.indexOf("/" + BuildConfig.APPLICATION_ID);
            if (p > 0) {
                location = location.substring(0, p);
            }
            result.set(i, location);
        }

        return result;
    }

    /**
     * Get the File instance corresponding to an audio file for the specified audio record.
     * If no such file exists, return null. If the return value is not null, that file is
     * guaranteed to exist, but there are no guarantees it will be readable.
     *
     * @param level the subject's level
     * @param audio the audio record
     * @param locationValues the available storage locations
     * @return the file if it exists
     */
    private static @Nullable GenderedFile getExistingFileForAudio(final int level, final PronunciationAudio audio,
                                                                  final Iterable<String> locationValues) {
        for (final String location: locationValues) {
            final File baseDirectory;
            if (location.equals("Internal")) {
                @Nullable File dir = ContextCompat.getNoBackupFilesDir(WkApplication.getInstance());
                if (dir == null) {
                    dir = WkApplication.getInstance().getFilesDir();
                }
                if (dir == null) {
                    continue;
                }
                baseDirectory = dir;
            }
            else {
                baseDirectory = new File(location);
            }
            final File audioDir = new File(baseDirectory, AUDIO_DIRECTORY_NAME);
            final File levelDir = new File(audioDir, Integer.toString(level));
            final GenderedFile mp3File = new GenderedFile(levelDir, String.format(Locale.ROOT, "%d.mp3", audio.getMetadata().getSourceId()),
                    audio.getMetadata().isMale());
            if (mp3File.exists()) {
                return mp3File;
            }
            final GenderedFile oggFile = new GenderedFile(levelDir, String.format(Locale.ROOT, "%d.ogg", audio.getMetadata().getSourceId()),
                    audio.getMetadata().isMale());
            if (oggFile.exists()) {
                return oggFile;
            }
        }
        return null;
    }

    /**
     * Check if we have any audio file for a subject's audio record.
     *
     * @param level the subject's level
     * @param audio the audio record from the subject
     * @param locationValues the available storage locations
     * @return true if any audio file exists
     */
    public static boolean hasAudioFileFor(final int level, final PronunciationAudio audio, final Iterable<String> locationValues) {
        return getExistingFileForAudio(level, audio, locationValues) != null;
    }

    /**
     * Get the file where an audio file should be stored if it will be stored on internal storage.
     * The result should never be null, but will be null if anything goes wrong finding this location.
     * If this returns a non-null value, the returned file may or may not exist, but its parent
     * directory is guaranteed to exist.
     *
     * @param level the subject's level
     * @param audio the audio record
     * @return the file or null is something went wrong
     */
    private static @Nullable GenderedFile getNewFileForAudioOnInternal(final int level, final PronunciationAudio audio) {
        if (isEmpty(audio.getUrl()) || isEmpty(audio.getContentType()) || audio.getMetadata().getSourceId() <= 0) {
            return null;
        }

        try {
            @Nullable File baseDirectory = ContextCompat.getNoBackupFilesDir(WkApplication.getInstance());
            if (baseDirectory == null) {
                baseDirectory = WkApplication.getInstance().getFilesDir();
            }
            if (baseDirectory == null) {
                return null;
            }
            if (!baseDirectory.exists() && !baseDirectory.mkdirs()) {
                return null;
            }
            baseDirectory = new File(baseDirectory, AUDIO_DIRECTORY_NAME);
            if (!baseDirectory.exists() && !baseDirectory.mkdirs()) {
                return null;
            }

            final File levelDir = new File(baseDirectory, Integer.toString(level));
            if (!levelDir.exists() && !levelDir.mkdirs()) {
                return null;
            }

            if (!levelDir.canWrite()) {
                return null;
            }

            final String extension = audio.getContentType().equals("audio/ogg") ? ".ogg" : ".mp3";
            final String fileName = audio.getMetadata().getSourceId() + extension;
            return new GenderedFile(levelDir, fileName, audio.getMetadata().isMale());
        }
        catch (final Exception e) {
            return null;
        }
    }

    /**
     * Get the file where an audio file should be stored if it will be stored on external storage.
     * The result can be null if the storage isn't mounted or is mounted read-only, or if anything
     * goes wrong finding this location. If this returns a non-null value, the returned file may
     * or may not exist, but its parent directory is guaranteed to exist.
     *
     * @param location the location to store under
     * @param level the subject's level
     * @param audio the audio record
     * @return the file or null is something went wrong
     */
    private static @Nullable GenderedFile getNewFileForAudioOnExternal(final String location, final int level, final PronunciationAudio audio) {
        if (isEmpty(audio.getUrl()) || isEmpty(audio.getContentType()) || audio.getMetadata().getSourceId() <= 0) {
            return null;
        }

        try {
            File baseDirectory = new File(location);
            final @Nullable String status = EnvironmentCompat.getStorageState(baseDirectory);
            if (!Environment.MEDIA_MOUNTED.equals(status)) {
                return null;
            }
            if (!baseDirectory.exists() && !baseDirectory.mkdirs()) {
                return null;
            }
            baseDirectory = new File(baseDirectory, AUDIO_DIRECTORY_NAME);
            if (!baseDirectory.exists() && !baseDirectory.mkdirs()) {
                return null;
            }

            final File levelDir = new File(baseDirectory, Integer.toString(level));
            if (!levelDir.exists() && !levelDir.mkdirs()) {
                return null;
            }

            if (!levelDir.canWrite()) {
                return null;
            }

            final String extension = audio.getContentType().equals("audio/ogg") ? ".ogg" : ".mp3";
            final String fileName = audio.getMetadata().getSourceId() + extension;
            return new GenderedFile(levelDir, fileName, audio.getMetadata().isMale());
        }
        catch (final Exception e) {
            return null;
        }
    }

    /**
     * Get the file where an audio file should be stored, taking into account user storage preferences.
     * The result should never be null, but will be null if anything goes wrong finding this location.
     * If this returns a non-null value, the returned file may or may not exist, but its parent
     * directory is guaranteed to exist. If the user wants audio to be stored on external storage,
     * but that storage is not available or writable, the result file will point to internal storage
     * instead.
     *
     * @param level the subject's level
     * @param audio the audio record
     * @return the file or null is something went wrong
     */
    public static @Nullable GenderedFile getNewFileForAudio(final int level, final PronunciationAudio audio) {
        final String location = GlobalSettings.Api.getAudioLocation();
        if (location.equals("Internal")) {
            return getNewFileForAudioOnInternal(level, audio);
        }
        final @Nullable GenderedFile externalFile = getNewFileForAudioOnExternal(location, level, audio);
        if (externalFile != null) {
            return externalFile;
        }
        return getNewFileForAudioOnInternal(level, audio);
    }

    /**
     * Get the temporary file for downloading a new audio file.
     *
     * @param targetFile the eventual target file this temp file is for
     * @return the file, which may or may not exist
     */
    public static @Nullable File getTempFile(final File targetFile) {
        try {
            final @Nullable File dir = targetFile.getParentFile();
            if (dir == null) {
                return null;
            }
            return new File(dir, "downloading.tmp");
        }
        catch (final Exception e) {
            return null;
        }
    }

    /**
     * Get a random existing audio file, taking into account the user's preference.
     * Prefer the primary reading.
     *
     * @param subject the subject to get audio for
     * @return the file, or null if none exists
     */
    private static @Nullable GenderedFile getOneAudioFile(final Subject subject) {
        final List<PronunciationAudio> shuffled = shuffle(subject.getParsedPronunciationAudios());
        if (shuffled.isEmpty()) {
            return null;
        }

        final VoicePreference voicePreference = GlobalSettings.Audio.getVoicePreference();
        final boolean malePreferred = voicePreference == MALE || voicePreference == ALTERNATE && !lastWasMale;
        final boolean femalePreferred = voicePreference == FEMALE || voicePreference == ALTERNATE && lastWasMale;

        final int level = subject.getLevel();
        final Iterable<String> locationValues = getLocationValues();

        final Comparator<PronunciationAudio> comparator = (o1, o2) -> {
            if (o1 == o2) {
                return 0;
            }
            if (hasAudioFileFor(level, o1, locationValues) && !hasAudioFileFor(level, o2, locationValues)) {
                return -1;
            }
            if (hasAudioFileFor(level, o2, locationValues) && !hasAudioFileFor(level, o1, locationValues)) {
                return 1;
            }
            if (subject.isPrimaryReading(o1.getMetadata().getPronunciation())
                    && !subject.isPrimaryReading(o2.getMetadata().getPronunciation())) {
                return -1;
            }
            if (subject.isPrimaryReading(o2.getMetadata().getPronunciation())
                    && !subject.isPrimaryReading(o1.getMetadata().getPronunciation())) {
                return 1;
            }
            if (malePreferred) {
                if (o1.getMetadata().isMale() && !o2.getMetadata().isMale()) {
                    return -1;
                }
                if (o2.getMetadata().isMale() && !o1.getMetadata().isMale()) {
                    return 1;
                }
            }
            if (femalePreferred) {
                if (o1.getMetadata().isFemale() && !o2.getMetadata().isFemale()) {
                    return -1;
                }
                if (o2.getMetadata().isFemale() && !o1.getMetadata().isFemale()) {
                    return 1;
                }
            }
            return 0;
        };

        Collections.sort(shuffled, comparator);

        return getExistingFileForAudio(level, shuffled.get(0), locationValues);
    }

    /**
     * Get a random existing audio file that must match a specific reading,
     * taking into account the user's preference.
     *
     * @param subject the subject to get audio for
     * @param reading the reading to find audio for
     * @return the file, or null if none exists
     */
    public static @Nullable GenderedFile getOneAudioFileMustMatch(final PronunciationAudioOwner subject,
                                                                  final @Nullable String reading) {
        if (reading == null) {
            return null;
        }

        final List<PronunciationAudio> shuffled = shuffle(subject.getParsedPronunciationAudios());
        if (shuffled.isEmpty()) {
            return null;
        }

        final VoicePreference voicePreference = GlobalSettings.Audio.getVoicePreference();
        final boolean malePreferred = voicePreference == MALE || voicePreference == ALTERNATE && !lastWasMale;
        final boolean femalePreferred = voicePreference == FEMALE || voicePreference == ALTERNATE && lastWasMale;

        final int level = subject.getLevel();
        final Iterable<String> locationValues = getLocationValues();

        final Comparator<PronunciationAudio> comparator = (o1, o2) -> {
            if (o1 == o2) {
                return 0;
            }
            if (hasAudioFileFor(level, o1, locationValues) && !hasAudioFileFor(level, o2, locationValues)) {
                return -1;
            }
            if (hasAudioFileFor(level, o2, locationValues) && !hasAudioFileFor(level, o1, locationValues)) {
                return 1;
            }
            if (isEqual(o1.getMetadata().getPronunciation(), reading) && !isEqual(o2.getMetadata().getPronunciation(), reading)) {
                return -1;
            }
            if (isEqual(o2.getMetadata().getPronunciation(), reading) && !isEqual(o1.getMetadata().getPronunciation(), reading)) {
                return 1;
            }
            if (malePreferred) {
                if (o1.getMetadata().isMale() && !o2.getMetadata().isMale()) {
                    return -1;
                }
                if (o2.getMetadata().isMale() && !o1.getMetadata().isMale()) {
                    return 1;
                }
            }
            if (femalePreferred) {
                if (o1.getMetadata().isFemale() && !o2.getMetadata().isFemale()) {
                    return -1;
                }
                if (o2.getMetadata().isFemale() && !o1.getMetadata().isFemale()) {
                    return 1;
                }
            }
            return 0;
        };

        Collections.sort(shuffled, comparator);

        final PronunciationAudio audio = shuffled.get(0);
        if (isEqual(audio.getMetadata().getPronunciation(), reading)) {
            return getExistingFileForAudio(level, audio, locationValues);
        }

        return null;
    }

    /**
     * Get a random existing audio file that should match a specific reading,
     * taking into account the user's preference. If no match is available, return the best
     * possible option.
     *
     * @param subject the subject to get audio for
     * @param reading the reading to find audio for
     * @return the file, or null if none exists
     */
    public static @Nullable GenderedFile getOneAudioFileShouldMatch(final Subject subject, final @Nullable String reading) {
        final @Nullable GenderedFile file = getOneAudioFileMustMatch(subject, reading);
        return file == null ? getOneAudioFile(subject) : file;
    }

    /**
     * Look for audio files for a subject and assign a number to the overall status
     * of the availability of audio for that subject.
     *
     * <p>
     *     Values for audioDownloadStatus:
     * </p>
     *
     * <ul>
     *     <li>0 - No audio available for this subject</li>
     *     <li>1 - Audio is available, but none have been downloaded yet</li>
     *     <li>2 - Audio is available, and some has been downloaded, but not all of it</li>
     *     <li>3 - Audio is available, and all have been downloaded</li>
     * </ul>
     *
     * @param level The level of the subject
     * @param pronunciationAudios List of audio records for the subject
     * @param locationValues the available storage locations
     * @return The audio download status as defined above
     */
    public static int findAudioDownloadStatus(final int level, final Collection<PronunciationAudio> pronunciationAudios,
                                              final Iterable<String> locationValues) {
        if (pronunciationAudios.isEmpty()) {
            return 0;
        }
        int numPresent = 0;
        int numAbsent = 0;
        for (final PronunciationAudio audio: pronunciationAudios) {
            if (hasAudioFileFor(level, audio, locationValues)) {
                numPresent++;
            }
            else {
                numAbsent++;
            }
        }
        if (numPresent == 0) {
            return 1;
        }
        else if (numAbsent == 0) {
            return 3;
        }
        else {
            return 2;
        }
    }

    /**
     * Update the audio download status for a level.
     *
     * @param level the level
     */
    public static void updateDownloadStatus(final int level) {
        final AppDatabase db = WkApplication.getDatabase();
        final Collection<SubjectPronunciationAudio> subjects = db.subjectViewsDao().getAudioByLevel(level);
        final Iterable<String> locationValues = getLocationValues();

        int numNoAudio = 0;
        int numMissingAudio = 0;
        int numPartialAudio = 0;
        int numFullAudio = 0;
        for (final PronunciationAudioOwner subject: subjects) {
            final int status = findAudioDownloadStatus(level, subject.getParsedPronunciationAudios(), locationValues);
            switch (status) {
                case 1:
                    numMissingAudio++;
                    break;
                case 2:
                    numPartialAudio++;
                    break;
                case 3:
                    numFullAudio++;
                    break;
                case 0:
                default:
                    numNoAudio++;
                    break;
            }
        }

        db.audioDownloadStatusDao().insertOrUpdate(level, subjects.size(), numNoAudio, numMissingAudio, numPartialAudio, numFullAudio);
        LiveAudioDownloadStatus.getInstance().update();
    }

    /**
     * Delete a directory and all of its contents.
     *
     * @param directory the directory to delete
     */
    private static void deleteDirectory(final File directory) {
        try {
            @androidx.annotation.Nullable
            final @Nullable File[] files = directory.listFiles();
            if (files != null) {
                for (final File file : files) {
                    deleteDirectory(file);
                }
            }
            //noinspection ResultOfMethodCallIgnored
            directory.delete();
        }
        catch (final Exception e) {
            //
        }
    }

    /**
     * Delete all audio files.
     */
    public static void deleteAllAudio() {
        @Nullable File baseDirectory = ContextCompat.getNoBackupFilesDir(WkApplication.getInstance());
        if (baseDirectory == null) {
            baseDirectory = WkApplication.getInstance().getFilesDir();
        }
        deleteDirectory(new File(baseDirectory, AUDIO_DIRECTORY_NAME));

        for (final String location: getLocationValues()) {
            if (location.equals("Internal")) {
                continue;
            }
            final File dir = new File(location);
            deleteDirectory(new File(dir, AUDIO_DIRECTORY_NAME));
        }
    }

    @TargetApi(26)
    private static void playAudioPost26(final AudioManager audioManager, final MediaPlayer player, final GenderedFile audioFile,
                                        final boolean useAudioFocus) throws Exception {
        final AudioAttributes attributes = new AudioAttributes.Builder().setContentType(AudioAttributes.CONTENT_TYPE_SPEECH).build();
        player.setAudioAttributes(attributes);
        player.setDataSource(WkApplication.getInstance(), Uri.fromFile(audioFile));
        player.prepare();
        final AudioManager.OnAudioFocusChangeListener listener = focusChange -> {
            try {
                if (focusChange == AudioManager.AUDIOFOCUS_LOSS
                        || focusChange == AudioManager.AUDIOFOCUS_LOSS_TRANSIENT
                        || focusChange == AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK) {
                    player.stop();
                }
            }
            catch (final Exception e) {
                //
            }
        };
        final AudioFocusRequest request = new AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK)
                .setAudioAttributes(attributes)
                .setOnAudioFocusChangeListener(listener)
                .build();
        if (useAudioFocus) {
            final int result = audioManager.requestAudioFocus(request);
            if (result != AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
                return;
            }
        }
        player.setOnCompletionListener(mp -> new Handler(Looper.getMainLooper()).postDelayed(() -> {
            try {
                mp.reset();
            }
            catch (final Exception e) {
                //
            }
            try {
                mp.release();
            }
            catch (final Exception e) {
                //
            }
            try {
                if (useAudioFocus) {
                    audioManager.abandonAudioFocusRequest(request);
                }
            }
            catch (final Exception e) {
                //
            }
        }, 1500));
        player.start();
    }

    @TargetApi(21)
    @SuppressWarnings({"deprecation", "RedundantSuppression"})
    private static void playAudioPost21(final AudioManager audioManager, final MediaPlayer player, final GenderedFile audioFile,
                                        final boolean useAudioFocus) throws Exception {
        player.setAudioAttributes(new AudioAttributes.Builder().setContentType(AudioAttributes.CONTENT_TYPE_SPEECH).build());
        player.setDataSource(WkApplication.getInstance(), Uri.fromFile(audioFile));
        player.prepare();
        final AudioManager.OnAudioFocusChangeListener listener = focusChange -> {
            try {
                if (focusChange == AudioManager.AUDIOFOCUS_LOSS
                        || focusChange == AudioManager.AUDIOFOCUS_LOSS_TRANSIENT
                        || focusChange == AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK) {
                    player.stop();
                }
            }
            catch (final Exception e) {
                //
            }
        };
        if (useAudioFocus) {
            final int result = audioManager.requestAudioFocus(listener, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK);
            if (result != AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
                return;
            }
        }
        player.setOnCompletionListener(mp -> new Handler(Looper.getMainLooper()).postDelayed(() -> {
            try {
                mp.reset();
            }
            catch (final Exception e) {
                //
            }
            try {
                mp.release();
            }
            catch (final Exception e) {
                //
            }
            try {
                if (useAudioFocus) {
                    audioManager.abandonAudioFocus(listener);
                }
            }
            catch (final Exception e) {
                //
            }
        }, 1500));
        player.start();
    }

    @SuppressWarnings({"deprecation", "RedundantSuppression"})
    private static void playAudioPre21(final AudioManager audioManager, final MediaPlayer player, final GenderedFile audioFile,
                                       final boolean useAudioFocus) throws Exception {
        player.setAudioStreamType(AudioManager.STREAM_MUSIC);
        player.setDataSource(WkApplication.getInstance(), Uri.fromFile(audioFile));
        player.prepare();
        final AudioManager.OnAudioFocusChangeListener listener = focusChange -> {
            try {
                if (focusChange == AudioManager.AUDIOFOCUS_LOSS
                        || focusChange == AudioManager.AUDIOFOCUS_LOSS_TRANSIENT
                        || focusChange == AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK) {
                    player.stop();
                }
            }
            catch (final Exception e) {
                //
            }
        };
        if (useAudioFocus) {
            final int result = audioManager.requestAudioFocus(listener, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK);
            if (result != AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
                return;
            }
        }
        player.setOnCompletionListener(mp -> new Handler(Looper.getMainLooper()).postDelayed(() -> {
            try {
                mp.reset();
            }
            catch (final Exception e) {
                //
            }
            try {
                mp.release();
            }
            catch (final Exception e) {
                //
            }
            try {
                if (useAudioFocus) {
                    audioManager.abandonAudioFocus(listener);
                }
            }
            catch (final Exception e) {
                //
            }
        }, 1500));
        player.start();
    }

    /**
     * Play an audio file for a subject. Take into account the user's preferences,
     * and try to get a match for the given reading.
     *
     * @param subject the subject
     * @param lastMatchedAnswer the reading to match if possible
     */
    public static void playAudio(final Subject subject, final @Nullable String lastMatchedAnswer) {
        if (WkApplication.getDatabase().propertiesDao().getIsMuted()) {
            return;
        }

        final @Nullable GenderedFile audioFile = getOneAudioFileShouldMatch(subject, lastMatchedAnswer);
        if (audioFile != null) {
            playLocalAudio(audioFile);
        } else {
            // Fallback to streaming
            final @Nullable String streamingUrl = getStreamingAudioUrl(subject, lastMatchedAnswer);
            if (streamingUrl != null) {
                playStreamingAudio(streamingUrl);
            }
        }
    }

    private static void playLocalAudio(GenderedFile audioFile) {
        safe(() -> {
            final MediaPlayer player = new MediaPlayer();
            savedMediaPlayer = player;

            // Use audioFile directly
            player.setDataSource(audioFile.getAbsolutePath());
            player.prepare();
            player.start();
        });
    }

    private static void playStreamingAudio(String url) {
        safe(() -> {
            final MediaPlayer player = new MediaPlayer();
            savedMediaPlayer = player;

            player.setDataSource(url);
            player.prepare();
            player.start();
        });
    }

    /**
     * Stream an audio file for a subject. Take into account the user's preferences,
     * and try to get a match for the given reading.
     *
     * @param subject the subject
     * @param lastMatchedAnswer the reading to match if possible
     */
    public static @Nullable String getStreamingAudioUrl(Subject subject, @Nullable String lastMatchedAnswer) {
        List<PronunciationAudio> audioList = subject.getParsedPronunciationAudios();
        if (audioList.isEmpty()) {
            return null;
        }

        // Filter based on user preferences or fallback to the first audio
        for (PronunciationAudio audio : audioList) {
            if (lastMatchedAnswer == null || audio.getMetadata().getPronunciation().equals(lastMatchedAnswer)) {
                return audio.getUrl();
            }
        }
        return audioList.get(0).getUrl(); // Default to the first audio if no match
    }

    /**
     * Schedule audio downloads for a set of subjects where needed, with a cap on the number of tasks scheduled.
     *
     * @param subjects the subjects to download for
     * @param maxCount the maximum number of downloads to schedule
     */
    public static void scheduleDownloadTasks(final Iterable<? extends PronunciationAudioOwner> subjects,
                                             final int maxCount) {
        if (GlobalSettings.getFirstTimeSetup() != 0) {
            final AppDatabase db = WkApplication.getDatabase();
            final Iterable<String> locationValues = getLocationValues();

            int count = 0;
            for (final PronunciationAudioOwner subject: subjects) {
                final int status = findAudioDownloadStatus(subject.getLevel(), subject.getParsedPronunciationAudios(), locationValues);
                if (status == 1 || status == 2) {
                    db.assertDownloadAudioTask(subject);
                    count++;
                    if (count >= maxCount) {
                        break;
                    }
                }
            }
        }
    }

    /**
     * Helper method: does the argument directory contain any regular files, recursively checked?.
     *
     * @param dir directory to scan
     * @return true if it does
     */
    private static boolean hasFiles(final File dir) {
        try {
            @androidx.annotation.Nullable
            final @Nullable File[] children = dir.listFiles();
            if (children != null) {
                for (final File child: children) {
                    if (child.isFile()) {
                        return true;
                    }
                    if (child.isDirectory() && hasFiles(child)) {
                        return true;
                    }
                }
            }
            return false;
        }
        catch (final Exception e) {
            return false;
        }
    }

    /**
     * Are there any audio files that are not in the preferred location?.
     *
     * @return true if there are
     */
    public static boolean hasAnyMisplacedAudioFiles() {
        for (final String location: getLocationValues()) {
            if (location.equals(GlobalSettings.Api.getAudioLocation())) {
                continue;
            }
            final File baseDirectory;
            if (location.equals("Internal")) {
                @Nullable File dir = ContextCompat.getNoBackupFilesDir(WkApplication.getInstance());
                if (dir == null) {
                    dir = WkApplication.getInstance().getFilesDir();
                }
                if (dir == null) {
                    continue;
                }
                baseDirectory = dir;
            }
            else {
                baseDirectory = new File(location);
            }
            final File audioDir = new File(baseDirectory, AUDIO_DIRECTORY_NAME);
            if (hasFiles(audioDir)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Helper method: get the number of regular files in the argument directory.
     *
     * @param dir directory to scan
     * @return the number of regular files
     */
    private static int numFiles(final File dir) {
        int count = 0;
        try {
            @androidx.annotation.Nullable
            final @Nullable File[] children = dir.listFiles();
            if (children != null) {
                for (final File child: children) {
                    if (child.isFile()) {
                        count++;
                    }
                    if (child.isDirectory()) {
                        count += numFiles(child);
                    }
                }
            }
        }
        catch (final Exception e) {
            //
        }
        return count;
    }

    /**
     * How many audio files are there that are not in the preferred location?.
     *
     * @return the number of regular files
     */
    public static int getNumMisplacedAudioFiles() {
        int count = 0;
        for (final String location: getLocationValues()) {
            if (location.equals(GlobalSettings.Api.getAudioLocation())) {
                continue;
            }
            final File baseDirectory;
            if (location.equals("Internal")) {
                @Nullable File dir = ContextCompat.getNoBackupFilesDir(WkApplication.getInstance());
                if (dir == null) {
                    dir = WkApplication.getInstance().getFilesDir();
                }
                if (dir == null) {
                    continue;
                }
                baseDirectory = dir;
            }
            else {
                baseDirectory = new File(location);
            }
            final File audioDir = new File(baseDirectory, AUDIO_DIRECTORY_NAME);
            count += numFiles(audioDir);
        }
        return count;
    }

    /**
     * Helper method: iterate over all files in the argument directory, recursively scanned.
     * The consumer is called for every regular file found, and it should return true if the
     * file system walk should be aborted.
     *
     * @param dir the directory to walk
     * @param consumer the consumer for encountered regular files
     * @return true if the walk should be aborted
     */
    private static boolean iterateMisplacedAudioFilesHelper(final File dir, final Function<? super File, Boolean> consumer) {
        try {
            @androidx.annotation.Nullable
            final @Nullable File[] children = dir.listFiles();
            if (children != null) {
                for (final File child: children) {
                    if (child.isFile() && consumer.apply(child)) {
                        return true;
                    }
                    if (child.isDirectory() && iterateMisplacedAudioFilesHelper(child, consumer)) {
                        return true;
                    }
                }
            }
        }
        catch (final Exception e) {
            //
        }
        return false;
    }

    /**
     * Helper method: iterate over all misplaced audio files, recursively scanned.
     * The consumer is called for every regular file found, and it should return true if the
     * file system walk should be aborted.
     *
     * @param consumer the consumer for encountered regular files
     */
    public static void iterateMisplacedAudioFiles(final Function<? super File, Boolean> consumer) {
        for (final String location: getLocationValues()) {
            if (location.equals(GlobalSettings.Api.getAudioLocation())) {
                continue;
            }
            final File baseDirectory;
            if (location.equals("Internal")) {
                @Nullable File dir = ContextCompat.getNoBackupFilesDir(WkApplication.getInstance());
                if (dir == null) {
                    dir = WkApplication.getInstance().getFilesDir();
                }
                if (dir == null) {
                    continue;
                }
                baseDirectory = dir;
            }
            else {
                baseDirectory = new File(location);
            }
            final File audioDir = new File(baseDirectory, AUDIO_DIRECTORY_NAME);
            try {
                iterateMisplacedAudioFilesHelper(audioDir, consumer);
            }
            catch (final Exception e) {
                //
            }
        }
    }

    /**
     * Get the location of the argument file, mapped to the location specified by the argument
     * base directory. The part of the file's path before the AUDIO_DIRECTORY_NAME segment is
     * replaced with the base directory, so that the file's path is mapped to the equivalent
     * path on another storage.
     *
     * @param file the file to map
     * @param baseDirectory the base directory of the storage location to map to
     * @return the mapped file or null if something went wrong
     */
    private static @Nullable File findDestinationFile(final File file, final File baseDirectory) {
        if (file.getName().equals(AUDIO_DIRECTORY_NAME)) {
            return new File(baseDirectory, AUDIO_DIRECTORY_NAME);
        }

        final @Nullable File parent = file.getParentFile();
        if (parent == null) {
            return null;
        }

        final @Nullable File destinationParent = findDestinationFile(parent, baseDirectory);
        if (destinationParent == null) {
            return null;
        }

        return new File(destinationParent, file.getName());
    }

    /**
    * Simple check specifically for kana only vocab audio file checking
    * @param subject the subject to check
    */
    public static boolean hasAudio(final Subject subject) {
        final Iterable<String> locationValues = getLocationValues(); // Assuming you have this method as it's used elsewhere
        final List<PronunciationAudio> audios = subject.getParsedPronunciationAudios(); // Get all audio files related to the subject

        for (PronunciationAudio audio : audios) {
            if (hasAudioFileFor(subject.getLevel(), audio, locationValues)) {
                return true;
            }
        }

        return false;
    }
    /**
     * Move a misplaced audio file to its preferred location.
     *
     * @param file the file to move
     */
    @SuppressWarnings("ResultOfMethodCallIgnored")
    public static void moveToPreferredLocation(final File file) {
        @Nullable InputStream is = null;
        @Nullable OutputStream os = null;
        try {
            final String location = GlobalSettings.Api.getAudioLocation();
            final File baseDirectory;
            if (location.equals("Internal")) {
                @Nullable File dir = ContextCompat.getNoBackupFilesDir(WkApplication.getInstance());
                if (dir == null) {
                    dir = WkApplication.getInstance().getFilesDir();
                }
                if (dir == null) {
                    return;
                }
                baseDirectory = dir;
            }
            else {
                baseDirectory = new File(location);
            }
            final @Nullable File destinationFile = findDestinationFile(file, baseDirectory);
            if (destinationFile == null) {
                return;
            }
            final @Nullable File parent = destinationFile.getParentFile();
            if (parent == null) {
                return;
            }
            if (!parent.exists() && !parent.mkdirs()) {
                return;
            }
            if (destinationFile.exists() && file.length() <= destinationFile.length()) {
                file.delete();
                return;
            }

            LOGGER.info("Moving file %s to %s...", file, destinationFile);

            final File tempFile = requireNonNull(getTempFile(destinationFile));
            is = new FileInputStream(file);
            os = new FileOutputStream(tempFile);
            StreamUtil.pump(is, os);
            is.close();
            is = null;
            os.close();
            os = null;
            if (destinationFile.exists()) {
                destinationFile.delete();
            }
            tempFile.renameTo(destinationFile);
            file.delete();
        }
        catch (final Exception e) {
            //
        }
        finally {
            if (is != null) {
                try {
                    is.close();
                }
                catch (final Exception e) {
                    //
                }
            }
            if (os != null) {
                try {
                    os.close();
                }
                catch (final Exception e) {
                    //
                }
            }
        }
    }
}
