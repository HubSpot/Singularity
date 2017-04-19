import { buildApiAction, buildJsonApiAction } from './base';

export const FetchTasksInState = buildApiAction(
  'FETCH_TASKS',
  (state, renderNotFoundIf404) => {
    const stateToFetch = state !== 'decommissioning' ? state : 'active';

    let propertyString = '?property=';
    const propertyJoin = '&property=';

    switch (stateToFetch) {
      case 'active':
        propertyString += ['offer.hostname', 'taskId', 'mesosTask.resources', 'rackId', 'taskRequest.request.requestType'].join(propertyJoin);
        break;
      case 'scheduled':
        propertyString += ['offer.hostname', 'taskId', 'mesosTask.resources', 'rackId', 'taskRequest.request.requestType', 'pendingTask'].join(propertyJoin);
        break;
      default:
        propertyString = '';
    }

    if (propertyString.includes('?')) {
      propertyString = propertyString + '&useWebCache=true'
    } else {
      propertyString = '?useWebCache=true'
    }

    return {
      url: `/tasks/${stateToFetch}${propertyString}`,
      renderNotFoundIf404
    };
  }
);

export const FetchScheduledTasksForRequest = buildApiAction(
  'FETCH_SCHEDULED_TASKS_FOR_REQUEST',
  (requestId) => ({
    url: `/tasks/scheduled/request/${requestId}?useWebCache=true`
  }),
  (requestId) => requestId
);

export const FetchTask = buildApiAction(
  'FETCH_TASK',
  (taskId) => ({
    url: `/tasks/task/${taskId}?useWebCache=true`,
  })
);

export const KillTask = buildJsonApiAction(
  'KILL_TASK',
  'DELETE',
  (taskId, data) => ({
    url: `/tasks/task/${taskId}`,
    body: data
  })
);

export const FetchTaskCleanups = buildApiAction(
  'FETCH_TASK_CLEANUPS',
  {url: '/tasks/cleaning?useWebCache=true'}
);

export const FetchTaskStatistics = buildApiAction(
  'FETCH_TASK_STATISTICS',
  (taskId, catchStatusCodes) => ({
    url: `/tasks/task/${taskId}/statistics`,
    catchStatusCodes
  })
);

export const RunCommandOnTask = buildJsonApiAction(
  'RUN_COMMAND_ON_TASK',
  'POST',
  (taskId, commandName) => ({
    url: `/tasks/task/${taskId}/command`,
    body: {name: commandName}
  })
);
