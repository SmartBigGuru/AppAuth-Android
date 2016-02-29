/*
 * Copyright 2015 Google Inc. All Rights Reserved.
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

import static net.openid.appauth.Preconditions.checkArgument;
import static net.openid.appauth.Preconditions.checkMapEntryFullyDefined;
import static net.openid.appauth.Preconditions.checkNotNull;
import static net.openid.appauth.Preconditions.checkNullOrNotEmpty;

import android.content.Intent;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.VisibleForTesting;
import android.text.TextUtils;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * A response to an authorization request.
 *
 * @see AuthorizationRequest
 * @see <a href="https://tools.ietf.org/html/rfc6749#section-4.1.2">
 * "The OAuth 2.0 Authorization Framework"</a>
 */
public class AuthorizationResponse {

    /**
     * The extra string used to store an {@link AuthorizationResponse} in an intent by
     * {@link #toIntent()}.
     */
    public static final String EXTRA_RESPONSE = "net.openid.appauth.AuthorizationResponse";

    /**
     * Indicates that a provided access token is a bearer token.
     * @see <a href="https://tools.ietf.org/html/rfc6749#section-7.1">""The OAuth 2.0
     * Authorization Framework" (RFC 6749), Section 7.1</a>
     */
    public static final String TOKEN_TYPE_BEARER = "bearer";

    @VisibleForTesting
    static final String KEY_REQUEST = "request";
    @VisibleForTesting
    static final String KEY_STATE = "state";
    @VisibleForTesting
    static final String KEY_TOKEN_TYPE = "token_type";
    @VisibleForTesting
    static final String KEY_AUTHORIZATION_CODE = "code";
    @VisibleForTesting
    static final String KEY_ACCESS_TOKEN = "access_token";
    @VisibleForTesting
    static final String KEY_EXPIRES_AT = "expires_at";
    @VisibleForTesting
    static final String KEY_EXPIRES_IN = "expires_in";
    @VisibleForTesting
    static final String KEY_ID_TOKEN = "id_token";
    @VisibleForTesting
    static final String KEY_SCOPE = "scope";
    @VisibleForTesting
    static final String KEY_ADDITIONAL_PARAMETERS = "additional_parameters";

    // KEY_EXPIRES_AT and KEY_ADDITIONAL_PARAMETERS are non-standard, so not included here
    private static final Set<String> BUILT_IN_KEYS = new HashSet<>(Arrays.asList(
            KEY_REQUEST,
            KEY_TOKEN_TYPE,
            KEY_STATE,
            KEY_AUTHORIZATION_CODE,
            KEY_ACCESS_TOKEN,
            KEY_EXPIRES_IN,
            KEY_ID_TOKEN,
            KEY_SCOPE
    ));

    /**
     * The authorization request associated with this response.
     */
    @NonNull
    public final AuthorizationRequest request;

    /**
     * The returned state parameter, which must match the value specified in the request.
     * AppAuth for Android ensures that this is the case.
     */
    @Nullable
    public final String state;

    /**
     * The type of the retrieved token. Typically this is "Bearer" when present. Otherwise,
     * another token_type value that the Client has negotiated with the Authorization Server.
     *
     * @see <a href="http://openid.net/specs/openid-connect-core-1_0.html#ImplicitAuthResponse">
     *     OpenID Connect Core 1.0 Specification, Section 3.2.2.5,
     *     "Successful Authentication Response"</a>
     */
    @Nullable
    public final String tokenType;

    /**
     * The authorization code generated by the authorization server.
     * Set when the response_type requested includes 'code'.
     */
    @Nullable
    public final String authorizationCode;

    /**
     * The access token retrieved as part of the authorization flow.
     * This is available when the {@link AuthorizationRequest#responseType response_type}
     * of the request included 'token'.
     *
     * @see <a href="http://openid.net/specs/openid-connect-core-1_0.html#ImplicitAuthResponse">
     *     OpenID Connect Core 1.0 Specification, Section 3.2.2.5,
     *     "Successful Authentication Response"</a>
     */
    @Nullable
    public final String accessToken;

    /**
     * The approximate expiration time of the access token, as milliseconds from the UNIX epoch.
     * Set when the requested {@link AuthorizationRequest#responseType response_type}
     * included 'token'.
     *
     * @see <a href="http://openid.net/specs/openid-connect-core-1_0.html#ImplicitAuthResponse">
     *     OpenID Connect Core 1.0 Specification, Section 3.2.2.5,
     *     "Successful Authentication Response"</a>
     */
    @Nullable
    public final Long accessTokenExpirationTime;

    /**
     * The id token retrieved as part of the authorization flow.
     * This is available when the {@link  AuthorizationRequest#responseType response_type}
     * of the request included 'id_token'.
     *
     * @see <a href="http://openid.net/specs/openid-connect-core-1_0.html#IDToken">
     *     OpenID Connect Core 1.0 Specification, Section 2,
     *     "ID Token"</a>
     *  @see <a href="http://openid.net/specs/openid-connect-core-1_0.html#ImplicitAuthResponse">
     *     OpenID Connect Core 1.0 Specification, Section 3.2.2.5,
     *     "Successful Authentication Response"</a>
     */
    @Nullable
    public final String idToken;

    /**
     * The scope of the returned access token. If this is not specified, the scope is assumed
     * to be the same as what was originally requested.
     */
    @Nullable
    public final String scope;

    /**
     * The additional, non-standard parameters in the response.
     */
    @NonNull
    public final Map<String, String> additionalParameters;

    /**
     * Creates instances of {@link AuthorizationResponse}.
     */
    public static final class Builder {

        @NonNull
        private AuthorizationRequest mRequest;

        @Nullable
        private String mState;

        @Nullable
        private String mTokenType;

        @Nullable
        private String mAuthorizationCode;

        @Nullable
        private String mAccessToken;

        @Nullable
        private Long mAccessTokenExpirationTime;

        @Nullable
        private String mIdToken;

        @Nullable
        private String mScope;

        @NonNull
        private Map<String, String> mAdditionalParameters;

        /**
         * Creates an authorization builder with the specified mandatory properties.
         */
        public Builder(@NonNull AuthorizationRequest request) {
            mRequest = checkNotNull(request, "authorization request cannot be null");
            mAdditionalParameters = new LinkedHashMap<>();
        }

        /**
         * Extracts authorization response parameters from the query portion of a redirect URI.
         */
        @NonNull
        public Builder fromUri(@NonNull Uri uri) {
            return fromUri(uri, SystemClock.INSTANCE);
        }

        @NonNull
        @VisibleForTesting
        Builder fromUri(@NonNull Uri uri, @NonNull Clock clock) {
            setState(uri.getQueryParameter(KEY_STATE));
            setTokenType(uri.getQueryParameter(KEY_TOKEN_TYPE));
            setAuthorizationCode(uri.getQueryParameter(KEY_AUTHORIZATION_CODE));
            setAccessToken(uri.getQueryParameter(KEY_ACCESS_TOKEN));
            setAccessTokenExpiresIn(UriUtil.getLongQueryParameter(uri, KEY_EXPIRES_IN), clock);
            setIdToken(uri.getQueryParameter(KEY_ID_TOKEN));
            setScope(uri.getQueryParameter(KEY_SCOPE));

            setAdditionalParameters(UriUtil.extractAdditionalParameters(uri, BUILT_IN_KEYS));
            return this;
        }

        /**
         * Specifies the OAuth 2 state.
         */
        @NonNull
        public Builder setState(@Nullable String state) {
            checkNullOrNotEmpty(state, "state must not be empty");
            mState = state;
            return this;
        }

        /**
         * Specifies the OAuth 2 token type.
         */
        @NonNull
        public Builder setTokenType(@Nullable String tokenType) {
            checkNullOrNotEmpty(tokenType, "tokenType must not be empty");
            mTokenType = tokenType;
            return this;
        }

        /**
         * Specifies the OAuth 2 authorization code.
         */
        @NonNull
        public Builder setAuthorizationCode(@Nullable String authorizationCode) {
            checkNullOrNotEmpty(authorizationCode, "authorizationCode must not be empty");
            mAuthorizationCode = authorizationCode;
            return this;
        }

        /**
         * Specifies the OAuth 2 access token.
         */
        @NonNull
        public Builder setAccessToken(@Nullable String accessToken) {
            checkNullOrNotEmpty(accessToken, "accessToken must not be empty");
            mAccessToken = accessToken;
            return this;
        }

        /**
         * Specifies the expiration period of the OAuth 2 access token.
         */
        @NonNull
        public Builder setAccessTokenExpiresIn(@Nullable Long expiresIn) {
            return setAccessTokenExpiresIn(expiresIn, SystemClock.INSTANCE);
        }

        /**
         * Specifies the relative expiration time of the access token, in seconds, using the
         * provided clock as the source of the current time.
         */
        @NonNull
        @VisibleForTesting
        public Builder setAccessTokenExpiresIn(@Nullable Long expiresIn, @NonNull Clock clock) {
            if (expiresIn == null) {
                mAccessTokenExpirationTime = null;
            } else {
                mAccessTokenExpirationTime = clock.getCurrentTimeMillis()
                        + TimeUnit.SECONDS.toMillis(expiresIn);
            }
            return this;
        }

        /**
         * Specifies the expiration time of the OAuth 2 access token.
         */
        @NonNull
        public Builder setAccessTokenExpirationTime(@Nullable Long expirationTime) {
            mAccessTokenExpirationTime = expirationTime;
            return this;
        }

        /**
         * Specifies the OAuth 2 Id token.
         */
        @NonNull
        public Builder setIdToken(@Nullable String idToken) {
            checkNullOrNotEmpty(idToken, "idToken cannot be empty");
            mIdToken = idToken;
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
         * @see <a href="https://tools.ietf.org/html/rfc6749#section-3.3"> "The OAuth 2.0
         * Authorization Framework" (RFC 6749), Section 3.3</a>
         */
        @NonNull
        public Builder setScopes(String... scopes) {
            if (scopes == null) {
                mScope = null;
            } else {
                setScopes(Arrays.asList(scopes));
            }
            return this;
        }

        /**
         * Specifies the set of case-sensitive scopes. Replaces any previously specified set of
         * scopes. Individual scope strings cannot be null or empty.
         *
         * @see <a href="https://tools.ietf.org/html/rfc6749#section-3.3"> "The OAuth 2.0
         * Authorization
         * Framework" (RFC 6749), Section 3.3</a>
         */
        @NonNull
        public Builder setScopes(@Nullable Iterable<String> scopes) {
            mScope = ScopeUtil.scopeIterableToString(scopes);
            return this;
        }

        /**
         * Specifies the additional set of parameters received as part of the response.
         */
        @NonNull
        public Builder setAdditionalParameters(@Nullable Map<String, String> additionalParameters) {
            mAdditionalParameters = new LinkedHashMap<>();
            if (additionalParameters == null) {
                return this;
            }

            for (Map.Entry<String, String> entry : additionalParameters.entrySet()) {
                checkMapEntryFullyDefined(entry,
                        "Additional parameters must have non-null keys and non-null values");
                checkArgument(!BUILT_IN_KEYS.contains(entry.getKey()),
                        "Additional parameter keys must not conflict with built in keys");
                mAdditionalParameters.put(entry.getKey(), entry.getValue());
            }

            return this;
        }

        /**
         * Builds the Authorization object.
         */
        @NonNull
        public AuthorizationResponse build() {
            return new AuthorizationResponse(
                    mRequest,
                    mState,
                    mTokenType,
                    mAuthorizationCode,
                    mAccessToken,
                    mAccessTokenExpirationTime,
                    mIdToken,
                    mScope,
                    Collections.unmodifiableMap(mAdditionalParameters));
        }
    }

    private AuthorizationResponse(
            @NonNull AuthorizationRequest request,
            @Nullable String state,
            @Nullable String tokenType,
            @Nullable String authorizationCode,
            @Nullable String accessToken,
            @Nullable Long accessTokenExpirationTime,
            @Nullable String idToken,
            @Nullable String scope,
            @NonNull Map<String, String> additionalParameters) {
        this.request = request;
        this.state = state;
        this.tokenType = tokenType;
        this.authorizationCode = authorizationCode;
        this.accessToken = accessToken;
        this.accessTokenExpirationTime = accessTokenExpirationTime;
        this.idToken = idToken;
        this.scope = scope;
        this.additionalParameters = additionalParameters;
    }

    /**
     * Determines whether the returned access token has expired.
     */
    public boolean hasAccessTokenExpired() {
        return hasAccessTokenExpired(SystemClock.INSTANCE);
    }

    @VisibleForTesting
    boolean hasAccessTokenExpired(@NonNull Clock clock) {
        return accessTokenExpirationTime != null
                && checkNotNull(clock).getCurrentTimeMillis() > accessTokenExpirationTime;
    }

    /**
     * Derives the set of scopes from the consolidated, space-delimited scopes in the
     * {@link #scope} field. If no scopes were specified on this response, the method will
     * return {@code null}.
     */
    @Nullable
    public Set<String> getScopeSet() {
        return ScopeUtil.scopeStringToSet(scope);
    }

    /**
     * Creates a follow-up request to exchange a received authorization code for tokens.
     */
    @NonNull
    public TokenRequest createTokenExchangeRequest() {
        return createTokenExchangeRequest(Collections.<String, String>emptyMap());
    }

    /**
     * Creates a follow-up request to exchange a received authorization code for tokens, including
     * the provided additional parameters.
     */
    @NonNull
    public TokenRequest createTokenExchangeRequest(
            @NonNull Map<String, String> additionalExchangeParameters) {
        Preconditions.checkNotNull(additionalExchangeParameters,
                "additionalExchangeParameters cannot be null");

        if (authorizationCode == null) {
            throw new IllegalStateException("authorizationCode not available for exchange request");
        }

        return new TokenRequest.Builder(
                request.configuration,
                request.clientId)
                .setGrantType(TokenRequest.GRANT_TYPE_AUTHORIZATION_CODE)
                .setRedirectUri(request.redirectUri)
                .setScope(request.scope)
                .setCodeVerifier(request.codeVerifier)
                .setAuthorizationCode(authorizationCode)
                .setAdditionalParameters(additionalExchangeParameters)
                .build();
    }

    /**
     * Converts the response to a JSON object for storage or transmission.
     */
    @NonNull
    public JSONObject toJson() {
        JSONObject json = new JSONObject();
        JsonUtil.put(json, KEY_REQUEST, request.toJson());
        JsonUtil.putIfNotNull(json, KEY_STATE, state);
        JsonUtil.putIfNotNull(json, KEY_TOKEN_TYPE, tokenType);
        JsonUtil.putIfNotNull(json, KEY_AUTHORIZATION_CODE, authorizationCode);
        JsonUtil.putIfNotNull(json, KEY_ACCESS_TOKEN, accessToken);
        JsonUtil.putIfNotNull(json, KEY_EXPIRES_AT, accessTokenExpirationTime);
        JsonUtil.putIfNotNull(json, KEY_ID_TOKEN, idToken);
        JsonUtil.putIfNotNull(json, KEY_SCOPE, scope);
        JsonUtil.put(json, KEY_ADDITIONAL_PARAMETERS,
                JsonUtil.mapToJsonObject(additionalParameters));
        return json;
    }

    /**
     * Converts the response to a JSON string for storage or transmission.
     */
    @NonNull
    public String toJsonString() {
        return toJson().toString();
    }

    /**
     * Reads an authorization response from a JSON string representation produced by
     * {@link #toJsonString()}.
     * @throws JSONException if the provided JSON does not match the expected structure.
     */
    @NonNull
    public static AuthorizationResponse fromJson(@NonNull String jsonStr) throws JSONException {
        return fromJson(new JSONObject(jsonStr));
    }

    /**
     * Reads an authorization response from a JSON object representation produced by
     * {@link #toJson()}.
     * @throws JSONException if the provided JSON does not match the expected structure.
     */
    @NonNull
    public static AuthorizationResponse fromJson(@NonNull JSONObject json) throws JSONException {
        if (!json.has(KEY_REQUEST)) {
            throw new IllegalArgumentException(
                "token request not provided and not found in JSON");
        }

        AuthorizationRequest request =
                AuthorizationRequest.fromJson(json.getJSONObject(KEY_REQUEST));

        return new AuthorizationResponse.Builder(request)
                .setTokenType(JsonUtil.getStringIfDefined(json, KEY_TOKEN_TYPE))
                .setAccessToken(JsonUtil.getStringIfDefined(json, KEY_ACCESS_TOKEN))
                .setAuthorizationCode(JsonUtil.getStringIfDefined(json, KEY_AUTHORIZATION_CODE))
                .setIdToken(JsonUtil.getStringIfDefined(json, KEY_ID_TOKEN))
                .setScope(JsonUtil.getStringIfDefined(json, KEY_SCOPE))
                .setState(JsonUtil.getStringIfDefined(json, KEY_STATE))
                .setAccessTokenExpirationTime(JsonUtil.getLongIfDefined(json, KEY_EXPIRES_AT))
                .setAdditionalParameters(
                        JsonUtil.getStringMap(json, KEY_ADDITIONAL_PARAMETERS))
                .build();
    }

    /**
     * Produces an intent containing this authorization response. Used to deliver the authorization
     * response to the registered handler after a call to
     * {@link AuthorizationService#performAuthorizationRequest}.
     */
    @NonNull
    public Intent toIntent() {
        Intent data = new Intent();
        data.putExtra(EXTRA_RESPONSE, this.toJsonString());
        return data;
    }

    /**
     * Extracts an authorization response from an intent produced by {@link #toIntent()}. Use
     * this to extract the response from the intent data passed to an activity registered as the
     * handler for {@link AuthorizationService#performAuthorizationRequest}.
     */
    @Nullable
    public static AuthorizationResponse fromIntent(@NonNull Intent dataIntent) {
        Preconditions.checkNotNull(dataIntent, "dataIntent must not be null");
        if (!dataIntent.hasExtra(EXTRA_RESPONSE)) {
            return null;
        }

        try {
            return AuthorizationResponse.fromJson(dataIntent.getStringExtra(EXTRA_RESPONSE));
        } catch (JSONException ex) {
            throw new IllegalArgumentException("Intent contains malformed auth response", ex);
        }
    }
}
