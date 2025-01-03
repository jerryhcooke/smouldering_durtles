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

import static com.smouldering_durtles.wk.util.ObjectSupport.safe;

import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import androidx.constraintlayout.widget.ConstraintLayout;

import com.smouldering_durtles.wk.GlobalSettings;
import com.smouldering_durtles.wk.R;
import com.smouldering_durtles.wk.db.model.SessionItem;
import com.smouldering_durtles.wk.db.model.Subject;
import com.smouldering_durtles.wk.enums.ActiveTheme;
import com.smouldering_durtles.wk.enums.FragmentTransitionAnimation;
import com.smouldering_durtles.wk.enums.QuestionType;
import com.smouldering_durtles.wk.model.FloatingUiState;
import com.smouldering_durtles.wk.model.Question;
import com.smouldering_durtles.wk.proxy.ViewProxy;
import com.smouldering_durtles.wk.util.AudioUtil;
import com.smouldering_durtles.wk.views.SubjectInfoView;

import javax.annotation.Nullable;

/**
 * Fragment for an Anki mode question.
 */
public final class AnkiSessionFragment extends AbstractSessionFragment {
    private @Nullable Question question = null;
    private @Nullable Subject subject = null;
    private boolean showingAnswer = false;

    private final ViewProxy ankiShowAnswerButton = new ViewProxy();
    private final ViewProxy ankiNextButton = new ViewProxy();
    private final ViewProxy ankiCorrectButton = new ViewProxy();
    private final ViewProxy ankiIncorrectButton = new ViewProxy();
    private final ViewProxy answer = new ViewProxy();
    private final ViewProxy synonyms = new ViewProxy();
    private final ViewProxy subjectInfo = new ViewProxy();
    private final ViewProxy scrollView = new ViewProxy();
    private final ViewProxy specialButton1 = new ViewProxy();
    private final ViewProxy specialButton2 = new ViewProxy();
    private final ViewProxy specialButton3 = new ViewProxy();
    private final ViewProxy questionView = new ViewProxy();
    private final ViewProxy buttonsView = new ViewProxy();

    /**
     * The constructor.
     */
    public AnkiSessionFragment() {
        super(R.layout.fragment_anki);
    }

    /**
     * Create a new instance with arguments set.
     *
     * @param subjectId the subject ID to show
     * @param questionType the type of the question to show
     * @return the fragment
     */
    public static AnkiSessionFragment newInstance(final long subjectId, final QuestionType questionType) {
        final AnkiSessionFragment fragment = new AnkiSessionFragment();

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
        if (session.isAnswered()) {
            ankiNextButton.requestFocus();
        }
        else if (showingAnswer) {
            ankiCorrectButton.requestFocus();
        }
        else {
            ankiShowAnswerButton.requestFocus();
        }
        updateViews();
    }

    @Override
    public void onViewCreatedLocal(final View view, final @Nullable Bundle savedInstanceState) {
        if (question == null || subject == null) {
            return;
        }

        onViewCreatedCommon(view, question, subject, true);

        ankiShowAnswerButton.setDelegate(view, R.id.ankiShowAnswerButton);
        ankiNextButton.setDelegate(view, R.id.ankiNextButton);
        ankiCorrectButton.setDelegate(view, R.id.ankiCorrectButton);
        ankiIncorrectButton.setDelegate(view, R.id.ankiIncorrectButton);
        answer.setDelegate(view, R.id.answer);
        synonyms.setDelegate(view, R.id.synonyms);
        subjectInfo.setDelegate(view, R.id.subjectInfo);
        scrollView.setDelegate(view, R.id.scrollView);
        specialButton1.setDelegate(view, R.id.specialButton1);
        specialButton2.setDelegate(view, R.id.specialButton2);
        specialButton3.setDelegate(view, R.id.specialButton3);
        questionView.setDelegate(view, R.id.questionView);
        buttonsView.setDelegate(view, R.id.buttonsView);

        // Swap the correct/incorrect buttons if the settings ask for it.
        if (GlobalSettings.Display.getSwapAnkiButtons()) {
            safe(() -> {
                final @Nullable ConstraintLayout.LayoutParams correctParams = (ConstraintLayout.LayoutParams) ankiCorrectButton.getLayoutParams();
                final @Nullable ConstraintLayout.LayoutParams incorrectParams = (ConstraintLayout.LayoutParams) ankiIncorrectButton.getLayoutParams();
                if (correctParams != null && incorrectParams != null) {
                    correctParams.startToEnd = ConstraintLayout.LayoutParams.UNSET;
                    correctParams.startToStart = ConstraintLayout.LayoutParams.PARENT_ID;
                    correctParams.endToEnd = ConstraintLayout.LayoutParams.UNSET;
                    correctParams.endToStart = R.id.ankiIncorrectButton;
                    incorrectParams.startToEnd = R.id.ankiCorrectButton;
                    incorrectParams.startToStart = ConstraintLayout.LayoutParams.UNSET;
                    incorrectParams.endToEnd = ConstraintLayout.LayoutParams.PARENT_ID;
                    incorrectParams.endToStart = ConstraintLayout.LayoutParams.UNSET;
                    ankiCorrectButton.setLayoutParams(correctParams);
                    ankiIncorrectButton.setLayoutParams(incorrectParams);
                }
            });
        }

        final @Nullable LinearLayout.LayoutParams qvparams = (LinearLayout.LayoutParams) questionView.getLayoutParams();
        if (qvparams != null) {
            final int height = GlobalSettings.Display.getQuizQuestionViewHeight();
            qvparams.height = height > 0 ? dp2px(height): ViewGroup.LayoutParams.WRAP_CONTENT;
            qvparams.weight = 0;
            questionView.setLayoutParams(qvparams);
        }

        // Resize the Anki buttons as needed
        final @Nullable ViewGroup.LayoutParams params = buttonsView.getLayoutParams();
        if (params != null) {
            params.height = dp2px(GlobalSettings.Display.getAnkiButtonsHeight());
            buttonsView.setLayoutParams(params);
        }

        updateViews();

        ankiShowAnswerButton.setOnClickListener(v -> safe(() -> {
            showingAnswer = true;
            playAudio();
            updateViews();
        }));

        ankiNextButton.setOnClickListener(v -> safe(() -> {
            if (!interactionEnabled) {
                return;
            }
            disableInteraction();
            showingAnswer = false;
            session.advance();
        }));

        ankiCorrectButton.setOnClickListener(v -> safe(() -> {
            if (!interactionEnabled) {
                return;
            }
            disableInteraction();
            showingAnswer = false;
            if (GlobalSettings.getTestMode() && !question.getItem().hasIncorrectAnswers()) {
                session.submitAnkiIncorrect();
            } else {
                session.submitAnkiCorrect();
            }
        }));

        ankiIncorrectButton.setOnClickListener(v -> safe(() -> {
            if (!interactionEnabled) {
                return;
            }
            disableInteraction();
            showingAnswer = false;
            session.submitAnkiIncorrect();
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
            ankiShowAnswerButton.enableInteraction();
            ankiCorrectButton.enableInteraction();
            ankiIncorrectButton.enableInteraction();
            ankiNextButton.enableInteraction();
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
            ankiShowAnswerButton.disableInteraction();
            ankiCorrectButton.disableInteraction();
            ankiIncorrectButton.disableInteraction();
            ankiNextButton.disableInteraction();
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
        hideSoftInput();
    }

    @Override
    public void updateViews() {
        if (question == null || subject == null) {
            return;
        }

        // Show or hide the various Anki buttons
        ankiShowAnswerButton.setVisibility(!session.isAnswered() && !showingAnswer);
        ankiNextButton.setVisibility(session.isAnswered());
        ankiCorrectButton.setVisibility(!session.isAnswered() && showingAnswer);
        ankiIncorrectButton.setVisibility(!session.isAnswered() && showingAnswer);

        ankiShowAnswerButton.setTextColor(ActiveTheme.getAnkiColors()[4]);
        ankiShowAnswerButton.setBackgroundColor(ActiveTheme.getAnkiColors()[0]);
        ankiNextButton.setTextColor(ActiveTheme.getAnkiColors()[4]);
        ankiNextButton.setBackgroundColor(ActiveTheme.getAnkiColors()[1]);
        ankiCorrectButton.setTextColor(ActiveTheme.getAnkiColors()[4]);
        ankiCorrectButton.setBackgroundColor(ActiveTheme.getAnkiColors()[2]);
        ankiIncorrectButton.setTextColor(ActiveTheme.getAnkiColors()[4]);
        ankiIncorrectButton.setBackgroundColor(ActiveTheme.getAnkiColors()[3]);

        answer.setVisibility(showingAnswer);
        answer.setText(question.getAnkiAnswerRichText(subject, session.getType()));
        answer.setTextSize(GlobalSettings.Font.getFontSizeAnkiAnswer());
        answer.setTextColor(ActiveTheme.getAnkiColors()[4]);
        answer.setBackgroundColor(ActiveTheme.getAnkiColors()[5]);
        if (question.getType().isAscii()) {
            answer.setRootLocale();
        }
        if (question.getType().isKana()) {
            answer.setJapaneseLocale();
        }

        synonyms.setVisibility(!session.isAnswered() && showingAnswer && question.getType().isMeaning() && subject.hasMeaningSynonyms());
        synonyms.setText(subject.getMeaningSynonymsRichText());
        synonyms.setTextSize(GlobalSettings.Font.getFontSizeAnkiAnswer());
        synonyms.setTextColor(ActiveTheme.getAnkiColors()[4]);
        synonyms.setBackgroundColor(ActiveTheme.getAnkiColors()[5]);

        // Show or hide the subject info dump
        subjectInfo.setVisibility(session.isAnswered());
        if (session.isAnswered()) {
            subjectInfo.setToolbar(null);
            subjectInfo.setMaxFontSize(GlobalSettings.Font.getMaxFontSizeQuiz());
            subjectInfo.setContainerType(SubjectInfoView.ContainerType.ANSWERED_QUESTION);
            subjectInfo.setSubject(this, subject);
        }

        // Show the special button 1 if allowed and relevant
        specialButton1.setVisibility(GlobalSettings.AdvancedOther.getSpecialButton1Behavior().canShow());
        specialButton1.setText(GlobalSettings.AdvancedOther.getSpecialButton1Behavior().getLabel());

        // Show the special button 2 if allowed and relevant
        specialButton2.setVisibility(GlobalSettings.AdvancedOther.getSpecialButton2Behavior().canShow());
        specialButton2.setText(GlobalSettings.AdvancedOther.getSpecialButton2Behavior().getLabel());

        // Show the special button 3 if allowed and relevant
        specialButton3.setVisibility(GlobalSettings.AdvancedOther.getSpecialButton3Behavior().canShow());
        specialButton3.setText(GlobalSettings.AdvancedOther.getSpecialButton3Behavior().getLabel());
    }

    @Override
    public FragmentTransitionAnimation getAnimation(final AbstractSessionFragment newFragment) {
        if (newFragment.getQuestion() != null && newFragment.getQuestion() != question) {
            return session.getQuestionChoiceReason().getAnimation();
        }
        return FragmentTransitionAnimation.RTL;
    }

    private void playAudio() {
        if (question == null || subject == null) {
            return;
        }

        if (FloatingUiState.audioPlayed) {
            return;
        }

        final boolean autoplay = GlobalSettings.Audio.getAutoplayAnkiReveal();

        if (autoplay && question.getType().isReading()) {
            FloatingUiState.audioPlayed = true;
            AudioUtil.playAudio(subject, null);
        }
    }
}
