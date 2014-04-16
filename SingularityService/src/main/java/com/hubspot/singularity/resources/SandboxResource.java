package com.hubspot.singularity.resources;

import java.io.File;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Collection;

import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import com.google.common.base.Optional;
import com.google.common.base.Throwables;
import com.google.inject.Inject;
import com.hubspot.mesos.json.MesosFileChunkObject;
import com.hubspot.mesos.json.MesosFileObject;
import com.hubspot.singularity.SingularityTaskHistory;
import com.hubspot.singularity.SingularityTaskId;
import com.hubspot.singularity.WebExceptions;
import com.hubspot.singularity.data.SandboxManager;
import com.hubspot.singularity.data.TaskManager;
import com.hubspot.singularity.data.history.HistoryManager;
import com.hubspot.singularity.mesos.SingularityLogSupport;

@Path("/sandbox")
@Produces({ MediaType.APPLICATION_JSON })
public class SandboxResource extends AbstractHistoryResource {
  
  private final SandboxManager sandboxManager;
  private final SingularityLogSupport logSupport;

  @Inject
  public SandboxResource(HistoryManager historyManager, TaskManager taskManager, SandboxManager sandboxManager, SingularityLogSupport logSupport) {
    super(historyManager, taskManager);
    
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
  
  private String getPath(String taskId, String path) {
    if (path == null) {
      return taskId;
    }
    return path;
  }
  
  @GET
  @Path("/{taskId}/browse")
  public Collection<MesosFileObject> browse(@PathParam("taskId") String taskId, @QueryParam("path") String path) {
    final SingularityTaskHistory history = checkHistory(taskId);

    final String slaveHostname = history.getTask().getOffer().getHostname();
    final String fullPath = new File(history.getDirectory().get(), getPath(taskId, path)).toString();

    return sandboxManager.browse(slaveHostname, fullPath);
  }

  @GET
  @Path("/{taskId}/read")
  public MesosFileChunkObject read(@PathParam("taskId") String taskId, @QueryParam("path") String path,
                                   @QueryParam("offset") Optional<Long> offset, @QueryParam("length") Optional<Long> length) {
    final SingularityTaskHistory history = checkHistory(taskId);

    final String slaveHostname = history.getTask().getOffer().getHostname();
    final String fullPath = new File(history.getDirectory().get(), getPath(taskId, path)).toString();

    final Optional<MesosFileChunkObject> maybeChunk = sandboxManager.read(slaveHostname, fullPath, offset, length);

    if (!maybeChunk.isPresent()) {
      throw WebExceptions.notFound("File %s does not exist for task ID %s", fullPath, taskId);
    }

    return maybeChunk.get();
  }

  @GET
  @Path("/{taskId}/download")
  public Response download(@PathParam("taskId") String taskId, @QueryParam("path") String path) {
    final SingularityTaskHistory history = checkHistory(taskId);

    final String slaveHostname = history.getTask().getOffer().getHostname();
    final String fullPath = new File(history.getDirectory().get(), getPath(taskId, path)).toString();

    try {
      final URI downloadUri = new URI("http", null, slaveHostname, 5051, "/files/download.json", String.format("path=%s", fullPath), null);

      return Response.temporaryRedirect(downloadUri).build();
    } catch (URISyntaxException e) {
      throw Throwables.propagate(e);
    }
  }
}
