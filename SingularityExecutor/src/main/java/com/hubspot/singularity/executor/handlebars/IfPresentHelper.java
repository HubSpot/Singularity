package com.hubspot.singularity.executor.handlebars;

import java.io.IOException;

import com.github.jknack.handlebars.Helper;
import com.github.jknack.handlebars.Options;
import com.google.common.base.Optional;

public class IfPresentHelper implements Helper<Object> {
  public static final Helper<Object> INSTANCE = new IfPresentHelper();

  public static final String NAME = "ifPresent";

  @Override
  public CharSequence apply(Object context, Options options) throws IOException {
    if ((context != null) && (context instanceof Optional)) {
      final Optional<Object> maybeContext = (Optional<Object>)context;

      if (maybeContext.isPresent()) {
        return options.fn(maybeContext.get());
      } else {
        return options.inverse();
      }
    } else {
      if (context != null) {
        return options.fn(context);
      } else {
        return options.inverse();
      }
    }
  }
}
