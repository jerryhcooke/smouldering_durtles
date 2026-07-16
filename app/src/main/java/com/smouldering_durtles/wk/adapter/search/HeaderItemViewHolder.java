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

package com.smouldering_durtles.wk.adapter.search;

import android.graphics.drawable.Drawable;
import android.view.View;

import androidx.core.content.ContextCompat;

import com.airbnb.lottie.SimpleColorFilter;
import com.smouldering_durtles.wk.R;
import com.smouldering_durtles.wk.proxy.ViewProxy;
import com.smouldering_durtles.wk.util.ThemeUtil;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Locale;

import javax.annotation.Nullable;

import static com.smouldering_durtles.wk.util.ObjectSupport.join;
import static com.smouldering_durtles.wk.util.ObjectSupport.safe;

/**
 * Abstract base class for view holders for advanced search result items.
 */
public abstract class HeaderItemViewHolder extends ResultItemViewHolder implements View.OnClickListener {
    private final ViewProxy arrowHead = new ViewProxy();
    protected final ViewProxy title = new ViewProxy();
    private final ViewProxy details = new ViewProxy();
    protected int total = 0;
    private int locked = 0;
    private int initial = 0;
    private int prePassed = 0;
    private int passed = 0;
    private int burned = 0;
    private @Nullable HeaderItem item = null;

    /**
     * The constructor.
     *
     * @param adapter the adapter this holder was created for
     * @param view the view for this holder
     */
    protected HeaderItemViewHolder(final SearchResultAdapter adapter, final View view) {
        super(adapter, view);
        arrowHead.setDelegate(view, R.id.arrowHead);
        title.setDelegate(view, R.id.title);
        details.setDelegate(view, R.id.details);
        //noinspection ThisEscapedInObjectConstruction
        view.setOnClickListener(this);
    }

    /**
     * The common part of bind() for header items.
     *
     * @param newItem the item
     */
    protected final void bindCommon(final HeaderItem newItem) {
        item = newItem;

        total = 0;
        locked = 0;
        initial = 0;
        prePassed = 0;
        passed = 0;
        burned = 0;
        item.iterateSubjects(t -> {
            total++;
            if (t.getSrsStage().isCompleted()) {
                burned++;
            }
            else if (t.getSrsStage().isInitial()) {
                initial++;
            }
            else if (t.getSrsStage().isLocked()) {
                locked++;
            }
            else if (t.isPassed()) {
                passed++;
            }
            else {
                prePassed++;
            }
        });

        final int icon = item.isCollapsed() ? R.drawable.ic_expand_less : R.drawable.ic_expand_more;
        final @Nullable Drawable drawable = ContextCompat.getDrawable(itemView.getContext(), icon);
        if (drawable != null) {
            drawable.setColorFilter(new SimpleColorFilter(ThemeUtil.getColor(androidx.appcompat.R.attr.colorPrimary)));
            arrowHead.setImageDrawable(drawable);
        }

        final Collection<String> parts = new ArrayList<>();
        if (locked > 0) {
            parts.add(String.format(Locale.ROOT, "%d locked", locked));
        }
        if (initial > 0) {
            parts.add(String.format(Locale.ROOT, "%d not started", initial));
        }
        if (prePassed > 0) {
            parts.add(String.format(Locale.ROOT, "%d in progress", prePassed));
        }
        if (passed > 0) {
            parts.add(String.format(Locale.ROOT, "%d passed", passed));
        }
        if (burned > 0) {
            parts.add(String.format(Locale.ROOT, "%d burned", burned));
        }
        details.setText(join(", ", "", "", parts));
    }

    @Override
    public final void onClick(final View v) {
        safe(() -> {
            if (item == null) {
                return;
            }
            final int position = getBindingAdapterPosition();
            if (item.isCollapsed()) {
                item.setCollapsed(false);
                final int count = item.getCount() - 1;
                adapter.notifyItemChanged(position);
                adapter.notifyItemRangeInserted(position + 1, count);
                adapter.getCollapsedTags().remove(item.getTag());
            }
            else {
                final int count = item.getCount() - 1;
                item.setCollapsed(true);
                adapter.notifyItemChanged(position);
                adapter.notifyItemRangeRemoved(position + 1, count);
                adapter.getCollapsedTags().add(item.getTag());
            }
        });
    }
}
