package com.hubspot.singularity.s3uploader;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Lists;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Module;

public class SingularityS3UploaderRunner {

  private final static Logger LOG = LoggerFactory.getLogger(SingularityS3UploaderRunner.class);
  
  public static void main(String... args) {
    new SingularityS3UploaderRunner().run(args);
  }
  
  private SingularityS3UploaderRunner() {}
  
  public void run(String[] args) {
    List<Module> modules = Lists.newArrayListWithCapacity(2);

    final Injector injector = Guice.createInjector(modules);
  }

}
