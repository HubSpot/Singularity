package com.hubspot.singularity.cache;

import io.atomix.core.Atomix;
import io.atomix.core.map.DistributedMap;
import io.atomix.core.map.DistributedMapBuilder;
import io.atomix.core.set.DistributedSet;
import io.atomix.core.set.DistributedSetBuilder;
import io.atomix.primitive.Consistency;
import io.atomix.primitive.Replication;
import io.atomix.protocols.backup.MultiPrimaryProtocol;

public class CacheObjectBuilder {

  public static <K, V> DistributedMap<K, V> newAtomixMap(Atomix atomix, String name, Class<K> keyClass, Class<V> valueClass, int cacheSize) {
    DistributedMapBuilder<K, V> distributedMapBuilder = atomix.<K, V>mapBuilder(name)
        .withNullValues(false)
        .withKeyType(keyClass)
        .withValueType(valueClass)
        .withCacheEnabled(true)
        .withCacheSize(cacheSize)
        .withCompatibleSerialization()
        .withRegistrationRequired(false)
        .withProtocol(MultiPrimaryProtocol.builder("in-memory-data")
            .withBackups(3) // TODO count for this?
            .withReplication(Replication.ASYNCHRONOUS)
            .withConsistency(Consistency.EVENTUAL).build());

    return distributedMapBuilder.build();
  }

  public static <T> DistributedSet<T> newAtomixSet(Atomix atomix, String name, Class<T> clazz) {
    DistributedSetBuilder<T> setBuilder = atomix.<T>setBuilder(name)
        .withRegistrationRequired(false)
        .withElementType(clazz)
        .withProtocol(MultiPrimaryProtocol.builder("in-memory-data")
            .withBackups(3)
            .withReplication(Replication.ASYNCHRONOUS)
            .withConsistency(Consistency.EVENTUAL).build());

    return setBuilder.build();
  }

}
