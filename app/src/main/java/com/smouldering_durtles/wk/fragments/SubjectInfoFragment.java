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

import android.os.Bundle;
import android.view.View;

import com.smouldering_durtles.wk.GlobalSettings;
import com.smouldering_durtles.wk.R;
import com.smouldering_durtles.wk.WkApplication;
import com.smouldering_durtles.wk.activities.AbstractActivity;
import com.smouldering_durtles.wk.db.model.Subject;
import com.smouldering_durtles.wk.enums.FragmentTransitionAnimation;
import com.smouldering_durtles.wk.proxy.ViewProxy;
import com.smouldering_durtles.wk.views.SubjectInfoView;
import com.smouldering_durtles.wk.views.SwipingScrollView;

import javax.annotation.Nullable;

import static com.smouldering_durtles.wk.util.ObjectSupport.runAsync;

import java.util.Objects;

/**
 * A fragment to show the full subject info dump.
 */
public final class SubjectInfoFragment extends AbstractFragment implements SwipingScrollView.OnSwipeListener {
    private final ViewProxy scrollView = new ViewProxy();
    private final ViewProxy subjectInfo = new ViewProxy();

    private @Nullable Subject currentSubject = null;

    /**
     * The constructor.
     */
    public SubjectInfoFragment() {
        super(R.layout.fragment_subject_info);
    }

    /**
     * Create a new instance with arguments set.
     *
     * @param id the subject ID
     * @param ids the context list of subject IDs
     * @return the fragment
     */
    public static SubjectInfoFragment newInstance(final long id, final long[] ids) {
        final SubjectInfoFragment fragment = new SubjectInfoFragment();
        final Bundle args = new Bundle();
        args.putLong("id", id);
        args.putLongArray("ids", ids);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    protected void onCreateLocal() {
        //
    }

    @Override
    protected void onResumeLocal() {
        subjectInfo.setToolbar(getToolbar());
    }

    @Override
    public void onViewCreatedLocal(final View view, final @Nullable Bundle savedInstanceState) {
        scrollView.setDelegate(view, R.id.scrollView);
        subjectInfo.setDelegate(view, R.id.subjectInfo);

        scrollView.setSwipeListener(this);

        final @Nullable Bundle args = getArguments();
        if (args != null) {
            final long subjectId = args.getLong("id", -1);
            if (subjectId != -1) {
                runAsync(
                        this,
                        () -> WkApplication.getDatabase().subjectDao().getById(subjectId),
                        result -> {
                            currentSubject = result;
                            updateViews();
                        });
            }
        }
    }

    @Override
    public @Nullable String getToolbarTitle() {
        return null;
    }

    @Override
    public int getToolbarBackgroundColor() {
        return 0;
    }

    @Override
    public void enableInteraction() {
        interactionEnabled = true;
    }

    @Override
    public void disableInteraction() {
        interactionEnabled = false;
    }

    @Override
    public @Nullable Subject getCurrentSubject() {
        return currentSubject;
    }

    @Override
    public void showOrHideSoftInput() {
        hideSoftInput();
    }

    @Override
    public void updateViews() {
        if (currentSubject != null) {
            subjectInfo.setMaxFontSize(GlobalSettings.Font.getMaxFontSizeBrowse());
            subjectInfo.setContainerType(SubjectInfoView.ContainerType.BROWSE);
            subjectInfo.setSubject(this, currentSubject);
        }
        updateCurrentSubject();
    }

    @Override
    public void onSwipeLeft(final SwipingScrollView view) {
        final @Nullable AbstractActivity activity = getAbstractActivity();
        final @Nullable Bundle args = getArguments();
        if (currentSubject == null || activity == null || args == null) {
            return;
        }
        final long[] ids = args.getLongArray("ids");
        if (ids == null) {
            return;
        }
        for (int i=1; i<ids.length; i++) {
            if (ids[i] == currentSubject.getId()) {
                activity.goToSubjectInfo(ids[i-1], ids, FragmentTransitionAnimation.LTR);
                break;
            }
        }
    }

    @Override
    public void onSwipeRight(final SwipingScrollView view) {
        final @Nullable AbstractActivity activity = getAbstractActivity();
        final @Nullable Bundle args = getArguments();
        if (currentSubject == null || activity == null || args == null) {
            return;
        }
        final long[] ids = Objects.requireNonNull(args.getLongArray("ids"));
        for (int i=0; i<ids.length-1; i++) {
            if (ids[i] == currentSubject.getId()) {
                activity.goToSubjectInfo(ids[i+1], ids, FragmentTransitionAnimation.RTL);
                break;
            }
        }
    }
}
