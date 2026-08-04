package com.smouldering_durtles.wk.adapter.lessonpicker;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.FrameLayout;
import android.widget.TextView;

import androidx.core.view.AccessibilityDelegateCompat;
import androidx.core.view.ViewCompat;
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.smouldering_durtles.wk.R;
import com.smouldering_durtles.wk.db.model.Subject;
import com.smouldering_durtles.wk.enums.LessonPickerCategory;
import com.smouldering_durtles.wk.enums.SubjectCardLayout;
import com.smouldering_durtles.wk.model.LessonPickerModel;
import com.smouldering_durtles.wk.model.SubjectCardBinder;

import java.util.List;
import java.util.Locale;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import static java.util.Objects.requireNonNull;

/**
 * RecyclerView adapter for the manual lesson picker.
 */
public final class LessonPickerAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    private static final int TYPE_CATEGORY_HEADER = 1;
    private static final int TYPE_LEVEL_HEADER = 2;
    private static final int TYPE_SUBJECT = 3;

    /**
     * Listener for subject row taps.
     */
    public interface Listener {
        /**
         * Handle a tap on a selectable subject row.
         *
         * @param subjectId the selected subject ID
         */
        void onSubjectClicked(long subjectId);
    }

    private final LessonPickerModel model;
    private final Listener listener;
    private final SubjectCardBinder binder =
            new SubjectCardBinder(SubjectCardLayout.COMPACT_NO_PROGRESSION);

    /**
     * The constructor.
     *
     * @param model the transient picker model
     * @param listener listener for selectable row taps
     */
    public LessonPickerAdapter(final LessonPickerModel model, final Listener listener) {
        this.model = model;
        this.listener = listener;
        setHasStableIds(true);
    }

    /**
     * Refresh all displayed rows after the model changes.
     */
    public void refresh() {
        notifyDataSetChanged();
    }

    @Override
    public int getItemCount() {
        return model.getItems().size();
    }

    @Override
    public int getItemViewType(final int position) {
        final LessonPickerModel.ListItem item = model.getItems().get(position);
        if (item.isTypeHeader()) {
            return TYPE_CATEGORY_HEADER;
        }
        if (item.isLevelHeader()) {
            return TYPE_LEVEL_HEADER;
        }
        return TYPE_SUBJECT;
    }

    @Override
    public long getItemId(final int position) {
        final LessonPickerModel.ListItem item = model.getItems().get(position);
        final @Nullable Subject subject = item.getSubject();
        return subject == null ? Long.MIN_VALUE + position : subject.getId();
    }

    @Override
    public @Nonnull RecyclerView.ViewHolder onCreateViewHolder(final ViewGroup parent,
                                                               final int viewType) {
        final LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        if (viewType == TYPE_CATEGORY_HEADER) {
            return new HeaderViewHolder(
                    inflater.inflate(R.layout.lesson_picker_type_header, parent, false));
        }
        if (viewType == TYPE_LEVEL_HEADER) {
            return new HeaderViewHolder(
                    inflater.inflate(R.layout.lesson_picker_level_header, parent, false));
        }
        return new SubjectViewHolder(
                inflater.inflate(R.layout.lesson_picker_subject_row, parent, false));
    }

    @Override
    public void onBindViewHolder(final RecyclerView.ViewHolder holder, final int position) {
        final LessonPickerModel.ListItem item = model.getItems().get(position);
        if (holder instanceof HeaderViewHolder) {
            ((HeaderViewHolder) holder).bind(item);
        }
        else if (holder instanceof SubjectViewHolder) {
            ((SubjectViewHolder) holder).bind(requireNonNull(item.getSubject()));
        }
    }

    private static String getCategoryTitle(final LessonPickerCategory category) {
        switch (category) {
            case RADICALS:
                return "Radicals";
            case KANJI:
                return "Kanji";
            case VOCABULARY:
            default:
                return "Vocabulary";
        }
    }

    private static String getSubjectTitle(final Subject subject) {
        final @Nullable String characters = subject.getCharacters();
        if (characters != null && !characters.isEmpty()) {
            return characters;
        }
        final @Nullable String slug = subject.getSlug();
        if (slug != null && !slug.isEmpty()) {
            return slug;
        }
        return Long.toString(subject.getId());
    }

    private static void makeCardPresentationOnly(final View view) {
        view.setClickable(false);
        view.setFocusable(false);
        ViewCompat.setImportantForAccessibility(view,
                ViewCompat.IMPORTANT_FOR_ACCESSIBILITY_NO_HIDE_DESCENDANTS);
        if (view instanceof ViewGroup) {
            final ViewGroup group = (ViewGroup) view;
            for (int i = 0; i < group.getChildCount(); i++) {
                makeCardPresentationOnly(group.getChildAt(i));
            }
        }
    }

    private static final class HeaderViewHolder extends RecyclerView.ViewHolder {
        private final TextView text;

        private HeaderViewHolder(final View view) {
            super(view);
            text = (TextView) view;
            ViewCompat.setAccessibilityHeading(text, true);
        }

        private void bind(final LessonPickerModel.ListItem item) {
            final @Nullable LessonPickerCategory category = item.getCategory();
            if (category == null) {
                text.setText(String.format(Locale.ROOT, "Level %d", item.getLevel()));
            }
            else {
                text.setText(getCategoryTitle(category));
            }
        }
    }

    private final class SubjectViewHolder extends RecyclerView.ViewHolder {
        private final TextView level;
        private final FrameLayout cardContainer;
        private final CheckBox selected;

        private SubjectViewHolder(final View view) {
            super(view);
            level = view.findViewById(R.id.lessonPickerSubjectLevel);
            cardContainer = view.findViewById(R.id.lessonPickerSubjectCard);
            selected = view.findViewById(R.id.lessonPickerSelected);
        }

        private void bind(final Subject subject) {
            final boolean isSelected = model.isSelected(subject.getId());
            final String selectionState = isSelected ? "selected" : "not selected";
            final String description = String.format(Locale.ROOT, "%s, Level %d, %s, %s",
                    getSubjectTitle(subject),
                    subject.getLevel(),
                    subject.getOneMeaning(),
                    selectionState);

            itemView.setTag(R.id.subjectId, subject.getId());
            itemView.setActivated(isSelected);
            itemView.setSelected(isSelected);
            itemView.setContentDescription(description);
            itemView.setOnClickListener(view -> listener.onSubjectClicked(subject.getId()));
            level.setText(String.format(Locale.ROOT, "Level %d", subject.getLevel()));
            selected.setChecked(isSelected);

            cardContainer.removeAllViews();
            final View card = binder.createView(subject.getType(), cardContainer);
            cardContainer.addView(card, new FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT));
            binder.bind(card, subject, view -> listener.onSubjectClicked(subject.getId()),
                    true, true);
            makeCardPresentationOnly(card);

            ViewCompat.setAccessibilityDelegate(itemView, new AccessibilityDelegateCompat() {
                @Override
                public void onInitializeAccessibilityNodeInfo(final View host,
                                                              final AccessibilityNodeInfoCompat info) {
                    super.onInitializeAccessibilityNodeInfo(host, info);
                    info.setClassName(CheckBox.class.getName());
                    info.setCheckable(true);
                    info.setChecked(isSelected);
                    info.setClickable(true);
                    info.setContentDescription(description);
                }
            });
        }
    }
}
