import { combineReducers } from 'redux';
import { routerReducer as routing } from 'react-router-redux';

import taskGroups from './taskGroups';
import activeRequest from './activeRequest';
import tasks from './tasks';
import api from './api';
import ui from './ui';
import { reducer as formReducer } from 'redux-form';
import { reducer as tailerReducer } from 'singularityui-tailer';
import tailerViewReducer from './tailerView';

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

const colors = (state = ['Default', 'Light', 'Dark']) => state;

const viewMode = (state = 'custom', action) => {
  if (action.type === 'LOG_SWITCH_VIEW_MODE' || action.type === 'LOG_INIT') {
    return action.viewMode;
  }
  return state;
};

const search = (state = '', action) => {
  if (action.type === 'LOG_INIT') {
    return action.search;
  }
  return state;
};

const taskSearch = (state = {}, action) => {
  if (action.type === 'UPDATE_TASK_SEARCH_FILTER') {
    return action.filter;
  }
  return state;
};


const logRequestLength = (state = 30000) => state;

const maxLines = (state = 100000) => state;

const showDebugInfo = (state = false, action) => {
  if (action.type === 'LOG_INIT') {
    return Boolean(window.localStorage.showDebugInfo) || false;
  }
  if (action.type === 'LOG_DEBUG_INFO') {
    window.localStorage.showDebugInfo = action.value;
    return action.value;
  }
  return state;
};

export default combineReducers({
  api,
  ui,
  routing,
  showDebugInfo,
  taskGroups,
  taskSearch,
  tasks,
  activeRequest,
  path,
  activeColor,
  colors,
  viewMode,
  search,
  logRequestLength,
  maxLines,
  form: formReducer,
  tailer: tailerReducer,
  tailerView: tailerViewReducer
});
