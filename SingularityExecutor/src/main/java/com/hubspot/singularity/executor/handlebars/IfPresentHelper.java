package com.hubspot.singularity.executor.handlebars;

import java.io.IOException;
import java.util.Optional;

import com.github.jknack.handlebars.Helper;
import com.github.jknack.handlebars.Options;

public class IfPresentHelper implements Helper<Object> {

  public static final String NAME = "ifPresent";

  @SuppressWarnings("unchecked")
  @Override
  public CharSequence apply(Object context, Options options) throws IOException {
    if (context instanceof Optional) {
      context = ((Optional<Object>) context).orElse(null);
    }

    if (context != null) {
      return options.fn(context);
    } else {
      return options.inverse();
    }
  }

}
