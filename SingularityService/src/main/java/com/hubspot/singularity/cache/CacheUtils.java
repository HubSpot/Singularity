package com.hubspot.singularity.cache;

import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import com.google.common.base.Optional;
import com.google.common.collect.MapDifference;
import com.google.common.collect.Maps;
import com.google.inject.Inject;

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
import io.atomix.utils.serializer.Namespaces;
import io.atomix.utils.serializer.Serializer;

public class CacheUtils {
  private static final Logger LOG = LoggerFactory.getLogger(CacheUtils.class);

  private final Serializer serializer;

  @Inject
  public CacheUtils() {
    this.serializer = getSerializer();
  }

  /*
   * Add our own serializer for Optional Present and Absent classes. Kryo does not use
   * Java or GWT serialization but instead uses bytecode generation to call the private constructor
   * directly. This breaks things like Absent.equals since it is implemented as this == object
   * and expects the objects to be the same instance
   */
  private Serializer getSerializer() {
    try {
      return Serializer.builder()
          .withNamespace(Namespaces.BASIC)
          .withRegistrationRequired(false)
          .withCompatibleSerialization()
          .addSerializer(new GuavaOptionalSerializer(), Class.forName("com.google.common.base.Present"), Class.forName("com.google.common.base.Absent"))
          .build();
    } catch (ClassNotFoundException t) {
      LOG.error("Guava classes not found", t);
      throw new RuntimeException(t);
    }
  }

  public static class GuavaOptionalSerializer extends com.esotericsoftware.kryo.Serializer<Optional> {
    {
      setAcceptsNull(false);
    }

    public void write(Kryo kryo, Output output, Optional object) {
      Object nullable = object.isPresent() ? object.get() : null;
      kryo.writeClassAndObject(output, nullable);
    }

    public Optional read(Kryo kryo, Input input, Class type) {
      return Optional.fromNullable(kryo.readClassAndObject(input));
    }

    public Optional copy(Kryo kryo, Optional original) {
      if (original.isPresent()) {
        return Optional.of(kryo.copy(original.get()));
      }
      return original;
    }
  }

  <K, V> DistributedMap<K, V> newAtomixMap(Atomix atomix, String name, Class<K> keyClass, Class<V> valueClass, int cacheSize) {
      DistributedMapBuilder<K, V> distributedMapBuilder = atomix.<K, V>mapBuilder(name)
          .withNullValues(false)
          .withKeyType(keyClass)
          .withValueType(valueClass)
          .withCacheEnabled(true)
          .withCacheSize(cacheSize)
          .withCompatibleSerialization()
          .withSerializer(serializer)
          .withRegistrationRequired(false)
          .withProtocol(MultiPrimaryProtocol.builder("in-memory-data")
              .withBackups(3) // TODO count for this?
              .withReplication(Replication.ASYNCHRONOUS)
              .withConsistency(Consistency.EVENTUAL).build());

      return distributedMapBuilder.build();
  }

  <T> DistributedSet<T> newAtomixSet(Atomix atomix, String name, Class<T> clazz) {
    DistributedSetBuilder<T> setBuilder = atomix.<T>setBuilder(name)
        .withRegistrationRequired(false)
        .withSerializer(serializer)
        .withElementType(clazz)
        .withCompatibleSerialization()
        .withProtocol(MultiPrimaryProtocol.builder("in-memory-data")
            .withBackups(3)
            .withReplication(Replication.ASYNCHRONOUS)
            .withConsistency(Consistency.EVENTUAL).build());

    return setBuilder.build();
  }

  <T> AtomicValue<T> newAtomicValue(Atomix atomix, String name, Class<T> clazz) {
    AtomicValueBuilder<T> valueBuilder = atomix.<T>atomicValueBuilder(name)
        .withRegistrationRequired(false)
        .withSerializer(serializer)
        .withProtocol(MultiPrimaryProtocol.builder("in-memory-data")
            .withBackups(3)
            .withReplication(Replication.ASYNCHRONOUS)
            .withConsistency(Consistency.EVENTUAL).build())
        .withValueType(clazz);
    return valueBuilder.build();
  }

  static <K, V> void syncMaps(Map<K, V> existing, Map<K, V> desired) {
    if (existing.isEmpty()) {
      desired.forEach(existing::put); // putAll not supported by atomix DelegatingAsyncDistributedMap
    } else {
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
  }

  static <T> void syncCollections(Set<T> existing, Collection<T> desired) {
    if (existing.isEmpty()) {
      existing.addAll(desired);
    } else {
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
}
