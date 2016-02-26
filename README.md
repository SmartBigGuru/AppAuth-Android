# AppAuth for Android

[![Build Status](https://www.bitrise.io/app/a09572dde99baf30.svg?token=DROliGDWTq1SbhaHTid9fw&branch=master)](https://www.bitrise.io/app/a09572dde99baf30)

AppAuth for Android is a client SDK for communicating with [OAuth 2.0]
(https://tools.ietf.org/html/rfc6749) and [OpenID Connect]
(http://openid.net/specs/openid-connect-core-1_0.html) providers. It strives to
directly map the requests and responses of those specifications, while following
the idiomatic style of the implementation language. In addition to mapping the
raw protocol flows, convenience methods are available to assist with common
tasks like performing an action with fresh tokens.

The library follows the best practices set out in [OAuth 2.0 for Native Apps]
(https://tools.ietf.org/html/draft-ietf-oauth-native-apps)
including using
[Custom Tabs](http://developer.android.com/tools/support-library/features.html#custom-tabs)
for the auth request. For this reason,
`WebView` is explicitly *not* supported due to usability and security reasons.

The library also supports the [PKCE](https://tools.ietf.org/html/rfc7636)
extension to OAuth which was created to secure authorization codes in public
clients when custom URI scheme redirects are used. The library is friendly to
other extensions (standard or otherwise) with the ability to handle additional
parameters in all protocol requests and responses.

## Specification

### Supported Android Versions

AppAuth supports Android API 16 (Jellybean) and above.

When a Custom Tabs implementation is provided by a browser on the device (for
example by
[Chrome](https://developer.chrome.com/multidevice/android/customtabs)), Custom
Tabs are used for authorization requests. Otherwise, the default browser is used
as a fallback.

### Authorization Server Support

Both Custom URI Schemes (all supported versions of Android) and App Links
(API 23+) can be used with the library.

In general, AppAuth can work with any Authorization Server (AS) that supports
[native apps](https://tools.ietf.org/html/draft-ietf-oauth-native-apps),
either through custom URI scheme redirects, or App Links.
AS's that assume all clients are web-based or require clients to maintain
confidentiality of the client secrets may not work well.

## Building the Project

### Prerequisites

The project requires the Android SDK for API level 23 (Marshmallow) to build,
though the produced binaries only require API level 16 (Jellybean) to be
used.

### Configure the Demo App

Follow the instructions in [app/README.md](app/README.md) to configure the
demo app with your own OAuth client (you need to update 3 configuration points
with your client info to try the demo).

### Building from the Command line

AppAuth for Android uses Gradle as its build system. In order to build
the library and app binaries, run `./gradlew assemble`.
The library AAR files are output to `library/build/outputs/aar`, while the
demo app is output to `app/build/outputs/apk`.
In order to run the tests and code analysis, run `./gradlew check`.

The build script attempts
to guess the location of your SDK by looking at the values of $ANDROID_SDK_HOME
and $ANDROID_HOME. If neither of these are defined or are not the SDK you
wish to use, you must create a `local.properties` file in the project root.
This file must define a property `sdk.dir` that points to your SDK root
directory. For example:

    sdk.dir=/path/to/android-sdk

### Building from Android Studio

In AndroidStudio, File -> New -> Import project. Select the root folder
(the one with the `build.gradle` file).

If you get an error like:
`Error:Could not find com.android.support:customtabs:23.2.0.` then be sure you
have installed the Android Support Library from the Android SDK Manager.
Follow the Android Studio prompts to resolve the dependencies automatically.

## Auth Flow

AppAuth supports both manual interaction with the Authorization Server
where you need to perform your own token exchanges, as well as convenience
methods that perform some of this logic for you. This example
performs a manual exchange, and stores the result as an `AuthState` object.

### Tracking authorization state

`AuthState` is a class that keeps track of the authorization and token
requests and responses, and provides a convenience method to call an API with
fresh tokens. This is the only object that you need to serialize to retain the
authorization state of the session. Typically, one would do this by storing
the authorization state in SharedPreferences or some other persistent store
private to the app:

```java
@NonNull public AuthState readAuthState() {
  SharedPreferences authPrefs = getSharedPreferences("auth", MODE_PRIVATE);
  String stateJson = authPrefs.getString("stateJson");
  AuthState state;
  if (stateStr != null) {
    return AuthState.fromJsonString(stateJson);
  } else {
    return new AuthState();
  }
}

public void writeAuthState(@NonNull AuthState state) {
  SharedPreferences authPrefs = getSharedPreferences("auth", MODE_PRIVATE);
  authPrefs.edit()
      .putString("stateJson", state.toJsonString())
      .apply();
}
```

### Configuration

You can configure AppAuth by specifying the endpoints directly:

```java
AuthorizationServiceConfiguration config =
        new AuthorizationServiceConfiguration(name, mAuthEndpoint, mTokenEndpoint);

// perform the auth request...
```

Or through discovery:

```java
Uri issuerUri = Uri.parse("https://accounts.google.com");
AuthorizationServiceConfiguration config =
    AuthorizationServiceConfiguration.fetchFromIssuer(
        issuerUri,
        new RetrieveConfigurationCallback() {
          @Override public void onFetchConfigurationCompleted(
              @Nullable AuthorizationServiceConfiguration serviceConfiguration,
              @Nullable AuthorizationException ex) {
            if (ex != null) {
                Log.w(TAG, "Failed to retrieve configuration for " + idp.name, ex);
            } else {
                // service configuration retrieved, proceed to authorization...
            }
          }
      });
```

### Authorizing

After configuring or retrieving an authorization service configuration,
an authorization request can be constructed for dispatch

```java
AuthorizationRequest req = new AuthorizationRequest.Builder(
    config,
    clientId,
    AuthorizationRequest.RESPONSE_TYPE_CODE,
    redirectUri);
```

Requests are dispatched with the help of `AuthorizationService`. As this
will open a custom tab or browser instance to fulfill this request, the
response is delivered via an intent to an activity of your choosing:

```java
AuthorizationService service = new AuthorizationService(context);
service.performAuthorizationRequest(
    req,
    new Intent(context, MyAuthResultHandlerActivity.class));
```

### Handling the Redirect

The response is delivered to the specified handler, and can be extracted
from the intent data:

```java
public void onCreate(Bundle b) {
  // ...
  AuthorizationResponse resp = AuthorizationResponse.fromIntent(getIntent());
  AuthorizationException ex = AuthorizationException.fromIntent(getIntent());
  if (resp != null) {
    // authorization succeeded
  } else {
    // authorization failed, check ex for more details
  }
  // ...
}
```

Given the auth response, a token request can be created to exchange the
authorization code:

```java
service.performTokenRequest(
    resp.createTokenExchangeRequest(),
    new AuthorizationService.TokenResponseCallback() {
      @Override public void onTokenRequestCompleted(
            TokenResponse resp, AuthorizationException ex) {
          if (resp != null) {
            // exchange succeeded
          } else {
            // authorization failed, check ex for more details
          }
        }
    });
```

### Making API Calls

With an updated AuthState based on the token exchange, it is then possible to
make requests using guaranteed fresh tokens at any future point:

```
AuthState state = readAuthState();
state.performActionWithFreshTokens(service, new AuthStateAction() {
  @Override public void execute(
      String accessToken,
      String idToken,
      AuthorizationException ex) {
    if (ex != null) {
      // negotiation for fresh tokens failed, check ex for more details
      return;
    }

    // use the access token to do something ...
  }
});
```

## API Documentation

Browse the [API documentation]
(http://openid.github.io/AppAuth-Android/docs/latest/).
