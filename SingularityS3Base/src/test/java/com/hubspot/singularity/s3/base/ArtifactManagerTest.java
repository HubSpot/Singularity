package com.hubspot.singularity.s3.base;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

import com.google.common.base.Throwables;
import com.hubspot.singularity.runner.base.configuration.SingularityRunnerBaseConfiguration;
import com.hubspot.singularity.runner.base.sentry.SingularityRunnerExceptionNotifier;
import com.hubspot.singularity.s3.base.config.SingularityS3Configuration;
import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;

public class ArtifactManagerTest {
  private ArtifactManager artifactManager;

  private File cacheDir = com.google.common.io.Files.createTempDir();

  @BeforeEach
  public void setup() {
    SingularityRunnerBaseConfiguration baseConfig = new SingularityRunnerBaseConfiguration();
    SingularityS3Configuration s3Config = new SingularityS3Configuration();

    s3Config.setArtifactCacheDirectory(cacheDir.toString());
    artifactManager =
      new ArtifactManager(
        baseConfig,
        s3Config,
        LoggerFactory.getLogger(ArtifactManagerTest.class),
        new SingularityRunnerExceptionNotifier(baseConfig)
      );
  }

  @Test
  public void itAbsorbsFileAlreadyExistsExceptionsWhenCopyingDuplicateFiles() {
    List<String> lines = Arrays.asList("Testing", "1", "2", "3");
    Path originalPath = write("original.txt", lines);

    assertThat(originalPath.toFile())
      .hasContent(String.join(System.lineSeparator(), lines));

    Path copyPath = Paths.get(cacheDir.toString() + "/copy.txt");
    assertThat(copyPath).doesNotExist();
    artifactManager.copy(originalPath, cacheDir.toPath(), "copy.txt");
    assertThat(copyPath).exists();

    // A redundant copy operation should not throw.
    artifactManager.copy(originalPath, cacheDir.toPath(), "copy.txt");
  }

  @Test
  public void itPropagatesFileAlreadyExistsExceptionsWhenCopyingNonDuplicateFiles() {
    Path firstPath = write("a.txt", Arrays.asList("Testing", "1", "2", "3"));
    Path secondPath = write("b.txt", Arrays.asList("Testing", "a", "b", "c"));

    try {
      artifactManager.copy(
        firstPath,
        secondPath.getParent(),
        secondPath.getFileName().toString()
      );
      fail(
        "Expected copy operation to throw when trying to overwrite non-duplicate file."
      );
    } catch (Exception e) {
      assertThat(Throwables.getRootCause(e))
        .isInstanceOf(FileAlreadyExistsException.class);
    }
  }

  public Path write(String fileName, List<String> lines) {
    try {
      File file = cacheDir.toPath().resolve(fileName).toFile();
      Path path = file.toPath();
      Files.write(path, lines, Charset.forName("UTF-8"));
      return path;
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }
}
