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

package com.smouldering_durtles.wk.components;

import android.content.Context;
import android.util.AttributeSet;

import androidx.preference.Preference;
import androidx.preference.PreferenceViewHolder;

import com.smouldering_durtles.wk.R;
import com.smouldering_durtles.wk.proxy.ViewProxy;
import com.smouldering_durtles.wk.util.ThemeUtil;

import static com.smouldering_durtles.wk.Constants.FONT_SIZE_NORMAL;
import static com.smouldering_durtles.wk.util.ObjectSupport.safe;

/**
 * A dummy preference class that isn't actually a preference - it's not clickable and just
 * a way to show a block of text in the preferences screen.
 */
public final class InfoPreference extends Preference {
    /**
     * The constructor.
     *
     * @param context Android context
     * @param attrs attributes from XML
     * @param defStyleAttr default style
     * @param defStyleRes default style resource
     */
    @SuppressWarnings({"WeakerAccess", "RedundantSuppression", "unused"})
    public InfoPreference(final Context context, final AttributeSet attrs, final int defStyleAttr, final int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        init();
    }

    /**
     * The constructor.
     *
     * @param context Android context
     * @param attrs attributes from XML
     * @param defStyleAttr default style
     */
    @SuppressWarnings({"WeakerAccess", "RedundantSuppression", "unused"})
    public InfoPreference(final Context context, final AttributeSet attrs, final int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    /**
     * The constructor.
     *
     * @param context Android context
     * @param attrs attributes from XML
     */
    @SuppressWarnings({"WeakerAccess", "RedundantSuppression", "unused"})
    public InfoPreference(final Context context, final AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    /**
     * The constructor.
     *
     * @param context Android context
     */
    @SuppressWarnings({"WeakerAccess", "RedundantSuppression", "unused"})
    public InfoPreference(final Context context) {
        super(context);
        init();
    }

    private void init() {
        setWidgetLayoutResource(R.layout.pref_api_key_help);
    }

    @Override
    public void onBindViewHolder(final PreferenceViewHolder holder) {
        safe(() -> {
            super.onBindViewHolder(holder);
            holder.itemView.setClickable(false);
            holder.itemView.setFocusable(false);
            final ViewProxy summary = new ViewProxy(holder.itemView, android.R.id.summary);
            summary.setTextColor(ThemeUtil.getColor(androidx.appcompat.R.attr.colorPrimary));
            summary.setTextSize(FONT_SIZE_NORMAL);
            summary.setLinkMovementMethod();
            summary.setVisibility(true);
        });
    }
}
