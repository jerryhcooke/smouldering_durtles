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

package com.smouldering_durtles.wk.fragments;

import android.os.Build;
import android.os.Bundle;
import android.text.Editable;
import android.text.InputType;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.TextWatcher;
import android.util.Log;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.CycleInterpolator;
import android.view.animation.TranslateAnimation;
import android.view.inputmethod.EditorInfo;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.smouldering_durtles.wk.GlobalSettings;
import com.smouldering_durtles.wk.R;
import com.smouldering_durtles.wk.WkApplication;
import com.smouldering_durtles.wk.db.model.SessionItem;
import com.smouldering_durtles.wk.db.model.Subject;
import com.smouldering_durtles.wk.enums.FragmentTransitionAnimation;
import com.smouldering_durtles.wk.enums.QuestionType;
import com.smouldering_durtles.wk.model.AnswerVerdict;
import com.smouldering_durtles.wk.model.FloatingUiState;
import com.smouldering_durtles.wk.model.Question;
import com.smouldering_durtles.wk.proxy.ViewProxy;
import com.smouldering_durtles.wk.util.HapticUtil;
import com.smouldering_durtles.wk.util.Logger;
import com.smouldering_durtles.wk.util.PseudoIme;

import java.util.Locale;
import java.util.Objects;

import javax.annotation.Nullable;

import static com.smouldering_durtles.wk.GlobalSettings.Review.enable_haptic_feedback_failure;
import static com.smouldering_durtles.wk.GlobalSettings.Review.enable_haptic_feedback_success;
import static com.smouldering_durtles.wk.util.ObjectSupport.isTrue;
import static com.smouldering_durtles.wk.util.ObjectSupport.orElse;
import static com.smouldering_durtles.wk.util.ObjectSupport.runAsync;
import static com.smouldering_durtles.wk.util.ObjectSupport.safe;

import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

/**
 * Fragment for an unanswered non-Anki mode question.
 */
public final class UnansweredSessionFragment extends AbstractSessionFragment {
    private static final Logger LOGGER = Logger.get(UnansweredSessionFragment.class);

    private @Nullable Question question;
    private @Nullable Subject subject;
    private @Nullable Subject matchingKanji = null;

    private final ViewProxy dontKnowButton = new ViewProxy();
    private final ViewProxy submitButton = new ViewProxy();
    private final ViewProxy specialButton1 = new ViewProxy();
    private final ViewProxy specialButton2 = new ViewProxy();
    private final ViewProxy specialButton3 = new ViewProxy();
    private final ViewProxy questionEdit = new ViewProxy();
    private final ViewProxy questionView = new ViewProxy();
    private final ViewProxy questionEditFrame = new ViewProxy();

    /**
     * The constructor.
     */
    public UnansweredSessionFragment() {
        super(R.layout.fragment_unanswered_question);
    }

    /**
     * Create a new instance with arguments set.
     *
     * @param subjectId the subject ID to show
     * @param questionType the type of the question to show
     * @return the fragment
     */
    public static UnansweredSessionFragment newInstance(final long subjectId, final QuestionType questionType) {
        final UnansweredSessionFragment fragment = new UnansweredSessionFragment();

        final Bundle args = new Bundle();
        args.putLong("subjectId", subjectId);
        args.putString("questionType", questionType.name());

        fragment.setArguments(args);
        return fragment;
    }

    @Override
    protected void onCreateLocal() {
        final @Nullable Bundle args = getArguments();
        if (args != null) {
            final long subjectId = args.getLong("subjectId", -1);
            final @Nullable SessionItem item = session.findItemBySubjectId(subjectId);
            if (item != null) {
                subject = item.getSubject();
                question = item.getQuestionByTypeStr(args.getString("questionType"));
            }
        }
    }

    @Override
    protected void onResumeLocal() {
        questionEdit.requestFocusInTouchMode();
    }

    @Override
    public void onViewCreatedLocal(final View view, final @Nullable Bundle savedInstanceState) {
        if (question == null || subject == null) {
            return;
        }

        final int originalBottomPadding = view.getPaddingBottom();

        ViewCompat.setOnApplyWindowInsetsListener(view, (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            Insets ime = insets.getInsets(WindowInsetsCompat.Type.ime());

            v.setPadding(
                    v.getPaddingLeft(),
                    v.getPaddingTop(),
                    v.getPaddingRight(),
                    originalBottomPadding + Math.max(systemBars.bottom, ime.bottom)
            );

            return insets;
        });

        onViewCreatedCommon(view, question, subject, false);

        dontKnowButton.setDelegate(view, R.id.dontKnowButton);
        submitButton.setDelegate(view, R.id.submitButton);
        specialButton1.setDelegate(view, R.id.specialButton1);
        specialButton2.setDelegate(view, R.id.specialButton2);
        specialButton3.setDelegate(view, R.id.specialButton3);
        questionEdit.setDelegate(view, R.id.questionEdit);
        questionView.setDelegate(view, R.id.questionView);
        questionEditFrame.setDelegate(view, R.id.questionEditFrame);

        questionEdit.setTag(true);
        addTextWatcherToEditText();
        addActionListenerToEditText();

        final @Nullable LinearLayout.LayoutParams params = (LinearLayout.LayoutParams) questionView.getLayoutParams();
        if (params != null) {
            if (GlobalSettings.Display.getStretchQuestionView()) {
                params.height = 0;
                params.weight = 1;
            }
            else {
                final int height = GlobalSettings.Display.getQuizQuestionViewHeight();
                params.height = height > 0 ? dp2px(height): ViewGroup.LayoutParams.WRAP_CONTENT;
                params.weight = 0;
            }
            questionView.setLayoutParams(params);
        }

        matchingKanji = null;
        final String characters = orElse(subject.getCharacters(), "");
        if (subject.getType().isVocabulary() && characters.length() == 1
                && GlobalSettings.AdvancedOther.getShakeOnMatchingKanji()) {
            runAsync(
                    this,
                    () -> WkApplication.getDatabase().subjectDao().getKanjiByCharacters(characters),
                    result -> matchingKanji = result);
        }

        // Show the Don't Know button if needed
        dontKnowButton.setVisibility(GlobalSettings.AdvancedOther.getDontKnowButtonBehavior().canShow());

        if (isLandscape()) {
            questionEditFrame.setBackgroundColor(question.getType().getHintColor());
        }

        questionEdit.setTextSize(GlobalSettings.Font.getFontSizeQuestionEdit());
        questionEdit.setHint(question.getHint(isLandscape()));

        int imeOptions = EditorInfo.IME_ACTION_UNSPECIFIED | EditorInfo.IME_FLAG_NO_FULLSCREEN;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && GlobalSettings.Keyboard.getEnableNoLearning()) {
            imeOptions |= EditorInfo.IME_FLAG_NO_PERSONALIZED_LEARNING;
        }
        if (question.getType().isAscii() && GlobalSettings.Keyboard.getForceAsciiMeaning()) {
            imeOptions |= EditorInfo.IME_FLAG_FORCE_ASCII;
        }
        else if (question.getType().isKana() && GlobalSettings.Keyboard.getForceAsciiReading()) {
            imeOptions |= EditorInfo.IME_FLAG_FORCE_ASCII;
        }
        questionEdit.setImeOptions(imeOptions);

        int inputType = InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS;
        if (GlobalSettings.Keyboard.getEnableAutoCorrectMeaning() && question.getType().isAscii()) {
            inputType |= InputType.TYPE_TEXT_FLAG_AUTO_CORRECT;
        }
        else if (GlobalSettings.Keyboard.getEnableAutoCorrectReading() && question.getType().isKana()) {
            inputType |= InputType.TYPE_TEXT_FLAG_AUTO_CORRECT;
        }
        if (GlobalSettings.Keyboard.getForceVisiblePasswordMeaning() && question.getType().isAscii()) {
            inputType |= InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD;
        }
        else if (GlobalSettings.Keyboard.getForceVisiblePasswordReading() && question.getType().isKana()) {
            inputType |= InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD;
        }
        questionEdit.setInputType(inputType);

        if (GlobalSettings.Keyboard.getImeHintMeaning() || GlobalSettings.Keyboard.getImeHintReading()) {
            if (question.getType().isAscii()) {
                if (GlobalSettings.Keyboard.getImeHintMeaning()) {
                    questionEdit.setImeHintLocales(Locale.ENGLISH);
                }
                else {
                    questionEdit.setImeHintLocales();
                }
            }
            if (question.getType().isKana()) {
                if (GlobalSettings.Keyboard.getImeHintReading()) {
                    questionEdit.setImeHintLocales(Locale.JAPANESE);
                }
                else {
                    questionEdit.setImeHintLocales();
                }
            }
        }

        if (GlobalSettings.Display.getCenterCaret()) {
            questionEdit.setGravity(Gravity.CENTER_HORIZONTAL);
        }
        questionEdit.setMinEms(10);
        questionEdit.setSingleLine();
        questionEdit.setContentDescription(question.getTitle());
        questionEdit.setText(FloatingUiState.getCurrentAnswer());
        if (question.getType().isAscii()) {
            questionEdit.setRootLocale();
        }
        if (question.getType().isKana()) {
            questionEdit.setJapaneseLocale();
        }

        if (isLandscape()) {
            questionEditFrame.setBackgroundColor(question.getType().getHintColor());
            questionEdit.setBackgroundColor(question.getType().getHintColor());
            questionEdit.setTextColor(question.getType().getHintContrastColor());
            questionEdit.setHintTextColor(question.getType().getEditTextHintColor());
        }

        dontKnowButton.setOnClickListener(v -> safe(() -> {
            if (!interactionEnabled) {
                return;
            }
            if (!session.isAnswered()) {
                disableInteraction();
                questionEdit.setTag(false);
                session.submitDontKnow();
            }
        }));

        submitButton.setOnClickListener(v -> safe(() -> {
            if (!interactionEnabled) {
                return;
            }
            disableInteraction();
            questionEdit.setTag(false);
            FloatingUiState.setCurrentAnswer(questionEdit.getText());
            LOGGER.info("Submitting answer '%s' for subject '%s' (%d) via submit button",
                    FloatingUiState.getCurrentAnswer(), subject.getCharacters(), subject.getId());
            final AnswerVerdict verdict = session.submit(matchingKanji);
            if (verdict.isRetry()) {
                Log.d("HapticFeedback", "Retry verdict received");
                final TranslateAnimation shake = new TranslateAnimation(0, 10, 0, 0);
                shake.setDuration(1000);
                shake.setInterpolator(new CycleInterpolator(7));
                questionEdit.startAnimation(shake);
                enableInteraction();
                questionEdit.setTag(true);
                questionEdit.requestFocusInTouchMode();
                showSoftInput(questionEdit);
                if (enable_haptic_feedback_failure()) {
                    Log.d("HapticFeedback", "Vibing");
                    HapticUtil.onWrong(requireView(), requireContext());
                }
            }
        }));
    }

    @Override
    public String getToolbarTitle() {
        return question == null ? "" : question.getTitle();
    }

    @Override
    public int getToolbarBackgroundColor() {
        return subject == null ? 0 : subject.getBackgroundColor();
    }

    @Override
    public void enableInteraction() {
        safe(() -> {
            dontKnowButton.enableInteraction();
            submitButton.enableInteraction();
            specialButton1.enableInteraction();
            specialButton2.enableInteraction();
            specialButton3.enableInteraction();
            interactionEnabled = true;
        });
    }

    @Override
    public void disableInteraction() {
        safe(() -> {
            interactionEnabled = false;
            dontKnowButton.disableInteraction();
            submitButton.disableInteraction();
            specialButton1.disableInteraction();
            specialButton2.disableInteraction();
            specialButton3.disableInteraction();
        });
    }

    @SuppressWarnings("SuspiciousGetterSetter")
    @Override
    public Subject getCurrentSubject() {
        return subject;
    }

    @Override
    public @Nullable SessionItem getItem() {
        return question == null ? null : question.getItem();
    }

    @Override
    public @Nullable Question getQuestion() {
        return question;
    }

    @Override
    public @Nullable Subject getSubject() {
        return subject;
    }

    @Override
    public void showOrHideSoftInput() {
        showSoftInput(questionEdit);
    }

    @Override
    public void updateViews() {
        //
    }

    @Override
    public FragmentTransitionAnimation getAnimation(final AbstractSessionFragment newFragment) {
        if (newFragment instanceof AnsweredSessionFragment && question == newFragment.getQuestion()) {
            return FragmentTransitionAnimation.NONE;
        }
        if (newFragment.getQuestion() != null && newFragment.getQuestion() != question) {
            return session.getQuestionChoiceReason().getAnimation();
        }
        return FragmentTransitionAnimation.RTL;
    }

    private void addTextWatcherToEditText() {
        if (question == null || subject == null) {
            return;
        }

        if (questionEdit.getTag(R.id.textWatcherAdded) != null) {
            return;
        }

        final TextWatcher textWatcher = new TextWatcher() {
            private SpannableString spannable = new SpannableString("");

            @Override
            public void beforeTextChanged(final CharSequence s, final int start, final int count, final int after) {
                //
            }

            @Override
            public void onTextChanged(final CharSequence s, final int start, final int before, final int count) {
                safe(() -> {
                    final boolean active = isTrue(questionEdit.getTag());
                    if (!active) {
                        return;
                    }
                    FloatingUiState.setCurrentAnswer(s.toString());
                    if (question.getType().isKana()) {
                        spannable = new SpannableString(s);
                        spannable.setSpan(this, start, start + count, Spanned.SPAN_COMPOSING);
                    }
                });
            }

            @Override
            public void afterTextChanged(final Editable s) {
                safe(() -> {
                    final boolean active = isTrue(questionEdit.getTag());
                    if (!active) {
                        return;
                    }
                    if (question.getType().isKana()) {
                        final int beginIndex = spannable.getSpanStart(this);
                        final int endIndex = spannable.getSpanEnd(this);
                        spannable.removeSpan(this);
                        PseudoIme.fixup(s, beginIndex, endIndex);
                    }
                });
            }
        };

        questionEdit.addTextChangedListener(textWatcher);
        questionEdit.setTag(R.id.textWatcherAdded, new Object());
    }

    private boolean handleEditorAction(final int actionId, final @Nullable KeyEvent event) {
        if (question == null || subject == null) {
            return false;
        }

        final boolean active = isTrue(questionEdit.getTag());
        if (!active) {
            return false;
        }
        boolean ok = event == null && actionId != 0;
        if (event != null && event.getKeyCode() == KeyEvent.KEYCODE_ENTER && event.getAction() == KeyEvent.ACTION_DOWN) {
            ok = true;
        }
        if (event != null && event.getKeyCode() == KeyEvent.KEYCODE_ENTER && event.getAction() == KeyEvent.ACTION_UP) {
            return true;
        }


        if (ok && interactionEnabled) {
            if (!session.isAnswered()) {
                questionEdit.setTag(false);
                final @Nullable Subject match = matchingKanji;
                FloatingUiState.setCurrentAnswer(questionEdit.getText());
                LOGGER.info("Submitting answer '%s' for subject '%s' (%d) via keyboard action",
                        FloatingUiState.getCurrentAnswer(), subject.getCharacters(), subject.getId());
                final AnswerVerdict verdict = session.submit(match);

                if (verdict.isOk() && enable_haptic_feedback_success()) {
                    HapticUtil.onCorrect(requireView(), requireContext());
                }

                if (verdict.isRetry()) {
                    Log.d("HapticFeedback", "A Retry verdict received");
                    final TranslateAnimation shake = new TranslateAnimation(0, 10, 0, 0);
                    shake.setDuration(1000);
                    shake.setInterpolator(new CycleInterpolator(7));
                    questionEdit.startAnimation(shake);
                    questionEdit.setTag(true);
                    if (enable_haptic_feedback_failure()) {
                        Log.d("HapticFeedback", "Vibing");
                        HapticUtil.onRetry(requireView(), requireContext());
                    }
                }
            }
            return true;
        }

        return false;



    }

    private void addActionListenerToEditText() {
        final TextView.OnEditorActionListener onEditorActionListener = (v, actionId, event) ->
                safe(false, () -> handleEditorAction(actionId, event));

        questionEdit.setOnEditorActionListener(onEditorActionListener);
    }
}
