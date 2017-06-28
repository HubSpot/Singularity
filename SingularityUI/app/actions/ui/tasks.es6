import { FetchTasksInState, FetchTaskCleanups } from '../../actions/api/tasks';

export const refresh = (state) => (dispatch) =>
  Promise.all([
    dispatch(FetchTasksInState.trigger(state || 'active', true)),
    dispatch(FetchTaskCleanups.trigger()),
  ]);
