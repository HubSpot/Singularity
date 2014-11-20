package com.hubspot.singularity.data.transcoders;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;
import static java.lang.reflect.Modifier.isStatic;

import java.lang.reflect.Method;

import javax.annotation.Nullable;

import com.hubspot.singularity.SingularityId;

public class IdTranscoder<T extends SingularityId> implements Transcoder<T> {

  private static final byte[] EMPTY_BYTES = new byte[0];

  private final Method valueOfMethod;

  IdTranscoder(final Class<T> clazz) {
    checkNotNull(clazz, "clazz is null");
    try {
      // SingularityId classes must have a static "valueOf" method for object construction.
      this.valueOfMethod = clazz.getMethod("valueOf", String.class);
    } catch (ReflectiveOperationException e) {
      throw new IllegalStateException(e);
    }
    checkState(isStatic(valueOfMethod.getModifiers()), "found valueOf method in %s but it is not static!", clazz.getSimpleName());
  }

  @Override
  public T fromBytes(@Nullable byte[] data) throws SingularityTranscoderException {
    return fromString(StringTranscoder.INSTANCE.fromBytes(data));
  }

  @SuppressWarnings("unchecked")
  public T fromString(@Nullable String id) throws SingularityTranscoderException {
    if (id == null) {
      return null;
    }

    try {
      return (T) valueOfMethod.invoke(null, id);
    } catch (ReflectiveOperationException e) {
      throw new SingularityTranscoderException(e);
    }
  }

  @Override
  public byte[] toBytes(@Nullable T object) throws SingularityTranscoderException {
    return object == null ? EMPTY_BYTES : StringTranscoder.INSTANCE.toBytes(object.getId());
  }
}
