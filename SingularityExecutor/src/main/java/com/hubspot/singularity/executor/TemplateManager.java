package com.hubspot.singularity.executor;

import java.io.BufferedWriter;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;

import com.github.mustachejava.Mustache;
import com.google.common.base.Throwables;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import com.hubspot.singularity.executor.config.SingularityExecutorModule;
import com.hubspot.singularity.executor.models.EnvironmentContext;
import com.hubspot.singularity.executor.models.RunnerContext;

@Singleton
public class TemplateManager {
  
  private final Mustache runnerTemplate;
  private final Mustache environmentTemplate;

  @Inject
  public TemplateManager(@Named(SingularityExecutorModule.RUNNER_TEMPLATE) Mustache runnerTemplate,
                         @Named(SingularityExecutorModule.ENVIRONMENT_TEMPLATE) Mustache environmentTemplate) {
    this.runnerTemplate = runnerTemplate;
    this.environmentTemplate = environmentTemplate;
  }
  
  private void writeTemplate(Path path, Mustache template, Object context) {
    try (final BufferedWriter writer = Files.newBufferedWriter(path, Charset.defaultCharset())) {
      template.execute(writer, context);
    } catch (Exception e) {
      throw Throwables.propagate(e);
    }
  }

  public void writeRunnerScript(Path destination, RunnerContext context) {
    writeTemplate(destination, runnerTemplate, context);
  }

  public void writeEnvironmentScript(Path destination, EnvironmentContext environmentContext) {
    writeTemplate(destination, environmentTemplate, environmentContext);
  }
}
