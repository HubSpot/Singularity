package com.hubspot.singularity.data.transcoders;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.reflect.TypeParameter;
import com.google.common.reflect.TypeToken;
import com.google.inject.Binder;
import com.google.inject.Inject;
import com.google.inject.Key;
import com.google.inject.Provider;
import com.google.inject.Scopes;
import com.hubspot.singularity.SingularityId;
import com.hubspot.singularity.config.SingularityConfiguration;

@SuppressWarnings("serial")
public final class SingularityJsonTranscoderBinder {

  public static <U> SingularityJsonTranscoderBinder bindTranscoder(Binder binder) {
    return new SingularityJsonTranscoderBinder(binder);
  }

  private final Binder binder;

  public SingularityJsonTranscoderBinder(Binder binder) {
    this.binder = binder;
  }

  public <T> void asJson(Class<T> clazz) {
    TypeToken<Transcoder<T>> typeToken = new TypeToken<Transcoder<T>>() {}.where(new TypeParameter<T>() {}, clazz);
    @SuppressWarnings("unchecked")
    Key<Transcoder<T>> key = (Key<Transcoder<T>>) Key.get(typeToken.getType());
    binder.bind(key).toProvider(new JsonTranscoderProvider<T>(clazz)).in(Scopes.SINGLETON);
  }

  public <T> void asCompressedJson(Class<T> clazz) {
    TypeToken<Transcoder<T>> typeToken = new TypeToken<Transcoder<T>>() {}.where(new TypeParameter<T>() {}, clazz);
    @SuppressWarnings("unchecked")
    Key<Transcoder<T>> key = (Key<Transcoder<T>>) Key.get(typeToken.getType());
    binder.bind(key).toProvider(new CompressingJsonTranscoderProvider<T>(clazz)).in(Scopes.SINGLETON);
  }

  public <T extends SingularityId> void asSingularityId(Class<T> clazz) {
    TypeToken<IdTranscoder<T>> typeToken = new TypeToken<IdTranscoder<T>>() {}.where(new TypeParameter<T>() {}, clazz);
    @SuppressWarnings("unchecked")
    Key<IdTranscoder<T>> key = (Key<IdTranscoder<T>>) Key.get(typeToken.getType());
    binder.bind(key).toInstance(new IdTranscoder<T>(clazz));
  }

  static class JsonTranscoderProvider<T> implements Provider<JsonTranscoder<T>> {
    private final Class<T> clazz;
    private ObjectMapper objectMapper;

    JsonTranscoderProvider(Class<T> clazz) {
      this.clazz = clazz;
    }

    @Inject
    void inject(ObjectMapper objectMapper) {
      this.objectMapper = checkNotNull(objectMapper, "objectMapper is null");
    }

    @Override
    public JsonTranscoder<T> get() {
      checkState(objectMapper != null, "objectMapper was never injected!");

      return new JsonTranscoder<T>(objectMapper, clazz);
    }
  }

  static class CompressingJsonTranscoderProvider<T> implements Provider<CompressingJsonTranscoder<T>> {
    private final Class<T> clazz;
    private ObjectMapper objectMapper;
    private SingularityConfiguration singularityConfiguration;

    CompressingJsonTranscoderProvider(Class<T> clazz) {
      this.clazz = clazz;
    }

    @Inject
    void inject(ObjectMapper objectMapper, SingularityConfiguration singularityConfiguration) {
      this.objectMapper = checkNotNull(objectMapper, "objectMapper is null");
      this.singularityConfiguration = checkNotNull(singularityConfiguration, "singularityConfiguration is null");
    }

    @Override
    public CompressingJsonTranscoder<T> get() {
      checkState(objectMapper != null, "objectMapper was never injected!");
      checkState(singularityConfiguration != null, "singularityConfiguration was never injected!");

      return new CompressingJsonTranscoder<T>(singularityConfiguration, objectMapper, clazz);
    }
  }

}
