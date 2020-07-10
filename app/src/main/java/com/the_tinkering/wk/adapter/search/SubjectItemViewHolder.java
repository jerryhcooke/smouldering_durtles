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

package com.the_tinkering.wk.adapter.search;

import android.view.View;

import com.the_tinkering.wk.Actment;
import com.the_tinkering.wk.R;
import com.the_tinkering.wk.db.model.Subject;
import com.the_tinkering.wk.enums.FragmentTransitionAnimation;
import com.the_tinkering.wk.livedata.SubjectChangeListener;
import com.the_tinkering.wk.livedata.SubjectChangeWatcher;
import com.the_tinkering.wk.proxy.ViewProxy;
import com.the_tinkering.wk.util.Logger;
import com.the_tinkering.wk.util.WeakLcoRef;

import javax.annotation.Nullable;

/**
 * View holder class for subject items.
 */
public abstract class SubjectItemViewHolder extends ResultItemViewHolder implements View.OnClickListener, SubjectChangeListener {
    private static final Logger LOGGER = Logger.get(SubjectItemViewHolder.class);

    @SuppressWarnings("JavaDoc")
    protected final ViewProxy button = new ViewProxy();
    @SuppressWarnings("JavaDoc")
    protected final ViewProxy details1 = new ViewProxy();
    @SuppressWarnings("JavaDoc")
    protected final ViewProxy details2 = new ViewProxy();
    @SuppressWarnings("JavaDoc")
    protected final ViewProxy details3 = new ViewProxy();
    @SuppressWarnings("JavaDoc")
    protected @Nullable Subject subject = null;
    private final WeakLcoRef<Actment> actmentRef;

    /**
     * The view for this holder, inflated but not yet bound.
     *
     * @param adapter the adapter this holder was created for
     * @param view the view
     * @param actment the actment this view belongs to
     */
    @SuppressWarnings("ThisEscapedInObjectConstruction")
    protected SubjectItemViewHolder(final SearchResultAdapter adapter, final View view, final Actment actment) {
        super(adapter, view);
        actmentRef = new WeakLcoRef<>(actment);
        button.setDelegate(view, R.id.button);
        details1.setDelegate(view, R.id.details1);
        details2.setDelegate(view, R.id.details2);
        details3.setDelegate(view, R.id.details3);
        SubjectChangeWatcher.getInstance().addListener(this);
        view.setOnClickListener(this);
        button.setOnClickListener(this);
    }

    @Override
    public final void bind(final ResultItem newItem) {
        if (!(newItem instanceof SubjectItem)) {
            return;
        }
        subject = ((SubjectItem) newItem).getSubject();
        bind();
    }

    @Override
    public final void onSubjectChange(@SuppressWarnings("ParameterHidesMemberVariable") final Subject subject) {
        if (this.subject != null && subject.getId() == this.subject.getId()) {
            this.subject = subject;
        }
        bind();
    }

    @Override
    public final boolean isInterestedInSubject(final long subjectId) {
        return subject != null && subject.getId() == subjectId;
    }

    @Override
    public final void onClick(final View v) {
        try {
            final @Nullable Actment theActment = actmentRef.getOrElse(null);
            if (theActment == null || subject == null) {
                return;
            }
            theActment.goToSubjectInfo(subject.getId(), adapter.getSubjectIds(), FragmentTransitionAnimation.RTL);
        } catch (final Exception e) {
            LOGGER.uerr(e);
        }
    }

    /**
     * Do the bind after the subject has been set on this instance.
     */
    protected abstract void bind();
}
