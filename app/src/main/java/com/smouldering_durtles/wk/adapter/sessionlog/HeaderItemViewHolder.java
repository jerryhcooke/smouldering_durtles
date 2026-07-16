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

package com.smouldering_durtles.wk.adapter.sessionlog;

import android.graphics.drawable.Drawable;
import android.view.View;

import androidx.core.content.ContextCompat;

import com.airbnb.lottie.SimpleColorFilter;
import com.smouldering_durtles.wk.R;
import com.smouldering_durtles.wk.proxy.ViewProxy;
import com.smouldering_durtles.wk.util.ThemeUtil;

import javax.annotation.Nullable;

import static com.smouldering_durtles.wk.util.ObjectSupport.safe;

/**
 * Abstract base class for view holders for advanced search result items.
 */
public final class HeaderItemViewHolder extends LogItemViewHolder implements View.OnClickListener {
    private final ViewProxy arrowHead = new ViewProxy();
    private final ViewProxy title = new ViewProxy();
    private final ViewProxy details = new ViewProxy();
    private @Nullable HeaderItem item = null;

    /**
     * The constructor.
     *
     * @param adapter the adapter this holder was created for
     * @param view the view for this holder
     */
    public HeaderItemViewHolder(final SessionLogAdapter adapter, final View view) {
        super(adapter, view);
        arrowHead.setDelegate(view, R.id.arrowHead);
        title.setDelegate(view, R.id.title);
        details.setDelegate(view, R.id.details);
        //noinspection ThisEscapedInObjectConstruction
        view.setOnClickListener(this);
    }

    @Override
    public void bind(final LogItem newItem) {
        if (!(newItem instanceof HeaderItem)) {
            return;
        }
        item = (HeaderItem) newItem;

        final int icon = item.isCollapsed() ? R.drawable.ic_expand_less : R.drawable.ic_expand_more;
        final @Nullable Drawable drawable = ContextCompat.getDrawable(itemView.getContext(), icon);
        if (drawable != null) {
            drawable.setColorFilter(new SimpleColorFilter(ThemeUtil.getColor(androidx.appcompat.R.attr.colorPrimary)));
            arrowHead.setImageDrawable(drawable);
        }

        title.setText(item.getTitleText());
        details.setText(item.getDetailsText());
    }

    @Override
    public void onClick(final View v) {
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
