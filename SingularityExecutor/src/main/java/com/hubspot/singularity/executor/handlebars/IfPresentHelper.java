package com.hubspot.singularity.executor.handlebars;

import java.io.IOException;

import com.github.jknack.handlebars.Helper;
import com.github.jknack.handlebars.Options;
import com.google.common.base.Optional;

public class IfPresentHelper implements Helper<Object> {

  public static final String NAME = "ifPresent";

  @SuppressWarnings("unchecked")
  @Override
  public CharSequence apply(Object context, Options options) throws IOException {
    if (context instanceof Optional) {
      context = ((Optional<Object>) context).orNull();
    }

    if (context != null) {
      return options.fn(context);
    } else {
      return options.inverse();
    }
  }

}
