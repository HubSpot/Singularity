package com.hubspot.singularity.data.transcoders;

import static java.lang.String.format;

public final class SingularityTranscoderException extends RuntimeException {

  private static final long serialVersionUID = 1L;

  public SingularityTranscoderException() {
    super();
  }

  public SingularityTranscoderException(final String msg, final Object... args) {
    super(msg == null ? "" : format(msg, args));
  }

  public SingularityTranscoderException(final Throwable t) {
    super(t);
  }

  public SingularityTranscoderException(final Throwable t, final String msg, final Object... args) {
    super(msg == null ? "" : format(msg, args), t);
  }
}
