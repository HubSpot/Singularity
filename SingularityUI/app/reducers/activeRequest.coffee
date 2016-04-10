ACTIONS = {
  LOG_INIT: (state, {requestId}) ->
    return Object.assign({}, state, {requestId})
  REQUEST_ACTIVE_TASKS: (state, {tasks}) ->
    return Object.assign({}, state, {activeTasks: tasks})
}

module.exports = (state={}, action) ->
  if action.type of ACTIONS
    return ACTIONS[action.type](state, action)
  else
    return state