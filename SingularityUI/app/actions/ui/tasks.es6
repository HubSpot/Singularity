import { FetchTasksInState, FetchTaskCleanups } from '../../actions/api/tasks';

export const refresh = (state, showResources) => (dispatch) =>
  Promise.all([
    dispatch(FetchTasksInState.trigger(state || 'active', true, showResources)),
    dispatch(FetchTaskCleanups.trigger()),
  ]);
