package com.hubspot.singularity.executor.handlebars;

import java.io.IOException;

import com.github.jknack.handlebars.Helper;
import com.github.jknack.handlebars.Options;

public class EscapeNewLinesAndQuotesHelper implements Helper<Object> {

  public static final String NAME = "escapeNewLinesAndQuotes";

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
      } else if (c == '"') {
        sb.append('\\');
        sb.append('"');
      } else {
        sb.append(c);
      }
    }

    sb.append('"');

    return sb.toString();
  }
}
