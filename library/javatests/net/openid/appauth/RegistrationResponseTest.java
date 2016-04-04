package net.openid.appauth;

import static net.openid.appauth.TestValues.TEST_APP_REDIRECT_URI;
import static net.openid.appauth.TestValues.TEST_CLIENT_ID;
import static net.openid.appauth.TestValues.TEST_CLIENT_SECRET;
import static net.openid.appauth.TestValues.TEST_CLIENT_SECRET_EXPIRES_AT;
import static net.openid.appauth.TestValues.getTestRegistrationRequest;
import static net.openid.appauth.TestValues.getTestServiceConfig;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.fail;

import android.net.Uri;

import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.runners.Enclosed;
import org.junit.runner.RunWith;
import org.robolectric.ParameterizedRobolectricTestRunner;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@RunWith(Enclosed.class)
@Config(manifest = Config.NONE)
public class RegistrationResponseTest {
    private static final Long TEST_CLIENT_ID_ISSUED_AT = 34L;
    private static final String TEST_REGISTRATION_ACCESS_TOKEN = "test_access_token";
    private static final String TEST_REGISTRATION_CLIENT_URI =
            "https://test.openid.com/register?client_id=" + TEST_CLIENT_ID;


    private static final String TEST_JSON = "{\n"
            + " \"client_id\": \"" + TEST_CLIENT_ID + "\",\n"
            + " \"client_id_issued_at\": \"" + TEST_CLIENT_ID_ISSUED_AT + "\",\n"
            + " \"client_secret\": \"" + TEST_CLIENT_SECRET + "\",\n"
            + " \"client_secret_expires_at\": \"" + TEST_CLIENT_SECRET_EXPIRES_AT + "\",\n"
            + " \"registration_access_token\": \"" + TEST_REGISTRATION_ACCESS_TOKEN + "\",\n"
            + " \"registration_client_uri\": \"" + TEST_REGISTRATION_CLIENT_URI + "\",\n"
            + " \"application_type\": " + RegistrationRequest.APPLICATION_TYPE_NATIVE + "\n"
            + "}";

    @RunWith(RobolectricTestRunner.class)
    @Config(manifest = Config.NONE)
    public static class RegistrationResponseSingleTest {
        private RegistrationResponse.Builder mMinimalBuilder;
        private JSONObject mJson;

        @Before
        public void setUp() throws Exception {
            mJson = new JSONObject(TEST_JSON);
            mMinimalBuilder = new RegistrationResponse.Builder(getTestRegistrationRequest());
        }

        @Test(expected = IllegalArgumentException.class)
        public void testBuilder_setAdditionalParams_withBuiltInParam() {
            mMinimalBuilder.setAdditionalParameters(
                    Collections.singletonMap(RegistrationResponse.PARAM_CLIENT_ID, "client1"));
        }

        @Test
        public void testFromJson() throws Exception {
            RegistrationResponse response = RegistrationResponse
                    .fromJson(getTestRegistrationRequest(), mJson);
            assertValues(response);
        }

        @Test
        public void testSerialize() throws Exception {
            JSONObject json = RegistrationResponse.fromJson(getTestRegistrationRequest(), mJson)
                    .serialize();

            assertThat(json.get(RegistrationResponse.KEY_REQUEST).toString())
                    .isEqualTo(getTestRegistrationRequest().serialize().toString());
            assertThat(json.getLong(RegistrationResponse.PARAM_CLIENT_ID_ISSUED_AT))
                    .isEqualTo(TEST_CLIENT_ID_ISSUED_AT);
            assertThat(json.getString(RegistrationResponse.PARAM_CLIENT_SECRET))
                    .isEqualTo(TEST_CLIENT_SECRET);
            assertThat(json.getLong(RegistrationResponse.PARAM_CLIENT_SECRET_EXPIRES_AT))
                    .isEqualTo(TEST_CLIENT_SECRET_EXPIRES_AT);
            assertThat(json.getString(RegistrationResponse.PARAM_REGISTRATION_ACCESS_TOKEN))
                    .isEqualTo(TEST_REGISTRATION_ACCESS_TOKEN);
            assertThat(JsonUtil.getUri(json, RegistrationResponse.PARAM_REGISTRATION_CLIENT_URI))
                    .isEqualTo(Uri.parse(TEST_REGISTRATION_CLIENT_URI));
        }

        @Test
        public void testSerialize_withAdditionalParameters() throws Exception {
            Map<String, String> additionalParameters = Collections.singletonMap("test1", "value1");
            JSONObject json = mMinimalBuilder.setClientId(TEST_CLIENT_ID)
                    .setAdditionalParameters(additionalParameters)
                    .build().serialize();
            assertThat(JsonUtil.getStringMap(json, RegistrationResponse.KEY_ADDITIONAL_PARAMETERS))
                    .isEqualTo(additionalParameters);
        }

        @Test(expected = IllegalArgumentException.class)
        public void testDeserialize_withoutRequest() throws Exception {
            RegistrationResponse.deserialize(mJson);
        }

        @Test
        public void testDeserialize() throws Exception {
            mJson.put(RegistrationResponse.KEY_REQUEST, getTestRegistrationRequest().serialize());
            RegistrationResponse response = RegistrationResponse.deserialize(mJson);
            assertValues(response);
        }

        @Test
        public void testHasExpired_withValidClientSecret() throws Exception {
            RegistrationResponse response = RegistrationResponse
                    .fromJson(getTestRegistrationRequest(), mJson);
            long now = TimeUnit.SECONDS.toMillis(TEST_CLIENT_SECRET_EXPIRES_AT - 1L);
            assertThat(response.hasClientSecretExpired(new TestClock(now))).isFalse();
        }

        @Test
        public void testHasExpired_withExpiredClientSecret() throws Exception {
            RegistrationResponse response = RegistrationResponse
                    .fromJson(getTestRegistrationRequest(), mJson);
            long now = TimeUnit.SECONDS.toMillis(TEST_CLIENT_SECRET_EXPIRES_AT + 1L);
            assertThat(response.hasClientSecretExpired(new TestClock(now))).isTrue();
        }

        private void assertValues(RegistrationResponse response) {
            assertThat(response.clientId).isEqualTo(TEST_CLIENT_ID);
            assertThat(response.clientIdIssuedAt).isEqualTo(TEST_CLIENT_ID_ISSUED_AT);
            assertThat(response.clientSecret).isEqualTo(TEST_CLIENT_SECRET);
            assertThat(response.clientSecretExpiresAt).isEqualTo(TEST_CLIENT_SECRET_EXPIRES_AT);
            assertThat(response.registrationAccessToken).isEqualTo(TEST_REGISTRATION_ACCESS_TOKEN);
            assertThat(response.registrationClientUri)
                    .isEqualTo(Uri.parse(TEST_REGISTRATION_CLIENT_URI));
            assertThat(response.additionalParameters)
                    .containsEntry(RegistrationRequest.PARAM_APPLICATION_TYPE,
                            RegistrationRequest.APPLICATION_TYPE_NATIVE);
        }
    }

    @RunWith(ParameterizedRobolectricTestRunner.class)
    @Config(manifest = Config.NONE)
    public static class RegistrationResponseParameterTest {
        private JSONObject mJson;
        private RegistrationRequest mMinimalRegistrationRequest;

        @Before
        public void setUp() throws Exception {
            mJson = new JSONObject(TEST_JSON);
            mMinimalRegistrationRequest = new RegistrationRequest.Builder(getTestServiceConfig(),
                    Arrays.asList(TEST_APP_REDIRECT_URI))
                    .build();
        }

        @Test
        public void testBuilder_fromJsonNWithMissingRequiredParameter() throws Exception {
            mJson.remove(mMissingParameter);
            try {
                RegistrationResponse.fromJson(mMinimalRegistrationRequest, mJson);
                fail("Expected MissingArgumentException not thrown.");
            } catch (RegistrationResponse.MissingArgumentException e) {
                assertThat(mMissingParameter).isEqualTo(e.getMissingField());
            }
        }

        @ParameterizedRobolectricTestRunner.Parameters(name = "Missing parameter = {0}")
        public static Collection<Object[]> data() {
            return Arrays.asList(new Object[][]{
                    {RegistrationResponse.PARAM_CLIENT_SECRET_EXPIRES_AT},
                    {RegistrationResponse.PARAM_REGISTRATION_ACCESS_TOKEN},
                    {RegistrationResponse.PARAM_REGISTRATION_CLIENT_URI}
            });
        }

        private String mMissingParameter;

        public RegistrationResponseParameterTest(String missingParameter) {
            mMissingParameter = missingParameter;
        }
    }
}


