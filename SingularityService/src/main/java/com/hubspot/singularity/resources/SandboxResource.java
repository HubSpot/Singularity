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
import com.hubspot.singularity.InvalidSingularityTaskIdException;
import com.hubspot.singularity.SingularityTaskHistory;
import com.hubspot.singularity.SingularityTaskId;
import com.hubspot.singularity.WebExceptions;
import com.hubspot.singularity.data.SandboxManager;
import com.hubspot.singularity.data.history.HistoryManager;
import com.hubspot.singularity.mesos.SingularityLogSupport;
import com.sun.jersey.api.NotFoundException;

@Path("/sandbox")
@Produces({ MediaType.APPLICATION_JSON })
public class SandboxResource {
  
  private final HistoryManager historyManager;
  private final SandboxManager sandboxManager;
  private final SingularityLogSupport logSupport;

  @Inject
  public SandboxResource(HistoryManager historyManager, SandboxManager sandboxManager, SingularityLogSupport logSupport) {
    this.historyManager = historyManager;
    this.sandboxManager = sandboxManager;
    this.logSupport = logSupport;
  }

  private SingularityTaskHistory checkHistory(String taskId) {
    SingularityTaskId taskIdObj = null;
    
    try {
      taskIdObj = SingularityTaskId.fromString(taskId);
    } catch (InvalidSingularityTaskIdException invalidException) {
      throw WebExceptions.badRequest(invalidException.getMessage());
    }
    
    final Optional<SingularityTaskHistory> maybeTaskHistory = historyManager.getTaskHistory(taskId, true);
    
    if (!maybeTaskHistory.isPresent()) {
      throw new NotFoundException(String.format("Task %s did not have a history", taskId));
    }

    if (!maybeTaskHistory.get().getDirectory().isPresent()) {
      logSupport.checkDirectory(taskIdObj);
      
      throw WebExceptions.badRequest("Task %s does not have a directory yet - check again soon (enqueued request to refetch)", taskId);
    }
    
    return maybeTaskHistory.get();
  }
  
  @GET
  @Path("/{taskId}/browse")
  public Collection<MesosFileObject> browse(@PathParam("taskId") String taskId, @QueryParam("path") @DefaultValue("") String path) {
    final SingularityTaskHistory history = checkHistory(taskId);

    final String slaveHostname = history.getTask().getOffer().getHostname();
    final String fullPath = new File(history.getDirectory().get(), path).toString();

    return sandboxManager.browse(slaveHostname, fullPath);
  }

  @GET
  @Path("/{taskId}/read")
  public MesosFileChunkObject read(@PathParam("taskId") String taskId, @QueryParam("path") @DefaultValue("") String path,
                                   @QueryParam("offset") Optional<Long> offset, @QueryParam("length") Optional<Long> length) {
    final SingularityTaskHistory history = checkHistory(taskId);

    final String slaveHostname = history.getTask().getOffer().getHostname();
    final String fullPath = new File(history.getDirectory().get(), path).toString();

    final Optional<MesosFileChunkObject> maybeChunk = sandboxManager.read(slaveHostname, fullPath, offset, length);

    if (!maybeChunk.isPresent()) {
      throw new NotFoundException(String.format("File %s does not exist for task ID %s", fullPath, taskId));
    }

    return maybeChunk.get();
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
