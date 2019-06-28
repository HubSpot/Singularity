package com.hubspot.singularity.cache;

import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import com.google.common.collect.MapDifference;
import com.google.common.collect.Maps;

import io.atomix.core.Atomix;
import io.atomix.core.map.DistributedMap;
import io.atomix.core.map.DistributedMapBuilder;
import io.atomix.core.set.DistributedSet;
import io.atomix.core.set.DistributedSetBuilder;
import io.atomix.core.value.AtomicValue;
import io.atomix.core.value.AtomicValueBuilder;
import io.atomix.primitive.Consistency;
import io.atomix.primitive.Replication;
import io.atomix.protocols.backup.MultiPrimaryProtocol;

class CacheUtils {

  static <K, V> DistributedMap<K, V> newAtomixMap(Atomix atomix, String name, Class<K> keyClass, Class<V> valueClass, int cacheSize) {
    DistributedMapBuilder<K, V> distributedMapBuilder = atomix.<K, V>mapBuilder(name)
        .withNullValues(false)
        .withKeyType(keyClass)
        .withValueType(valueClass)
        .withCacheEnabled(true)
        .withCacheSize(cacheSize)
        .withRegistrationRequired(false)
        .withProtocol(MultiPrimaryProtocol.builder("in-memory-data")
            .withBackups(3) // TODO count for this?
            .withReplication(Replication.ASYNCHRONOUS)
            .withConsistency(Consistency.EVENTUAL).build());

    return distributedMapBuilder.build();
  }

  static <T> DistributedSet<T> newAtomixSet(Atomix atomix, String name, Class<T> clazz) {
    DistributedSetBuilder<T> setBuilder = atomix.<T>setBuilder(name)
        .withRegistrationRequired(false)
        .withElementType(clazz)
        .withProtocol(MultiPrimaryProtocol.builder("in-memory-data")
            .withBackups(3)
            .withReplication(Replication.ASYNCHRONOUS)
            .withConsistency(Consistency.EVENTUAL).build());

    return setBuilder.build();
  }

  static <T> AtomicValue<T> newAtomicValue(Atomix atomix, String name, Class<T> clazz) {
    AtomicValueBuilder<T> valueBuilder = atomix.<T>atomicValueBuilder(name)
        .withRegistrationRequired(false)
        .withProtocol(MultiPrimaryProtocol.builder("in-memory-data")
            .withBackups(3)
            .withReplication(Replication.ASYNCHRONOUS)
            .withConsistency(Consistency.EVENTUAL).build())
        .withValueType(clazz);
    return valueBuilder.build();
  }

  static <K, V> void syncMaps(Map<K, V> existing, Map<K, V> desired) {
    MapDifference<K, V> difference = Maps.difference(existing, desired);
    if (difference.areEqual()) {
      return;
    }
    for (K key : difference.entriesDiffering().keySet()) {
      existing.put(key, desired.get(key));
    }
    for (K key : difference.entriesOnlyOnRight().keySet()) {
      existing.put(key, desired.get(key));
    }
    difference.entriesOnlyOnLeft().keySet().forEach(existing::remove);
  }

  static <T> void syncCollections(Set<T> existing, Collection<T> desired) {
    existing.addAll(desired);
    Set<T> toRemove = new HashSet<>();
    for (T item : existing) {
      if (!desired.contains(item)) {
        toRemove.add(item);
      }
    }
    existing.removeAll(toRemove);
  }
}
