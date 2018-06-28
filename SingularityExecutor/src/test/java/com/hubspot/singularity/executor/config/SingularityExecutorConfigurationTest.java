package com.hubspot.singularity.executor.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

import java.io.File;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

import javax.validation.Validator;

import org.junit.Test;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.jknack.handlebars.Handlebars;
import com.github.jknack.handlebars.Template;
import com.google.common.base.Optional;
import com.google.common.base.Throwables;
import com.hubspot.singularity.executor.SingularityExecutorLogrotateFrequency;
import com.hubspot.singularity.executor.models.LogrotateAdditionalFile;
import com.hubspot.singularity.executor.models.LogrotateTemplateContext;
import com.hubspot.singularity.runner.base.config.SingularityRunnerBaseModule;
import com.hubspot.singularity.runner.base.config.SingularityRunnerConfigurationProvider;

public class SingularityExecutorConfigurationTest {

    @Test
    public void itLoadsDockerAuthConfig() {
        SingularityExecutorConfiguration config = loadConfig("config/executor-conf-dockerauth.yaml");

        assertThat(config.getDockerAuthConfig().isPresent()).isTrue();
        assertThat(config.getDockerAuthConfig().get().isFromDockerConfig()).isFalse();
        assertThat(config.getDockerAuthConfig().get().getUsername()).isEqualTo("dockeruser");
        assertThat(config.getDockerAuthConfig().get().getPassword()).isEqualTo("dockerpassword");
        assertThat(config.getDockerAuthConfig().get().getServerAddress()).isEqualTo("https://private.docker.registry/path");
    }

    @Test
    public void itLoadsDockerAuthFromConfigFileIfSpecified() {
        SingularityExecutorConfiguration config = loadConfig("config/executor-conf-dockerauth-fromconfig.yaml");

        assertThat(config.getDockerAuthConfig().isPresent()).isTrue();
        assertThat(config.getDockerAuthConfig().get().isFromDockerConfig()).isTrue();
    }
    
    private SingularityExecutorConfiguration loadConfig(String file)  {
        try {
            ObjectMapper mapper = new SingularityRunnerBaseModule(null).providesYamlMapper();
            Validator validator = mock(Validator.class);
            
            Field mapperField = SingularityRunnerConfigurationProvider.class.getDeclaredField("objectMapper");
            mapperField.setAccessible(true);
            
            Field validatorField = SingularityRunnerConfigurationProvider.class.getDeclaredField("validator");
            validatorField.setAccessible(true);
            
            SingularityRunnerConfigurationProvider<SingularityExecutorConfiguration> configProvider = new SingularityRunnerConfigurationProvider<>(
                    SingularityExecutorConfiguration.class, 
                    Optional.of(new File(getClass().getClassLoader().getResource(file).toURI()).getAbsolutePath()));

            mapperField.set(configProvider, mapper);
            validatorField.set(configProvider, validator);
            
            return configProvider.get();
        }
        catch(Exception e) {
            throw Throwables.propagate(e);
        }
    }

    @Test
    public void itOverridesGlobalLogrotateFrequency() throws Exception {
        Handlebars handlebars = new Handlebars();
        Template template = handlebars.compile(SingularityExecutorModule.LOGROTATE_TEMPLATE);

        LogrotateTemplateContext context = mock(LogrotateTemplateContext.class);

        doReturn(SingularityExecutorLogrotateFrequency.WEEKLY.getLogrotateValue()).when(context).getLogrotateFrequency();

        List<LogrotateAdditionalFile> testExtraFiles = new ArrayList<>();
        testExtraFiles.add(new LogrotateAdditionalFile("/tmp/testfile.txt", "txt", "%Y%m%d",
            SingularityExecutorLogrotateFrequency.DAILY));

        doReturn(testExtraFiles).when(context).getExtrasFiles();

        // This sample output template, when copied into a staged Mesos slave and run with `logrotate -d <configFileName>`
        // confirms that a testfile.txt at the /tmp/testfile.txt will be cycled daily instead of weekly
        String text = template.apply(context);

        // Assert that our config has both weekly and daily scopes, and that daily occurs second (thus overrides weekly
        // in the /tmp/testfile.txt YAML object).

        // Admittedly, YAML serialization would be better, but given this code doesn't actually test much without
        // a binary of `logrotate` to run against, it's not a big deal.
        assertThat(text.contains("weekly")).isTrue();
        assertThat(text.contains("daily")).isTrue();
        assertThat(text.indexOf("daily")).isGreaterThan(text.indexOf("weekly"));

    }

}
