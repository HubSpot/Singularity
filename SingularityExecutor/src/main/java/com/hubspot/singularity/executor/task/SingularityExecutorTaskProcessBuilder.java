package com.hubspot.singularity.executor.task;

import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.Callable;

import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import com.hubspot.deploy.EmbeddedArtifact;
import com.hubspot.deploy.ExecutorData;
import com.hubspot.deploy.ExternalArtifact;
import com.hubspot.singularity.executor.ArtifactManager;
import com.hubspot.singularity.executor.TemplateManager;
import com.hubspot.singularity.executor.config.SingularityExecutorConfiguration;
import com.hubspot.singularity.executor.models.EnvironmentContext;
import com.hubspot.singularity.executor.models.RunnerContext;

public class SingularityExecutorTaskProcessBuilder implements Callable<ProcessBuilder> {

  private final String taskId;
  private final ArtifactManager artifactManager;
  private final TemplateManager templateManager;
  private final SingularityExecutorConfiguration configuration;
  
  private final Path taskDirectory;
  private final Path executorOut;

  private final ExecutorData executorData;
  
  public SingularityExecutorTaskProcessBuilder(String taskId, ArtifactManager artifactManager, TemplateManager templateManager, SingularityExecutorConfiguration configuration, ExecutorData executorData) {
    this.executorData = executorData;
    this.taskId = taskId;
    this.artifactManager = artifactManager;
    this.templateManager = templateManager;
    this.configuration = configuration;
    
    this.taskDirectory = configuration.getTaskDirectoryPath(taskId);
    this.executorOut = configuration.getExecutorBashLogPath(taskId);
  }
  
  public ArtifactManager getArtifactManager() {
    return artifactManager;
  }

  @Override
  public ProcessBuilder call() throws Exception {
    downloadFiles(executorData);
    extractFiles(executorData);
    
    ProcessBuilder processBuilder = buildProcessBuilder(executorData);
  
    return processBuilder;
  }

  private Path getPath(String filename) {
    return taskDirectory.resolve(filename);
  }
  
  private void extractFiles(ExecutorData executorData) {
    for (EmbeddedArtifact artifact : executorData.getEmbeddedArtifacts()) {
      artifactManager.extract(artifact, taskDirectory);
    }
  }
  
  private void downloadFiles(ExecutorData executorData) {
    for (ExternalArtifact artifact : executorData.getExternalArtifacts()) {
      Path fetched = artifactManager.fetch(artifact);
      
      Preconditions.checkState(fetched.getFileName().toString().endsWith(".tar.gz"), "%s did not appear to be a tar archive (did not end with .tar.gz)", fetched.getFileName());
      
      artifactManager.untar(fetched, taskDirectory);
    }
  }
  
  private ProcessBuilder buildProcessBuilder(ExecutorData executorData) {
    templateManager.writeEnvironmentScript(getPath("deploy.env"), new EnvironmentContext(executorData));
    templateManager.writeRunnerScript(getPath("runner.sh"), new RunnerContext(executorData.getCmd(), executorData.getUser().or(configuration.getDefaultRunAsUser()), configuration.getServiceLog(), taskId)); 
    
    List<String> command = Lists.newArrayList();
    command.add("bash");
    command.add("runner.sh");
    
    ProcessBuilder processBuilder = new ProcessBuilder(command);
    
    processBuilder.directory(taskDirectory.toFile());
    
    processBuilder.redirectError(executorOut.toFile());
    processBuilder.redirectOutput(executorOut.toFile());
    
    return processBuilder;
  }

}
