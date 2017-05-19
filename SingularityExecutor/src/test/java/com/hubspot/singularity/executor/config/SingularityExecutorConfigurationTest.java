package com.hubspot.singularity.executor.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import java.io.File;
import java.lang.reflect.Field;

import javax.validation.Validator;

import org.junit.Test;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Optional;
import com.google.common.base.Throwables;
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

}
