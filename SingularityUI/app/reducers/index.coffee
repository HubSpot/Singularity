{ combineReducers } = require 'redux'

taskGroups = require './taskGroups'
taskIds = require './taskIds'

path = (state='', action) ->
  if action.type is 'LOG_INIT'
    return action.path
  return state

activeColor = (state='default', action) ->
  if action.type is 'LOG_INIT'
    return window.localStorage.logColor || 'default'
  else if action.type is 'LOG_SELECT_COLOR'
    window.localStorage.logColor = action.color
    return action.color
  return state

colors = (state=[]) -> state

viewMode = (state='custom', action) ->
  if action.type is 'LOG_SWITCH_VIEW_MODE'
    return action.viewMode
  return state

search = (state='', action) ->
  if action.type is 'LOG_INIT'
    return action.search
  return state

logRequestLength = (state=30000, action) ->
  return state

maxLines = (state=1000, action) ->
  return state

activeRequest = (state={}, action) ->
  if action.type is 'LOG_INIT'
    return Object.assign({}, state, {requestId: action.requestId})
  if action.type is 'REQUEST_ACTIVE_TASKS'
    return Object.assign({}, state, {activeTasks: action.tasks})
  return state

module.exports = combineReducers({taskGroups, taskIds, activeRequest, path, activeColor, colors, viewMode, search, logRequestLength, maxLines})