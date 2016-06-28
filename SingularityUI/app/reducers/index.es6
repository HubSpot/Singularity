import { combineReducers } from 'redux';

import taskGroups from './taskGroups';
import activeRequest from './activeRequest';
import tasks from './tasks';
import api from './api';

import ui from './ui';

const path = (state = '', action) => {
  if (action.type === 'LOG_INIT') {
    return action.path;
  }
  return state;
};

const activeColor = (state = 'default', action) => {
  if (action.type === 'LOG_INIT') {
    return window.localStorage.logColor || 'default';
  } else if (action.type === 'LOG_SELECT_COLOR') {
    window.localStorage.logColor = action.color;
    return action.color;
  }
  return state;
};

const colors = (state = []) => state;

const viewMode = function(state = 'custom', action) {
  if (action.type === 'LOG_SWITCH_VIEW_MODE' || action.type === 'LOG_INIT') {
    return action.viewMode;
  }
  return state;
};

const search = function(state = '', action) {
  if (action.type === 'LOG_INIT') {
    return action.search;
  }
  return state;
};

const logRequestLength = (state = 30000, action) => state;

const maxLines = (state = 100000, action) => state;

const showDebugInfo = function(state = false, action) {
  if (action.type === 'LOG_INIT') {
    return Boolean(window.localStorage.showDebugInfo) || false;
  }
  if (action.type === 'LOG_DEBUG_INFO') {
    window.localStorage.showDebugInfo = action.value;
    return action.value;
  }
  return state;
};

const rootReducer = combineReducers({
  api,
  ui,
  showDebugInfo,
  taskGroups,
  tasks,
  activeRequest,
  path,
  activeColor,
  colors,
  viewMode,
  search,
  logRequestLength,
  maxLines
});

export default rootReducer;
