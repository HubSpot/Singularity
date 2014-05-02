package com.hubspot.singularity.logwatcher.logrotate;

import java.nio.file.Path;

import com.github.mustachejava.Mustache;
import com.google.inject.Inject;
import com.google.inject.name.Named;
import com.hubspot.singularity.logwatcher.config.SingularityLogWatcherModule;
import com.hubspot.singularity.runner.base.shared.TemplateManagerBase;

public class LogrotateTemplateManager extends TemplateManagerBase {
  
  private final Mustache logrotateTemplate;

  @Inject
  public LogrotateTemplateManager(@Named(SingularityLogWatcherModule.LOGROTATE_TEMPLATE) Mustache logrotateTemplate) {
    this.logrotateTemplate = logrotateTemplate;
  }
  
  public void writeRunnerScript(Path destination, LogrotateTemplateContext context) {
    writeTemplate(destination, logrotateTemplate, context);
  }

}
