{ combineReducers } = require 'redux'

taskGroups = require './taskGroups'
activeRequest = require './activeRequest'
tasks = require './tasks'

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

maxLines = (state=100000, action) ->
  return state

showDebugInfo = (state=false, action) ->
  if action.type is 'LOG_INIT'
    return Boolean(window.localStorage.showDebugInfo) || false
  if action.type is 'LOG_DEBUG_INFO'
    window.localStorage.showDebugInfo = action.value
    return action.value
  return state

module.exports = combineReducers({showDebugInfo, taskGroups, tasks, activeRequest, path, activeColor, colors, viewMode, search, logRequestLength, maxLines})