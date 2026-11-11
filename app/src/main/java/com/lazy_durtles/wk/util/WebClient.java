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

package com.lazy_durtles.wk.util;

import com.lazy_durtles.wk.BuildConfig;
import com.lazy_durtles.wk.GlobalSettings;
import com.lazy_durtles.wk.Identification;
import com.lazy_durtles.wk.WkApplication;
import com.lazy_durtles.wk.db.AppDatabase;
import com.lazy_durtles.wk.db.model.Subject;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.net.CookieHandler;
import java.net.CookieManager;
import java.net.CookiePolicy;
import java.util.Locale;

import javax.annotation.Nullable;

import okhttp3.FormBody;
import okhttp3.JavaNetCookieJar;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.ResponseBody;

import static com.lazy_durtles.wk.util.ObjectSupport.isEmpty;
import static com.lazy_durtles.wk.util.ObjectSupport.safe;
import static com.lazy_durtles.wk.util.ObjectSupport.safeNullable;
import static java.util.Objects.requireNonNull;

/**
 * A singleton to hold some state related to the current web login, and code to access the web API.
 */
public final class WebClient {
    private static final Logger LOGGER = Logger.get(WebClient.class);

    private static final WebClient instance = new WebClient();
    private static final String LOGIN_URL = "https://www.wanikani.com/login";
    private static final String DASHBOARD_URL = "https://www.wanikani.com/dashboard";
    private static final String RESURRECT_URL = "https://www.wanikani.com/assignments/%d/resurrect";
    private static final String BURN_URL = "https://www.wanikani.com/assignments/%d/burn";

    /*
     * 0 = no login attempted yet
     * 1 = last login attempt was successful
     * 2 = last login attempt failed because of incorrect credentials
     * 3 = last login attempt failed for other reasons
     */
    private int lastLoginState = 0;
    private String lastLoginMessage = "";
    private String authenticityToken = "";

    private WebClient() {
        //
    }

    /**
     * Get the singleton instance.
     *
     * @return the instance
     */
    public static WebClient getInstance() {
        return instance;
    }

    /**
     * The state of the last login attempt.
     * @return the value
     */
    public int getLastLoginState() {
        return lastLoginState;
    }

    /**
     * A status message for the last login attempt.
     * @return the value
     */
    public String getLastLoginMessage() {
        return lastLoginMessage;
    }

    @SuppressWarnings("ConstantConditions")
    private boolean findAuthenticityToken(final @Nullable ResponseBody body) {
        return safe(false, () -> {
            if (body == null) {
                return false;
            }
            final Document doc = Jsoup.parse(body.string(), LOGIN_URL);
            for (final Element meta: doc.getElementsByTag("meta")) {
                final @Nullable String name = meta.attr("name");
                final @Nullable String value = meta.attr("content");
                if (name != null && value != null && name.equals("csrf-token")) {
                    authenticityToken = value;
                    return true;
                }
            }
            return false;
        });
    }

    private static @Nullable Response getUrl(final String url) {
        return safeNullable(() -> {
            LOGGER.info("Fetching: %s", url);
            final OkHttpClient client = new OkHttpClient.Builder()
                    .cookieJar(new JavaNetCookieJar(CookieHandler.getDefault()))
                    .build();
            final Request request = new Request.Builder()
                    .header("User-Agent", Identification.APP_NAME_UA + "/" + BuildConfig.VERSION_NAME)
                    .url(url)
                    .build();
            return client.newCall(request).execute();
        });
    }

    private static @Nullable Response postUrl(final String url, final RequestBody requestBody) {
        return safeNullable(() -> {
            LOGGER.info("Posting: %s", url);
            final OkHttpClient client = new OkHttpClient.Builder()
                    .cookieJar(new JavaNetCookieJar(CookieHandler.getDefault()))
                    .build();
            final Request request = new Request.Builder()
                    .header("User-Agent", Identification.APP_NAME_UA + "/" + BuildConfig.VERSION_NAME)
                    .url(url)
                    .post(requestBody)
                    .build();
            return client.newCall(request).execute();
        });
    }

    private boolean fetchLoginPage() {
        return safe(() -> {
            lastLoginState = 3;
            lastLoginMessage = "Login page could not be fetched or parsed";
            return false;
        }, () -> {
            try (final @Nullable Response response = getUrl(LOGIN_URL)) {
                if (response == null || !response.isSuccessful()) {
                    lastLoginState = 3;
                    lastLoginMessage = "Login page could not be fetched";
                    return false;
                }
                if (!findAuthenticityToken(response.body())) {
                    lastLoginState = 3;
                    lastLoginMessage = "Login page does not contain a CSRF token";
                    return false;
                }
                return true;
            }
        });
    }

    private void performLogin(final String password) {
        safe(() -> {
            lastLoginState = 3;
            lastLoginMessage = "Login failed";
            return "";
        }, () -> {
            final AppDatabase db = WkApplication.getDatabase();
            final RequestBody formData = new FormBody.Builder()
                    .add("utf8", "✓")
                    .add("authenticity_token", authenticityToken)
                    .add("user[login]", requireNonNull(db.propertiesDao().getUsername()))
                    .add("user[password]", password)
                    .add("user[remember_me]", "0")
                    .build();
            try (final @Nullable Response response = postUrl(LOGIN_URL, formData)) {
                if (response == null) {
                    lastLoginState = 3;
                    lastLoginMessage = "Login failed";
                    return "";
                }
                if (response.isSuccessful() && response.request().url().toString().equals(DASHBOARD_URL)) {
                    lastLoginState = 1;
                    lastLoginMessage = "Login successful";
                    return "";
                }
                return safe(() -> {
                    lastLoginState = 3;
                    lastLoginMessage = "Login failed";
                    return "";
                }, () -> {
                    final Document doc = Jsoup.parse(requireNonNull(response.body()).string(), LOGIN_URL);
                    final Elements divs = doc.getElementsByClass("alert-error");
                    if (!divs.isEmpty()) {
                        final Element div = divs.get(0);
                        if (div.text().contains("Invalid login or password")) {
                            lastLoginState = 2;
                            lastLoginMessage = "Invalid username or password";
                            return "";
                        }
                    }
                    lastLoginState = 3;
                    lastLoginMessage = "Login failed";
                    return "";
                });
            }
        });
    }

    /**
     * Perform a login on the web site.
     */
    public void doLogin() {
        safe(() -> {
            CookieHandler.setDefault(new CookieManager(null, CookiePolicy.ACCEPT_ALL));
            final @Nullable String password = GlobalSettings.Api.getWebPassword();
            if (isEmpty(password)) {
                lastLoginState = 3;
                lastLoginMessage = "No password specified in configuration";
                return;
            }
            if (!fetchLoginPage()) {
                return;
            }
            performLogin(password);
        });
    }

    /**
     * Resurrect a subject.
     *
     * @param subject The subject
     * @return true if it was successful
     */
    public boolean resurrect(final Subject subject) {
        final String url = isEmpty(subject.getDocumentUrl()) ? DASHBOARD_URL : subject.getDocumentUrl();
        try (final @Nullable Response response1 = getUrl(url)) {
            if (response1 == null) {
                return false;
            }
            if (!findAuthenticityToken(response1.body())) {
                return false;
            }
        }

        final RequestBody formData = new FormBody.Builder()
                .add("_method", "put")
                .add("authenticity_token", authenticityToken)
                .build();
        try (final @Nullable Response response2 = postUrl(String.format(Locale.ROOT, RESURRECT_URL, subject.getId()), formData)) {
            if (response2 == null) {
                return false;
            }
            return response2.isSuccessful();
        }
    }

    /**
     * Burn a subject that has been previously resurrected.
     *
     * @param subject The subject
     * @return true if it was successful
     */
    public boolean burn(final Subject subject) {
        final String url = isEmpty(subject.getDocumentUrl()) ? DASHBOARD_URL : subject.getDocumentUrl();
        try (final @Nullable Response response1 = getUrl(url)) {
            if (response1 == null) {
                return false;
            }
            if (!findAuthenticityToken(response1.body())) {
                return false;
            }
        }

        final RequestBody formData = new FormBody.Builder()
                .add("_method", "put")
                .add("authenticity_token", authenticityToken)
                .build();
        try (final @Nullable Response response2 = postUrl(String.format(Locale.ROOT, BURN_URL, subject.getId()), formData)) {
            if (response2 == null) {
                return false;
            }
            return response2.isSuccessful();
        }
    }
}
