package com.hubspot.singularity.config;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.google.inject.Inject;
import com.hubspot.singularity.scheduler.SingularitySchedulerTestBase;
import io.dropwizard.configuration.ConfigurationSourceProvider;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class MergingSourceProviderTest extends SingularitySchedulerTestBase {
  private static final String DEFAULT_PATH = "/configs/default.yaml";
  private static final String OVERRIDE_PATH = "/configs/override.yaml";
  private static final String JUST_A_STRING_PATH = "/configs/just_a_string.yaml";
  private static final String DOESNT_EXIST_PATH = "/configs/doesnt_exist.yaml";

  private static final YAMLFactory YAML_FACTORY = new YAMLFactory();

  @Inject
  private ObjectMapper objectMapper;

  public MergingSourceProviderTest() {
    super(false);
  }

  private ConfigurationSourceProvider buildConfigurationSourceProvider(
    String baseFilename
  ) {
    final Class<?> klass = getClass();

    return new MergingSourceProvider(
      new ConfigurationSourceProvider() {

        @Override
        public InputStream open(String path) throws IOException {
          final InputStream stream = klass.getResourceAsStream(path);
          if (stream == null) {
            throw new FileNotFoundException(
              "File " + path + " not found in test resources directory"
            );
          }
          return stream;
        }
      },
      baseFilename,
      objectMapper,
      YAML_FACTORY
    );
  }

  @Test
  public void testMergedConfigs() throws Exception {
    final InputStream mergedConfigStream = buildConfigurationSourceProvider(DEFAULT_PATH)
      .open(OVERRIDE_PATH);
    final SingularityConfiguration mergedConfig = objectMapper.readValue(
      YAML_FACTORY.createParser(mergedConfigStream),
      SingularityConfiguration.class
    );

    assertEquals(100, mergedConfig.getCheckDeploysEverySeconds());
    assertEquals("baseuser", mergedConfig.getDatabaseConfiguration().get().getUser());
    assertEquals(
      "overridepassword",
      mergedConfig.getDatabaseConfiguration().get().getPassword()
    );
  }

  @Test
  public void testNonObjectFails() throws Exception {
    Assertions.assertThrows(
      SingularityConfigurationMergeException.class,
      () -> buildConfigurationSourceProvider(DEFAULT_PATH).open(JUST_A_STRING_PATH)
    );
  }

  @Test
  public void testFileNoExistFail() throws Exception {
    Assertions.assertThrows(
      FileNotFoundException.class,
      () -> {
        buildConfigurationSourceProvider(DEFAULT_PATH).open(DOESNT_EXIST_PATH);
        buildConfigurationSourceProvider(DOESNT_EXIST_PATH).open(OVERRIDE_PATH);
      }
    );
  }
}
