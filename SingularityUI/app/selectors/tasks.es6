import { createSelector } from 'reselect';
import micromatch from 'micromatch';
import fuzzy from 'fuzzy';

import Utils from '../utils';

const getTaskCleanups = (state) => state.api.taskCleanups;
const getTasks = (state) => state.tasks;
const getCleanups = (state) => state.cleanups;

export const getBouncesForRequest = (requestId) => createSelector(
  [getTaskCleanups],
  (taskCleanups) => (
  taskCleanups.data || []).filter((cleanup) => (
    (cleanup.cleanupType === 'BOUNCING' || cleanup.cleanupType === 'INCREMENTAL_BOUNCE') && cleanup.taskId.requestId === requestId
  ))
);

// warning, these selectors are dependent not on the application store
// but rather on the arg passed in a render method
// TODO: use redux for state so this doesn't happen
export const getDecomissioningTasks = createSelector(
  [getTasks, getCleanups],
  (tasks, cleanups) => {
    return _.without(_.map(cleanups, (cleanup) => {
      if (cleanup.cleanupType === 'DECOMISSIONING') {
        return _.find(tasks, (task) => task.taskId.id === cleanup.taskId.id);
      }
      return undefined;
    }), undefined);
  }
);

const getFilter = (state) => ({
  state: state.filter.taskStatus,
  requestTypes: state.filter.requestTypes,
  filterText: state.filter.filterText
});

export const getFilteredTasks = createSelector(
  [getTasks, getFilter],
  (tasks, filter) => {
    // Filter by requestType
    if (filter.state === 'active') {
      tasks = _.filter(tasks, (task) => {
        return task.taskRequest && _.contains(filter.requestTypes, task.taskRequest.request.requestType);
      });
    }

    // Filter by glob or fuzzy string
    if (filter.filterText) {
      const host = {extract: (task) => `${task.taskId && task.taskId.host}`};
      const id = {extract: (task) => `${task.taskId ? task.taskId.id : task.pendingTask.pendingTaskId.id}`};
      const rack = {extract: (task) => `${task.taskId && task.taskId.rackId}`};

      if (Utils.isGlobFilter(filter.filterText)) {
        const hostMatch = _.filter(tasks, (task) => {
          return micromatch.any(host.extract(task), `${filter.filterText}*`);
        });
        const idMatch = _.filter(tasks, (task) => {
          return micromatch.any(id.extract(task), `${filter.filterText}*`);
        });
        const rackMatch = _.filter(tasks, (task) => {
          return micromatch.any(rack.extract(task), `${filter.filterText}*`);
        });
        tasks = _.union(hostMatch, idMatch, rackMatch).reverse();
      } else {
        _.each(tasks, (task) => {
          task.id = id.extract(task);
        });
        const hostMatch = fuzzy.filter(filter.filterText.replace(/-/g, '_'), tasks, host);
        const idMatch = fuzzy.filter(filter.filterText, tasks, id);
        const rackMatch = fuzzy.filter(filter.filterText, tasks, rack);
        tasks = Utils.fuzzyFilter(filter.filterText, _.union(rackMatch, hostMatch, idMatch));
      }
    }

    return tasks;
  }
);
