package com.hubspot.singularity.data.transcoders;

import javax.annotation.Nullable;

public interface Transcoder<T> {

  T fromBytes(@Nullable byte[] data) throws SingularityTranscoderException;

  byte[] toBytes(@Nullable T object) throws SingularityTranscoderException;

}
