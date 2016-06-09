package com.hubspot.singularity.executor.handlebars;


import java.io.IOException;

import com.github.jknack.handlebars.Helper;
import com.github.jknack.handlebars.Options;

public class IfHasNewLinesHelper implements Helper<Object> {

  public static final String NAME = "ifHasNewLines";

  @SuppressWarnings("unchecked")
  @Override
  public CharSequence apply(Object context, Options options) throws IOException {
    if (context.toString().contains("\n")) {
      return options.fn();
    } else {
      return options.inverse();
    }
  }

}
