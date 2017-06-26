package com.hubspot.singularity;

import java.util.List;

import com.google.common.base.Optional;
import com.google.common.collect.Lists;

public final class JsonHelpers {

  private JsonHelpers() {
    throw new AssertionError("do not instantiate");
  }

  public static <T> Optional<List<T>> copyOfList(Optional<List<T>> list) {
    return list.isPresent() ? Optional.of(Lists.newArrayList(list.get())) : list;
  }

}
