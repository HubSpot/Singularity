package com.hubspot.singularity.mesos;

import static java.nio.charset.StandardCharsets.UTF_8;

import org.apache.mesos.v1.Protos;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.hubspot.singularity.SingularityCreateResult;
import com.hubspot.singularity.SingularityTaskShellCommandUpdate;
import com.hubspot.singularity.data.TaskManager;
import com.hubspot.singularity.data.transcoders.SingularityTranscoderException;
import com.hubspot.singularity.data.transcoders.Transcoder;

@Singleton
public class SingularityMesosFrameworkMessageHandler {

  private static final Logger LOG = LoggerFactory.getLogger(SingularityMesosFrameworkMessageHandler.class);

  private final TaskManager taskManager;
  private final Transcoder<SingularityTaskShellCommandUpdate> commandUpdateTranscoder;

  @Inject
  public SingularityMesosFrameworkMessageHandler(TaskManager taskManager, Transcoder<SingularityTaskShellCommandUpdate> commandUpdateTranscoder) {
    this.taskManager = taskManager;
    this.commandUpdateTranscoder = commandUpdateTranscoder;
  }

  public void handleMessage(Protos.ExecutorID executorId, Protos.AgentID slaveId, byte[] data) {
    SingularityTaskShellCommandUpdate shellUpdate = null;
    try {
      shellUpdate = commandUpdateTranscoder.fromBytes(data);

      SingularityCreateResult saved = taskManager.saveTaskShellCommandUpdate(shellUpdate);

      LOG.debug("Saved {} with result {}", shellUpdate, saved);
    } catch (SingularityTranscoderException ste) {
      LOG.warn("Framework message {} not a commandUpdate", new String(data, UTF_8));
    } catch (Exception e) {
      LOG.error("While processing framework message {}", shellUpdate, e);
    }
  }

}
