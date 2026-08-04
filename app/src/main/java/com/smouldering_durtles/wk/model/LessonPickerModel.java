package com.smouldering_durtles.wk.model;

import com.smouldering_durtles.wk.db.model.Subject;
import com.smouldering_durtles.wk.enums.LessonPickerCategory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import javax.annotation.Nullable;

/**
 * Transient state and deterministic display ordering for manually selected lessons.
 */
public final class LessonPickerModel {
    /**
     * The outcome of changing one subject's selection state.
     */
    public enum ToggleResult {
        /**
         * The subject is now selected.
         */
        SELECTED,

        /**
         * The subject is no longer selected.
         */
        DESELECTED,

        /**
         * The configured selection limit prevented a new selection.
         */
        LIMIT_REACHED
    }

    /**
     * A type header, level header, or selectable subject in the picker list.
     */
    public static final class ListItem {
        private final @Nullable LessonPickerCategory category;
        private final int level;
        private final @Nullable Subject subject;

        private ListItem(final @Nullable LessonPickerCategory category,
                         final int level,
                         final @Nullable Subject subject) {
            this.category = category;
            this.level = level;
            this.subject = subject;
        }

        /**
         * Create a type header.
         *
         * @param category the category displayed by the header
         * @return the header item
         */
        public static ListItem typeHeader(final LessonPickerCategory category) {
            return new ListItem(category, 0, null);
        }

        /**
         * Create a level header.
         *
         * @param level the level displayed by the header
         * @return the header item
         */
        public static ListItem levelHeader(final int level) {
            return new ListItem(null, level, null);
        }

        /**
         * Create a selectable subject row.
         *
         * @param subject the subject displayed by the row
         * @return the subject row
         */
        public static ListItem subject(final Subject subject) {
            return new ListItem(null, 0, subject);
        }

        /**
         * Is this a type header?.
         *
         * @return true if this item represents a type header
         */
        public boolean isTypeHeader() {
            return category != null;
        }

        /**
         * Is this a level header?.
         *
         * @return true if this item represents a level header
         */
        public boolean isLevelHeader() {
            return category == null && subject == null;
        }

        /**
         * Is this a selectable subject row?.
         *
         * @return true if this item represents a subject row
         */
        public boolean isSubject() {
            return subject != null;
        }

        /**
         * The category of a type header.
         *
         * @return the category, or null for other item types
         */
        public @Nullable LessonPickerCategory getCategory() {
            return category;
        }

        /**
         * The level of a level header.
         *
         * @return the level, or zero for other item types
         */
        public int getLevel() {
            return level;
        }

        /**
         * The subject of a selectable row.
         *
         * @return the subject, or null for header item types
         */
        public @Nullable Subject getSubject() {
            return subject;
        }
    }

    private static final Comparator<Subject> DISPLAY_ORDER = Comparator
            .comparingInt(LessonPickerModel::getCategoryOrder)
            .thenComparingInt(Subject::getLevel)
            .thenComparingInt(Subject::getLessonPosition)
            .thenComparingLong(Subject::getId);

    private final int maximumSelectionCount;
    private final List<Subject> availableSubjects = new ArrayList<>();
    private final LinkedHashSet<Long> selectedSubjectIds = new LinkedHashSet<>();
    private List<ListItem> items = Collections.emptyList();

    /**
     * The constructor.
     *
     * @param maximumSelectionCount the greatest number of subjects that may be selected
     */
    public LessonPickerModel(final int maximumSelectionCount) {
        this.maximumSelectionCount = Math.max(0, maximumSelectionCount);
    }

    /**
     * Filter candidates to one picker category without changing their supplied order.
     *
     * @param subjects the candidate subjects
     * @param category the category to retain
     * @return an immutable list of matching subjects
     */
    public static List<Subject> filter(final List<Subject> subjects,
                                       final LessonPickerCategory category) {
        final List<Subject> result = new ArrayList<>();
        for (final Subject subject: subjects) {
            if (category.matches(subject)) {
                result.add(subject);
            }
        }
        return Collections.unmodifiableList(result);
    }

    /**
     * Intersect saved subject IDs with current availability in picker display order.
     *
     * @param subjects the current available subjects
     * @param subjectIds the IDs selected before revalidation
     * @return immutable current subjects selected by ID, in picker display order
     */
    public static List<Subject> retainSelected(final List<Subject> subjects, final long[] subjectIds) {
        final Set<Long> selectedIds = new HashSet<>();
        for (final long subjectId: subjectIds) {
            selectedIds.add(subjectId);
        }

        final Set<Long> retainedIds = new HashSet<>();
        final List<Subject> result = new ArrayList<>();
        for (final Subject subject: subjects) {
            if (selectedIds.contains(subject.getId()) && retainedIds.add(subject.getId())) {
                result.add(subject);
            }
        }
        result.sort(DISPLAY_ORDER);
        return Collections.unmodifiableList(result);
    }

    /**
     * Replace the availability snapshot and discard selections that are no longer available.
     *
     * @param subjects the current available subjects
     */
    public void setAvailableSubjects(final List<Subject> subjects) {
        availableSubjects.clear();
        availableSubjects.addAll(subjects);
        final Set<Long> availableIds = new HashSet<>();
        for (final Subject subject: availableSubjects) {
            availableIds.add(subject.getId());
        }
        selectedSubjectIds.retainAll(availableIds);
        items = buildItems(availableSubjects);
    }

    /**
     * Restore selected subject IDs before the current availability snapshot is loaded.
     *
     * @param subjectIds the subject IDs saved during activity recreation
     */
    public void restoreSelectedSubjectIds(final long[] subjectIds) {
        selectedSubjectIds.clear();
        for (final long subjectId: subjectIds) {
            if (selectedSubjectIds.size() >= maximumSelectionCount) {
                break;
            }
            selectedSubjectIds.add(subjectId);
        }
    }

    /**
     * Select or deselect a subject ID.
     *
     * @param subjectId the subject ID to change
     * @return the resulting change
     */
    public ToggleResult toggle(final long subjectId) {
        if (selectedSubjectIds.remove(subjectId)) {
            return ToggleResult.DESELECTED;
        }
        if (selectedSubjectIds.size() >= maximumSelectionCount) {
            return ToggleResult.LIMIT_REACHED;
        }
        selectedSubjectIds.add(subjectId);
        return ToggleResult.SELECTED;
    }

    /**
     * Is a subject currently selected?.
     *
     * @param subjectId the subject ID to inspect
     * @return true if the subject is selected
     */
    public boolean isSelected(final long subjectId) {
        return selectedSubjectIds.contains(subjectId);
    }

    /**
     * The current selection count.
     *
     * @return the selected subject count
     */
    public int getSelectedCount() {
        return selectedSubjectIds.size();
    }

    /**
     * The selection limit configured when this model was created.
     *
     * @return the maximum number of selected subjects
     */
    public int getMaximumSelectionCount() {
        return maximumSelectionCount;
    }

    /**
     * Return a defensive copy of the selected subject IDs in selection order.
     *
     * @return the selected subject IDs
     */
    public long[] getSelectedSubjectIds() {
        final long[] result = new long[selectedSubjectIds.size()];
        int index = 0;
        for (final long subjectId: selectedSubjectIds) {
            result[index++] = subjectId;
        }
        return result;
    }

    /**
     * Return the selected available subjects in their deterministic picker display order.
     *
     * @return immutable selected available subjects
     */
    public List<Subject> getSelectedAvailableSubjects() {
        return retainSelected(availableSubjects, getSelectedSubjectIds());
    }

    /**
     * The immutable display items built from the current availability snapshot.
     *
     * @return the type headers, level headers, and subject rows
     */
    public List<ListItem> getItems() {
        return items;
    }

    private static int getCategoryOrder(final Subject subject) {
        for (final LessonPickerCategory category: LessonPickerCategory.values()) {
            if (category.matches(subject)) {
                return category.ordinal();
            }
        }
        return LessonPickerCategory.values().length;
    }

    private static List<ListItem> buildItems(final List<Subject> subjects) {
        final List<Subject> orderedSubjects = new ArrayList<>();
        final Set<Long> seenIds = new HashSet<>();
        for (final Subject subject: subjects) {
            if (seenIds.add(subject.getId())) {
                orderedSubjects.add(subject);
            }
        }
        orderedSubjects.sort(DISPLAY_ORDER);

        final List<ListItem> result = new ArrayList<>();
        LessonPickerCategory previousCategory = null;
        int previousLevel = 0;
        for (final Subject subject: orderedSubjects) {
            final LessonPickerCategory category = getCategory(subject);
            if (category == null) {
                continue;
            }
            if (category != previousCategory) {
                result.add(ListItem.typeHeader(category));
                previousCategory = category;
                previousLevel = 0;
            }
            if (subject.getLevel() != previousLevel) {
                result.add(ListItem.levelHeader(subject.getLevel()));
                previousLevel = subject.getLevel();
            }
            result.add(ListItem.subject(subject));
        }
        return Collections.unmodifiableList(result);
    }

    private static @Nullable LessonPickerCategory getCategory(final Subject subject) {
        for (final LessonPickerCategory category: LessonPickerCategory.values()) {
            if (category.matches(subject)) {
                return category;
            }
        }
        return null;
    }
}
