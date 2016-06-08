import { combineReducers } from 'redux';

import taskGroups from './taskGroups';
import activeRequest from './activeRequest';
import tasks from './tasks';

let path = function(state='', action) {
  if (action.type === 'LOG_INIT') {
    return action.path;
  }
  return state;
};

let activeColor = function(state='default', action) {
  if (action.type === 'LOG_INIT') {
    return window.localStorage.logColor || 'default';
  } else if (action.type === 'LOG_SELECT_COLOR') {
    window.localStorage.logColor = action.color;
    return action.color;
  }
  return state;
};

let colors = (state=[]) => state;

let viewMode = function(state='custom', action) {
  if (action.type === 'LOG_SWITCH_VIEW_MODE') {
    return action.viewMode;
  }
  return state;
};

let search = function(state='', action) {
  if (action.type === 'LOG_INIT') {
    return action.search;
  }
  return state;
};

let logRequestLength = (state=30000, action) => state;

let maxLines = (state=100000, action) => state;

let showDebugInfo = function(state=false, action) {
  if (action.type === 'LOG_INIT') {
    return Boolean(window.localStorage.showDebugInfo) || false;
  }
  if (action.type === 'LOG_DEBUG_INFO') {
    window.localStorage.showDebugInfo = action.value;
    return action.value;
  }
  return state;
};

export default combineReducers({showDebugInfo, taskGroups, tasks, activeRequest, path, activeColor, colors, viewMode, search, logRequestLength, maxLines});
