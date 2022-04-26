package com.hubspot.singularity.executor.handlebars;

import com.github.jknack.handlebars.Helper;
import com.github.jknack.handlebars.Options;
import java.io.IOException;

public class IfHasNewLinesOrBackticksHelper implements Helper<Object> {

  public static final String NAME = "ifHasNewLinesOrBackticks";

  @SuppressWarnings("unchecked")
  @Override
  public CharSequence apply(Object context, Options options) throws IOException {
    if (context.toString().contains("\n") || context.toString().contains("`")) {
      return options.fn();
    } else {
      return options.inverse();
    }
  }
}
