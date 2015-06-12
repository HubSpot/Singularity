package com.hubspot.singularity.executor.handlebars;

import java.io.IOException;

import com.github.jknack.handlebars.Helper;
import com.github.jknack.handlebars.Options;

public class IfStringsEqualHelper implements Helper<Object> {
  public static final String NAME = "ifStringsEqual";

  @Override
  public CharSequence apply(Object context, Options options) throws IOException {
    if (context.toString().equals(options.param(0).toString())) {
      return options.fn(context);
    } else {
      return options.inverse(context);
    }
  }
}
