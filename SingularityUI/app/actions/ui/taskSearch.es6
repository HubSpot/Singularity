import { FetchTaskSearchParams } from '../../actions/api/history';

export const UpdateFilter = (filter) => ({
  filter,
  type: 'UPDATE_TASK_SEARCH_FILTER'
});

export const refresh = (requestId, count, page) => (dispatch) =>
  Promise.all([
    dispatch(FetchTaskSearchParams.clear()),
    dispatch(FetchTaskSearchParams.trigger(requestId, count, page)),
    dispatch(UpdateFilter({ requestId })),
  ]);