/*
 * Copyright 2015 The AppAuth for Android Authors. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the
 * License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied. See the License for the specific language governing permissions and
 * limitations under the License.
 */

package net.openid.appauth;

import static net.openid.appauth.AdditionalParamsProcessor.checkAdditionalParams;
import static net.openid.appauth.AdditionalParamsProcessor.extractAdditionalParams;
import static net.openid.appauth.Preconditions.checkNotEmpty;
import static net.openid.appauth.Preconditions.checkNotNull;
import static net.openid.appauth.Preconditions.checkNullOrNotEmpty;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.VisibleForTesting;
import android.text.TextUtils;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * A response to a token request.
 *
 * @see TokenRequest
 * @see <a href="https://tools.ietf.org/html/rfc6749#section-4.1.4">"The OAuth 2.0
 * Authorization Framework" (RFC 6749), Section 4.1.4</a>
 */
public class TokenResponse {

    /**
     * Indicates that a provided access token is a bearer token.
     * @see <a href="https://tools.ietf.org/html/rfc6749#section-7.1">""The OAuth 2.0
     * Authorization Framework" (RFC 6749), Section 7.1</a>
     */
    public static final String TOKEN_TYPE_BEARER = "bearer";

    @VisibleForTesting
    static final String KEY_REQUEST = "request";

    @VisibleForTesting
    static final String KEY_EXPIRES_AT = "expires_at";

    // TODO: rename all KEY_* below to PARAM_*
    @VisibleForTesting
    static final String KEY_TOKEN_TYPE = "token_type";

    @VisibleForTesting
    static final String KEY_ACCESS_TOKEN = "access_token";

    @VisibleForTesting
    static final String KEY_EXPIRES_IN = "expires_in";

    @VisibleForTesting
    static final String KEY_REFRESH_TOKEN = "refresh_token";

    @VisibleForTesting
    static final String KEY_ID_TOKEN = "id_token";

    @VisibleForTesting
    static final String KEY_SCOPE = "scope";

    @VisibleForTesting
    static final String KEY_ADDITIONAL_PARAMETERS = "additionalParameters";

    private static final Set<String> BUILT_IN_PARAMS = new HashSet<>(Arrays.asList(
            KEY_TOKEN_TYPE,
            KEY_ACCESS_TOKEN,
            KEY_EXPIRES_IN,
            KEY_REFRESH_TOKEN,
            KEY_ID_TOKEN,
            KEY_SCOPE
    ));

    /**
     * The token request associated with this response.
     */
    @NonNull
    public final TokenRequest request;

    /**
     * The type of the token returned. Typically this is {@link #TOKEN_TYPE_BEARER}, or some
     * other token type that the client has negotiated with the authorization service.
     * @see <a href="https://tools.ietf.org/html/rfc6749#section-4.1.4">"The OAuth 2.0
     * Authorization Framework" (RFC 6749), Section 4.1.4</a>
     * @see <a href="https://tools.ietf.org/html/rfc6749#section-5.1">"The OAuth 2.0
     * Authorization Framework" (RFC 6749), Section 5.1</a>
     */
    @Nullable
    public final String tokenType;

    /**
     * The access token, if provided.
     * @see <a href="https://tools.ietf.org/html/rfc6749#section-5.1">"The OAuth 2.0
     * Authorization Framework" (RFC 6749), Section 5.1</a>
     */
    @Nullable
    public final String accessToken;

    /**
     * The expiration time of the access token, if provided. If an access token is provided but the
     * expiration time is not, then the expiration time is typically some default value specified
     * by the identity provider through some other means, such as documentation or an additional
     * non-standard field.
     */
    @Nullable
    public final Long accessTokenExpirationTime;

    /**
     * The ID token describing the authenticated user, if provided.
     * @see <a href="http://openid.net/specs/openid-connect-core-1_0.html#IDToken">"OpenID
     * Connect Core 1.0", Section 2</a>
     */
    @Nullable
    public final String idToken;

    /**
     * The refresh token, if provided.
     * @see <a href="https://tools.ietf.org/html/rfc6749#section-5.1">"The OAuth 2.0
     * Authorization Framework" (RFC 6749), Section 5.1</a>
     */
    @Nullable
    public final String refreshToken;

    /**
     * The scope of the access token. If the scope is identical to that originally
     * requested, then this value is optional.
     * @see <a href="https://tools.ietf.org/html/rfc6749#section-5.1">"The OAuth 2.0
     * Authorization Framework" (RFC 6749), Section 5.1</a>
     */
    @Nullable
    public final String scope;

    /**
     * Additional, non-standard parameters in the response.
     */
    @NonNull
    public final Map<String, String> additionalParameters;

    /**
     * Creates instances of {@link TokenResponse}.
     */
    public static final class Builder {
        @NonNull
        private TokenRequest mRequest;

        @Nullable
        private String mTokenType;

        @Nullable
        private String mAccessToken;

        @Nullable
        private Long mAccessTokenExpirationTime;

        @Nullable
        private String mIdToken;

        @Nullable
        private String mRefreshToken;

        @Nullable
        private String mScope;

        @NonNull
        private Map<String, String> mAdditionalParameters;

        /**
         * Creates a token response associated with the specified request.
         */
        public Builder(@NonNull TokenRequest request) {
            setRequest(request);
            mAdditionalParameters = Collections.emptyMap();
        }

        /**
         * Extracts token response fields from a JSON string.
         * @throws JSONException if the JSON is malformed or has incorrect value types for fields.
         */
        @NonNull
        public Builder fromResponseJsonString(@NonNull String jsonStr) throws JSONException {
            checkNotEmpty(jsonStr, "json cannot be null or empty");
            return fromResponseJson(new JSONObject(jsonStr));
        }

        /**
         * Extracts token response fields from a JSON object.
         * @throws JSONException if the JSON is malformed or has incorrect value types for fields.
         */
        @NonNull
        public Builder fromResponseJson(@NonNull JSONObject json) throws JSONException {
            try {
                setTokenType(JsonUtil.getString(json, KEY_TOKEN_TYPE));
                setAccessToken(JsonUtil.getStringIfDefined(json, KEY_ACCESS_TOKEN));
                if (json.has(KEY_EXPIRES_AT)) {
                    setAccessTokenExpirationTime(json.getLong(KEY_EXPIRES_AT));
                }
                if (json.has(KEY_EXPIRES_IN)) {
                    setAccessTokenExpiresIn(json.getLong(KEY_EXPIRES_IN));
                }
                setRefreshToken(JsonUtil.getStringIfDefined(json, KEY_REFRESH_TOKEN));
                setIdToken(JsonUtil.getStringIfDefined(json, KEY_ID_TOKEN));
                setAdditionalParameters(extractAdditionalParams(json, BUILT_IN_PARAMS));

                return this;
            } catch (JSONException ex) {
                // JSONException should only be thrown if a get() call is made for a key which
                // cannot be found. As all calls are guarded by has() calls, this exception should
                // therefore not be thrown.
                throw new IllegalStateException(
                        "JSONException thrown in violation of contract",
                        ex);
            }
        }

        /**
         * Specifies the request associated with this response. Must not be null.
         */
        @NonNull
        public Builder setRequest(@NonNull TokenRequest request) {
            mRequest = checkNotNull(request, "request cannot be null");
            return this;
        }

        /**
         * Specifies the token type of the access token in this response. If not null, the value
         * must be non-empty.
         */
        @NonNull
        public Builder setTokenType(@Nullable String tokenType) {
            mTokenType = checkNullOrNotEmpty(tokenType, "token type must not be empty if defined");
            return this;
        }

        /**
         * Specifies the access token. If not null, the value must be non-empty.
         */
        @NonNull
        public Builder setAccessToken(@Nullable String accessToken) {
            mAccessToken = checkNullOrNotEmpty(accessToken,
                    "access token cannot be empty if specified");
            return this;
        }

        /**
         * Sets the relative expiration time of the access token, in seconds, using the default
         * system clock as the source of the current time.
         */
        @NonNull
        public Builder setAccessTokenExpiresIn(@NonNull Long expiresIn) {
            return setAccessTokenExpiresIn(expiresIn, SystemClock.INSTANCE);
        }

        /**
         * Sets the relative expiration time of the access token, in seconds, using the provided
         * clock as the source of the current time.
         */
        @NonNull
        @VisibleForTesting
        Builder setAccessTokenExpiresIn(@Nullable Long expiresIn, @NonNull Clock clock) {
            if (expiresIn == null) {
                mAccessTokenExpirationTime = null;
            } else {
                mAccessTokenExpirationTime = clock.getCurrentTimeMillis()
                        + TimeUnit.SECONDS.toMillis(expiresIn);
            }
            return this;
        }

        /**
         * Sets the exact expiration time of the access token, in milliseconds since the UNIX epoch.
         */
        @NonNull
        public Builder setAccessTokenExpirationTime(@Nullable Long expiresAt) {
            mAccessTokenExpirationTime = expiresAt;
            return this;
        }

        /**
         * Specifies the ID token. If not null, the value must be non-empty.
         */
        public Builder setIdToken(@Nullable String idToken) {
            mIdToken = checkNullOrNotEmpty(idToken, "id token must not be empty if defined");
            return this;
        }

        /**
         * Specifies the refresh token. If not null, the value must be non-empty.
         */
        public Builder setRefreshToken(@Nullable String refreshToken) {
            mRefreshToken = checkNullOrNotEmpty(refreshToken,
                    "refresh token must not be empty if defined");
            return this;
        }

        /**
         * Specifies the encoded scope string, which is a space-delimited set of
         * case-sensitive scope identifiers. Replaces any previously specified scope.
         *
         * @see <a href="https://tools.ietf.org/html/rfc6749#section-3.3"> "The OAuth 2.0
         * Authorization
         * Framework" (RFC 6749), Section 3.3</a>
         */
        @NonNull
        public Builder setScope(@Nullable String scope) {
            if (TextUtils.isEmpty(scope)) {
                mScope = null;
            } else {
                setScopes(scope.split(" +"));
            }
            return this;
        }

        /**
         * Specifies the set of case-sensitive scopes. Replaces any previously specified set of
         * scopes. Individual scope strings cannot be null or empty.
         *
         * <p>Scopes specified here are used to obtain a "down-scoped" access token, where the
         * set of scopes specified <em>must</em> be a subset of those already granted in
         * previous requests.
         *
         * @see <a href="https://tools.ietf.org/html/rfc6749#section-3.3"> "The OAuth 2.0
         * Authorization Framework" (RFC 6749), Section 3.3</a>
         * @see <a href="https://tools.ietf.org/html/rfc6749#section-6"> "The OAuth 2.0
         * Authorization Framework" (RFC 6749), Section 6</a>
         */
        @NonNull
        public Builder setScopes(String... scopes) {
            if (scopes == null) {
                scopes = new String[0];
            }
            setScopes(Arrays.asList(scopes));
            return this;
        }

        /**
         * Specifies the set of case-sensitive scopes. Replaces any previously specified set of
         * scopes. Individual scope strings cannot be null or empty.
         *
         * <p>Scopes specified here are used to obtain a "down-scoped" access token, where the
         * set of scopes specified <em>must</em> be a subset of those already granted in
         * previous requests.
         *
         * @see <a href="https://tools.ietf.org/html/rfc6749#section-3.3"> "The OAuth 2.0
         * Authorization
         * Framework" (RFC 6749), Section 3.3</a>
         * @see <a href="https://tools.ietf.org/html/rfc6749#section-6"> "The OAuth 2.0
         * Authorization Framework" (RFC 6749), Section 6</a>
         */
        @NonNull
        public Builder setScopes(@Nullable Iterable<String> scopes) {
            mScope = AsciiStringListUtil.iterableToString(scopes);
            return this;
        }

        /**
         * Specifies the additional, non-standard parameters received as part of the response.
         */
        @NonNull
        public Builder setAdditionalParameters(@Nullable Map<String, String> additionalParameters) {
            mAdditionalParameters = checkAdditionalParams(additionalParameters, BUILT_IN_PARAMS);
            return this;
        }

        /**
         * Creates the token response instance.
         */
        public TokenResponse build() {
            return new TokenResponse(
                    mRequest,
                    mTokenType,
                    mAccessToken,
                    mAccessTokenExpirationTime,
                    mIdToken,
                    mRefreshToken,
                    mScope,
                    mAdditionalParameters);
        }
    }

    TokenResponse(
            @NonNull TokenRequest request,
            @Nullable String tokenType,
            @Nullable String accessToken,
            @Nullable Long accessTokenExpirationTime,
            @Nullable String idToken,
            @Nullable String refreshToken,
            @Nullable String scope,
            @NonNull Map<String, String> additionalParameters) {
        this.request = request;
        this.tokenType = tokenType;
        this.accessToken = accessToken;
        this.accessTokenExpirationTime = accessTokenExpirationTime;
        this.idToken = idToken;
        this.refreshToken = refreshToken;
        this.scope = scope;
        this.additionalParameters = additionalParameters;
    }

    /**
     * Derives the set of scopes from the consolidated, space-delimited scopes in the
     * {@link #scope} field. If no scopes were specified on this response, the method will
     * return {@code null}.
     */
    @Nullable
    public Set<String> getScopeSet() {
        return AsciiStringListUtil.stringToSet(scope);
    }

    /**
     * Converts the token response to a JSON object, for storage or transmission.
     */
    public JSONObject toJson() {
        JSONObject json = new JSONObject();
        JsonUtil.put(json, KEY_REQUEST, request.toJson());
        JsonUtil.putIfNotNull(json, KEY_TOKEN_TYPE, tokenType);
        JsonUtil.putIfNotNull(json, KEY_ACCESS_TOKEN, accessToken);
        JsonUtil.putIfNotNull(json, KEY_EXPIRES_AT, accessTokenExpirationTime);
        JsonUtil.putIfNotNull(json, KEY_ID_TOKEN, idToken);
        JsonUtil.putIfNotNull(json, KEY_REFRESH_TOKEN, refreshToken);
        JsonUtil.putIfNotNull(json, KEY_SCOPE, scope);
        JsonUtil.put(json, KEY_ADDITIONAL_PARAMETERS,
                JsonUtil.mapToJsonObject(additionalParameters));
        return json;
    }

    /**
     * Converts the token response to a JSON string, for storage or transmission.
     */
    public String toJsonString() {
        return toJson().toString();
    }

    /**
     * Reads a token response from a JSON string produced by {@link #toJsonString()}.
     * @throws JSONException if the JSON is malformed or missing required fields.
     */
    @NonNull
    public static TokenResponse fromJson(@NonNull String jsonStr) throws JSONException {
        return fromJson(null, jsonStr);
    }

    /**
     * Reads a token response from a JSON string produced by {@link #toJson()}.
     * @throws JSONException if the JSON is malformed or missing required fields.
     */
    @NonNull
    public static TokenResponse fromJson(@NonNull JSONObject json) throws JSONException {
        return fromJson(null, json);
    }

    /**
     * Reads a token response from a JSON string, and associates it with the provided request.
     * If a request is not provided, its serialized form is expected to be found in the JSON
     * (as if produced by a prior call to {@link #toJsonString()}.
     * @throws JSONException if the JSON is malformed or missing required fields.
     */
    @NonNull
    public static TokenResponse fromJson(
            @Nullable TokenRequest request,
            @NonNull String jsonStr)
            throws JSONException {
        checkNotEmpty(jsonStr, "jsonStr cannot be null or empty");
        return fromJson(request, new JSONObject(jsonStr));
    }

    /**
     * Reads a token response from a JSON string, and associates it with the provided request.
     * If a request is not provided, its serialized form is expected to be found in the JSON
     * (as if produced by a prior call to {@link #toJson()}.
     * @throws JSONException if the JSON is malformed or missing required fields.
     */
    @NonNull
    public static TokenResponse fromJson(
            @Nullable TokenRequest request,
            @NonNull JSONObject json)
            throws JSONException {

        TokenRequest extractedRequest;
        if (request != null) {
            extractedRequest = request;
        } else {
            if (!json.has(KEY_REQUEST)) {
                throw new IllegalArgumentException(
                        "token request not provided and not found in JSON");
            }
            extractedRequest = TokenRequest.fromJson(json.getJSONObject(KEY_REQUEST));
        }

        return new TokenResponse.Builder(extractedRequest)
                .fromResponseJson(json)
                .build();
    }
}
