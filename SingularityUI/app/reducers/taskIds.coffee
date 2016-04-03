ACTIONS = {
  # The logger is being initialized
  LOG_INIT: (state, {taskIdGroups}) ->
    _.flatten(taskIdGroups)
  # Add a group of tasks to the logger
  LOG_ADD_TASK_GROUP: (state, {taskIds}) ->
    state.concat(taskIds)
  # Remove a task from the logger
  LOG_REMOVE_TASK: (state, {taskId}) ->
    _.without(state, taskId)
}

module.exports = (state=[], action) ->
  if action.type of ACTIONS
    return ACTIONS[action.type](state, action)
  else
    return state