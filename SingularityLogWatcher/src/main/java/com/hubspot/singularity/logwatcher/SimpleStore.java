package com.hubspot.singularity.logwatcher;

import java.io.Closeable;
import java.util.List;

import com.google.common.base.Optional;
import com.hubspot.singularity.runner.base.shared.TailMetadata;

public interface SimpleStore extends Closeable {

  @SuppressWarnings("serial")
  class StoreException extends RuntimeException {

    public StoreException(String message, Throwable cause) {
      super(message, cause);
    }

    public StoreException(String message) {
      super(message);
    }

    public StoreException(Throwable cause) {
      super(cause);
    }

    public StoreException() {
      super();
    }

  }

  void start();

  void markConsumed(TailMetadata tail) throws StoreException;

  void savePosition(TailMetadata tail, long position) throws StoreException;

  Optional<Long> getPosition(TailMetadata tail) throws StoreException;

  List<TailMetadata> getTails();

  void registerListener(TailMetadataListener listener);

  void removeListener(TailMetadataListener listener);

}
