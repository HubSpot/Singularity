import * as TaskActions from '../../actions/api/task';

const initialState = {};

export default function task(state = initialState, action) {
  let newData = {};
  switch (action.type) {
    case TaskActions.FETCH_TASK_ERROR:
      newData[action.taskId] = {
        isFetching: false,
        error: action.error
      };
      return _.extend({}, state, newData);
    case TaskActions.FETCH_TASK_SUCCESS:
      newData[action.taskId] = {
        isFetching: false,
        error: null,
        receivedAt: Date.now(),
        data: action.data
      };
      return _.extend({}, state, newData);
    case TaskActions.FETCH_TASK_STARTED:
      // Request initiated
      newData[action.taskId] = {
        isFetching: true,
        error: null
      };
      return _.extend({}, state, newData);
    default:
      return state;
  }
}
