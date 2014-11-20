package com.hubspot.singularity;

import java.util.List;
import java.util.Map;

import com.google.common.base.Optional;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

public final class JsonHelpers {

  private JsonHelpers() {
    throw new AssertionError("do not instantiate");
  }

  public static <T> Optional<List<T>> copyOfList(Optional<List<T>> list) {
    return list.isPresent() ? Optional.<List<T>> of(Lists.newArrayList(list.get())) : list;
  }

  public static <K, V> Optional<Map<K, V>> copyOfMap(Optional<Map<K, V>> map) {
    return map.isPresent() ? Optional.<Map<K, V>> of(Maps.newHashMap(map.get())) : map;
  }

}
