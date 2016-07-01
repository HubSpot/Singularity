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
  taskCleanups.data || []).filter((tc) => (
    tc.cleanupType === 'BOUNCING' && tc.taskId.requestId === requestId
  ))
);

// warning, these selectors are dependent not on the application store
// but rather on the arg passed in a render method
// TODO: use redux for state so this doesn't happen
export const getDecomissioningTasks = createSelector(
  [getTasks, getCleanups],
  (tasks, cleanups) => {
    return _.without(_.map(cleanups, (c) => {
      if (c.cleanupType === 'DECOMISSIONING') {
        return _.find(tasks, (t) => t.taskId.id === c.taskId.id);
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
      const host = {extract: (t) => `${t.taskId && t.taskId.host}`};
      const id = {extract: (t) => `${t.taskId ? t.taskId.id : t.pendingTask.pendingTaskId.id}`};
      const rack = {extract: (t) => `${t.taskId && t.taskId.rackId}`};

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
        _.each(tasks, (t) => {
          t.id = id.extract(t);
        });
        const hostMatch = fuzzy.filter(filter.filterText, tasks, host);
        const idMatch = fuzzy.filter(filter.filterText, tasks, id);
        const rackMatch = fuzzy.filter(filter.filterText, tasks, rack);
        tasks = _.uniq(
          _.pluck(
            _.sortBy(
              _.union(
                rackMatch, hostMatch, idMatch
              ),
              (t) => Utils.fuzzyAdjustScore(filter.filterText, t)
            ),
            'original'
          ).reverse()
        );
      }
    }

    return tasks;
  }
);
