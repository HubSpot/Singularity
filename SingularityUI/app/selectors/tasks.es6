import { createSelector } from 'reselect';

const getTaskCleanups = (state) => state.api.taskCleanups;

export const getBouncesForRequest = (requestId) => createSelector(
  [getTaskCleanups],
  (taskCleanups) => (
  taskCleanups.data || []).filter((tc) => (
    tc.cleanupType === 'BOUNCING' && tc.taskId.requestId === requestId
  ))
);
