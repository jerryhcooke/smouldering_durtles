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

import android.app.Activity;
import android.content.Context;
import android.content.res.Configuration;
import android.os.Bundle;
import android.view.View;
import android.view.ViewTreeObserver;
import android.view.inputmethod.InputMethodManager;

import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.Fragment;

import com.smouldering_durtles.wk.Actment;
import com.smouldering_durtles.wk.activities.AbstractActivity;
import com.smouldering_durtles.wk.db.model.Subject;
import com.smouldering_durtles.wk.enums.FragmentTransitionAnimation;
import com.smouldering_durtles.wk.util.Logger;

import java.util.List;

import javax.annotation.Nullable;

import static com.smouldering_durtles.wk.util.ObjectSupport.safe;

/**
 * Abstract superclass for the various quiz fragments.
 */
public abstract class AbstractFragment extends Fragment implements Actment {
    private static final Logger LOGGER = Logger.get(AbstractFragment.class);

    /**
     * Is interaction with e.g. buttons on this display currently enabled?.
     */
    protected boolean interactionEnabled = true;

    /**
     * A window focus listener waiting to show the IME, set when showSoftInput() was called while
     * the window did not have focus yet. Null when no request is pending.
     */
    private @Nullable ViewTreeObserver.OnWindowFocusChangeListener pendingSoftInputListener = null;

    /**
     * The view that pendingSoftInputListener will show the IME for. Null when no request is pending.
     */
    private @Nullable View pendingSoftInputView = null;

    /**
     * The constructor.
     *
     * @param layoutId the layout resource ID for this fragment
     */
    protected AbstractFragment(final int layoutId) {
        super(layoutId);
    }

    @Override
    public final void onCreate(final @Nullable Bundle savedInstanceState) {
        safe(() -> {
            super.onCreate(savedInstanceState);
            onCreateLocal();
        });
    }

    @Override
    public final void onResume() {
        safe(() -> {
            super.onResume();
            updateToolbar();
            onResumeLocal();
        });
    }

    @Override
    public final void onDestroyView() {
        // A deferred IME request must not outlive the view it targets.
        removePendingSoftInputListener();
        super.onDestroyView();
    }

    @Override
    public final void onViewCreated(final View view, final @Nullable Bundle savedInstanceState) {
        safe(() -> {
            super.onViewCreated(view, savedInstanceState);
            onViewCreatedLocal(view, savedInstanceState);
        });
    }

    /**
     * True if the device is currently in landscape mode.
     *
     * @return true if in landscape mode
     */
    protected final boolean isLandscape() {
        return requireContext().getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE;
    }

    /**
     * Hide the soft keyboard.
     */
    protected final void hideSoftInput() {
        // A show request deferred until the window regains focus must not outlive the decision to
        // hide the keyboard - otherwise it fires later and pops the IME back up.
        removePendingSoftInputListener();

        safe(() -> {
            final @Nullable AbstractActivity activity = getAbstractActivity();
            if (activity == null) {
                return;
            }

            @Nullable View view = activity.getCurrentFocus();
            if (view == null) {
                view = new View(activity);
            }

            final @Nullable InputMethodManager imm = (InputMethodManager) activity.getSystemService(Context.INPUT_METHOD_SERVICE);
            if (imm != null) {
                imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
            }
        });
    }

    /**
     * Show the soft keyboard.
     *
     * <p>The IME can only be shown for a view whose window currently has focus. This method is
     * routinely called from onResume, which runs <em>before</em> the window regains focus, so the
     * request is deferred to the next window focus gain when needed. Until Android 17 the system
     * restored the IME's previous visibility across a configuration change by itself, which masked
     * the early request being dropped; from Android 17 on it does not, so the deferral is what
     * keeps the keyboard up across a rotation.
     *
     * @param view the view to attach the IME to.
     */
    @SuppressWarnings("MethodMayBeStatic")
    protected final void showSoftInput(final View view) {
        // This request supersedes any earlier deferred one. A stale listener must not survive: it
        // only fires on a focus *change*, so one registered for a view that turned out to be
        // attached to an already-focused window never fires at all, and would otherwise sit there
        // until the next unrelated focus loss and regain - popping the IME up out of nowhere.
        removePendingSoftInputListener();

        if (view.hasWindowFocus()) {
            requestSoftInput(view);
            return;
        }

        final ViewTreeObserver.OnWindowFocusChangeListener listener = hasFocus -> {
            if (!hasFocus) {
                return;
            }
            removePendingSoftInputListener();
            requestSoftInput(view);
        };
        pendingSoftInputListener = listener;
        pendingSoftInputView = view;
        view.getViewTreeObserver().addOnWindowFocusChangeListener(listener);
    }

    /**
     * Ask the IME to show itself for a view that already has window focus.
     *
     * @param view the view to attach the IME to.
     */
    private static void requestSoftInput(final View view) {
        final @Nullable InputMethodManager imm = (InputMethodManager) view.getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm == null) {
            LOGGER.info("No InputMethodManager available, cannot show the soft keyboard");
            return;
        }
        if (!imm.showSoftInput(view, InputMethodManager.SHOW_IMPLICIT)) {
            // Not fatal - the user can still tap the field - but it means the answer field is
            // silently unusable, so it must not disappear without a trace.
            LOGGER.info("Request to show the soft keyboard was refused (window focus: %s, view focus: %s)",
                    view.hasWindowFocus(), view.hasFocus());
        }
    }

    /**
     * Detach the pending window focus listener, if any, and forget it.
     */
    private void removePendingSoftInputListener() {
        final @Nullable ViewTreeObserver.OnWindowFocusChangeListener listener = pendingSoftInputListener;
        final @Nullable View view = pendingSoftInputView;
        pendingSoftInputListener = null;
        pendingSoftInputView = null;
        if (listener == null || view == null) {
            return;
        }
        final ViewTreeObserver observer = view.getViewTreeObserver();
        if (observer.isAlive()) {
            observer.removeOnWindowFocusChangeListener(listener);
        }
    }

    @Override
    public final @Nullable Toolbar getToolbar() {
        final @Nullable AbstractActivity activity = getAbstractActivity();
        if (activity == null) {
            return null;
        }
        return activity.getToolbar();
    }

    /**
     * Call updateCurrentSubject() on the activity this fragment is attached to.
     */
    protected final void updateCurrentSubject() {
        final @Nullable AbstractActivity activity = getAbstractActivity();
        if (activity == null) {
            return;
        }
        activity.updateCurrentSubject();
    }

    /**
     * Update the toolbar's title and background color based on the fragment's specifications.
     */
    protected final void updateToolbar() {
        safe(() -> {
            final @Nullable Toolbar toolbar = getToolbar();
            if (toolbar != null) {
                final @Nullable CharSequence title = getToolbarTitle();
                if (title != null) {
                    toolbar.setTitle(title);
                }
                final int color = getToolbarBackgroundColor();
                if (color != 0) {
                    toolbar.setBackgroundColor(color);
                }
            }
        });
    }

    /**
     * Get the absract activity that this fragment belongs to.
     * Same as getActivity() but does a check and cast for AbstractActivity.
     * @return the activity or null if not attached
     */
    protected final @Nullable AbstractActivity getAbstractActivity() {
        final @Nullable Activity activity = getActivity();
        if (activity instanceof AbstractActivity) {
            return (AbstractActivity) activity;
        }
        return null;
    }

    @Override
    public final void goToActivity(final Class<? extends AbstractActivity> clas) {
        final @Nullable AbstractActivity activity = getAbstractActivity();
        if (activity != null) {
            activity.goToActivity(clas);
        }
    }

    @Override
    public final void goToMainActivity() {
        final @Nullable AbstractActivity activity = getAbstractActivity();
        if (activity != null) {
            activity.goToMainActivity();
        }
    }

    /**
     * Go to the resurrect activity with the supplied list of subject IDs to resurrect.
     *
     * @param ids the subject IDs
     */
    protected final void goToResurrectActivity(final long[] ids) {
        final @Nullable AbstractActivity activity = getAbstractActivity();
        if (activity != null) {
            activity.goToResurrectActivity(ids);
        }
    }

    /**
     * Go to the burn activity with the supplied list of subject IDs to burn.
     *
     * @param ids the subject IDs
     */
    protected final void goToBurnActivity(final long[] ids) {
        final @Nullable AbstractActivity activity = getAbstractActivity();
        if (activity != null) {
            activity.goToBurnActivity(ids);
        }
    }

    @Override
    public final void goToSubjectInfo(final long id, final List<Long> ids, final FragmentTransitionAnimation animation) {
        final @Nullable AbstractActivity activity = getAbstractActivity();
        if (activity != null) {
            activity.goToSubjectInfo(id, ids, animation);
        }
    }

    @Override
    public final void goToSubjectInfo(final long id, final long[] ids, final FragmentTransitionAnimation animation) {
        final @Nullable AbstractActivity activity = getAbstractActivity();
        if (activity != null) {
            activity.goToSubjectInfo(id, ids, animation);
        }
    }

    @Override
    public final void goToSearchResult(final int searchType, final String searchParameters, final @Nullable String presetName) {
        final @Nullable AbstractActivity activity = getAbstractActivity();
        if (activity != null) {
            activity.goToSearchResult(searchType, searchParameters, presetName);
        }
    }

    /**
     * Translate DIPs to pixels.
     *
     * @param dp the dimension in DIPs
     * @return the corresponding number of pixels
     */
    protected final int dp2px(final int dp) {
        return (int) (dp * getResources().getDisplayMetrics().density);
    }

    /**
     * The subclass-specific part of onCreate.
     */
    protected abstract void onCreateLocal();

    /**
     * The subclass-specific part of onResume.
     */
    protected abstract void onResumeLocal();

    /**
     * Per-subclass part of onViewCreated
     * @param view the root view
     * @param savedInstanceState the instance state
     */
    protected abstract void onViewCreatedLocal(final View view, final @Nullable Bundle savedInstanceState);

    /**
     * Get the toolbar title for this fragment.
     *
     * @return the title
     */
    protected abstract @Nullable CharSequence getToolbarTitle();

    /**
     * Get the toolbar background color for this fragment.
     *
     * @return the title
     */
    protected abstract int getToolbarBackgroundColor();

    /**
     * Enable interactivity on this fragment. This makes buttons clickable, etc.
     */
    public abstract void enableInteraction();

    /**
     * Disable interactivity on this fragment. This makes buttons clickable, etc.
     */
    public abstract void disableInteraction();

    /**
     * Get the subject that this fragment is currently dealing with,
     * or null if there is no specific subject.
     *
     * @return the subject
     */
    public abstract @Nullable Subject getCurrentSubject();

    /**
     * Show or hide the soft keyboard, depending on whether it is needed here.
     */
    public abstract void showOrHideSoftInput();

    /**
     * Update variable view state for this fragment.
     */
    public abstract void updateViews();

    /**
     * Get a query string to pre-fill the search bar when it expands,
     * or null if no query should be pre-filled.
     *
     * @return the query or null
     */
    public @Nullable String getPrefillSearchQuery() {
        return null;
    }
}
