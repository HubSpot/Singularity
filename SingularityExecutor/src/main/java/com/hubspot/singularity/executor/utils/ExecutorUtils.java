package com.hubspot.singularity.executor.utils;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

import org.apache.mesos.ExecutorDriver;
import org.apache.mesos.Protos;
import org.slf4j.Logger;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.inject.Inject;
import com.google.inject.name.Named;
import com.hubspot.singularity.executor.config.SingularityExecutorConfiguration;
import com.hubspot.singularity.runner.base.config.SingularityRunnerBaseModule;

public class ExecutorUtils {

  private final SingularityExecutorConfiguration configuration;
  private final ObjectMapper objectMapper;
  
  @Inject
  public ExecutorUtils(SingularityExecutorConfiguration configuration, @Named(SingularityRunnerBaseModule.JSON_MAPPER) ObjectMapper objectMapper) {
    this.configuration = configuration;
    this.objectMapper = objectMapper;
  }
  
  public void sendStatusUpdate(ExecutorDriver driver, Protos.TaskInfo taskInfo, Protos.TaskState taskState, String message, Logger taskLogger) {
    taskLogger.info("Sending status update \"{}\" ({})", message, taskState.name());

    message = message.substring(0, Math.min(configuration.getMaxTaskMessageLength(), message.length()));
    
    try {
      final Protos.TaskStatus.Builder builder = Protos.TaskStatus.newBuilder()
          .setTaskId(taskInfo.getTaskId())
          .setState(taskState)
          .setMessage(message);
    
      driver.sendStatusUpdate(builder.build());
    } catch (Throwable t) {
      taskLogger.error("While sending status update", t);
    }
  }
  
  public boolean writeObject(Object o, Path path, Logger log) {
    try {
      final byte[] bytes = objectMapper.writeValueAsBytes(o);
      
      log.info("Writing {} bytes of {} to {}", new Object[] { Integer.toString(bytes.length), o.toString(), path.toString() });
        
      Files.write(path, bytes, StandardOpenOption.WRITE, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
    
      return true;
    } catch (Throwable t) {
      log.error("Failed writing {}", o.toString(), t);
      return false;
    }
  }
  
}
