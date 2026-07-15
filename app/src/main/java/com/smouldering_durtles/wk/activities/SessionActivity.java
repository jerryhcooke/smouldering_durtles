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

package com.smouldering_durtles.wk.activities;

import android.content.res.Configuration;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.AnimationSet;
import android.view.animation.DecelerateInterpolator;

import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import com.smouldering_durtles.wk.GlobalSettings;
import com.smouldering_durtles.wk.R;
import com.smouldering_durtles.wk.db.model.SessionItem;
import com.smouldering_durtles.wk.db.model.Subject;
import com.smouldering_durtles.wk.enums.CloseEnoughAction;
import com.smouldering_durtles.wk.enums.FragmentTransitionAnimation;
import com.smouldering_durtles.wk.enums.SessionType;
import com.smouldering_durtles.wk.fragments.AbstractFragment;
import com.smouldering_durtles.wk.fragments.AbstractSessionFragment;
import com.smouldering_durtles.wk.livedata.LiveSessionProgress;
import com.smouldering_durtles.wk.livedata.LiveSessionState;
import com.smouldering_durtles.wk.model.FloatingUiState;
import com.smouldering_durtles.wk.model.Question;
import com.smouldering_durtles.wk.model.Session;
import com.smouldering_durtles.wk.proxy.ViewProxy;
import com.smouldering_durtles.wk.util.AudioUtil;
import com.smouldering_durtles.wk.util.Logger;
import com.smouldering_durtles.wk.util.TextUtil;

import javax.annotation.Nullable;

import static com.smouldering_durtles.wk.Constants.LANDSCAPE_ACTION_BAR_HEIGHT;
import static com.smouldering_durtles.wk.util.ObjectSupport.safe;

/**
 * The core activity for session handling. This class combines the full session workflow,
 * from lesson presentation to summary.
 *
 * <p>
 *     This activity heavily relies on the Session singleton for the workflow logic.
 * </p>
 */
public final class SessionActivity extends AbstractActivity {
    private static final Logger LOGGER = Logger.get(SessionActivity.class);

    private final Session session = Session.getInstance();
    private boolean finished = false;

    private final ViewProxy levelUpToastText = new ViewProxy();
    private final ViewProxy closeToastText = new ViewProxy();
    private final ViewProxy closeMessageText = new ViewProxy();
    private final ViewProxy alternativesToastText = new ViewProxy();

    /**
     * The constructor.
     */
    public SessionActivity() {
        super(R.layout.activity_session, R.menu.generic_options_menu);
    }

    @Override
    protected void onCreateLocal(final @Nullable Bundle savedInstanceState) {
        LiveSessionProgress.getInstance().observe(this, t -> safe(() -> {
            new ViewProxy(this, R.id.progress).setText(session.getProgressText());
            if (session.getCurrentQuestion() == null && session.isActive()) {
                LOGGER.info("Current question has been yanked from under our feet - move to next one");
                disableInteraction();
                if (session.isAnswered()) {
                    session.advanceQuietly();
                }
                FloatingUiState.setCurrentAnswer("");
            }
            updateFragment();
        }));

        LiveSessionState.getInstance().observe(this, t -> safe(this::updateFragment));

        // If the device is in landscape mode, make some changes to make the view less tall.
        // The toolbar becomes smaller, and the question type view is hidden, replaced with
        // hint for the question type.
        if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE) {
            final @Nullable Toolbar toolbar = getToolbar();
            if (toolbar != null) {
                toolbar.getLayoutParams().height = dp2px(LANDSCAPE_ACTION_BAR_HEIGHT);
            }
        }

        updateFragment();
    }

    @Override
    protected void onResumeLocal() {
        updateFragment();
    }

    @Override
    protected void onPauseLocal() {
        //
    }

    @Override
    protected void enableInteractionLocal() {
        final @Nullable AbstractFragment fragment = getCurrentFragment();
        if (fragment != null) {
            fragment.enableInteraction();
        }
    }

    @Override
    protected void disableInteractionLocal() {
        final @Nullable AbstractFragment fragment = getCurrentFragment();
        if (fragment != null) {
            fragment.disableInteraction();
        }
    }

    @Override
    protected boolean showWithoutApiKey() {
        return false;
    }

    private void playAudio(final @Nullable Question question, final @Nullable Subject subject, final @Nullable String lastMatchedAnswer) {
        if (FloatingUiState.audioPlayed || question == null || subject == null || !question.getType().isReading()) {
            return;
        }

        if (GlobalSettings.getAutoPlay(session.getType())) {
            FloatingUiState.audioPlayed = true;
            AudioUtil.playAudio(subject, lastMatchedAnswer);
        }
    }

    private void updateFragment() {
        session.chooseQuestion();

        final @Nullable AbstractFragment currentFragment = getCurrentFragment();
        final @Nullable AbstractSessionFragment currentSessionFragment =
                (currentFragment instanceof AbstractSessionFragment) ? (AbstractSessionFragment) currentFragment : null;
        final @Nullable Question question = session.getCurrentQuestion();
        final @Nullable SessionItem item = session.getCurrentItem();
        final @Nullable Subject subject = (item == null) ? null : item.getSubject();

        if (session.isInactive()) {
            if (!finished) {
                finished = true;
                goToMainActivity();
            }
            return;
        }

        final boolean lightningMode = session.isLightningModeActive();
        final boolean ankiMode = question != null && GlobalSettings.getAnkiMode(session.getType(), question.getType());

        if (lightningMode && session.isAnswered() && session.isCorrect()
                && FloatingUiState.lastVerdict != null && FloatingUiState.lastVerdict.isNearMatch()
                && GlobalSettings.Review.getCloseEnoughAction() == CloseEnoughAction.ACCEPT_WITH_TOAST_NO_LM) {
            FloatingUiState.lingerOnAnswer = true;
        }

        if (session.isAnswered() && session.isCorrect()) {
            playAudio(question, subject, ankiMode ? null : FloatingUiState.getLastMatchedAnswer());
            if (lightningMode && !FloatingUiState.lingerOnAnswer) {
                session.advance();
                updateFragment();
                return;
            }
        }

        final AbstractSessionFragment newFragment = session.getNewFragment(currentSessionFragment, ankiMode);
        final FragmentTransitionAnimation animation = (currentSessionFragment == null || currentFragment == newFragment)
                ? FragmentTransitionAnimation.NONE
                : currentSessionFragment.getAnimation(newFragment);

        if (newFragment != currentFragment) {
            collapseSearchBox();
            final FragmentManager manager = getSupportFragmentManager();
            final FragmentTransaction transaction = manager.beginTransaction();
            if (GlobalSettings.Display.getSlideAnimations()) {
                animation.apply(transaction);
            }
            transaction.replace(R.id.fragment, newFragment);
            transaction.commitNow();
        }

        newFragment.showOrHideSoftInput();
        newFragment.updateViews();

        levelUpToastText.setDelegate(this, R.id.levelUpToastText);
        showSrsStageChangeToast(levelUpToastText);

        alternativesToastText.setDelegate(this, R.id.alternativesToastText);
        showAlternativesToast(alternativesToastText);

        final @Nullable Menu menu = getMenu();
        if (menu != null) {
            final @Nullable MenuItem viewLastFinishedItem = menu.findItem(R.id.action_view_last_finished);
            if (viewLastFinishedItem != null) {
                viewLastFinishedItem.setVisible(session.getLastFinishedSubjectId() != -1);
            }

            final @Nullable MenuItem backToPresentationItem = menu.findItem(R.id.action_back_to_presentation);
            if (backToPresentationItem != null) {
                backToPresentationItem.setVisible(session.getType() == SessionType.LESSON && session.isActive());
            }
        }

        if (lightningMode && !FloatingUiState.lingerOnAnswer) {
            closeToastText.setDelegate(this, R.id.closeToastText);
            showCloseToast(closeToastText);
        }
        else {
            closeMessageText.setDelegate(this, R.id.closeMessageText);
            showCloseMessage(closeMessageText);
        }

        updateCurrentSubject();
        enableInteraction();
    }

    private static void showCloseToast(final ViewProxy view) {
        if (FloatingUiState.lastVerdict == null || !FloatingUiState.showCloseToast
                || !GlobalSettings.Review.getCloseEnoughAction().isShowToast()) {
            FloatingUiState.showCloseToast = false;
            return;
        }

        final String message = String.format("Your answer ('%s') was a bit off, the closest correct answer was '%s'. "
                + "Check the meaning to make sure you are correct.",
                FloatingUiState.lastVerdict.getGivenAnswer(),
                FloatingUiState.lastVerdict.getMatchedAnswer());
        view.setText(message);

        final Animation fadeIn = new AlphaAnimation(0, 1);
        fadeIn.setInterpolator(new DecelerateInterpolator());
        fadeIn.setDuration(500);

        final Animation fadeOut = new AlphaAnimation(1, 0);
        fadeOut.setInterpolator(new AccelerateInterpolator());
        fadeOut.setStartOffset(3000);
        fadeOut.setDuration(1500);

        final AnimationSet animationSet = new AnimationSet(false);
        animationSet.addAnimation(fadeIn);
        animationSet.addAnimation(fadeOut);
        animationSet.setRepeatCount(0);

        animationSet.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(final Animation animation) {
                //
            }

            @Override
            public void onAnimationEnd(final Animation animation) {
                view.setVisibility(View.GONE);
                view.clearAnimation();
            }

            @Override
            public void onAnimationRepeat(final Animation animation) {
                view.setVisibility(View.GONE);
                view.clearAnimation();
            }
        });

        view.setAnimation(animationSet);
        view.setVisibility(View.VISIBLE);
        FloatingUiState.showCloseToast = false;
    }

    private static void showCloseMessage(final ViewProxy view) {
        if (FloatingUiState.lastVerdict == null || !FloatingUiState.showCloseToast
                || !GlobalSettings.Review.getCloseEnoughAction().isShowToast()) {
            view.setVisibility(false);
            FloatingUiState.showCloseToast = false;
            return;
        }

        final String message = String.format("Your answer ('%s') was a bit off, the closest correct answer was '%s'. "
                + "Check the meaning to make sure you are correct.",
                FloatingUiState.lastVerdict.getGivenAnswer(),
                FloatingUiState.lastVerdict.getMatchedAnswer());
        view.setText(message);
        view.setVisibility(View.VISIBLE);
        FloatingUiState.showCloseToast = false;
    }

    private void showSrsStageChangeToast(final ViewProxy view) {
        if (!FloatingUiState.showSrsStageChangedToast
                || FloatingUiState.toastOldSrsStage == FloatingUiState.toastNewSrsStage || !Session.getInstance().getType().isSrsRelevant()
                || !GlobalSettings.Review.getEnableSrsToast()) {
            FloatingUiState.showSrsStageChangedToast = false;
            return;
        }
        FloatingUiState.showSrsStageChangedToast = false;

        final String htmlMessage = String.format("<img src=\"*\"/>  %s", FloatingUiState.toastNewSrsStage.getName());
        final CharSequence message = TextUtil.renderHtml(htmlMessage, source -> {
            final int imageId = FloatingUiState.toastNewSrsStage.compareTo(FloatingUiState.toastOldSrsStage) > 0
                    ? R.drawable.ic_arrow_up : R.drawable.ic_arrow_down;
            final @Nullable Drawable img = ContextCompat.getDrawable(this, imageId);
            if (img != null) {
                img.setBounds(0, 0, dp2px(20), dp2px(20));
            }
            return img;
        });
        view.setText(message);

        final Animation fadeIn = new AlphaAnimation(0, 1);
        fadeIn.setInterpolator(new DecelerateInterpolator());
        fadeIn.setDuration(500);

        final Animation fadeOut = new AlphaAnimation(1, 0);
        fadeOut.setInterpolator(new AccelerateInterpolator());
        fadeOut.setStartOffset(750);
        fadeOut.setDuration(1500);

        final AnimationSet animationSet = new AnimationSet(false);
        animationSet.addAnimation(fadeIn);
        animationSet.addAnimation(fadeOut);
        animationSet.setRepeatCount(0);

        animationSet.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(final Animation animation) {
                //
            }

            @Override
            public void onAnimationEnd(final Animation animation) {
                view.setVisibility(View.GONE);
                view.clearAnimation();
            }

            @Override
            public void onAnimationRepeat(final Animation animation) {
                view.setVisibility(View.GONE);
                view.clearAnimation();
            }
        });

        view.setAnimation(animationSet);
        view.setVisibility(View.VISIBLE);
    }

    private static void showAlternativesToast(final ViewProxy view) {
        if (FloatingUiState.lastVerdict == null
                || FloatingUiState.alternativesForLastCorrectAnswer == null
                || !FloatingUiState.showAlternativesToast
                || !GlobalSettings.Review.getEnableAlternativesToast()) {
            FloatingUiState.showAlternativesToast = false;
            return;
        }

        final String message = "Alt: " + FloatingUiState.alternativesForLastCorrectAnswer;
        view.setText(message);

        final Animation fadeIn = new AlphaAnimation(0, 1);
        fadeIn.setInterpolator(new DecelerateInterpolator());
        fadeIn.setDuration(500);

        final Animation fadeOut = new AlphaAnimation(1, 0);
        fadeOut.setInterpolator(new AccelerateInterpolator());
        fadeOut.setStartOffset(750);
        fadeOut.setDuration(1500);

        final AnimationSet animationSet = new AnimationSet(false);
        animationSet.addAnimation(fadeIn);
        animationSet.addAnimation(fadeOut);
        animationSet.setRepeatCount(0);

        animationSet.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(final Animation animation) {
                //
            }

            @Override
            public void onAnimationEnd(final Animation animation) {
                view.setVisibility(View.GONE);
                view.clearAnimation();
            }

            @Override
            public void onAnimationRepeat(final Animation animation) {
                view.setVisibility(View.GONE);
                view.clearAnimation();
            }
        });

        view.setAnimation(animationSet);
        view.setVisibility(View.VISIBLE);
        FloatingUiState.showAlternativesToast = false;
    }
}
