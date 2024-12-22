
## Smouldering Durtles changelog

### Version 1.1.5 2024-12-22
- You can now stream audio for vocab if not downloaded
- Drawables replaced where WaniKani now use Unicode for radicals
- Donation link added to About page as requested

### Version 1.1.4 2024-12-21

Addition of a link to privacy notice as required by Google now.

### Version 1.1.3 2024-12-21

Removal of USE_EXPLICIT_ALARM permission as required by Google

### Version 1.1.2, 2024-12-21

Thanks to @LotusEcho's work there's now a workaround for WaniKani's API bug!

- A new 'Sync this subject' button is present in the top right menu of every subject item.
- Thanks to @Togtja there is now more compatability with High Contrast Text 
- Thanks to @dlsf there's now soft rejection of mistypes of provided kana characters

### Version 1.1.1, 2024-01-15

More bug fixes, thanks to @hummusw's excellent assistance:

- Restored text to the 'Previous' button
- Restored swiping behaviour in lesson mode
- Realigned the SRS indicator to prevent going out of frame

### Version 1.1.0, 2024-01-12

Just bug fixes in this one:

- Swipe gestures have been removed as these are not compatible with the review timeline in its current form
- Automatic theme switching now working more consistently thanks to @hummusw

### Version 1.0.9, 2023-11-07

New in this version:

- Added haptic feedback options (found under Lesson/Review Settings) for correct and incorrect answers
- Added a monochrome icon option, thanks for the work @the-wright-jamie
- Added a setting for limiting how many levels to show on the progress bars, thanks for the work @Cammymoop

### Version 1.0.8, 2023-10-22

New in this version:

- Added haptic feedback options (found under Lesson/Review Settings) for correct and incorrect answers
- Added a monochrome icon option, thanks for the work @the-wright-jamie
- Added a setting for limiting how many levels to show on the progress bars, thanks for the work @Cammymoop

### Version 1.0.6, 2023-05-27

More bugs squished 👾

New backup and restore feature for your custom settings 📦

- Notifications for Android 13 restored
- Audio buttons for kana-only vocab are in
- Several other small fixes as well!

### Version 1.0.5, 2023-05-24

This update features a manual merge in of later commits from the dev brach of Flaming Durtles that I originally missed - the base code of Smouldering Durtles now contains all those changes combined with the additions I added.

I've added in the requested alternative meanings toast option (thanks @gergelycsegzi) as well as fixing several bugs.

Added a new Nord theme and updated several info pages.

### Version 1.0.4, 2023-05-18

All issues resolved (for now!)

I'm happy to say this fixes the last of the issues, namely:

- Gradient that remained after the advanced dialogs closed is gone
- Color picker now is all in the right order (though an app restart is still required to change fully, as with Flaming Durtles)
- Reset message is fixed

### Version 1.0.3, 2023-05-16

Remaining issues fixed! (except for one)

This release resolves all outstanding issues except for the one present with the overlays in the advanced dialogs!

### Version 1.0.2 (2nd Beta), 2023-05-14

Widgets (and hopefully notifications) fixed

- Resolved issue with PendingIntent immutability
- Added required SCHEDULE_EEXACT_ALARM permission for Android 13
- Added user prompt for permission when toggling on notification on Android 13
- Resolved issue with widgets not updating

### Version 1.0.2 (Beta), 2023-05-12

All Preference Prompts (Theoretically) Fixed

- Search URL and Number Preference preference dialog pop-ups both now work
- Overlay isn't fully dismissed after dialog closed, issue known of and working on

### Version 1.0.1, 2023-05-11

Bug squashing 💥

- Resolved issue with search customising dialogs not appearing (thanks @patarapolw for reporting!)
- Resolved issue with review accounts appearing on the dashboard (thanks @MTLVMPR for reporting!)

## Flaming Durtles changelog

This is a copy of the changelog for Flaming Durtles before the Smouldering Durtles fork.
An entry for version 2.3.1 has been added based off the Google Play release notes.

### Version 2.3.1, 2022-04-18:

- Fix for background tasks interfering with UI updates.

### Version 2.3.0, 2022-04-17:

- Update for Android 12 compatibility (to avoid the app being downrated as obsolete in the Play Store)

### Version 2.2.0, 2020-08-02:

- Split the settings related to the presentation of the subject info dump off into their own category,
  mostly to avoid letting the Display settings grow too large.

- Added animated stroke order diagrams to radicals and kanji. Can be enabled under Settings -> Subject Info.

- Reduce the sensitivity of horizontal swipe gestures, to avoid taps being accidentally interpreted as swipes.

- Added a menu option to go back to the lesson presentation after the lesson quiz has already started.

- Add a little extra delay after playing vocab audio to allow playback to settle before releasing the media player.

- Fix for the 'incorrect answer less than X hours ago' filter not working properly.

- Added subject selection rules to advanced settings. If you're starting a session and there are more candidate items for the session
  than will fit based on your settings, normally the excess is simply chopped off the end after ordering. With these settings you can add rules for
  that selection process to say things like 'at least 10 current-level items' or 'no more than 5 radicals'. This will never reduce the
  session size, it only prioritizes what to pick if there are too many candidates for the session.

### Version 2.1.0, 2020-07-28:

- Android 11 compatibility

- A few small layout fixes

- Add an option to star subjects for search filters and self-study.

- The SRS breakdown boxes on the dashboard are now clickable and will take you to a search result page for that box's category.
  Same for the level progression chart below that.

- Timestamps on the timeline bar chart and on the widget will now use a 12-hour clock or a 24-hour clock depending on device settings.
  12-hour clock timestamps are in short form, i.e. "1pm" or "1.30am" rather than the full "1 p.m." or "1.30 a.m.".

- Add an option to adjust notification updates to happen only for new reviews, once per hour, or continuously whenever
  the data for the notifications changes.

- Improve alarm robustness, making it more likely that notification updates, widget updates and background syncs happen reliably every hour,
  hopefully even when the app is subject to aggressive battery optimization. If you're very careful about battery lifetime, and you have in
  the past excluded FD from battery optimization, you may want to try re-enabling it for FD. Updates should now happen properly
  even with battery optimization enabled, although it will still depend on the specific modifications made by each device manufacturer.
  Unfortunately, misbehaving apps have resulted in Android imposing more and more draconian restrictions that shouldn't be necessary.
  The current implementation is probably the best I can do without FD becoming a massive battery hog.

- Add a mute option to the menu. If enabled, no vocab audio is played at all, under any circumstances.

- FD now requests audio focus when playing vocab audio. This means that if another app (such as a music player) is playing audio at the same
  time, then FD will request that the other app temporarily gives up its audio focus and that it pauses or lowers its volume. And when the
  audio is finished playing, the temporary audio focus is given back. If FD can't get audio focus for some reason, the vocab audio is not played.
  If you don't play music or other media while using FD, this should make no difference. Audio focus can be disabled in settings.

- Star ratings and search presets can be imported and exported with a new action in the settings menu.

- Anki mode colours can now be customized in theme customization as well.

- Added a session log option to the menu. When a session is active, this can be used to get an overview of items in the session,
  their status, and all events related to the session: submitted answers, undos, etc. By default, much of the information on incomplete
  items is hidden, but can be unlocked from advanced settings.

### Version 2.0.0, 2020-07-20:

- Add a hint when answering a reading question incorrectly because of the common niyuu/nyuu にゆう/にゅう mistake.

- Switch the logic for allowing sync/downloads on WiFi only to check for an unmetered connection instead. This makes
  no difference in most situations, but this takes into account the possibility of unmetered connections that are not WiFi,
  and of metered connections that are WiFi (such as hotspots created by another Android device). Basically, if you're using
  a metered WiFi connection, FD will act as if you're on cellular data.

- Add a small drop shadow to text in a few places to increase contrast and readability, especially in the Light theme.

- Hide reading in subject cards if shown during a quiz and reading-related information is suppressed.

- Refactor out some legacy functionality in preparation for Android 11 support.

- Add a temporary, experimental option to change the layout of the subject cards in search results and subject tables.
  This option will only be there temporarily to try out some different layout options, in a future update it will disappear
  or be replaced with something more permanent.

- Add a 'burned less/more than X days ago' filter to the advanced search form.

- On the session summary screen, add a button to bulk-resurrect items in that session that had incorrect answers.

### Version 1.26.0, 2020-07-10:

- Update pitch info data for new vocab subjects

- Fix subject info dump after undo

- Clickable links in about, subject info, etc. work again

- Fix a lesson queue issue that can incorrectly put burned subjects back into the lesson queue

- Fix a color issue when the Post-60 progress bar is shown in reverse

- Option to disable the checkmark / X animation shown when submitting a correct / incorrect answer

- Configurable search engine URLs

- When showing a radical that doesn't have its own character, display the image in all places instead of the name. I've finally
  gotten rid of the last occurrences of the names, except for one: the search preview when you search from the app's toolbar will
  still show the name instead of the image, and that's harder to get rid of for various technical reasons. Maybe I'll revisit that
  at some point, but for now I'm content to just leave that one be.

- The browse screen is now a combined screen to browse subjects by level, search by keywords, and advanced search similar to the
  old self-study quiz filters. Using any of these three search options will show search results, including a sub menu "Search result..."
  where you can do things like save a search preset, bulk resurrect or starting a self-study quiz.

- The search result screen has been overhauled to use a card-based layout and collapsible headers. All subject tables that appear in various
  places in the app have been updated to use the same card-based layout. Radicals and kanji are shown in square-ish blocks, while vocab
  use a wider layout to accomodate for the long vocab titles in the database. Overall, the display is bulkier for vocab items, but more compact
  for radicals and kanji since (on a typical phone screen) they will fit 4 or more to a row. I think the net result is a big improvement.

- When looking at the full subject info dump for a single subject, you can swipe left and right to look through the various subjects
  in the list you navigated from. For example, if you look at the radical 一 (Ground), and tap on the kanji 一 (One) in the 'Used in:' table,
  you can swipe right to see 三, 上 and the other kanji that use the 一 radical.

- Starting a self-study quiz uses the same search form to filter subjects as the advanced search form on the browse/search screen. It's also
  possible to start a self-study quiz based on any of your search presets. The number of items in a self-study quiz can be configured under
  Settings -> Lesson/review.

- Add a few short tutorial views to the browse/search screens. There is now one option in Settings (main page, near the bottom) to reset all
  of the UI confirmations and tutorials.

- Plus a massive amount of behind-the-scenes code cleanup to improve performance, readability and maintainability of the source code.

With this release I have finished everything I wanted to finish before open sourcing the code. I'm going to prep a public repository for that
on GitHub over the next few days. As I've said in the past, I don't have the energy or bandwidth to run FD as an active open source project
with lots of contributors, but if you want to contribute something, you can. Just please make sure to talk to me in advance if you want to
contribute something substantial, to avoid wasting time on something that won't or can't be included.

### Version 1.25.0, 2020-06-26:

- Small bug fixes, layout tweaks, performance improvements, stability improvements.

- Remember edits to study materials when navigating away from the app for a moment. Add ability to reorder synonyms.

- Theme customizations: option to change some dynamic colours in a theme.

- Include old names/mnemonics for radicals from before the December 2018 content update.

- Setting to hide readings in lessons until tapped.

- Split settings for showing the subject info dump in Settings -> Lesson/Review into separate settings for correct and incorrect answers.

- Show a 'Show more' button in the subject info dump for a staged reveal of subject information, going from Nothing to Answers Only to
  Unquizzed Information to Everything.

- Option to show Joyo and JLPT progress on the dashboard.

### Version 1.24.0, 2020-06-18:

- Small layout tweaks, mostly to make things fit better on small-screen devices.

- New converged session screen that combines all of the session workflow. This is mostly behind-the-scenes only, the only visible part
  of this change is that it's possible to have slide animations when moving from one question to another during the session.

- Ability to resurrect and re-burn subjects. To use this, you must put your WK password in the app under API settings.

- Option to set a keyboard language hint, to give keyboards an opportunity to switch layout between reading and meaning questions.
  Only works on Android 7.1 and up, and doesn't always have a noticeable effect.

- Option to fix the height of the main quiz question view. Be careful with this one; the app does not check if the chosen height is
  usable, so setting this too small or too large will cause various layout problems.

- Remove support for the old /srs_stages API endpoint, enable support for multiple SRS systems.

- Do a Wiktionary or Jisho lookup by doing a long press on a subject title.

- More fine-grained notification priority/category settings.

- Add option to briefly disable lightning mode when a meaning answer is 'close enough but not exactly correct'.

Resurrecting (and similarly, burning items that were previously resurrected) is implemented differently from all other API interaction
since the V2 API does not (yet) support resurrecting. This functionality uses a web login and the undocumented interface used by the
web site. The downside is that this interface may randomly break whenever the WK devs update the web site. The UI for resurrecting items
has been completely separated from the rest of the app to make sure that if it breaks, the rest of the app will just continue to function
normally.

To use this functionality, you'll have to put your WK password into the app, under API settings. For now, the only resurrection interface is
to resurrect/burn a single item. If you're looking at the subject info dump on any screen, and the item is eligible to be resurrected/burned,
you'll see an extra button at the very bottom.

I plan to add more convenient resurrect features, such as bulk-resurrecting items in self-study that you have incorrectly answered.
But it's all going to be sideline actions that happen outside of the core workflow of the app, so that a breaking web site change doesn't
cripple the app comnpletely.

### Version 1.23.0, 2020-06-12:

- More fine-grained options for audio autoplay.

- When shuffling an item back into the queue, try to enforce a delay so the item doesn't come back immediately.
  Note that the item can still come back immediately if the session is almost finished or if the ordering settings require it.

- Swipe to navigate in lesson presentation, include (optional) slide animations to make the navigation more visually distinct.

- A small bug fix for accelerated level items (levels 1 and 2).

- Add an option to (not) stretch the quiz question view when keyboard input is active.

### Version 1.22.0, 2020-06-10:

- Lots more refactoring to improve the behind-the-scenes quality of the code, and to be
  better prepared for new app features and changes to WaniKani itself.

- The quiz UI has been overhauled to be more flexible, and will now adjust more dynamically to different display sizes
  and showing/hiding the soft keyboard. There's now also a setting to set the maximum font size for the main
  question text.

- Improve query building for self-study. This doesn't change anything visually, but the self-study should stop
  randomly forgetting its presets. And I'm prepping the self-study to support multiple presets as well.

- Update the pitch info reference data.

- Update libraries and tools to the latest Android SDK versions, in preparation for Android 11 compatibility.

- A few minor UI tweaks to make things look better on various screens.

- Fix the tag colouring in mnemonic text so it doesn't overflow on long spans of text. Also added an option to
  disable this colouring and revert to the old style of bold text.

- Add a support/feedback page to the app, to (hopefully) help users to find the forums or email for support.

- Add more lesson ordering options, and add an option to shuffle session items after the selection for the
  session has been made.

- Remove some no-longer needed locking operations to (hopefully) resolve some performance problems reported by
  a few users.

As part of this update, I have officially decided to drop support for landscape mode on phones, unless you have
a hardware keyboard. Landscape mode has always been a mess on phone-sized displays, since soft keyboards will
typically cover all or nearly all of the screen. I've always tried to make landscape work at least somewhat, but for
phone-sized screens I'm going to stop trying. If landscape mode works for you on a phone, then that's great,
but if you have problems with it you're on your own. Landscape mode is still usable if you have a hardware
keyboard or a tablet.

### Version 1.21.0, 2020-05-29:

- Option to extend or reduce the time covered by the timeline bar chart, including horizontal scrolling if applicable.

- Add background colouring of text in mnemonics. Instead of showing tags like <kanji> and <radical> in the subject info
  as bold text, show the text in the colouring appropriate for the selected theme (as is done on the website).

- Add a 'Black Breeze' theme that is almost identical to 'Dark Breeze', but with fully black backgrounds. This can look
  better on some OLED-type displays. Note: for now only the background is changed. I don't want this theme to drift
  significantly away from Dark Breeze, but let me know if it needs some tweaking.

- Some cleanup of old code after some heavy recent refactoring.

### Version 1.20.0, 2020-05-24:

- Large behind-the-scenes refactoring to prepare for the upcoming multi-SRS features.

- Ask for confirmation before abandoning or wrapping up a session.

- Libraries and tools updated to the latest Android SDK versions.

- Add an app widget that can show the number of available lessons/reviews on the home screen.

- Split the Anki mode settings into separate settings for meanings and readings.

- Configurable font sizes for the subject tables on the dashboard (critical condition, recent unlocks,
  recently burned) and answers in Anki mode.

- Configurable button height for Anki mode buttons.

- Option to swap the correct/incorrect Anki mode buttons.

- Add a keyboard hints notice to the dashboard, to assist new users. Don't worry, it's dismissable.

### Version 1.19.0, 2020-04-13:

- Mark above-level items as locked for SRS breakdown, even if they are unlocked. This is relevant
  for subjects that are moved to a level higher than the user's level after the user has unlocked
  them.

- Small layout tweak to avoid some ugly clipping in the quiz view.

- Now that the V2 API is out of beta, use the WK API's versioning scheme.

- Improve keyboard compatibility with default settings.

- Make showing/hiding of soft keyboard more robust (hopefully).

- Small colour adjustment to the red/green background for correct/incorrect answers in the Dark Breeze theme.

- Big refactor of the ignore/undo buttons:
  - Rename the ignore and undo buttons to "special buttons"
  - Add a third special button in the top left corner
  - Adjust the buttons' labels depending on the chosen behavior
  - Add a 'skip' behaviour as an option, to shuffle an item back into the queue without undo'ing

- Refactor timeline implementation in preparation for timeline with more than 24 hours. Along the way,
  make the text for the number of upcoming reviews more informative.

### Version 1.18.0, 2020-04-05:

- When a lesson/review is done outside of the app and a background sync is done, automatically remove it
  from the current session if it's in there.

- Adjust rendering of the Otsutome font to add some extra padding at the top, to avoid clipping.

- Update build environment for new versions of Android Studio, emulators, tools and libraries.

- A few small workflow adjustments to avoid crashes when tapping through a session very quickly.

- Update Sawarabi Mincho font to the latest version.

- Update reference data for self-study filters and pitch info.

- When scanning for audio files, don't consider hidden/removed subjects as having audio.

- Add an option to time notifications exactly rather than approximately. This consumes more batter power,
  so this option should only be used if you have regular problems with notifications arriving late.

- Slight font size adjustment on the dashboard for narrow displays with large numbers of lessons and/or reviews.

### Version 1.17.7, 2019-11-XX:

- Cosmetic bug fix for reading answers ending in 'n'.

- Remove another possible race condition in session workflow.

- Improvements to session wrapup to reduce UI confusion.

- UI bug fix that can cause question text to become frozen after long-clicking to copy question text.

### Version 1.17.5, 2019-11-17:

- Support text selection for text views that also support clickable links.

- Avoid a crash that can happen for some users when using random font support.

- Hide summary table in subject info dump if no answers are shown.

- After an app crash/restart, resume the saved session on exactly the question where it was left off.

- (Hopefully) prevent the subject info dump from scrolling the screen automatically in the quiz screen.
  (Partial, work in progress.)

- Preserve some UI state when navigating away from the quiz activity, to prevent a few glitches
  like audio playing twice.
  
- Fix a UI workflow issue where the screen is not updated after wrapping up the session.

### Version 1.17.4, 2019-11-11:

- Long clicking on a subject title / quiz question text will allow copying the text to the clipboard.

- Adding more debug logging for investigation of bugs I can't reproduce.

- Bug fix for correct answers being counted as incorrect.

- Heavy armoring against crashes. It should be much more difficult for the app to crash, and instead
  the app will log error messages instead. Many possible errors are silently being ignored now, but at
  least they should leave a more useful debug log now instead of hard-crashing the app.

### Version 1.17.2, 2019-11-10:

- Add an extra spacer at the end of search results to fix a small layout problem.

- New overall progress bar on the dashboard for level 60 users.

- New lesson/review breakdown on the dashboard.

- Add an extra save button for the notes/synonyms screens at the top, and make the notes input boxes auto-size.

- Show a sync reminder if the user passed an item and may have pending unlocks to sync.

Re-done changes from 1.16:

- Set volume control stream so that adjusting the volume from inside the app will adjust media volume by default.

- (Partial) Resume session correctly if the app has been restarted after starting the session.

### Version 1.17.1, 2019-11-06:

- Add logging in session workflow for better diagnostics in the future.

- Add an option to change the scale of the Y-axis grid lines in the timeline bar chart.

- Fix a crash bug that can happen when inputting a space for a meaning answer.

Re-done changes from 1.16:

- Crash protection for sessions with delayed processing - it shouldn't be possible anymore to lose multiple review results in a crash.

- Add option to hide the waterfall line in the timeline chart.

- Add style "next SRS stage" for the timeline bar chart.

### Version 1.17.0, 2019-11-05:

- Revert all changes in 1.16 because of strange problems with incorrect review results.

### Version 1.15.0, 2019-10-27:

- Fix a layout problem in pitch info diagrams

- Performance improvements for subject tables such as in the level browser

- Remake a bunch of views as custom views for more flexibility and performance:
  level progress bars, timeline bar chart, question text/image during quizzes,
  subject title in subject info dump, subject title in subject tables

- Adjust the layout of question text to use available space more efficiently, and
  avoid any more overlap with other UI elements

- Include waterfall line chart on the timeline chart

- Set stream type when playing audio, hopefully avoiding a reported problem where audio
  plays with ringtone volume instead of media volume

- Add focus hints to facilitate hardware keyboard navigation during lesson/review sessions

- Count not-started items separately on the browse level screen

- Highlight the current level on the browse overview screen

- Add a few more Japanese locale hints to hopefully prevent more Chinese character variations from popping up

- When a question is rendered poorly in a custom font, you can tap the question text to revert to the Android default typeface

- Add an option to auto-sync when the app is opened

- Update a few fonts

### Version 1.14.0, 2019-10-20:

- Remove predictive logic option, and settle the logic based on the current API design

- Fix JLPT reference data

- Fix SRS stage filters in self-study for locked and initiate items

- Some behind-the-scenes refactoring to make the code more readable and maintainable

- First phase of pitch info

- Clean up search box behaviour

- Option to show in a kanji reading question whether the required answer is on or kun'yomi

- Clean up the transition when switching themes

### Version 1.13.0, 2019-10-13:

- (Hopefully) prevent a case where the app and the site could get out of sync and not
  get back in sync again.
  
- Fix date parsing which goes wrong on old (pre-Lollipop) Android versions

- Make the entire UI themable, and add a dark theme based on Dark Breeze by Valeth

- Fix a settings bug, where advanced settings can't be disabled once enabled

- Setting to swap the 'visually similar' and 'used in' tables in the subject info dump

- Setting to center the caret in the quiz answer edit box

- Setting to delay the next/submit button briefly after typing in an incorrect answer

### Version 1.12.0, 2019-10-08:

- Performance improvements

- Restore compatibility with Android 4.1 and up

### Version 1.11.0, 2019-10-06:

- Fix a small logic error that would disable the submit button after a shake-and-retry.

- Move debug log to the database.

- Remove resurrected boolean from internal assignment model (this is an internal API field
  that WK is dropping from the API data model, and I'm dropping it to match).

- Some more behind-the-scenes tweaks to reference data handling.

- Move from Levenshtein to Optimal String Alignment for typo lenience, to better match
  the implementation on the WK site.

- Reduce the luminance of the Passed colour for the level progression chart, to make it
  more colourblindness-friendly.
  
- Support multiple readings for vocabulary audio.

- Allow downloading of audio to external storage.

- Option to alternate between male and female voices.

- Migrate to target SDK version 29 (Android 10).

### Version 1.10.0, 2019-09-28:

- Recognize &lt;reading&gt; tag in mnemonics and make them bold.

- Some more crash protection.

- Make font size for the question input text box configurable.

- The pseudo-IME now works better with multi-character input, which should improve compatibility
  with glide/slide/swipe typing.

- Autoplay audio during lesson presentation.

- More efficient handling of reference data for self-study filters (also preparation for
  pitch info and semantic-phonetic composition).
  
- Make the action for a "close enough" meaning answer configurable: accept silently,
  accept with a toast, shake-and-retry or reject.
  
- Remove duplicates from components, amalgamations and visually similar subjects.

- Improve typo lenience for non-letter, non-digit characters, like in "O'clock".

- Setting to adjust font size in subject info dump.

- Add a self-study filter for recently missed items.

- Include reference data (frequency, Joyo grade, JLPT level) in subject info dump.

### Version 1.9.0, 2019-09-21:

- Add label to the timeline bar chart with the number of level-up items in each time slot.

- Add options to adjust the maximum font size in the subject info dump.

- More flexible options to give some items priority in lesson/review sessions.

- Some of the review order options were incorrectly implemented, they've been fixed.

- (Hopefully) resolved some more possible crashes.

- Put component subjects in WK database order instead of by level.

- Fix a layout problem where the play button in the subject info dump could get pushed
  off to the side.
  
- Add an option to import more fonts from downloads or other apps on the device.

### Version 1.8.0, 2019-09-14:

- Big behind the scenes overhaul of nearly all UI code. Should improve performance on
  older devices as well, and allow easier layout updates.

- Fix level progress bar colours to be consistent.

- Use the level progression endpoint to determine level duration.

- Split settings over multiple screens.

- Adjust layout of the top part of subject info to flow better for wide vocab items,
  and with bigger characters for lessons.
  
- Use images instead of characters for radicals 'saw' and 'lion'.

- Try to detect characters that cannot be rendered in a font, and fall back to the
  Android default font for those.

- Add an option to disable the SRS up/downgrade toast.

- Add an option to not show answers immediately after typing an answer.

- Add an option to disable the shake-and-retry on single-kanji vocab where the giving
  reading is incorrect, but is an alternative reading for the kanji.

- Make the behavior of the Ignore and Undo buttons configurable.

- On the browse screen, show numbers of subjects of each type in the level: total,
  locked, in progress, passed, burned.

- At the end of a session, show an overview of correctly and incorrectly answered items.

- Add an option to force an ASCII-capable keyboard for inputting answers.

- Add an option to show a notification when a meaning answer is given that is "close enough"
  but not exactly correct.
  
- On the dashboard, list the number of reviews upcoming in 24h or the time until the
  next reviews if that's more than 24 hours away.

### Version 1.7.0, 2019-09-05:

- Option to show timeline chart as item types instead of SRS stages.

- Pin scrollview for lesson presentation to scroll up when moving to the next lesson.

- Tweak level progress bar colours.

- Undo now puts the last question back into the queue instead of immediately offering it again.

- Show newlines in meaning/reading notes and enforce the 500 character limit on them.

- Add button on audio download screen to delete all audio files.

- Add 'shuffle' option to lesson order.

- Make the quiz logic a bit more robust so that level-up notifications show up more consistently,
  and also on sessions with delayed reporting.

- Disable predictive logic that doesn't work with the current API.

- Fixed a bug that could mess up the display of the level progress bars on the dashboard.

- Show better feedback during long-running sync tasks.

- First stab at supporting multiple fonts (jitai support).

- Set Japanese locale for text views that contain Japanese text - should improve font rendering for
  some devices.

- Add a Submit button on non-Anki quiz screen. Put a Next/Submit button in the lower right as well,
  to improve one-handed navigation.

- Add a quick add-synonym button to the subject info dump.

### Version 1.6.0, 2019-08-31:

- Optional background sync.

- Correct question input hint for meaning questions.

- Put notifications in category 'recommendation' instead of 'reminder'.

- Wrap up session doesn't require completing the current item if it hasn't
  been answered at least once already.

- Squeeze in the SRS up/downgrade toast a little to avoid overlap.

- Show a small up-arrow in the timeline chart over bars that contain items you need to pass to level up.

- Fixed a nasty bug where syncing could get stuck if you unlock new items while offline.

### Version 1.5.0, 2019-08-27:

- Sort recently burned items by burned date instead of unlock date.

- Redo level progress bars to be faster and more accurate (for items that drop back
  down to Apprentice after passing).

- Also show SRS stage up/downgrade toast for changes within Apprentice or within Guru.

- Improve looks of the SRS stage up/downgrade toast.

- Improved SRS stage up/downgrade to match server-side logic better.

- Improved level-up handling (for real this time, I hope...).

- Add setting for low-priority (silent) notifications.

- Add optional SRS indicator during reviews.

- Add subject level to subject info dump.

- When choosing the next question in a session, increase the chances of choosing an item that has already
  had at least one question answered. This keeps the number of items to go when wrapping up a session
  low.

### Version 1.4.0, 2019-08-21:

- Fixed a bug where a level-up wasn't noticed soon enough.

- Show user synonyms in Anki mode.

- More control over what information is shown after an answer has been given.

- Fixed a red-green colour bug.

- Update subject information displayed if the subject details change in the background.

- Filter out whitespace when inputting a reading answer.

- Show a toast when a subject levels up or down in SRS stage.

- Stability: fixed a couple of possible crashes.

### Version 1.3.0, 2019-08-19:

- Add option to hide context sentence translations until tapped.

- Redo the layout of the level progression bar chart.

- Fixed a bug that could cause problems when WaniKani moves subjects to a higher level than
  the user's level.
  
- Limit network access to WiFi if desired. Option to disable audio auto-download.

- Fixed a bug where a level-up wasn't noticed soon enough.

### Version 1.2.0, 2019-08-18:

- Handle vacation mode. If vacation mode is active, disable lessons and reviews and hide the
  timeline bar chart. All other functionalities of the app remain available.

- The Undo button now also works in Anki mode.

- Multiple undo is now possible, as long as the last item hasn't already been reported back
  to the almighty Crabigator.

- Normally the app will try to report a lesson/review item to the almighty Crabigator
  as soon as you've answered both meaning and reading. There is now an option to delay this
  upload until the entire session is finished. (This will also allow unlimited undo.)

- A few tweaks to the layout to make the UI more intuitive, and more usable in landscape
  orientation.
  
- Added an option to disable lightning mode.

### Version 1.1.0, 2019-08-17:

- Added an internal debug log. If you run into problems, you can help me find and fix them
  by sending me your debug log. To do so, go to the settings page, and the opton for that is
  at the bottom of the screen.

- First-time setup improvement: finish the first-time setup process sooner, without waiting for
  background audio downloads.

- Added an option to flush all pending background tasks. This may be useful in case of problems
  with your API key or WK server problems. But be careful: any lessons and reviews that have not
  been synced yet will be lost! You'll have to go online and sync once to get those lost
  lessons/reviews back.

- Added some extra help text in settings to assist users in setting up an API token with the
  right permissions.

- Remove whitespace from the API token before using it, to avoid an accidental space or newline
  sneaking in.

- Fixed a problem where the horizontal level progression bars on the dashboard were not hidden
  properly for passed levels.

- Added an option to allow auto-correct to work while typing answers.

- Added an option "meaning before reading"

- A few layout and colour improvements

