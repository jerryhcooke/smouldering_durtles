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

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.GridLayoutManager;

import com.smouldering_durtles.wk.GlobalSettings;
import com.smouldering_durtles.wk.R;
import com.smouldering_durtles.wk.WkApplication;
import com.smouldering_durtles.wk.activities.AbstractActivity;
import com.smouldering_durtles.wk.activities.SessionActivity;
import com.smouldering_durtles.wk.adapter.search.SearchResultAdapter;
import com.smouldering_durtles.wk.db.AppDatabase;
import com.smouldering_durtles.wk.db.Converters;
import com.smouldering_durtles.wk.db.model.SearchPreset;
import com.smouldering_durtles.wk.db.model.Subject;
import com.smouldering_durtles.wk.enums.SearchSortOrder;
import com.smouldering_durtles.wk.livedata.LiveSearchPresets;
import com.smouldering_durtles.wk.model.AdvancedSearchParameters;
import com.smouldering_durtles.wk.model.Session;
import com.smouldering_durtles.wk.proxy.ViewProxy;
import com.smouldering_durtles.wk.util.SearchUtil;
import com.smouldering_durtles.wk.util.ThemeUtil;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

import javax.annotation.Nullable;

import static com.smouldering_durtles.wk.util.ObjectSupport.isEmpty;
import static com.smouldering_durtles.wk.util.ObjectSupport.runAsync;
import static com.smouldering_durtles.wk.util.ObjectSupport.safe;

/**
 * Fragment to show a simple search result.
 */
public final class SearchResultFragment extends AbstractFragment {
    private int searchType = 0;
    private String searchParameters = "1";
    private String searchDescription = "";
    private @Nullable String presetName = null;
    private boolean canResurrect = false;
    private boolean canBurn = false;

    @SuppressWarnings("ThisEscapedInObjectConstruction")
    private final SearchResultAdapter adapter = new SearchResultAdapter(this);

    private final ViewProxy numHits = new ViewProxy();
    private final ViewProxy resultView = new ViewProxy();
    private final ViewProxy tutorialText = new ViewProxy();
    private final ViewProxy tutorialDismiss = new ViewProxy();

    /**
     * The constructor.
     */
    public SearchResultFragment() {
        super(R.layout.fragment_search_result);
    }

    /**
     * Create a new instance with arguments set.
     *
     * @param searchType the type for this search
     * @param searchParameters the type-specific parameters for this search
     * @param presetName the preset used to get here, or null if no preset
     * @return the fragment
     */
    public static SearchResultFragment newInstance(final int searchType, final String searchParameters, final @Nullable String presetName) {
        final SearchResultFragment fragment = new SearchResultFragment();

        final Bundle args = new Bundle();
        args.putInt("searchType", searchType);
        args.putString("searchParameters", searchParameters);
        if (searchType == 0) {
            args.putString("searchDescription", "Level " + searchParameters);
        }
        else if (searchType == 1) {
            args.putString("searchDescription", "'" + searchParameters + "'");
        }
        else if (searchType == 2) {
            args.putString("searchDescription", "Advanced search");
        }
        if (presetName != null) {
            args.putString("presetName", presetName);
        }

        fragment.setArguments(args);
        return fragment;
    }

    /**
     * The name of the preset currently being shown.
     *
     * @return the name or null if this is a new search
     */
    public @Nullable String getPresetName() {
        return presetName;
    }

    @Override
    protected void onCreateLocal() {
        final @Nullable Bundle args = getArguments();
        if (args != null) {
            searchType = args.getInt("searchType", 0);
            searchParameters = args.getString("searchParameters", "1");
            searchDescription = args.getString("searchDescription", "");
            presetName = args.getString("presetName", null);
        }

        setHasOptionsMenu(true);
    }

    @Override
    protected void onResumeLocal() {
        updateMenu();

        if (GlobalSettings.Tutorials.getSearchResultDismissed()) {
            tutorialText.setParentVisibility(false);
        }
        else {
            tutorialText.setText("This screen shows the results from the search you chose before."
                    + " You can click on the various headers in the list to collapse/expand groups"
                    + " of subjects.\n"
                    + "Other actions such as refining your search, saving a preset or "
                    + "starting a self-study session can be accessed from the menu.");
            tutorialText.setParentVisibility(true);
            tutorialDismiss.setOnClickListener(v -> {
                GlobalSettings.Tutorials.setSearchResultDismissed(true);
                tutorialText.setParentVisibility(false);
            });
        }
    }

    @SuppressLint("NewApi")
    @Override
    public void onViewCreatedLocal(final View view, final @Nullable Bundle savedInstanceState) {
        resultView.setDelegate(view, R.id.resultView);
        numHits.setDelegate(view, R.id.numHits);
        tutorialText.setDelegate(view, R.id.tutorialText);
        tutorialDismiss.setDelegate(view, R.id.tutorialDismiss);

        if (savedInstanceState != null) {
            final @Nullable Collection<String> tags = savedInstanceState.getStringArrayList("collapsedTags");
            if (tags == null) {
                adapter.setCollapsedTags(Collections.emptyList());
            }
            else {
                adapter.setCollapsedTags(tags);
            }
        }

        final DisplayMetrics metrics = view.getContext().getResources().getDisplayMetrics();
        int spans = (int) ((metrics.widthPixels / metrics.density + 10) / 90);
        if (spans < 1) {
            spans = 1;
        }
        final int numSpans = spans;
        final GridLayoutManager layoutManager = new GridLayoutManager(view.getContext(), spans);
        final GridLayoutManager.SpanSizeLookup spanSizeLookup = new GridLayoutManager.SpanSizeLookup() {
            @Override
            public int getSpanSize(final int position) {
                return adapter.getItemSpanSize(position, numSpans);
            }
        };
        spanSizeLookup.setSpanGroupIndexCacheEnabled(true);
        spanSizeLookup.setSpanIndexCacheEnabled(true);
        layoutManager.setSpanSizeLookup(spanSizeLookup);
        resultView.setLayoutManager(layoutManager);
        resultView.setAdapter(adapter);
        resultView.setHasFixedSize(true);

        runAsync(this, () -> {
            if (searchType == 0) {
                final int level = Integer.parseInt(searchParameters, 10);
                adapter.setSortOrder(SearchSortOrder.TYPE);
                final AdvancedSearchParameters parameters = new AdvancedSearchParameters();
                parameters.minLevel = level;
                parameters.maxLevel = level;
                adapter.setParameters(parameters);
            }
            if (searchType == 1) {
                adapter.setSortOrder(SearchSortOrder.TYPE);
            }
            if (searchType == 2) {
                final AdvancedSearchParameters parameters = Converters.getObjectMapper().readValue(searchParameters, AdvancedSearchParameters.class);
                adapter.setSortOrder(parameters.sortOrder);
                adapter.setParameters(parameters);
            }
            return SearchUtil.searchSubjects(searchType, searchParameters);
        }, result -> {
            if (result != null) {
                adapter.setResult(result);
                canResurrect = result.stream().anyMatch(Subject::isResurrectable);
                canBurn = result.stream().anyMatch(Subject::isBurnable);
            }
            updateViews();
        });
    }

    @Override
    public String getToolbarTitle() {
        if (presetName == null) {
            return "Search results";
        }
        return presetName;
    }

    @Override
    public int getToolbarBackgroundColor() {
        return ThemeUtil.getColor(R.attr.toolbarColorBackground);
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
        return null;
    }

    @Override
    public void showOrHideSoftInput() {
        hideSoftInput();
    }

    @Override
    public void updateViews() {
        updateMenu();
        numHits.setTextFormat("%s: %d subject(s) found", searchDescription, adapter.getNumSubjects());
    }

    @Override
    public @Nullable String getPrefillSearchQuery() {
        if (searchType == 1) {
            return searchParameters;
        }
        return null;
    }

    private void savePreset(final String name) {
        final SearchPreset preset = new SearchPreset();
        preset.name = name;
        preset.type = searchType;
        preset.data = searchParameters;

        runAsync(this, () -> {
            WkApplication.getDatabase().searchPresetDao().setPreset(preset.name, preset.type, preset.data);
            return null;
        }, result -> Toast.makeText(requireContext(), "Preset '" + preset.name + "' saved", Toast.LENGTH_SHORT).show());
    }

    @SuppressLint("NewApi")
    private void updateStars(final int newNumStars) {
        final List<Subject> subjects = adapter.getSubjects();

        final String message = String.format(Locale.ROOT, "Do you want to set the star rating for %d subject%s to %d star%s?",
                subjects.size(), subjects.size() == 1 ? "" : "s", newNumStars, newNumStars == 1 ? "" : "s");

        new AlertDialog.Builder(requireContext())
                .setTitle("Set star ratings?")
                .setMessage(message)
                .setNegativeButton("No", (dialog, which) -> {})
                .setPositiveButton("Yes", (dialog, which) -> safe(() -> runAsync(getActivity(), () -> {
                    final AppDatabase db = WkApplication.getDatabase();
                    subjects.forEach(subject -> db.subjectDao().updateStars(subject.getId(), newNumStars));
                    return null;
                }, result -> Toast.makeText(requireContext(), "Star ratings updated", Toast.LENGTH_SHORT).show())))
                .create().show();
    }

    @Override
    public void onSaveInstanceState(final Bundle outState) {
        safe(() -> {
            super.onSaveInstanceState(outState);
            outState.putStringArrayList("collapsedTags", new ArrayList<>(adapter.getCollapsedTags()));
        });
    }

    @Override
    public void onCreateOptionsMenu(final Menu menu, final MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        updateMenu();
    }

    @Override
    public boolean onOptionsItemSelected(final MenuItem item) {
        final int itemId = item.getItemId();

        if (itemId == R.id.action_search_result_refine) {
            if (searchType != 1) {
                adapter.setShowingForm(!adapter.isShowingForm(), resultView.getLayoutManager());
            }
            return true;
        }
        if (itemId == R.id.action_search_result_save_preset) {
            final LayoutInflater layoutInflater = LayoutInflater.from(requireContext());
            final View promptView = layoutInflater.inflate(R.layout.input_dialog, null);
            final ViewProxy textView = new ViewProxy(promptView, R.id.textView);
            final ViewProxy editText = new ViewProxy(promptView, R.id.editText);
            final ArrayAdapter<String> presetAdapter =
                    new ArrayAdapter<>(requireContext(), R.layout.spinner_item, LiveSearchPresets.getInstance().getNames());
            editText.setArrayAdapter(presetAdapter);
            textView.setText("Choose a name for the search preset");
            editText.setHint("Name");
            if (!isEmpty(presetName)) {
                editText.setText(presetName);
            }
            final AlertDialog theDialog = new AlertDialog.Builder(requireContext())
                    .setTitle("Preset name")
                    .setView(promptView)
                    .setNegativeButton("Cancel", (dialog, which) -> safe(this::hideSoftInput))
                    .setPositiveButton("Save", (dialog, which) -> safe(() -> {
                        final String name = editText.getText();
                        if (!isEmpty(name)) {
                            savePreset(editText.getText());
                        }
                        hideSoftInput();
                    })).create();
            editText.setOnEditorActionListener((v, actionId, event) -> safe(false, () -> {
                boolean ok = event == null && actionId != 0;
                if (event != null && event.getKeyCode() == KeyEvent.KEYCODE_ENTER && event.getAction() == KeyEvent.ACTION_DOWN) {
                    ok = true;
                }
                if (event != null && event.getKeyCode() == KeyEvent.KEYCODE_ENTER && event.getAction() == KeyEvent.ACTION_UP) {
                    return true;
                }
                if (ok) {
                    final String name = editText.getText();
                    if (!isEmpty(name)) {
                        savePreset(editText.getText());
                    }
                    hideSoftInput();
                    theDialog.dismiss();
                    return true;
                }
                return false;
            }));
            theDialog.show();
            editText.requestFocus();
            if (editText.getDelegate() != null) {
                showSoftInput(editText.getDelegate());
            }
            return true;
        }
        if (itemId == R.id.action_search_result_self_study) {
            final List<Subject> subjects = adapter.getSubjects();
            runAsync(this, () -> {
                if (!subjects.isEmpty() && Session.getInstance().isInactive()) {
                    Session.getInstance().startNewSelfStudySession(subjects);
                }
                return null;
            }, result -> goToActivity(SessionActivity.class));
            return true;
        }
        if (itemId == R.id.action_search_result_resurrect) {
            goToResurrectActivity(adapter.getResurrectableSubjectIds());
            return true;
        }
        if (itemId == R.id.action_search_result_burn) {
            goToBurnActivity(adapter.getBurnableSubjectIds());
            return true;
        }
        if (itemId == R.id.action_search_result_star0) {
            updateStars(0);
            return true;
        }
        if (itemId == R.id.action_search_result_star1) {
            updateStars(1);
            return true;
        }
        if (itemId == R.id.action_search_result_star2) {
            updateStars(2);
            return true;
        }
        if (itemId == R.id.action_search_result_star3) {
            updateStars(3);
            return true;
        }
        if (itemId == R.id.action_search_result_star4) {
            updateStars(4);
            return true;
        }
        if (itemId == R.id.action_search_result_star5) {
            updateStars(5);
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private void updateMenu() {
        final @Nullable AbstractActivity activity = getAbstractActivity();
        if (activity == null) {
            return;
        }

        final @Nullable Menu menu = activity.getMenu();
        if (menu != null) {
            final @Nullable MenuItem subMenu = menu.findItem(R.id.action_search_result);
            if (subMenu != null) {
                final @Nullable MenuItem refineItem = menu.findItem(R.id.action_search_result_refine);
                if (refineItem != null) {
                    refineItem.setVisible(searchType != 1);
                }
                final @Nullable MenuItem savePresetItem = menu.findItem(R.id.action_search_result_save_preset);
                if (savePresetItem != null) {
                    savePresetItem.setVisible(true);
                }
                final @Nullable MenuItem selfStudyItem = menu.findItem(R.id.action_search_result_self_study);
                if (selfStudyItem != null) {
                    selfStudyItem.setVisible(adapter.getNumSubjects() > 0);
                }
                final @Nullable MenuItem resurrectItem = menu.findItem(R.id.action_search_result_resurrect);
                if (resurrectItem != null) {
                    resurrectItem.setVisible(canResurrect);
                }
                final @Nullable MenuItem burnItem = menu.findItem(R.id.action_search_result_burn);
                if (burnItem != null) {
                    burnItem.setVisible(canBurn);
                }
                final @Nullable MenuItem star0Item = menu.findItem(R.id.action_search_result_star0);
                if (star0Item != null) {
                    star0Item.setVisible(GlobalSettings.Other.getEnableStarRatings());
                }
                final @Nullable MenuItem star1Item = menu.findItem(R.id.action_search_result_star1);
                if (star1Item != null) {
                    star1Item.setVisible(GlobalSettings.Other.getEnableStarRatings());
                }
                final @Nullable MenuItem star2Item = menu.findItem(R.id.action_search_result_star2);
                if (star2Item != null) {
                    star2Item.setVisible(GlobalSettings.Other.getEnableStarRatings());
                }
                final @Nullable MenuItem star3Item = menu.findItem(R.id.action_search_result_star3);
                if (star3Item != null) {
                    star3Item.setVisible(GlobalSettings.Other.getEnableStarRatings());
                }
                final @Nullable MenuItem star4Item = menu.findItem(R.id.action_search_result_star4);
                if (star4Item != null) {
                    star4Item.setVisible(GlobalSettings.Other.getEnableStarRatings());
                }
                final @Nullable MenuItem star5Item = menu.findItem(R.id.action_search_result_star5);
                if (star5Item != null) {
                    star5Item.setVisible(GlobalSettings.Other.getEnableStarRatings());
                }
                subMenu.setVisible(true);
            }
        }
    }
}
