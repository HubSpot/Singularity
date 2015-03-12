package com.hubspot.singularity.smtp;

import static java.nio.charset.StandardCharsets.UTF_8;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;

import de.neuland.jade4j.template.TemplateLoader;

public final class JadeTemplateLoader {
  private JadeTemplateLoader() {
    throw new AssertionError("do not instantiate");
  }

  public static final TemplateLoader JADE_LOADER = new TemplateLoader() {

    @Override
    public Reader getReader(String name) throws IOException {
      return new InputStreamReader(ClassLoader.getSystemResourceAsStream(name), UTF_8);
    }

    @Override
    public long getLastModified(String name) throws IOException {
      return -1;
    }
  };


}
