package com.hubspot.singularity.runner.base.jackson;

import com.fasterxml.jackson.core.Version;
import com.fasterxml.jackson.databind.Module;

public class ObfuscateModule extends Module {
  @Override
  public String getModuleName() {
    return "ObfuscateModule";
  }

  @Override
  public Version version() {
    return Version.unknownVersion();
  }

  @Override
  public void setupModule(SetupContext context) {
    context.appendAnnotationIntrospector(new ObfuscateAnnotationIntrospector());
  }
}
