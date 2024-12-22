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

package com.smouldering_durtles.wk;

/**
 * A bunch of globally available constants that are used all over the place.
 */
public final class Constants {
    /**
     * Fully transparent color, i.e. doesn't show anything at all.
     */
    public static final int TRANSPARENT = 0x00FFFFFF;

    /**
     * Small font size for small text.
     */
    public static final int FONT_SIZE_SMALL = 12;

    /**
     * Normal small-ish font size for most text.
     */
    public static final int FONT_SIZE_NORMAL = 14;

    /**
     * The number of DIPs to reserve for one column in the browse overview screen.
     */
    public static final int BROWSE_OVERVIEW_COLUMN_WIDTH = 40;

    /**
     * The height of the action bar in quiz activity in landscape mode.
     */
    public static final int LANDSCAPE_ACTION_BAR_HEIGHT = 33;

    /**
     * The number of theme customization options offered.
     */
    public static final int NUM_THEME_CUSTOMIZATION_OPTIONS = 33;

    /**
     * Short names of weekdays for timeline bar chart.
     */
    @SuppressWarnings("PublicStaticArrayField")
    public static final String[] WEEKDAY_NAMES = {"", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun"};

    /**
     * Time units in milliseconds: second.
     */
    public static final long SECOND = 1000;

    /**
     * Time units in milliseconds: minute.
     */
    public static final long MINUTE = 60 * SECOND;

    /**
     * Time units in milliseconds: hour.
     */
    public static final long HOUR = 60 * MINUTE;

    /**
     * Time units in milliseconds: day.
     */
    public static final long DAY = 24 * HOUR;

    /**
     * Time units in milliseconds: week.
     */
    public static final long WEEK = 7 * DAY;

    /**
     * Time units in milliseconds: month (30 days).
     */
    public static final long MONTH = 30 * DAY;

    /**
     * Cap for number of search results.
     */
    public static final int MAX_SEARCH_HITS = 250;

    /**
     * Maximum size of the debug log.
     */
    public static final int LOG_FILE_SIZE = 2 * 1024 * 1024;

    /**
     * Version of the static reference data, used to trigger a full reload if needed.
     */
    public static final int REFERENCE_DATA_VERSION = 9;

    /**
     * API version.
     */
    public static final String API_VERSION = "20170710";

    /**
     * HTTP status code for when the API tells us to slow down.
     */
    public static final int HTTP_TOO_MANY_REQUESTS = 429;

    /**
     * HTTP status code for when the API refuses to accept a lesson/review result or another entity.
     */
    public static final int HTTP_UNPROCESSABLE_ENTITY = 422;

    /**
     * Number of tries on an API call before giving up.
     */
    public static final int NUM_API_TRIES = 3;

    /**
     * Delay between API call retries.
     */
    public static final long API_RETRY_DELAY = 5 * SECOND;

    /**
     * The default threshold for when to consider an item to be overdue (i.e. it's been too long since the item's next review became available).
     */
    public static final float DEFAULT_OVERDUE_THRESHOLD = 0.20f;

    /**
     * Subdirectory name for locally stored vocab pronunciation audio files.
     */
    public static final String AUDIO_DIRECTORY_NAME = "pronunciation-audio";

    /**
     * Subdirectory name for locally stored fonts.
     */
    public static final String FONTS_DIRECTORY_NAME = "fonts";

    /**
     * Document to show when the user has not yet provided an API key.
     */
    public static final String NO_API_KEY_HELP_DOCUMENT = "<h3>Welcome to " + Identification.APP_NAME+ " " + BuildConfig.VERSION_NAME + "!</h3><br/> "
            + Identification.APP_NAME + " is an app for <a href=\"https://www.wanikani.com/\">WaniKani</a>, the kanji learning"
            + " service created by <a href=\"https://www.tofugu.com/\">Tofugu</a>.<br/><br/>"
            + " To be able to use this app, you must have an active WaniKani account and a valid API token."
            + " You haven't yet provided a token yet, or it's expired, so please provide one now to continue. <br/><br/> "
            + " The only personal data this app stires is your API key, which is used solely for accessing WaniKani’s service. A full privacy policy is available <a href=\"https://jhcooke.com/sd\">here</a>.<br/><br/> "
            + " If you've not got an account, visit <a href=\"https://www.wanikani.com/\">WaniKani</a> to make one.<br/><br/>"
            + " To create a token, go to your <a href=\"https://www.wanikani.com/settings/personal_access_tokens\">settings page</a>."
            + " Click 'Generate a new token' and when prompted make sure to give the token at least these permissions:<br/><br/>"
            + " - <tt>assignments:start</tt><br/>"
            + " - <tt>reviews:create</tt><br/>"
            + " - <tt>study_materials:create</tt><br/>"
            + " - <tt>study_materials:update</tt><br/><br/>"
            + " Then, copy the token and paste it into the input field above.";

    /**
     * Document to show on the about screen.
     */
    public static final String ABOUT_DOCUMENT = "<h3>Welcome to " + Identification.APP_NAME + " " + BuildConfig.VERSION_NAME + "!</h3><br/> "
            + Identification.APP_NAME + " is an app for <a href=\"https://www.wanikani.com/\">WaniKani</a>, the kanji learning"
            + " service created by <a href=\"https://www.tofugu.com/\">Tofugu</a>.<br/><br/>"
            + " Are you new to WaniKani? Check out their <a href=\"https://knowledge.wanikani.com/\">knowledge base</a>"
            + " for more information.<br/><br/> "
            + Identification.APP_NAME + " was re-coded by " + Identification.AUTHOR_NAME + " (known as Joeni) &lt;<a href=\"mailto:" + Identification.AUTHOR_EMAIL
            + "\">" + Identification.AUTHOR_EMAIL + "</a>&gt; based on the original work of Ernst Jan Plugge who developed the original code. <br/><br/>"
            + "This app will always be free and open source, but if you'd like to support the development, you can do so by <a href='https://ko-fi.com/jerrycooke' target='_blank'>donating</a>. <br/><br/>"
            + "It has been independently developed, and is not supported by, affiliated with, or endorsed by Tofugu.<br/><br/>"
            + Identification.APP_NAME + " has been independently developed, and is not supported by, affiliated with, or endorsed by Tofugu. <br/><br/>"
            + "Tis app uses these Open Source components:<ul>"
            + " <li>&nbsp;<a href=\"https://github.com/FasterXML/jackson\">The Jackson Project</a>"
            + " <li>&nbsp;<a href=\"https://airbnb.io/lottie/\">Lottie</a>"
            + " <li>&nbsp;<a href=\"https://jsoup.org/\">JSoup</a>"
            + " <li>&nbsp;<a href=\"https://square.github.io/okhttp/\">OkHttp</a>"
            + " <li>&nbsp;<a href=\"https://github.com/Madrapps/Pikolo\">Pikolo color picker</a>"
            + " <li>&nbsp;<a href=\"https://github.com/KanjiVG/kanjivg\">KanjiVG</a>"
            + "</ul><br/>"
            + " It was also heavily influenced by:<ul>"
            + " <li>&nbsp;<a href=\"https://community.wanikani.com/t/ios-tsurukame-native-app-with-offline-lessons-and-reviews/30635\">Tsurukame</a>"
            + " <li>&nbsp;<a href=\"https://community.wanikani.com/c/wanikani/api-and-third-party-apps\">The WaniKani Developer Community</a>"
            + "</ul><br/>"
            + "The following freely available fonts are included in unmodified form:<ul>"
            + " <li>&nbsp;Sawarabi Mincho and Gothic, (C) mshio at the <a href=\"https://sawarabi-fonts.osdn.jp/en/\">sawarabi-fonts project on OSDN</a>,"
            + " covered by the <a href=\"https://creativecommons.org/licenses/by/3.0/\">CC Attribution V3.0</a> license.<br/>"
            + " <li>&nbsp;M+ P Type-1, (C) the <a href=\"https://mplus-fonts.osdn.jp/about-en.html\">M+ fonts project on OSDN</a>.<br/>"
            + " <li>&nbsp;<a href=\"https://fonts.google.com/specimen/Kosugi\">Kosugi</a>, created by MOTOYA.<br/>"
            + " <li>&nbsp;<a href=\"https://fonts.google.com/specimen/Kosugi+Maru\">Kosugi Maru</a>, created by MOTOYA.<br/>"
            + " <li>&nbsp;<a href=\"http://rooms.webcrow.jp/font/index.html\">Otsutome</a>.<br/>"
            + " <li>&nbsp;<a href=\"http://pm85122.onamae.jp/851Gkktt.html\">851 Gochikakutto</a>."
            + "</ul>";

    /**
     * Document to show on the support/feedback screen.
     */
    public static final String SUPPORT_DOCUMENT = "<h3>Welcome to " + Identification.APP_NAME + " " + BuildConfig.VERSION_NAME + "!</h3><br/> "
            + " If you are having problems with this app, the best way to get support is to post to the"
            + " <a href=\"" + Identification.SUPPORT_URL + "\">support thread on the forums</a>.<br/><br/>"
            + " On the forums I can most effectively help you resolve your problems, and you can get in touch with other"
            + " users as well.<br/><br/>"
            + " Alternatively, you can send email to"
            + " <a href=\"mailto:" + Identification.AUTHOR_EMAIL + "\">" + Identification.AUTHOR_EMAIL + "</a>.<br/><br/>"
            + " If you are having problems with the WaniKani service itself, check out their"
            + " <a href=\"https://knowledge.wanikani.com/\">knowledge base</a>, you will find answers to a lot of questions there.<br/><br/>"
            + " If you want to leave a review, please feel free to do so"
            + " <a href=\"" + Identification.STORE_URL + "\">on the Play Store</a>. I appreciate all honest feedback, regardless of the score"
            + " you want to give, but keep in mind that a store review is not an effective way to get app issues resolved."
            + " To have your issues addressed, please use the forums or email instead (or in addition to the review).";

    /**
     * The 'here be dragons' warning before allowing the user to change advanced settings.
     */
    public static final String ENABLE_ADVANCED_WARNING = "<b>Warning:</b> Changing advanced settings will alter the presentation"
            + " of lessons and reviews. If used unwisely, this may cause unbalanced workloads or slower progress, and may"
            + " harm your long-term retention. You should <b>only change these settings if you are an experienced user</b> and"
            + " know what you're doing. Are you sure you want to enable these settings?";

    /**
     * Confirmation warning for resetting UI confirmations and tutorials.
     */
    public static final String RESET_TUTORIALS_WARNING = "This will re-enable all UI confirmations and reinstate all tutorial messages."
            + " Are you sure you want to continue?";

    /**
     * Confirmation warning for resetting the database.
     */
    public static final String RESET_DATABASE_WARNING = "<b>Warning:</b> Resetting the database will <b>clear all content</b> from"
            + " the app's database except for your settings. Any work in progress will be immediately lost and all data"
            + " will be re-downloaded from WaniKani. This process may take a little while, depending on your level/device"
            + " Are you sure you want to reset the database?";

    /**
     * Confirmation warning for flushing tasks.
     */
    public static final String FLUSH_TASKS_WARNING = "<b>Warning:</b> Flushing the background tasks will abandon any lessons and reviews that"
            + " have not been synced yet, meaning that as far as WaniKani is concerned, they never happened."
            + " Are you sure you want to flush all background tasks?";

    /**
     * Help text to remind the user the API key must have certain permissions set.
     */
    public static final String API_KEY_PERMISSION_NOTICE = "The API token you supply here <b>must</b> have some"
            + " write-permissions enabled. Without these, some functionality will not work."
            + " Create your API token <a href=\"https://www.wanikani.com/settings/personal_access_tokens\">here</a>.<br/>"
            + " Needed permissions:<ul>"
            + " <li>&nbsp;<tt>assignments:start</tt><br/>"
            + " <li>&nbsp;<tt>reviews:create</tt><br/>"
            + " <li>&nbsp;<tt>study_materials:create</tt><br/>"
            + " <li>&nbsp;<tt>study_materials:update</tt></ul>";

    /**
     * A note about the temporary status of experimental preferences.
     */
    public static final String EXPERIMENTAL_PREFERENCE_STATUS_NOTICE = "<b>These settings are of an experimental and temporary nature.</b>"
            + " They can change arbitrarily at any time, and will at some point disappear completely."
            + " They exist only for short-term experiments in layout and new functionalities."
            + " <b>Don't get attached to them. They will go away.</b>";

    /**
     * A note about subject selection settings.
     */
    public static final String SUBJECT_SELECTION_NOTICE = "If there are more candidates for your session than will fit,"
            + " the settings below will let you influence how the subjects for the session are selected.";

    /**
     * Confirmation warning for deleting downloaded audio.
     */
    public static final String DELETE_AUDIO_WARNING = "<b>Warning:</b> This will delete all audio files that have been downloaded so far.<br/><br/>"
            + " Are you sure you want to continue?";

    /**
     * Body text for keyboard help.
     */
    public static final String KEYBOARD_HELP_DOCUMENT_INTRO = "<h3>Welcome to " + Identification.APP_NAME + "!</h3><br/> "
            + "To get the most out of this app, it's important that your keyboard will work well with it. You can use any keyboard with "
            + Identification.APP_NAME + ", but you may need to tweak a few app settings for your specific keyboard. Please don't hesitate to try "
            + "different keyboard settings.";

    /**
     * Body text for keyboard help.
     */
    public static final String KEYBOARD_HELP_DOCUMENT_1 = "<br/><h4>If you are using Swiftkey</h4><br/> "
            + "Swiftkey has a few annoying bugs. To get Swiftkey to work properly with " + Identification.APP_NAME + ", I recommend these app settings: "
            + "<br/><br/><ul><li>&nbsp;Enable 'visible password' for both meanings and readings "
            + "<li>&nbsp;Don't allow auto-correct "
            + "<li>&nbsp;Disable 'no personalized learning'</ul> ";

    /**
     * Body text for keyboard help.
     */
    public static final String KEYBOARD_HELP_DOCUMENT_2 = "<br/><h4>If your keyboard is giving you inappropriate hints</h4><br/> "
            + "This is probably because of auto-suggest or auto-correct. " + Identification.APP_NAME + " tries to prevent this, "
            + "but some keyboards are buggy and don't respect that. To prevent this hinting, I recommend these settings: "
            + "<br/><br/><ul><li>&nbsp;Enable 'visible password' for both meanings and readings "
            + "<li>&nbsp;Don't allow auto-correct "
            + "<li>&nbsp;Enable 'no personalized learning'</ul> ";

    /**
     * Body text for keyboard help.
     */
    public static final String KEYBOARD_HELP_DOCUMENT_3 = "<br/><h4>If your keyboard offers to auto-fill passwords for you</h4><br/> "
            + "This is because of the 'visible password' setting. It prevents buggy keyboards from giving you hints, but it can also backfire. "
            + "To prevent this auto-fill, I recommend these settings: "
            + "<br/><br/><ul><li>&nbsp;Disable 'visible password' for both meanings and readings</ul>";

    /**
     * Body text for keyboard help.
     */
    public static final String KEYBOARD_HELP_DOCUMENT_4 = "<br/><h4>If you can't use your own language keyboard (Hebrew, Russian, ...)</h4><br/> "
            + "This is probably because " + Identification.APP_NAME + " asks for an ASCII keyboard for meaning questions. "
            + "To fix this, I recommend these settings: "
            + "<br/><br/><ul><li>&nbsp;Disable 'force ASCII keyboard' for meanings</ul>";

    /**
     * Body text for keyboard help.
     */
    public static final String KEYBOARD_HELP_DOCUMENT_5 = "<br/><h4>I want to use my Kana keyboard for readings, and an ASCII keyboard for meanings</h4><br/> "
            + "This is not possible because of Google's security restrictions, but there is a workaround which may work for you: "
            + "<br/><br/>First of all, if you use Android 7.1 or later, you can enable a keyboard language hint in the keyboard settings. "
            + "When combined with the right settings for your keyboard, your keyboard may auto-switch languages. If that doesn't work: "
            + "<br/><br/><ul><li>&nbsp;Install a keyboard that has both an ASCII layout and a Kana layout, such as Gboard "
            + "<li>&nbsp;Enable 'force ASCII keyboard' for meanings, disable it for readings "
            + "<li>&nbsp;Enable 'visible password' for meanings, disable it for readings "
            + "<li>&nbsp;Start a review session "
            + "<li>&nbsp;On your first reading question, change to your Kana layout</ul><br/> "
            + "If the trick works, your keyboard will now automatically switch between Kana and ASCII for the rest of the session. "
            + "This trick doesn't work for every device, and it doesn't work for all keyboards, but it may work for you.";

    /**
     * Document to show on the digraph help screen, part 1.
     */
    public static final String DIGRAPH_HELP_DOCUMENT_1 = "<h3>Digraph information</h3><br/> "
            + "While most kana (hiragana or katakana) stand on their own, it's possible for two kana to combine"
            + " into a single unit called a 'digraph'. When this happens, one of the kana is written small."
            + " The digraph represents a single sound that is distinct from the two sounds represented by the"
            + " kana characters separately.<br/><br/>"
            + "Example: にゆ is typed 'niyu' and pronounced 'ni-yu'. But にゅ is typed 'nyu' and pronounced"
            + " a bit like the English word 'new'. Just leave out the i in ni, and the two sounds are contracted.<br/><br/>"
            + "Another common case is the small つ. This indicates that the following consonant is doubled."
            + " Example: にき is typed 'niki' and pronounced 'ni-ki'. But にっき is typed 'nikki' and pronounced"
            + " with the 'k' lengthened.<br/><br/>"
            + "As a beginner, it can be hard to notice the difference"
            + " between regular and small kana, but over time you will get used to it.<br/><br/>"
            + "A few practical examples:";

    /**
     * Document to show on the digraph help screen, part 2.
     */
    public static final String DIGRAPH_HELP_DOCUMENT_2 =
            "For more information:<br/><br/>"
            + "<a href=\"https://www.tofugu.com/japanese/how-to-type-in-japanese/\">Tofugu - How to type in Japanese</a><br/><br/>"
            + "<a href=\"https://www.tofugu.com/japanese/learn-hiragana/\">Tofugu - Learn Hiragana</a><br/><br/>"
            + "<a href=\"https://www.tofugu.com/japanese/learn-katakana/\">Tofugu - Learn Katakana</a>";

    private Constants() {
        //
    }
}
