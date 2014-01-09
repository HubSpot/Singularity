package com.hubspot.singularity.resources;

import com.google.common.base.Optional;
import com.google.common.base.Throwables;
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
import javax.ws.rs.core.Response;
import java.io.File;
import java.net.URI;
import java.net.URISyntaxException;
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
  public Collection<MesosFileObject> browse(@PathParam("taskId") String taskId, @QueryParam("path") @DefaultValue("") String path) {
    final Optional<SingularityTaskHistory> maybeTaskHistory = historyManager.getTaskHistory(taskId, true);

    if (!maybeTaskHistory.isPresent()) {
      throw new NotFoundException(String.format("Task ID %s does not exist", taskId));
    }

    if (!maybeTaskHistory.get().getDirectory().isPresent()) {
      throw new BadRequestException(String.format("Task ID %s does not have a directory yet", taskId));
    }

    final String slaveHostname = maybeTaskHistory.get().getTask().getOffer().getHostname();
    final String fullPath = new File(maybeTaskHistory.get().getDirectory().get(), path).toString();

    return sandboxManager.browse(slaveHostname, fullPath);
  }

  @GET
  @Path("/{taskId}/read")
  public MesosFileChunkObject read(@PathParam("taskId") String taskId, @QueryParam("path") @DefaultValue("") String path,
                                   @QueryParam("offset") Optional<Long> offset, @QueryParam("length") Optional<Long> length) {
    final Optional<SingularityTaskHistory> maybeTaskHistory = historyManager.getTaskHistory(taskId, true);

    if (!maybeTaskHistory.isPresent()) {
      throw new NotFoundException(String.format("Task ID %s does not exist", taskId));
    }

    if (!maybeTaskHistory.get().getDirectory().isPresent()) {
      throw new BadRequestException(String.format("Task ID %s does not have a directory yet", taskId));
    }

    final String slaveHostname = maybeTaskHistory.get().getTask().getOffer().getHostname();
    final String fullPath = new File(maybeTaskHistory.get().getDirectory().get(), path).toString();

    final Optional<MesosFileChunkObject> maybeChunk = sandboxManager.read(slaveHostname, fullPath, offset, length);

    if (!maybeChunk.isPresent()) {
      throw new NotFoundException(String.format("File %s does not exist for task ID %s", fullPath, taskId));
    }

    return maybeChunk.get();
  }

  @GET
  @Path("/{taskId}/download")
  public Response download(@PathParam("taskId") String taskId, @QueryParam("path") String path) {
    final Optional<SingularityTaskHistory> maybeTaskHistory = historyManager.getTaskHistory(taskId, true);

    if (!maybeTaskHistory.isPresent()) {
      throw new NotFoundException(String.format("Task ID %s does not exist", taskId));
    }

    if (!maybeTaskHistory.get().getDirectory().isPresent()) {
      throw new BadRequestException(String.format("Task ID %s does not have a directory yet", taskId));
    }

    final String slaveHostname = maybeTaskHistory.get().getTask().getOffer().getHostname();
    final String fullPath = new File(maybeTaskHistory.get().getDirectory().get(), path).toString();

    try {
      final URI downloadUri = new URI("http", slaveHostname, "5051", "/files/download.json", String.format("path=%s", fullPath));

      return Response.temporaryRedirect(downloadUri).build();
    } catch (URISyntaxException e) {
      throw Throwables.propagate(e);
    }
  }
}
