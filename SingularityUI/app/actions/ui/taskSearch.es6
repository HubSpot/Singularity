import { FetchTaskSearchParams } from '../../actions/api/history';

export const UpdateFilter = (filter) => ({
  filter,
  type: 'UPDATE_TASK_SEARCH_FILTER'
});
