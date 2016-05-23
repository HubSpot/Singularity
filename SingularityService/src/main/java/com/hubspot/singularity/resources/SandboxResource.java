package com.hubspot.singularity.resources;

import static com.hubspot.singularity.WebExceptions.badRequest;
import static com.hubspot.singularity.WebExceptions.checkNotFound;
import static com.hubspot.singularity.WebExceptions.notFound;

import java.io.File;
import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.util.Collection;
import java.util.List;
import java.util.regex.Pattern;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;

import com.google.common.base.Function;
import com.google.common.base.Optional;
import com.google.common.base.Splitter;
import com.google.common.base.Strings;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.inject.Inject;
import com.hubspot.mesos.json.MesosFileChunkObject;
import com.hubspot.mesos.json.MesosFileObject;
import com.hubspot.singularity.SingularityAuthorizationScope;
import com.hubspot.singularity.SingularitySandbox;
import com.hubspot.singularity.SingularitySandboxFile;
import com.hubspot.singularity.SingularityService;
import com.hubspot.singularity.SingularityTaskHistory;
import com.hubspot.singularity.SingularityTaskId;
import com.hubspot.singularity.SingularityUser;
import com.hubspot.singularity.auth.SingularityAuthorizationHelper;
import com.hubspot.singularity.config.SingularityConfiguration;
import com.hubspot.singularity.data.DeployManager;
import com.hubspot.singularity.data.SandboxManager;
import com.hubspot.singularity.data.SandboxManager.SlaveNotFoundException;
import com.hubspot.singularity.data.TaskManager;
import com.hubspot.singularity.data.history.HistoryManager;
import com.hubspot.singularity.mesos.SingularityLogSupport;
import com.wordnik.swagger.annotations.Api;
import com.wordnik.swagger.annotations.ApiOperation;
import com.wordnik.swagger.annotations.ApiParam;

@Path(SandboxResource.PATH)
@Produces({MediaType.APPLICATION_JSON})
@Api(description="Provides a proxy to Mesos sandboxes.", value=SandboxResource.PATH)
public class SandboxResource extends AbstractHistoryResource {
  public static final String PATH = SingularityService.API_BASE_PATH + "/sandbox";

  private final SandboxManager sandboxManager;
  private final SingularityLogSupport logSupport;
  private final SingularityConfiguration configuration;

  @Inject
  public SandboxResource(HistoryManager historyManager, TaskManager taskManager, SandboxManager sandboxManager, DeployManager deployManager, SingularityLogSupport logSupport,
      SingularityConfiguration configuration, SingularityAuthorizationHelper authorizationHelper, Optional<SingularityUser> user) {
    super(historyManager, taskManager, deployManager, authorizationHelper, user);

    this.configuration = configuration;
    this.sandboxManager = sandboxManager;
    this.logSupport = logSupport;
  }

  private SingularityTaskHistory checkHistory(String taskId) {
    final SingularityTaskId taskIdObj = getTaskIdObject(taskId);
    final SingularityTaskHistory taskHistory = getTaskHistoryRequired(taskIdObj);

    if (!taskHistory.getDirectory().isPresent()) {
      logSupport.checkDirectory(taskIdObj);

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
  @ApiOperation("Retrieve information about a specific task's sandbox.")
  public SingularitySandbox browse(@ApiParam("The task ID to browse") @PathParam("taskId") String taskId,
      @ApiParam("The path to browse from") @QueryParam("path") String path) {
    authorizationHelper.checkForAuthorizationByTaskId(taskId, user, SingularityAuthorizationScope.READ);

    final String currentDirectory = getCurrentDirectory(taskId, path);
    final SingularityTaskHistory history = checkHistory(taskId);

    final String slaveHostname = history.getTask().getOffer().getHostname();
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
  @ApiOperation("Retrieve part of the contents of a file in a specific task's sandbox.")
  public MesosFileChunkObject read(@ApiParam("The task ID of the sandbox to read from") @PathParam("taskId") String taskId,
      @ApiParam("The path to the file to be read") @QueryParam("path") String path,
      @ApiParam("Optional string to grep for") @QueryParam("grep") Optional<String> grep,
      @ApiParam("Byte offset to start reading from") @QueryParam("offset") Optional<Long> offset,
      @ApiParam("Maximum number of bytes to read") @QueryParam("length") Optional<Long> length,
      @ApiParam("Drop invalid UTF-8 characters") @QueryParam("dropInvalidUTF8") Optional<Boolean> dropInvalidUTF8) {
    authorizationHelper.checkForAuthorizationByTaskId(taskId, user, SingularityAuthorizationScope.READ);

    final SingularityTaskHistory history = checkHistory(taskId);

    final String slaveHostname = history.getTask().getOffer().getHostname();
    final String fullPath = new File(history.getDirectory().get(), path).toString();

    try {
      final Optional<MesosFileChunkObject> maybeChunk = sandboxManager.read(slaveHostname, fullPath, offset, length);

      checkNotFound(maybeChunk.isPresent(), "File %s does not exist for task ID %s", fullPath, taskId);

      //TODO: make this work
      if (grep.isPresent() && !Strings.isNullOrEmpty(grep.get())) {
        return maybeChunk.get();
      }

      return maybeChunk.get();
    } catch (SlaveNotFoundException snfe) {
      throw notFound("Slave @ %s was not found, it is probably offline", slaveHostname);
    }
  }

}
