package com.hubspot.singularity.executor;

import java.nio.file.Path;

import com.github.mustachejava.Mustache;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import com.hubspot.singularity.executor.config.SingularityExecutorModule;
import com.hubspot.singularity.executor.models.EnvironmentContext;
import com.hubspot.singularity.executor.models.RunnerContext;
import com.hubspot.singularity.runner.base.config.TemplateManagerBase;

@Singleton
public class TemplateManager extends TemplateManagerBase {
  
  private final Mustache runnerTemplate;
  private final Mustache environmentTemplate;

  @Inject
  public TemplateManager(@Named(SingularityExecutorModule.RUNNER_TEMPLATE) Mustache runnerTemplate,
                         @Named(SingularityExecutorModule.ENVIRONMENT_TEMPLATE) Mustache environmentTemplate) {
    this.runnerTemplate = runnerTemplate;
    this.environmentTemplate = environmentTemplate;
  }
  
  public void writeRunnerScript(Path destination, RunnerContext context) {
    writeTemplate(destination, runnerTemplate, context);
  }

  public void writeEnvironmentScript(Path destination, EnvironmentContext environmentContext) {
    writeTemplate(destination, environmentTemplate, environmentContext);
  }
}
