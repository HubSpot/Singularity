package com.hubspot.singularity.executor.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.jknack.handlebars.Handlebars;
import com.github.jknack.handlebars.Template;
import com.hubspot.singularity.executor.SingularityExecutorLogrotateFrequency;
import com.hubspot.singularity.executor.models.LogrotateAdditionalFile;
import com.hubspot.singularity.executor.models.LogrotateTemplateContext;
import com.hubspot.singularity.runner.base.config.SingularityRunnerBaseModule;
import com.hubspot.singularity.runner.base.config.SingularityRunnerConfigurationProvider;
import java.io.File;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import javax.validation.Validator;
import org.junit.jupiter.api.Test;

public class SingularityExecutorConfigurationTest {

  @Test
  public void itLoadsDockerAuthConfig() {
    SingularityExecutorConfiguration config = loadConfig(
      "config/executor-conf-dockerauth.yaml"
    );

    assertThat(config.getDockerAuthConfig().isPresent()).isTrue();
    assertThat(config.getDockerAuthConfig().get().isFromDockerConfig()).isFalse();
    assertThat(config.getDockerAuthConfig().get().getUsername()).isEqualTo("dockeruser");
    assertThat(config.getDockerAuthConfig().get().getPassword())
      .isEqualTo("dockerpassword");
    assertThat(config.getDockerAuthConfig().get().getServerAddress())
      .isEqualTo("https://private.docker.registry/path");
  }

  @Test
  public void itLoadsDockerAuthFromConfigFileIfSpecified() {
    SingularityExecutorConfiguration config = loadConfig(
      "config/executor-conf-dockerauth-fromconfig.yaml"
    );

    assertThat(config.getDockerAuthConfig().isPresent()).isTrue();
    assertThat(config.getDockerAuthConfig().get().isFromDockerConfig()).isTrue();
  }

  private SingularityExecutorConfiguration loadConfig(String file) {
    try {
      ObjectMapper mapper = new SingularityRunnerBaseModule(null).providesYamlMapper();
      Validator validator = mock(Validator.class);

      Field mapperField =
        SingularityRunnerConfigurationProvider.class.getDeclaredField("objectMapper");
      mapperField.setAccessible(true);

      Field validatorField =
        SingularityRunnerConfigurationProvider.class.getDeclaredField("validator");
      validatorField.setAccessible(true);

      SingularityRunnerConfigurationProvider<SingularityExecutorConfiguration> configProvider = new SingularityRunnerConfigurationProvider<>(
        SingularityExecutorConfiguration.class,
        Optional.of(
          new File(getClass().getClassLoader().getResource(file).toURI())
          .getAbsolutePath()
        )
      );

      mapperField.set(configProvider, mapper);
      validatorField.set(configProvider, validator);

      return configProvider.get();
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  @Test
  public void itKeepsSizeAndTimeBasedLogrotateThresholdsSeparate() throws Exception {
    Handlebars handlebars = new Handlebars();
    Template hourlyTemplate = handlebars.compile(
      SingularityExecutorModule.LOGROTATE_HOURLY_TEMPLATE
    );
    Template sizeBasedTemplate = handlebars.compile(
      SingularityExecutorModule.LOGROTATE_SIZE_BASED_TEMPLATE
    );

    List<LogrotateAdditionalFile> testExtraFilesHourly = new ArrayList<>();
    testExtraFilesHourly.add(
      new LogrotateAdditionalFile(
        "/tmp/testfile.txt",
        "txt",
        "%Y%m%d",
        Optional.empty(),
        Optional.of("10M")
      )
    );

    LogrotateTemplateContext context = mock(LogrotateTemplateContext.class);

    doReturn("weekly").when(context).getLogrotateFrequency();
    doReturn(testExtraFilesHourly).when(context).getExtrasFilesSizeBased();
    doReturn(false).when(context).isGlobalLogrotateHourly();

    String hourlyOutput = hourlyTemplate.apply(context);
    String sizeBasedOutput = sizeBasedTemplate.apply(context);

    assertThat(sizeBasedOutput).contains("10M");
    assertThat(sizeBasedOutput).doesNotContain("hourly");
    assertThat(sizeBasedOutput).doesNotContain("daily");
    assertThat(sizeBasedOutput).doesNotContain("weekly"); // Global frequency

    assertThat(hourlyOutput).doesNotContain("10M");
    assertThat(hourlyOutput).contains("weekly"); // Global frequency
  }

  @Test
  public void itProperlyGeneratesThreeLogrotateConfigs() throws Exception {
    Handlebars handlebars = new Handlebars();
    Template sizeBasedTemplate = handlebars.compile(
      SingularityExecutorModule.LOGROTATE_SIZE_BASED_TEMPLATE
    );
    Template hourlyTemplate = handlebars.compile(
      SingularityExecutorModule.LOGROTATE_HOURLY_TEMPLATE
    );
    Template nonHourlyTemplate = handlebars.compile(
      SingularityExecutorModule.LOGROTATE_TEMPLATE
    );

    LogrotateTemplateContext context = mock(LogrotateTemplateContext.class);

    List<LogrotateAdditionalFile> testExtraFiles = new ArrayList<>();
    List<LogrotateAdditionalFile> testExtraFilesHourly = new ArrayList<>();
    List<LogrotateAdditionalFile> testExtraFilesSizeBased = new ArrayList<>();

    testExtraFiles.add(
      new LogrotateAdditionalFile(
        "/tmp/testfile.txt",
        "txt",
        "%Y%m%d",
        Optional.of(SingularityExecutorLogrotateFrequency.MONTHLY),
        Optional.empty()
      )
    );

    testExtraFilesHourly.add(
      new LogrotateAdditionalFile(
        "/tmp/testfile-hourly.txt",
        "txt",
        "%Y%m%d",
        Optional.of(SingularityExecutorLogrotateFrequency.HOURLY),
        Optional.empty()
      )
    );

    testExtraFilesSizeBased.add(
      new LogrotateAdditionalFile(
        "/tmp/testfile-sizebased.txt",
        "txt",
        "%Y%m%d",
        Optional.empty(),
        Optional.of("10M")
      )
    );

    doReturn(SingularityExecutorLogrotateFrequency.WEEKLY.getLogrotateValue())
      .when(context)
      .getLogrotateFrequency();
    doReturn(testExtraFiles).when(context).getExtrasFiles();
    doReturn(testExtraFilesHourly).when(context).getExtrasFilesHourlyOrMoreFrequent();
    doReturn(testExtraFilesSizeBased).when(context).getExtrasFilesSizeBased();
    doReturn(false).when(context).isGlobalLogrotateHourly();

    // This sample output template, when copied into a staged Mesos slave and run with `logrotate -d <configFileName>`
    // confirms that a testfile.txt at the /tmp/testfile.txt will be cycled daily instead of weekly
    String hourlyOutput = hourlyTemplate.apply(context);
    String nonHourlyOutput = nonHourlyTemplate.apply(context);
    String sizeBasedOutput = sizeBasedTemplate.apply(context);

    // Assert that our config has both weekly and daily scopes, and that daily occurs second (thus overrides weekly
    // in the /tmp/testfile-hourly.txt YAML object).

    // Admittedly, YAML serialization would be better, but given this code doesn't actually test much without
    // a binary of `logrotate` to run against, it's not a big deal.
    assertThat(hourlyOutput.contains("weekly")).isTrue();
    assertThat(hourlyOutput.contains("daily")).isTrue();
    assertThat(hourlyOutput.indexOf("daily"))
      .isGreaterThan(hourlyOutput.indexOf("weekly"));

    // Assert that our config has both weekly and monthly scopes, and that monthly occurs second (thus overrides weekly
    // in the /tmp/testfile.txt YAML object).
    assertThat(nonHourlyOutput.contains("weekly")).isTrue();
    assertThat(nonHourlyOutput.contains("monthly")).isTrue();
    assertThat(nonHourlyOutput.indexOf("monthly"))
      .isGreaterThan(hourlyOutput.indexOf("weekly"));

    assertThat(sizeBasedOutput.contains("hourly")).isFalse();
    assertThat(sizeBasedOutput.contains("daily")).isFalse();
    assertThat(sizeBasedOutput.contains("weekly")).isFalse();
    assertThat(sizeBasedOutput.contains("monthly")).isFalse();
    assertThat(sizeBasedOutput.contains("size 10M")).isTrue();
  }
}
