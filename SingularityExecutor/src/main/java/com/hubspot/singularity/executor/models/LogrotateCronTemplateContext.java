package com.hubspot.singularity.executor.models;

import java.nio.file.Paths;

import com.hubspot.singularity.executor.SingularityExecutorLogrotateFrequency;
import com.hubspot.singularity.executor.config.SingularityExecutorConfiguration;
import com.hubspot.singularity.executor.task.SingularityExecutorTaskDefinition;

public class LogrotateCronTemplateContext {
    private final String cronSchedule;
    private final String logrotateCommand;
    private final String logrotateStateFile;
    private final String logrotateConfig;

    public LogrotateCronTemplateContext(SingularityExecutorConfiguration configuration, SingularityExecutorTaskDefinition taskDefinition, SingularityExecutorLogrotateFrequency logrotateFrequency) {
        this.logrotateCommand = configuration.getLogrotateCommand();
        this.logrotateStateFile = taskDefinition.getLogrotateStateFilePath().toString();
        this.logrotateConfig = Paths.get(configuration.getLogrotateConfDirectory()).resolve(taskDefinition.getTaskId()).toString();
        this.cronSchedule = logrotateFrequency.getCronSchedule().get();
    }

    public String getLogrotateCommand() {
        return logrotateCommand;
    }

    public String getLogrotateStateFile() {
        return logrotateStateFile;
    }

    public String getLogrotateConfig() {
        return logrotateConfig;
    }

    public String getCronSchedule() {
        return cronSchedule;
    }
}
