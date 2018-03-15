package com.hubspot.singularity.resources;

import static com.hubspot.singularity.WebExceptions.badRequest;
import static com.hubspot.singularity.WebExceptions.checkBadRequest;
import static com.hubspot.singularity.WebExceptions.checkNotFound;
import static com.hubspot.singularity.WebExceptions.notFound;

import java.io.File;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;

import com.google.common.base.Function;
import com.google.common.base.Splitter;
import com.google.common.base.Strings;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.inject.Inject;
import com.hubspot.mesos.json.MesosFileChunkObject;
import com.hubspot.mesos.json.MesosFileObject;
import com.hubspot.singularity.api.auth.SingularityAuthorizationScope;
import com.hubspot.singularity.api.auth.SingularityUser;
import com.hubspot.singularity.api.logs.SingularitySandbox;
import com.hubspot.singularity.api.logs.SingularitySandboxFile;
import com.hubspot.singularity.api.task.SingularityTaskHistory;
import com.hubspot.singularity.api.task.SingularityTaskId;
import com.hubspot.singularity.auth.SingularityAuthorizationHelper;
import com.hubspot.singularity.config.ApiPaths;
import com.hubspot.singularity.config.SingularityConfiguration;
import com.hubspot.singularity.data.DeployManager;
import com.hubspot.singularity.data.SandboxManager;
import com.hubspot.singularity.data.SandboxManager.SlaveNotFoundException;
import com.hubspot.singularity.data.TaskManager;
import com.hubspot.singularity.data.history.HistoryManager;
import com.hubspot.singularity.mesos.SingularityMesosExecutorInfoSupport;

import io.dropwizard.auth.Auth;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.tags.Tags;

@Path(ApiPaths.SANDBOX_RESOURCE_PATH)
@Produces({MediaType.APPLICATION_JSON})
@Schema(title = "Provides a proxy to Mesos sandboxes")
@Tags({@Tag(name = "Sandbox")})
public class SandboxResource extends AbstractHistoryResource {
  private final SandboxManager sandboxManager;
  private final SingularityMesosExecutorInfoSupport logSupport;
  private final SingularityConfiguration configuration;

  @Inject
  public SandboxResource(HistoryManager historyManager, TaskManager taskManager, SandboxManager sandboxManager, DeployManager deployManager, SingularityMesosExecutorInfoSupport logSupport,
      SingularityConfiguration configuration, SingularityAuthorizationHelper authorizationHelper) {
    super(historyManager, taskManager, deployManager, authorizationHelper);

    this.configuration = configuration;
    this.sandboxManager = sandboxManager;
    this.logSupport = logSupport;
  }

  private SingularityTaskHistory checkHistory(String taskId, SingularityUser user) {
    final SingularityTaskId taskIdObj = getTaskIdObject(taskId);
    final SingularityTaskHistory taskHistory = getTaskHistoryRequired(taskIdObj, user);

    if (!taskHistory.getDirectory().isPresent()) {
      logSupport.checkDirectoryAndContainerId(taskIdObj);

      throw badRequest("Task %s does not have a directory yet - check again soon (enqueued request to refetch)", taskId);
    }

    return taskHistory;
  }

  private String getCurrentDirectory(String taskId, String currentDirectory) {
    if (currentDirectory != null) {
      return currentDirectory;
    }
    if (configuration.isSandboxDefaultsToTaskId()) {
      return taskId;
    }
    return "";
  }

  @GET
  @Path("/{taskId}/browse")
  @Operation(
      summary = "Retrieve information about a specific task's sandbox",
      responses = {
          @ApiResponse(responseCode = "404", description = "A slave or task with the specified id was not found")
      }
  )
  public SingularitySandbox browse(
      @Parameter(hidden = true) @Auth SingularityUser user,
      @Parameter(required = true, description = "The task ID to browse") @PathParam("taskId") String taskId,
      @Parameter(required = true, description = "The path to browse from") @QueryParam("path") String path) {
    authorizationHelper.checkForAuthorizationByTaskId(taskId, user, SingularityAuthorizationScope.READ);

    // Remove all trailing slashes from the path
    if (path != null) {
      path = path.replaceAll("\\/+$", "");
    }

    final String currentDirectory = getCurrentDirectory(taskId, path);
    final SingularityTaskHistory history = checkHistory(taskId, user);

    final String slaveHostname = history.getTask().getHostname();
    final String pathToRoot = history.getDirectory().get();
    final String fullPath = new File(pathToRoot, currentDirectory).toString();

    final int substringTruncationLength = currentDirectory.length() == 0 ? pathToRoot.length() + 1 : pathToRoot.length() + currentDirectory.length() + 2;

    try {
      Collection<MesosFileObject> mesosFiles = sandboxManager.browse(slaveHostname, fullPath);
      List<SingularitySandboxFile> sandboxFiles = Lists.newArrayList(Iterables.transform(mesosFiles, new Function<MesosFileObject, SingularitySandboxFile>() {

        @Override
        public SingularitySandboxFile apply(MesosFileObject input) {
          return new SingularitySandboxFile(input.getPath().substring(substringTruncationLength), input.getMtime(), input.getSize(), input.getMode());
        }

      }));

      return new SingularitySandbox(sandboxFiles, pathToRoot, currentDirectory, slaveHostname);
    } catch (SlaveNotFoundException snfe) {
      throw notFound("Slave @ %s was not found, it is probably offline", slaveHostname);
    }
  }

  @GET
  @Path("/{taskId}/read")
  @Operation(
      summary = "Retrieve part of the contents of a file in a specific task's sandbox",
      responses = {
          @ApiResponse(responseCode = "404", description = "A slave, task, or file with the specified id was not found")
      }
  )
  public MesosFileChunkObject read(
      @Parameter(hidden = true) @Auth SingularityUser user,
      @Parameter(required = true, description = "The task ID of the sandbox to read from") @PathParam("taskId") String taskId,
      @Parameter(required = true, description = "The path to the file to be read") @QueryParam("path") String path,
      @Parameter(description = "Optional string to grep for") @QueryParam("grep") Optional<String> grep,
      @Parameter(description = "Byte offset to start reading from") @QueryParam("offset") Optional<Long> offset,
      @Parameter(description = "Maximum number of bytes to read") @QueryParam("length") Optional<Long> length) {
    authorizationHelper.checkForAuthorizationByTaskId(taskId, user, SingularityAuthorizationScope.READ);

    final SingularityTaskHistory history = checkHistory(taskId, user);

    checkBadRequest(!Strings.isNullOrEmpty(path), "Must specify 'path'");

    final String slaveHostname = history.getTask().getHostname();
    final String fullPath = new File(history.getDirectory().get(), path).toString();

    try {
      final Optional<MesosFileChunkObject> maybeChunk = sandboxManager.read(slaveHostname, fullPath, offset, length);

      checkNotFound(maybeChunk.isPresent(), "File %s does not exist for task ID %s", fullPath, taskId);

      if (grep.isPresent() && !Strings.isNullOrEmpty(grep.get())) {
        final Pattern grepPattern = Pattern.compile(grep.get());
        final StringBuilder strBuilder = new StringBuilder(maybeChunk.get().getData().length());

        for (String line : Splitter.on("\n").split(maybeChunk.get().getData())) {
          if (grepPattern.matcher(line).find()) {
            strBuilder.append(line);
            strBuilder.append("\n");
          }
        }

        return new MesosFileChunkObject(strBuilder.toString(), maybeChunk.get().getOffset(), Optional.of(maybeChunk.get().getOffset() + maybeChunk.get().getData().length()));
      }

      return maybeChunk.get();
    } catch (SlaveNotFoundException snfe) {
      throw notFound("Slave @ %s was not found, it is probably offline", slaveHostname);
    }
  }

}
