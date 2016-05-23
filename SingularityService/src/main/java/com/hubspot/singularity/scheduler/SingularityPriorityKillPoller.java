package com.hubspot.singularity.scheduler;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Optional;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.hubspot.mesos.JavaUtils;
import com.hubspot.singularity.SingularityPriorityRequestParent;
import com.hubspot.singularity.SingularityRequestWithState;
import com.hubspot.singularity.SingularityTaskCleanup;
import com.hubspot.singularity.SingularityTaskId;
import com.hubspot.singularity.TaskCleanupType;
import com.hubspot.singularity.config.SingularityConfiguration;
import com.hubspot.singularity.data.PriorityManager;
import com.hubspot.singularity.data.RequestManager;
import com.hubspot.singularity.data.TaskManager;

@Singleton
public class SingularityPriorityKillPoller extends SingularityLeaderOnlyPoller {
    private static final Logger LOG = LoggerFactory.getLogger(SingularityScheduledJobPoller.class);

    private final PriorityManager priorityManager;
    private final RequestManager requestManager;
    private final TaskManager taskManager;

    private final double defaultTaskPriorityLevel;

    @Inject
    public SingularityPriorityKillPoller(PriorityManager priorityManager, RequestManager requestManager, TaskManager taskManager, SingularityConfiguration configuration) {
        super(configuration.getCheckPriorityKillsEveryMillis(), TimeUnit.MILLISECONDS);

        this.priorityManager = priorityManager;
        this.requestManager = requestManager;
        this.taskManager = taskManager;

        this.defaultTaskPriorityLevel = configuration.getDefaultTaskPriorityLevel();
    }


    @Override
    public void runActionOnPoll() {
        final Optional<SingularityPriorityRequestParent> maybePriorityKill = priorityManager.getActivePriorityKill();

        if (!maybePriorityKill.isPresent()) {
            LOG.trace("No priority kill to process.");
            return;
        }

        LOG.info("Handling priority kill {}", maybePriorityKill.get());

        final Map<String, Double> requestIdToTaskPriority = new HashMap<>();
        final double minPriorityLevel = maybePriorityKill.get().getPriorityRequest().getMinimumPriorityLevel();
        final long now = System.currentTimeMillis();

        for (SingularityRequestWithState requestWithState : requestManager.getRequests()) {
            requestIdToTaskPriority.put(requestWithState.getRequest().getId(), requestWithState.getRequest().getTaskPriorityLevel().or(defaultTaskPriorityLevel));
        }

        int killedTaskCount = 0;
        for (SingularityTaskId taskId : taskManager.getActiveTaskIds()) {
            if (!requestIdToTaskPriority.containsKey(taskId.getRequestId())) {
                continue;
            }

            final double taskPriorityLevel = requestIdToTaskPriority.get(taskId.getRequestId());

            if (taskPriorityLevel < minPriorityLevel) {
                LOG.info("Killing {} since priority level {} is less than {}", taskId.getId(), taskPriorityLevel, minPriorityLevel);
                taskManager.createTaskCleanup(new SingularityTaskCleanup(maybePriorityKill.get().getUser(), TaskCleanupType.PRIORITY_KILL, now, taskId, maybePriorityKill.get().getPriorityRequest().getMessage(), maybePriorityKill.get().getPriorityRequest().getActionId()));
                killedTaskCount++;
            }
        }

        LOG.info("Finished priority kill {} in {} for {} tasks", priorityManager, JavaUtils.duration(now), killedTaskCount);
        priorityManager.deleteActivePriorityKill();
    }
}
