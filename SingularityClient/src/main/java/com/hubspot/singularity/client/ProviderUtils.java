package com.hubspot.singularity.client;

import javax.inject.Provider;

class ProviderUtils
{
  public static <T> Provider<T> of(final T instance) { // XXX: this seems like it should be in a base library somewhere?
    return new Provider<T>() {
      @Override
      public T get()
      {
        return instance;
      }
    };
  }
}
