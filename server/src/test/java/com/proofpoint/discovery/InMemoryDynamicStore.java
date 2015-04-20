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
package com.proofpoint.discovery;

import com.google.common.base.Supplier;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Maps;
import com.proofpoint.units.Duration;
import org.joda.time.DateTime;

import javax.annotation.concurrent.ThreadSafe;
import javax.inject.Inject;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Predicates.and;
import static com.google.common.collect.Collections2.transform;
import static com.google.common.collect.Iterables.filter;
import static com.proofpoint.discovery.DynamicServiceAnnouncement.toServiceWith;
import static com.proofpoint.discovery.Service.matchesPool;
import static com.proofpoint.discovery.Service.matchesType;

@ThreadSafe
public class InMemoryDynamicStore
        implements DynamicStore
{
    private final Map<Id<Node>, Entry> descriptors = Maps.newHashMap();
    private final Duration maxAge;
    private final Supplier<DateTime> currentTime;

    @Inject
    public InMemoryDynamicStore(DiscoveryConfig config, Supplier<DateTime> timeSource)
    {
        this.currentTime = timeSource;
        this.maxAge = config.getMaxAge();
    }

    @Override
    public synchronized void put(Id<Node> nodeId, DynamicAnnouncement announcement)
    {
        checkNotNull(nodeId, "nodeId is null");
        checkNotNull(announcement, "announcement is null");

        Set<Service> services = ImmutableSet.copyOf(transform(announcement.getServiceAnnouncements(), toServiceWith(nodeId, announcement.getLocation(), announcement.getPool())));

        DateTime expiration = currentTime.get().plusMillis((int) maxAge.toMillis());
        descriptors.put(nodeId, new Entry(expiration, services));
    }

    @Override
    public synchronized void delete(Id<Node> nodeId)
    {
        checkNotNull(nodeId, "nodeId is null");

        descriptors.remove(nodeId);
    }

    @Override
    public synchronized Set<Service> getAll()
    {
        removeExpired();

        ImmutableSet.Builder<Service> builder = ImmutableSet.builder();
        for (Entry entry : descriptors.values()) {
            builder.addAll(entry.getServices());
        }
        return builder.build();
    }

    @Override
    public synchronized Set<Service> get(String type)
    {
        checkNotNull(type, "type is null");

        return ImmutableSet.copyOf(filter(getAll(), matchesType(type)));
    }

    @Override
    public synchronized Set<Service> get(String type, String pool)
    {
        checkNotNull(type, "type is null");
        checkNotNull(pool, "pool is null");

        return ImmutableSet.copyOf(filter(getAll(), and(matchesType(type), matchesPool(pool))));
    }

    private synchronized void removeExpired()
    {
        Iterator<Entry> iterator = descriptors.values().iterator();

        DateTime now = currentTime.get();
        while (iterator.hasNext()) {
            Entry entry = iterator.next();

            if (now.isAfter(entry.getExpiration())) {
                iterator.remove();
            }
        }
    }

    private static class Entry
    {
        private final Set<Service> services;
        private final DateTime expiration;

        Entry(DateTime expiration, Set<Service> services)
        {
            this.expiration = expiration;
            this.services = ImmutableSet.copyOf(services);
        }

        public DateTime getExpiration()
        {
            return expiration;
        }

        public Set<Service> getServices()
        {
            return services;
        }
    }
}
