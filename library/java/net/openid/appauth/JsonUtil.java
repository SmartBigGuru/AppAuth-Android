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

import static net.openid.appauth.Preconditions.checkMapEntryFullyDefined;
import static net.openid.appauth.Preconditions.checkNotNull;

import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Utility methods for JSON object manipulation, avoiding unnecessary checked exceptions.
 */
final class JsonUtil {

    private JsonUtil() {}

    public static void put(
            @NonNull JSONObject json,
            @NonNull String field,
            @NonNull int value) {
        checkNotNull(json, "json must not be null");
        checkNotNull(field, "field must not be null");
        checkNotNull(value, "value must not be null");

        try {
            json.put(field, value);
        } catch (JSONException ex) {
            throw new IllegalStateException("JSONException thrown in violation of contract, ex");
        }
    }

    public static void put(
            @NonNull JSONObject json,
            @NonNull String field,
            @NonNull String value) {
        checkNotNull(json, "json must not be null");
        checkNotNull(field, "field must not be null");
        checkNotNull(value, "value must not be null");
        try {
            json.put(field, value);
        } catch (JSONException ex) {
            throw new IllegalStateException("JSONException thrown in violation of contract", ex);
        }
    }

    public static void put(
            @NonNull JSONObject json,
            @NonNull String field,
            @NonNull JSONArray value) {
        checkNotNull(json, "json must not be null");
        checkNotNull(field, "field must not be null");
        checkNotNull(value, "value must not be null");
        try {
            json.put(field, value);
        } catch (JSONException ex) {
            throw new IllegalStateException("JSONException thrown in violation of contract", ex);
        }
    }

    public static void put(
            @NonNull JSONObject json,
            @NonNull String field,
            @NonNull JSONObject value) {
        checkNotNull(json, "json must not be null");
        checkNotNull(field, "field must not be null");
        checkNotNull(value, "value must not be null");
        try {
            json.put(field, value);
        } catch (JSONException ex) {
            throw new IllegalStateException("JSONException thrown in violation of contract", ex);
        }
    }

    public static void putIfNotNull(
            @NonNull JSONObject json,
            @NonNull String field,
            @Nullable String value) {
        checkNotNull(json, "json must not be null");
        checkNotNull(field, "field must not be null");
        if (value == null) {
            return;
        }
        try {
            json.put(field, value);
        } catch (JSONException ex) {
            throw new IllegalStateException("JSONException thrown in violation of contract", ex);
        }
    }

    public static void putIfNotNull(
            @NonNull JSONObject json,
            @NonNull String field,
            @Nullable Uri value) {
        checkNotNull(json, "json must not be null");
        checkNotNull(field, "field must not be null");
        if (value == null) {
            return;
        }
        try {
            json.put(field, value.toString());
        } catch (JSONException ex) {
            throw new IllegalStateException("JSONException thrown in violation of contract", ex);
        }
    }

    public static void putIfNotNull(
            @NonNull JSONObject json,
            @NonNull String field,
            @Nullable Long value) {
        checkNotNull(json, "json must not be null");
        checkNotNull(field, "field must not be null");
        if (value == null) {
            return;
        }
        try {
            json.put(field, value);
        } catch (JSONException ex) {
            throw new IllegalStateException("JSONException thrown in violation of contract", ex);
        }
    }

    public static void putIfNotNull(
            @NonNull JSONObject json,
            @NonNull String field,
            @Nullable JSONObject value) {
        checkNotNull(json, "json must not be null");
        checkNotNull(field, "field must not be null");
        if (value == null) {
            return;
        }
        try {
            json.put(field, value);
        } catch (JSONException ex) {
            throw new IllegalStateException("JSONException thrown in violation of contract", ex);
        }
    }

    @NonNull
    public static String getString(
            @NonNull JSONObject json,
            @NonNull String field)
            throws JSONException {
        checkNotNull(json, "json must not be null");
        checkNotNull(field, "field must not be null");
        if (!json.has(field)) {
            throw new JSONException("field \"" + field + "\" not found in json object");
        }

        String value = json.getString(field);
        if (value == null) {
            throw new JSONException("field \"" + field + "\" is mapped to a null value");
        }
        return value;
    }

    public static String getStringIfDefined(
            @NonNull JSONObject json,
            @NonNull String field) throws JSONException {
        checkNotNull(json, "json must not be null");
        checkNotNull(field, "field must not be null");
        if (!json.has(field)) {
            return null;
        }

        String value = json.getString(field);
        if (value == null) {
            throw new JSONException("field \"" + field + "\" is mapped to a null value");
        }
        return value;
    }

    public static Uri getUri(
            @NonNull JSONObject json,
            @NonNull String field)
            throws JSONException {
        checkNotNull(json, "json must not be null");
        checkNotNull(field, "field must not be null");

        String value = json.getString(field);
        if (value == null) {
            throw new JSONException("field \"" + field + "\" is mapped to a null value");
        }
        return Uri.parse(value);
    }

    @Nullable
    public static Uri getUriIfDefined(
            @NonNull JSONObject json,
            @NonNull String field)
            throws JSONException {
        checkNotNull(json, "json must not be null");
        checkNotNull(field, "field must not be null");
        if (!json.has(field)) {
            return null;
        }

        String value = json.getString(field);
        if (value == null) {
            throw new JSONException("field \"" + field + "\" is mapped to a null value");
        }

        return Uri.parse(value);
    }

    @Nullable
    public static Long getLongIfDefined(
            @NonNull JSONObject json,
            @NonNull String field)
            throws JSONException {
        checkNotNull(json, "json must not be null");
        checkNotNull(field, "field must not be null");
        if (!json.has(field)) {
            return null;
        }

        return json.getLong(field);
    }

    @NonNull
    public static ArrayList<String> getStringList(
            @NonNull JSONObject json,
            @NonNull String field) throws JSONException {
        checkNotNull(json, "json must not be null");
        checkNotNull(field, "field must not be null");
        if (!json.has(field)) {
            throw new JSONException("field \"" + field + "\" not found in json object");
        }

        JSONArray array = json.getJSONArray(field);
        return toStringList(array);
    }

    @NonNull
    public static Map<String, String> getStringMap(JSONObject json, String field)
            throws JSONException {
        LinkedHashMap<String, String> stringMap = new LinkedHashMap<>();
        checkNotNull(json, "json must not be null");
        checkNotNull(field, "field must not be null");
        if (!json.has(field)) {
            return stringMap;
        }

        JSONObject mapJson = json.getJSONObject(field);
        Iterator<String> mapKeys = mapJson.keys();
        while (mapKeys.hasNext()) {
            String key = mapKeys.next();
            String value = checkNotNull(mapJson.getString(key),
                    "additional parameter values must not be null");
            stringMap.put(key, value);
        }
        return stringMap;
    }

    @NonNull
    public static ArrayList<String> toStringList(@Nullable JSONArray jsonArray)
            throws JSONException {
        ArrayList<String> arrayList = new ArrayList<>();
        if (jsonArray != null) {
            for (int i = 0; i < jsonArray.length(); i++) {
                arrayList.add(checkNotNull(jsonArray.get(i)).toString());
            }
        }
        return arrayList;
    }

    @NonNull
    public static JSONArray toJsonArray(@NonNull ArrayList<String> strings) {
        checkNotNull(strings, "strings cannot be null");
        JSONArray jsonArray = new JSONArray();
        for (String str : strings) {
            jsonArray.put(str);
        }
        return jsonArray;
    }

    @NonNull
    public static JSONObject mapToJsonObject(@NonNull Map<String, String> map) {
        checkNotNull(map);
        JSONObject json = new JSONObject();
        for (Map.Entry<String, String> entry : map.entrySet()) {
            checkMapEntryFullyDefined(entry, "map entries must not have null keys or values");
            JsonUtil.put(json, entry.getKey(), entry.getValue());
        }
        return json;
    }

    public static <T> T get(JSONObject json, Field<T> field) {
        try {
            if (!json.has(field.key)) {
                return field.defaultValue;
            }
            return field.convert(json.getString(field.key));
        } catch (JSONException e) {
            // all appropriate steps are taken above to avoid a JSONException. If it is still
            // thrown, indicating an implementation change, throw an exception
            throw new IllegalStateException("unexpected JSONException", e);
        }
    }

    public static <T> List<T> get(JSONObject json, ListField<T> field) {
        try {
            if (!json.has(field.key)) {
                return field.defaultValue;
            }
            Object value = json.get(field.key);
            if (!(value instanceof JSONArray)) {
                throw new IllegalStateException(field.key
                        + " does not contain the expected JSON array");
            }
            JSONArray arrayValue = (JSONArray) value;
            ArrayList<T> values = new ArrayList<>();
            for (int i = 0; i < arrayValue.length(); i++) {
                values.add(field.convert(arrayValue.getString(i)));
            }
            return values;
        } catch (JSONException e) {
            // all appropriate steps are taken above to avoid a JSONException. If it is still
            // thrown, indicating an implementation change, throw an excpetion
            throw new IllegalStateException("unexpected JSONException", e);
        }
    }

    abstract static class Field<T> {
        /**
         * The metadata key within the discovery document.
         */
        public final String key;

        /**
         * The default value for this metadata entry, as defined by the OpenID Connect
         * specification.
         */
        public final T defaultValue;

        /**
         * Creates a metadata value abstraction with the given key and default value.
         */
        Field(String key, T defaultValue) {
            this.key = key;
            this.defaultValue = defaultValue;
        }

        /**
         * Converts the string representation of the value to the correct type.
         */
        abstract T convert(String value);
    }

    static final class UriField extends Field<Uri> {
        /**
         * Creates a metadata value abstraction with the given key and default URI value.
         */
        UriField(String key, Uri defaultValue) {
            super(key, defaultValue);
        }

        /**
         * Creates a metadata abstraction with the given key and a null URI default value.
         */
        UriField(String key) {
            this(key, null);
        }

        @Override
        Uri convert(String value) {
            return Uri.parse(value);
        }
    }

    static final class StringField extends Field<String> {
        /**
         * Creates a metadata abstraction with the given key and string default value.
         */
        StringField(String key, String defaultValue) {
            super(key, defaultValue);
        }

        /**
         * Creates a metadata abstraction with the given key and a null string default value.
         */
        StringField(String key) {
            this(key, null);
        }

        @Override
        String convert(String value) {
            return value;
        }
    }

    static final class BooleanField extends Field<Boolean> {

        /**
         * Creates a metadata abstraction with the given key and default boolean value.
         */
        BooleanField(String key, boolean defaultValue) {
            super(key, defaultValue);
        }

        @Override
        Boolean convert(String value) {
            return Boolean.parseBoolean(value);
        }
    }

    abstract static class ListField<T> {
        public final String key;
        public final List<T> defaultValue;

        ListField(String key, List<T> defaultValue) {
            this.key = key;
            this.defaultValue = defaultValue;
        }

        abstract T convert(String value);
    }

    static final class StringListField extends ListField<String> {

        StringListField(String key) {
            super(key, null);
        }

        StringListField(String key, List<String> defaultValue) {
            super(key, defaultValue);
        }

        @Override
        String convert(String value) {
            return value;
        }
    }
}
