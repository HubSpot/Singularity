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
import com.hubspot.singularity.SingularityPriorityFreezeParent;
import com.hubspot.singularity.SingularityRequestWithState;
import com.hubspot.singularity.SingularityTaskCleanup;
import com.hubspot.singularity.SingularityTaskId;
import com.hubspot.singularity.SingularityTaskShellCommandRequestId;
import com.hubspot.singularity.TaskCleanupType;
import com.hubspot.singularity.config.SingularityConfiguration;
import com.hubspot.singularity.data.PriorityManager;
import com.hubspot.singularity.data.RequestManager;
import com.hubspot.singularity.data.TaskManager;

@Singleton
public class SingularityPriorityKillPoller extends SingularityLeaderOnlyPoller {
    private static final Logger LOG = LoggerFactory.getLogger(SingularityPriorityKillPoller.class);

    private final PriorityManager priorityManager;
    private final RequestManager requestManager;
    private final TaskManager taskManager;

    @Inject
    public SingularityPriorityKillPoller(PriorityManager priorityManager, RequestManager requestManager, TaskManager taskManager, SingularityConfiguration configuration) {
        super(configuration.getCheckPriorityKillsEveryMillis(), TimeUnit.MILLISECONDS);

        this.priorityManager = priorityManager;
        this.requestManager = requestManager;
        this.taskManager = taskManager;
    }


    @Override
    public void runActionOnPoll() {
        if (!priorityManager.checkPriorityKillExists()) {
            LOG.trace("No priority freeze to process.");
            return;
        }

        final Optional<SingularityPriorityFreezeParent> maybePriorityFreeze = priorityManager.getActivePriorityFreeze();

        if (!maybePriorityFreeze.isPresent() || !maybePriorityFreeze.get().getPriorityFreeze().isKillTasks()) {
            LOG.trace("Priority freeze does not exist.");
            priorityManager.clearPriorityKill();
            return;
        }

        LOG.info("Handling priority freeze {}", maybePriorityFreeze.get());

        final long now = System.currentTimeMillis();
        int cancelledPendingTaskCount = 0;
        int killedTaskCount = 0;

        try {
            final double minPriorityLevel = maybePriorityFreeze.get().getPriorityFreeze().getMinimumPriorityLevel();

            // map request ID to priority level
            final Map<String, Double> requestIdToTaskPriority = new HashMap<>();
            for (SingularityRequestWithState requestWithState : requestManager.getRequests()) {
                requestIdToTaskPriority.put(requestWithState.getRequest().getId(), priorityManager.getTaskPriorityLevelForRequest(requestWithState.getRequest()));
            }

            // kill active tasks below minimum priority level
            for (SingularityTaskId taskId : taskManager.getActiveTaskIds()) {
                if (!requestIdToTaskPriority.containsKey(taskId.getRequestId())) {
                    LOG.trace("Unable to lookup priority level for task {}, skipping...", taskId);
                    continue;
                }

                final double taskPriorityLevel = requestIdToTaskPriority.get(taskId.getRequestId());

                if (taskPriorityLevel < minPriorityLevel) {
                    LOG.info("Killing active task {} since priority level {} is less than {}", taskId.getId(), taskPriorityLevel, minPriorityLevel);
                    taskManager.createTaskCleanup(
                        new SingularityTaskCleanup(maybePriorityFreeze.get().getUser(), TaskCleanupType.PRIORITY_KILL, now, taskId, maybePriorityFreeze.get().getPriorityFreeze().getMessage(),
                            maybePriorityFreeze.get().getPriorityFreeze().getActionId(), Optional.<SingularityTaskShellCommandRequestId>absent()));
                    killedTaskCount++;
                }
            }

        } finally {
            priorityManager.clearPriorityKill();
            LOG.info("Finished killing active tasks for priority freeze {} in {} for {} active tasks, {} pending tasks", maybePriorityFreeze, JavaUtils.duration(now), killedTaskCount, cancelledPendingTaskCount);
        }
    }
}
