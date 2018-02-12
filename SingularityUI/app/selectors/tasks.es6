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
      const getHost = (task) => task.taskId && task.taskId.host || '';
      const getId = (task) => (task.taskId ? task.taskId.id : task.pendingTask.pendingTaskId.id) || '';
      const getRack = (task) => task.taskId && task.taskId.rackId || '';

      if (Utils.isGlobFilter(filter.filterText)) {
        const hostMatches = _.filter(tasks, (task) => (
          micromatch.isMatch(getHost(task), `${filter.filterText}*`)
        ));
        const idMatches = _.filter(tasks, (task) => (
          micromatch.isMatch(getId(task), `${filter.filterText}*`)
        ));
        const rackMatches = _.filter(tasks, (task) => (
          micromatch.isMatch(getRack(task), `${filter.filterText}*`)
        ));
        tasks = _.union(hostMatches, idMatches, rackMatches);
      } else {
        const hostMatches = fuzzy.filter(filter.filterText.replace(/-/g, '_'), tasks, {
          extract: getHost
        });
        // Allow searching by the first letter of each word by applying same
        // search heuristics to just the upper case characters of each option
        const idMatches = fuzzy.filter(filter.filterText, tasks, {
          extract: Utils.isAllUpperCase(filter.filterText)
            ? (task) => Utils.getUpperCaseCharacters(getId(task))
            : getId,
        });
        const rackMatches = fuzzy.filter(filter.filterText, tasks, {
          extract: getRack
        });
        tasks = Utils.fuzzyFilter(filter.filterText, _.union(hostMatches, idMatches, rackMatches));
      }
    }

    return tasks;
  }
);
