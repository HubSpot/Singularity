import { createSelector } from 'reselect';
import micromatch from 'micromatch';
import Utils from '../utils';

const getTaskCleanups = (state) => state.api.taskCleanups;
const getTasks = (state) => state.tasks;
const getCleanups = (state) => state.cleanups;

export const getBouncesForRequest = (requestId) => createSelector(
  [getTaskCleanups],
  (taskCleanups) => (
  taskCleanups.data || []).filter((tc) => (
    tc.cleanupType === 'BOUNCING' || tc.cleanupType === 'INCREMENTAL_BOUNCE' && tc.taskId.requestId === requestId
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
      const host = task => `${task.taskId && task.taskId.host}`;
      const id = task => `${task.taskId ? task.taskId.id : task.pendingTask.pendingTaskId.id}`;
      const rack = task => `${task.taskId && task.taskId.rackId}`;

      if (Utils.isGlobFilter(filter.filterText)) {
        const hostMatch = _.filter(tasks, (task) => {
          return micromatch.any(host(task).toLowerCase(), `*${filter.filterText.toLowerCase()}*`);
        });
        const idMatch = _.filter(tasks, (task) => {
          return micromatch.any(id(task).toLowerCase(), `*${filter.filterText.toLowerCase()}*`);
        });
        const rackMatch = _.filter(tasks, (task) => {
          return micromatch.any(rack(task).toLowerCase(), `*${filter.filterText.toLowerCase()}*`);
        });
        tasks = _.sortBy(
          _.union(
            rackMatch, hostMatch, idMatch
          ),
          task => (micromatch.any(id(task).toLowerCase(), `${filter.filterText.toLowerCase()}*`) ? 1 : 0)
        ).reverse();
      } else {
        const hostMatch = _.filter(tasks, task => host(task).toLowerCase().indexOf(filter.filterText.replace(/-/g, '_').toLowerCase()) > -1);
        const idMatch = _.filter(tasks, task => id(task).toLowerCase().indexOf(filter.filterText.toLowerCase()) > -1);
        const rackMatch = _.filter(tasks, task => rack(task).toLowerCase().indexOf(filter.filterText.toLowerCase()) > -1);
        tasks = _.uniq(
          _.sortBy(
            _.union(
              rackMatch, hostMatch, idMatch
            ),
            task => (id(task).startsWith(filter.filterText.toLowerCase()) ? 1 : 0)
          )
        ).reverse();
      }
    }

    return tasks;
  }
);
