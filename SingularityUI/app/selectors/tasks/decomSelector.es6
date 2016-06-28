import { createSelector } from 'reselect';

const getTasks = (state) => state.tasks;
const getCleanups = (state) => state.cleanups;

export default createSelector([getTasks, getCleanups], (tasks, cleanups) => {
  return _.without(_.map(cleanups, (c) => {
    if (c.cleanupType === 'DECOMISSIONING') {
      return _.find(tasks, (t) => t.taskId.id === c.taskId.id);
    }
  }), undefined);
});
