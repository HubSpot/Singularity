import { combineReducers } from 'redux';

import refresh from './refresh';
import form from './form';
import globalSearch from './globalSearch';
import temporaryUserSettings from './temporaryUserSettings';

export default combineReducers({
  temporaryUserSettings,
  refresh,
  form,
  globalSearch
});
