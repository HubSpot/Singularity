package com.hubspot.singularity.executor;

import static java.nio.charset.StandardCharsets.UTF_8;

import java.io.BufferedWriter;
import java.nio.file.Files;
import java.nio.file.Path;

import com.github.jknack.handlebars.Template;
import com.google.common.base.Throwables;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import com.hubspot.singularity.executor.config.SingularityExecutorModule;
import com.hubspot.singularity.executor.models.DockerContext;
import com.hubspot.singularity.executor.models.EnvironmentContext;
import com.hubspot.singularity.executor.models.LogrotateTemplateContext;
import com.hubspot.singularity.executor.models.RunnerContext;

@Singleton
public class TemplateManager {

  private final Template runnerTemplate;
  private final Template environmentTemplate;
  private final Template logrotateTemplate;
  private final Template dockerTemplate;

  @Inject
  public TemplateManager(@Named(SingularityExecutorModule.RUNNER_TEMPLATE) Template runnerTemplate,
                         @Named(SingularityExecutorModule.ENVIRONMENT_TEMPLATE) Template environmentTemplate,
                         @Named(SingularityExecutorModule.LOGROTATE_TEMPLATE) Template logrotateTemplate,
                         @Named(SingularityExecutorModule.DOCKER_TEMPLATE) Template dockerTemplate
                         ) {
    this.runnerTemplate = runnerTemplate;
    this.environmentTemplate = environmentTemplate;
    this.logrotateTemplate = logrotateTemplate;
    this.dockerTemplate = dockerTemplate;
  }

  public void writeRunnerScript(Path destination, RunnerContext runnerContext) {
    writeTemplate(destination, runnerTemplate, runnerContext);
  }

  public void writeEnvironmentScript(Path destination, EnvironmentContext environmentContext) {
    writeTemplate(destination, environmentTemplate, environmentContext);
  }

  public void writeLogrotateFile(Path destination, LogrotateTemplateContext logRotateContext) {
    writeTemplate(destination, logrotateTemplate, logRotateContext);
  }

  public void writeDockerScript(Path destination, DockerContext dockerContext) {
    writeTemplate(destination, dockerTemplate, dockerContext);
  }

  private void writeTemplate(Path path, Template template, Object context) {
    try (final BufferedWriter writer = Files.newBufferedWriter(path, UTF_8)) {
      template.apply(context, writer);
    } catch (Exception e) {
      throw Throwables.propagate(e);
    }
  }

}
