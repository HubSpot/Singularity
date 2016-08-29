const ACTIONS = {
  LOG_INIT(state, {requestId}) {
    return Object.assign({}, state, {requestId});
  },
  REQUEST_ACTIVE_TASKS(state, {tasks}) {
    return Object.assign({}, state, {activeTasks: tasks});
  }
};

export default function(state = {}, action) {
  if (action.type in ACTIONS) {
    return ACTIONS[action.type](state, action);
  }
  return state;
}
