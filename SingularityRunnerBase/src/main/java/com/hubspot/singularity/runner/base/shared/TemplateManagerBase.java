package com.hubspot.singularity.runner.base.shared;

import java.io.BufferedWriter;
import java.nio.file.Files;
import java.nio.file.Path;

import com.github.mustachejava.Mustache;
import com.google.common.base.Throwables;
import com.hubspot.mesos.JavaUtils;

public class TemplateManagerBase {

  protected void writeTemplate(Path path, Mustache template, Object context) {
    try (final BufferedWriter writer = Files.newBufferedWriter(path, JavaUtils.CHARSET_UTF8)) {
      template.execute(writer, context);
    } catch (Exception e) {
      throw Throwables.propagate(e);
    }
  }

}
