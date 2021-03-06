/*
 * Copyright 2010 Proofpoint, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.proofpoint.discovery.store;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.auto.value.AutoValue;
import com.google.common.base.MoreObjects;
import com.proofpoint.discovery.Service;
import com.proofpoint.json.JsonCodec;

import javax.annotation.Nullable;
import java.util.List;

import static com.google.common.base.Preconditions.checkArgument;
import static java.nio.charset.StandardCharsets.UTF_8;

@AutoValue
public abstract class Entry
{
    private static final JsonCodec<List<Service>> SERVICE_LIST_CODEC = JsonCodec.listJsonCodec(Service.class);

    @JsonCreator
    public static Entry entry(@JsonProperty("key") byte[] key,
            @Nullable @JsonProperty("value") byte[] value,
            @JsonProperty("timestamp") long timestamp,
            @Nullable @JsonProperty("maxAgeInMs") Long maxAgeInMs,
            @Nullable @JsonProperty("announcer") String announcer)
    {
        checkArgument(maxAgeInMs == null || maxAgeInMs > 0, "maxAgeInMs must be greater than 0");
        return new AutoValue_Entry(key, value == null ? null : SERVICE_LIST_CODEC.fromJson(value), timestamp, maxAgeInMs,
                announcer);
    }

    public static Entry entry(byte[] key,
            @Nullable List<Service> services,
            long timestamp,
            @Nullable Long maxAgeInMs,
            @Nullable String announcer)
    {
        checkArgument(maxAgeInMs == null || maxAgeInMs > 0, "maxAgeInMs must be greater than 0");
        return new AutoValue_Entry(key, services, timestamp, maxAgeInMs, announcer);
    }

    @JsonProperty
    @SuppressWarnings("mutable")
    public abstract byte[] getKey();

    @Nullable
    @JsonProperty("value")
    public byte[] getBytesValue()
    {
        List<Service> value = getValue();
        if (value == null) {
            return null;
        }
        return SERVICE_LIST_CODEC.toJsonBytes(value);
    }

    @Nullable
    public abstract List<Service> getValue();

    @JsonProperty
    public abstract long getTimestamp();

    @Nullable
    @JsonProperty
    public abstract Long getMaxAgeInMs();

    @Nullable
    @JsonProperty
    public abstract String getAnnouncer();

    @Override
    public String toString()
    {
        return MoreObjects.toStringHelper(this)
                .add("key", new String(getKey(), UTF_8))
                .add("value", getValue())
                .add("timestamp", getTimestamp())
                .add("maxAgeInMs", getMaxAgeInMs())
                .add("announcer", getAnnouncer())
                .toString();
    }
}

