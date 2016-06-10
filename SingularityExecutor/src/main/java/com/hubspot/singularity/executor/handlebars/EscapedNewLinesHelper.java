package com.hubspot.singularity.executor.handlebars;

import java.io.IOException;

import com.github.jknack.handlebars.Helper;
import com.github.jknack.handlebars.Options;

public class EscapedNewLinesHelper implements Helper<Object> {

  public static final String NAME = "escapedNewLines";

  @Override
  public CharSequence apply(Object context, Options options) throws IOException {
    if (context == null) {
      return "\"\"";
    }

    final StringBuilder sb = new StringBuilder();

    sb.append('"');

    for (char c : context.toString().toCharArray()) {
      if (c == '\n') {
        sb.append('\\');
        sb.append('n');
      } else {
        sb.append(c);
      }
    }

    sb.append('"');

    return sb.toString();
  }
}
