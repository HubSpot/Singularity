import * as TaskActions from '../../actions/api/task';

const initialState = {};

export default function task(state = initialState, action) {
  let newData = {};
  switch (action.type) {
    case TaskActions.FETCH_TASK_CLEAR:
      return initialState;
    case TaskActions.FETCH_TASK_ERROR:
      newData[action.taskId] = {
        isFetching: false,
        error: action.error
      };
      if (state[action.taskId]) {
        newData[action.taskId] = _.extend(state[action.taskId], newData[action.taskId]);
      }
      return _.extend({}, state, newData);
    case TaskActions.FETCH_TASK_SUCCESS:
      newData[action.taskId] = {
        isFetching: false,
        error: null,
        receivedAt: Date.now(),
        data: action.data
      };
      if (state[action.taskId]) {
        newData[action.taskId] = _.extend(state[action.taskId], newData[action.taskId]);
      }
      return _.extend({}, state, newData);
    case TaskActions.FETCH_TASK_STARTED:
      // Request initiated
      newData[action.taskId] = {
        isFetching: true,
        error: null
      };
      if (state[action.taskId]) {
        newData[action.taskId] = _.extend(state[action.taskId], newData[action.taskId]);
      }
      return _.extend({}, state, newData);
    default:
      return state;
  }
}
