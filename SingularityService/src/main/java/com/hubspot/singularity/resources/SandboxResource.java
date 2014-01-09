package com.hubspot.singularity.resources;

import com.google.common.base.Optional;
import com.google.inject.Inject;
import com.hubspot.mesos.json.MesosFileChunkObject;
import com.hubspot.mesos.json.MesosFileObject;
import com.hubspot.singularity.BadRequestException;
import com.hubspot.singularity.SingularityTaskHistory;
import com.hubspot.singularity.data.SandboxManager;
import com.hubspot.singularity.data.history.HistoryManager;
import com.sun.jersey.api.NotFoundException;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import java.io.File;
import java.util.Collection;

@Path("/sandbox")
@Produces({ MediaType.APPLICATION_JSON })
public class SandboxResource {
  private final HistoryManager historyManager;
  private final SandboxManager sandboxManager;

  @Inject
  public SandboxResource(HistoryManager historyManager, SandboxManager sandboxManager) {
    this.historyManager = historyManager;
    this.sandboxManager = sandboxManager;
  }

  @GET
  @Path("/{taskId}/browse")
  public Collection<MesosFileObject> browse(@PathParam("taskId") String taskId, @QueryParam("path") String path) {
    final Optional<SingularityTaskHistory> maybeTaskHistory = historyManager.getTaskHistory(taskId, true);

    if (!maybeTaskHistory.isPresent()) {
      throw new NotFoundException(String.format("Task ID %s does not exist", taskId));
    }

    if (!maybeTaskHistory.get().getDirectory().isPresent()) {
      throw new BadRequestException(String.format("Task ID %s does not have a directory yet", taskId));
    }

    return sandboxManager.browse(maybeTaskHistory.get().getTask().getOffer().getHostname(), new File(maybeTaskHistory.get().getDirectory().get(), path).toString());
  }

  @GET
  @Path("/{taskId}/read")
  public MesosFileChunkObject read(@PathParam("taskId") String taskId, @QueryParam("path") String path,
                                   @QueryParam("offset") long offset, @QueryParam("length") long length) {
    final Optional<SingularityTaskHistory> maybeTaskHistory = historyManager.getTaskHistory(taskId, true);

    if (!maybeTaskHistory.isPresent()) {
      throw new NotFoundException(String.format("Task ID %s does not exist", taskId));
    }

    if (!maybeTaskHistory.get().getDirectory().isPresent()) {
      throw new BadRequestException(String.format("Task ID %s does not have a directory yet", taskId));
    }

    final String fullPath = new File(maybeTaskHistory.get().getDirectory().get(), path).toString();

    return sandboxManager.read(maybeTaskHistory.get().getTask().getOffer().getHostname(), fullPath, offset, length);
  }
}
