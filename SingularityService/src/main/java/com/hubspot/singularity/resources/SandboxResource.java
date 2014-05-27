package com.hubspot.singularity.resources;

import com.google.common.base.Optional;
import com.google.common.base.Strings;
import com.google.common.base.Throwables;
import com.google.inject.Inject;
import com.hubspot.mesos.json.MesosFileChunkObject;
import com.hubspot.mesos.json.MesosFileObject;
import com.hubspot.singularity.SingularityService;
import com.hubspot.singularity.SingularityTaskHistory;
import com.hubspot.singularity.SingularityTaskId;
import com.hubspot.singularity.WebExceptions;
import com.hubspot.singularity.config.SingularityConfiguration;
import com.hubspot.singularity.data.DeployManager;
import com.hubspot.singularity.data.SandboxManager;
import com.hubspot.singularity.data.SandboxManager.SlaveNotFoundException;
import com.hubspot.singularity.data.TaskManager;
import com.hubspot.singularity.data.history.HistoryManager;
import com.hubspot.singularity.mesos.SingularityLogSupport;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.File;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Collection;

@Path(SingularityService.API_BASE_PATH + "/sandbox")
@Produces({ MediaType.APPLICATION_JSON })
public class SandboxResource extends AbstractHistoryResource {
  
  private final SandboxManager sandboxManager;
  private final SingularityLogSupport logSupport;
  private final SingularityConfiguration configuration;

  @Inject
  public SandboxResource(HistoryManager historyManager, TaskManager taskManager, SandboxManager sandboxManager, DeployManager deployManager, SingularityLogSupport logSupport, SingularityConfiguration configuration) {
    super(historyManager, taskManager, deployManager);
    
    this.configuration = configuration;
    this.sandboxManager = sandboxManager;
    this.logSupport = logSupport;
  }

  private SingularityTaskHistory checkHistory(String taskId) {
    final SingularityTaskId taskIdObj = getTaskIdObject(taskId);
    final SingularityTaskHistory taskHistory = getTaskHistory(taskIdObj);
    
    if (!taskHistory.getDirectory().isPresent()) {
      logSupport.checkDirectory(taskIdObj);
      
      throw WebExceptions.badRequest("Task %s does not have a directory yet - check again soon (enqueued request to refetch)", taskId);
    }
    
    return taskHistory;
  }
  
  private String getDefaultPath(String taskId, String qPath) {
    if (!Strings.isNullOrEmpty(qPath)) {
      return qPath;
    }
    if (configuration.isSandboxDefaultsToTaskId()) {
      return taskId;
    }
    return "";
  }
  
  @GET
  @Path("/{taskId}/browse")
  public Collection<MesosFileObject> browse(@PathParam("taskId") String taskId, @QueryParam("path") String qPath) {
    final String path = getDefaultPath(taskId, qPath);
    final SingularityTaskHistory history = checkHistory(taskId);

    final String slaveHostname = history.getTask().getOffer().getHostname();
    final String fullPath = new File(history.getDirectory().get(), path).toString();

    try {
      return sandboxManager.browse(slaveHostname, fullPath);
    } catch (SlaveNotFoundException snfe) {
      throw WebExceptions.notFound("Slave @ %s was not found, it is probably offline", slaveHostname);
    }
  }

  @GET
  @Path("/{taskId}/read")
  public MesosFileChunkObject read(@PathParam("taskId") String taskId, @QueryParam("path") String qPath,
                                   @QueryParam("offset") Optional<Long> offset, @QueryParam("length") Optional<Long> length) {
    final String path = getDefaultPath(taskId, qPath);
    final SingularityTaskHistory history = checkHistory(taskId);

    final String slaveHostname = history.getTask().getOffer().getHostname();
    final String fullPath = new File(history.getDirectory().get(), path).toString();

    try {
      final Optional<MesosFileChunkObject> maybeChunk = sandboxManager.read(slaveHostname, fullPath, offset, length);

      if (!maybeChunk.isPresent()) {
        throw WebExceptions.notFound("File %s does not exist for task ID %s", fullPath, taskId);
      }

      return maybeChunk.get();
    } catch (SlaveNotFoundException snfe) {
      throw WebExceptions.notFound("Slave @ %s was not found, it is probably offline", slaveHostname);
    }
  }

  @GET
  @Path("/{taskId}/download")
  public Response download(@PathParam("taskId") String taskId, @QueryParam("path") String path) {
    final SingularityTaskHistory history = checkHistory(taskId);

    final String slaveHostname = history.getTask().getOffer().getHostname();
    final String fullPath = new File(history.getDirectory().get(), path).toString();

    try {
      final URI downloadUri = new URI("http", null, slaveHostname, 5051, "/files/download.json", String.format("path=%s", fullPath), null);

      return Response.temporaryRedirect(downloadUri).build();
    } catch (URISyntaxException e) {
      throw Throwables.propagate(e);
    }
  }
}
