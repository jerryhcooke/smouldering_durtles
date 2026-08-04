package com.smouldering_durtles.wk.model;

import com.smouldering_durtles.wk.db.model.Subject;
import com.smouldering_durtles.wk.db.model.SubjectEntity;
import com.smouldering_durtles.wk.enums.LessonPickerCategory;
import com.smouldering_durtles.wk.enums.SubjectType;

import org.junit.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Tests for the deterministic lesson picker data model.
 */
public final class LessonPickerModelTest {
    /**
     * Each category must map subjects to the picker grouping users see.
     */
    @Test
    public void categoriesMatchTheirSubjectsIncludingBothVocabularyTypes() {
        final Subject radical = subject(101, SubjectType.WANIKANI_RADICAL, 1, 1);
        final Subject kanji = subject(102, SubjectType.WANIKANI_KANJI, 1, 2);
        final Subject vocabulary = subject(103, SubjectType.WANIKANI_VOCAB, 1, 3);
        final Subject kanaVocabulary = subject(104, SubjectType.WANIKANI_KANA_VOCAB, 1, 4);

        assertTrue(LessonPickerCategory.RADICALS.matches(radical));
        assertFalse(LessonPickerCategory.RADICALS.matches(kanji));
        assertTrue(LessonPickerCategory.KANJI.matches(kanji));
        assertFalse(LessonPickerCategory.KANJI.matches(vocabulary));
        assertTrue(LessonPickerCategory.VOCABULARY.matches(vocabulary));
        assertTrue(LessonPickerCategory.VOCABULARY.matches(kanaVocabulary));
        assertFalse(LessonPickerCategory.VOCABULARY.matches(radical));
    }

    /**
     * Filtering a category must not silently replace the DAO's candidate order.
     */
    @Test
    public void filterKeepsMatchingCandidatesInTheirExistingOrder() {
        final Subject vocabulary = subject(301, SubjectType.WANIKANI_VOCAB, 3, 3);
        final Subject radical = subject(101, SubjectType.WANIKANI_RADICAL, 1, 1);
        final Subject kanaVocabulary = subject(302, SubjectType.WANIKANI_KANA_VOCAB, 3, 4);
        final Subject kanji = subject(201, SubjectType.WANIKANI_KANJI, 2, 2);

        assertSubjectIds(new long[] {301, 302}, LessonPickerModel.filter(
                Arrays.asList(vocabulary, radical, kanaVocabulary, kanji),
                LessonPickerCategory.VOCABULARY));
    }

    /**
     * The list must be grouped predictably so row recycling cannot change the displayed hierarchy.
     */
    @Test
    public void itemsGroupCategoriesAndLevelsWithVocabularyTogether() {
        final LessonPickerModel model = new LessonPickerModel(10);
        model.setAvailableSubjects(Arrays.asList(
                subject(405, SubjectType.WANIKANI_KANA_VOCAB, 2, 2),
                subject(302, SubjectType.WANIKANI_VOCAB, 1, 4),
                subject(203, SubjectType.WANIKANI_KANJI, 2, 2),
                subject(102, SubjectType.WANIKANI_RADICAL, 2, 1),
                subject(404, SubjectType.WANIKANI_VOCAB, 2, 2),
                subject(101, SubjectType.WANIKANI_RADICAL, 1, 3),
                subject(201, SubjectType.WANIKANI_KANJI, 1, 2),
                subject(301, SubjectType.WANIKANI_VOCAB, 1, 4)));

        final List<LessonPickerModel.ListItem> items = model.getItems();

        assertEquals(17, items.size());
        assertTypeHeader(items.get(0), LessonPickerCategory.RADICALS);
        assertLevelHeader(items.get(1), 1);
        assertSubject(items.get(2), 101);
        assertLevelHeader(items.get(3), 2);
        assertSubject(items.get(4), 102);
        assertTypeHeader(items.get(5), LessonPickerCategory.KANJI);
        assertLevelHeader(items.get(6), 1);
        assertSubject(items.get(7), 201);
        assertLevelHeader(items.get(8), 2);
        assertSubject(items.get(9), 203);
        assertTypeHeader(items.get(10), LessonPickerCategory.VOCABULARY);
        assertLevelHeader(items.get(11), 1);
        assertSubject(items.get(12), 301);
        assertSubject(items.get(13), 302);
        assertLevelHeader(items.get(14), 2);
        assertSubject(items.get(15), 404);
        assertSubject(items.get(16), 405);
    }

    /**
     * The selection limit must reject only the first extra selection and leave the existing set untouched.
     */
    @Test
    public void toggleSelectsDeselectsAndRejectsSelectionsBeyondTheMaximum() {
        final LessonPickerModel model = new LessonPickerModel(2);

        assertEquals(LessonPickerModel.ToggleResult.SELECTED, model.toggle(101));
        assertEquals(1, model.getSelectedCount());
        assertTrue(model.isSelected(101));
        assertEquals(LessonPickerModel.ToggleResult.SELECTED, model.toggle(102));
        assertEquals(LessonPickerModel.ToggleResult.LIMIT_REACHED, model.toggle(103));
        assertEquals(2, model.getSelectedCount());
        assertFalse(model.isSelected(103));
        assertEquals(LessonPickerModel.ToggleResult.DESELECTED, model.toggle(101));
        assertEquals(1, model.getSelectedCount());
        assertFalse(model.isSelected(101));
        assertArrayEquals(new long[] {102}, model.getSelectedSubjectIds());
        assertEquals(2, model.getMaximumSelectionCount());
    }

    /**
     * Restored IDs must be accepted before availability loads without exceeding the current limit.
     */
    @Test
    public void restoreSelectedSubjectIdsDeduplicatesAndCapsBeforeAvailabilityLoads() {
        final LessonPickerModel model = new LessonPickerModel(2);

        model.restoreSelectedSubjectIds(new long[] {301, 301, 201, 101});

        assertArrayEquals(new long[] {301, 201}, model.getSelectedSubjectIds());
        model.setAvailableSubjects(Arrays.asList(
                subject(201, SubjectType.WANIKANI_KANJI, 1, 2),
                subject(301, SubjectType.WANIKANI_VOCAB, 1, 3)));
        assertSubjectIds(new long[] {201, 301}, model.getSelectedAvailableSubjects());
    }

    /**
     * A refreshed availability snapshot must drop selected subjects that are no longer available.
     */
    @Test
    public void settingAvailableSubjectsRemovesSelectedIdsNoLongerAvailable() {
        final LessonPickerModel model = new LessonPickerModel(3);
        model.setAvailableSubjects(Arrays.asList(
                subject(201, SubjectType.WANIKANI_KANJI, 1, 2),
                subject(301, SubjectType.WANIKANI_VOCAB, 1, 3)));
        assertEquals(LessonPickerModel.ToggleResult.SELECTED, model.toggle(201));
        assertEquals(LessonPickerModel.ToggleResult.SELECTED, model.toggle(301));

        model.setAvailableSubjects(Arrays.asList(
                subject(301, SubjectType.WANIKANI_VOCAB, 1, 3),
                subject(401, SubjectType.WANIKANI_VOCAB, 1, 4)));

        assertArrayEquals(new long[] {301}, model.getSelectedSubjectIds());
        assertSubjectIds(new long[] {301}, model.getSelectedAvailableSubjects());
    }

    /**
     * Revalidation uses current subjects once each and preserves the visible picker order, not saved-ID order.
     */
    @Test
    public void retainSelectedReturnsUniqueCurrentSubjectsInPickerDisplayOrder() {
        final Subject vocabulary = subject(301, SubjectType.WANIKANI_VOCAB, 1, 4);
        final Subject radical = subject(101, SubjectType.WANIKANI_RADICAL, 2, 2);
        final Subject kanaVocabulary = subject(302, SubjectType.WANIKANI_KANA_VOCAB, 1, 3);
        final Subject duplicateVocabulary = subject(301, SubjectType.WANIKANI_VOCAB, 1, 4);
        final Subject kanji = subject(201, SubjectType.WANIKANI_KANJI, 1, 1);

        assertSubjectIds(new long[] {101, 201, 302, 301}, LessonPickerModel.retainSelected(
                Arrays.asList(vocabulary, radical, kanaVocabulary, duplicateVocabulary, kanji),
                new long[] {301, 999, 101, 302, 301, 201}));
    }

    private static void assertTypeHeader(final LessonPickerModel.ListItem item,
                                         final LessonPickerCategory category) {
        assertTrue(item.isTypeHeader());
        assertEquals(category, item.getCategory());
    }

    private static void assertLevelHeader(final LessonPickerModel.ListItem item, final int level) {
        assertTrue(item.isLevelHeader());
        assertEquals(level, item.getLevel());
    }

    private static void assertSubject(final LessonPickerModel.ListItem item, final long subjectId) {
        assertTrue(item.isSubject());
        assertEquals(subjectId, item.getSubject().getId());
    }

    private static void assertSubjectIds(final long[] expected, final List<Subject> subjects) {
        final long[] actual = new long[subjects.size()];
        for (int i = 0; i < subjects.size(); i++) {
            actual[i] = subjects.get(i).getId();
        }
        assertArrayEquals(expected, actual);
    }

    private static Subject subject(final long id,
                                   final SubjectType type,
                                   final int level,
                                   final int lessonPosition) {
        final SubjectEntity entity = new SubjectEntity();
        entity.id = id;
        entity.type = type;
        entity.level = level;
        entity.lessonPosition = lessonPosition;
        return new Subject(entity);
    }
}
