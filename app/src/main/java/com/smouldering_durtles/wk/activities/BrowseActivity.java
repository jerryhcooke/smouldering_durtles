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

package com.smouldering_durtles.wk.activities;

import android.app.SearchManager;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;

import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import com.smouldering_durtles.wk.Identification;
import com.smouldering_durtles.wk.R;
import com.smouldering_durtles.wk.enums.FragmentTransitionAnimation;
import com.smouldering_durtles.wk.fragments.BrowseOverviewFragment;
import com.smouldering_durtles.wk.fragments.SearchResultFragment;
import com.smouldering_durtles.wk.fragments.SessionLogFragment;
import com.smouldering_durtles.wk.fragments.SubjectInfoFragment;

import javax.annotation.Nullable;

import static com.smouldering_durtles.wk.util.ObjectSupport.safe;

/**
 * An activity for browsing/searching the subject database.
 */
public final class BrowseActivity extends AbstractActivity {
    /**
     * The constructor.
     */
    public BrowseActivity() {
        super(R.layout.activity_browse, R.menu.generic_options_menu);
    }

    @Override
    protected void onCreateLocal(final @Nullable Bundle savedInstanceState) {
        if (savedInstanceState == null) {
            if (Intent.ACTION_SEARCH.equals(getIntent().getAction())) {
                final @Nullable String query = getIntent().getStringExtra(SearchManager.QUERY);
                if (query != null) {
                    loadSearchResultFragment(null, 1, query);
                    return;
                }
            }

            if (Intent.ACTION_SEND.equals(getIntent().getAction()) && "text/plain".equals(getIntent().getType())) {
                final @Nullable String sharedText = getIntent().getStringExtra(Intent.EXTRA_TEXT);
                if (sharedText != null) {
                    loadSearchResultFragment(null, 1, sharedText);
                    return;
                }
            }

            final long id = getIntent().getLongExtra("id", -1);
            if (id > 0) {
                long[] ids = getIntent().getLongArrayExtra("ids");
                if (ids == null) {
                    ids = new long[0];
                }
                loadSubjectInfoFragment(id, ids, FragmentTransitionAnimation.NONE);
                return;
            }

            final int searchType = getIntent().getIntExtra("searchType", -1);
            if (searchType >= 0) {
                final @Nullable String searchParameters = getIntent().getStringExtra("searchParameters");
                final @Nullable String presetName = getIntent().getStringExtra("presetName");
                if (searchParameters != null) {
                    loadSearchResultFragment(presetName, searchType, searchParameters);
                    return;
                }
            }

            final boolean sessionLog = getIntent().getBooleanExtra("sessionLog", false);
            if (sessionLog) {
                loadSessionLogFragment();
                return;
            }

            final @Nullable Uri uri = getIntent().getData();
            if (uri != null && Identification.APP_URI_SCHEME.equals(uri.getScheme()) && "subject-info".equals(uri.getAuthority())) {
                final @Nullable String s = uri.getPath();
                if (s != null) {
                    final int p = s.lastIndexOf('/');
                    if (p >= 0) {
                        safe(() -> {
                            final long searchId = Long.parseLong(s.substring(p+1));
                            loadSubjectInfoFragment(searchId, new long[0], FragmentTransitionAnimation.NONE);
                        });
                        return;
                    }
                }
            }

            loadOverviewFragment();
        }
    }

    @Override
    protected void onResumeLocal() {
        //
    }

    @Override
    protected void onPauseLocal() {
        //
    }

    @Override
    protected void enableInteractionLocal() {
        //
    }

    @Override
    protected void disableInteractionLocal() {
        //
    }

    @Override
    protected boolean showWithoutApiKey() {
        return false;
    }

    private void loadOverviewFragment() {
        final Fragment fragment = new BrowseOverviewFragment();
        final FragmentManager manager = getSupportFragmentManager();
        final FragmentTransaction transaction = manager.beginTransaction();
        if (getCurrentFragment() != null) {
            transaction.addToBackStack(null);
            FragmentTransitionAnimation.RTL.apply(transaction);
        }
        transaction.replace(R.id.fragment, fragment);
        transaction.commit();
        hideSearchResultMenu();
    }

    /**
     * Show a search result fragment.
     *
     * @param presetName the name of the preset used, or null if not available
     * @param searchType the type of search 0=browse level, 1=keyword search, 2=advanced search
     * @param searchParameters the type-specific parameters for this search
     */
    public void loadSearchResultFragment(final @Nullable String presetName, final int searchType, final String searchParameters) {
        final Fragment fragment = SearchResultFragment.newInstance(searchType, searchParameters, presetName);
        final FragmentManager manager = getSupportFragmentManager();
        final FragmentTransaction transaction = manager.beginTransaction();
        if (getCurrentFragment() != null) {
            transaction.addToBackStack(null);
            FragmentTransitionAnimation.RTL.apply(transaction);
        }
        transaction.replace(R.id.fragment, fragment);
        transaction.commit();
    }

    /**
     * Show a subject info fragment.
     *
     * @param id the subject's ID
     * @param ids the list of context subject IDs
     * @param animation the transition animation to use for the transition
     */
    public void loadSubjectInfoFragment(final long id, final long[] ids, final FragmentTransitionAnimation animation) {
        final Fragment fragment = SubjectInfoFragment.newInstance(id, ids);
        final FragmentManager manager = getSupportFragmentManager();
        final FragmentTransaction transaction = manager.beginTransaction();
        if (getCurrentFragment() != null) {
            transaction.addToBackStack(null);
            animation.apply(transaction);
        }
        transaction.replace(R.id.fragment, fragment);
        transaction.commit();
    }

    /**
     * Show a log of the current session.
     */
    public void loadSessionLogFragment() {
        final Fragment fragment = SessionLogFragment.newInstance();
        final FragmentManager manager = getSupportFragmentManager();
        final FragmentTransaction transaction = manager.beginTransaction();
        if (getCurrentFragment() != null) {
            transaction.addToBackStack(null);
        }
        transaction.replace(R.id.fragment, fragment);
        transaction.commit();
    }

    /**
     * Hide the search results submenu.
     */
    public void hideSearchResultMenu() {
        final @Nullable Menu menu = getMenu();
        if (menu != null) {
            final @Nullable MenuItem subMenu = menu.findItem(R.id.action_search_result);
            if (subMenu != null) {
                subMenu.setVisible(false);
            }
        }
    }
}
