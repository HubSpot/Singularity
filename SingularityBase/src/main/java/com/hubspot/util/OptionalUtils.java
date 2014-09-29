package com.hubspot.util;

import java.util.Optional;

public class OptionalUtils {
  public static <T> Optional<T> convert(com.google.common.base.Optional<T> o)
  {
    return Optional.ofNullable(o.orNull());
  }

  @SafeVarargs
  public static <T> Optional<T> firstPresent(Optional<T>... os)
  {
    for (Optional<T> o : os) {
      if (o.isPresent()) {
        return o;
      }
    }
    return Optional.empty();
  }
}
