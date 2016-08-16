import { combineReducers } from 'redux';

import refresh from './refresh';
import form from './form';
import globalSearch from './globalSearch';

export default combineReducers({
  refresh,
  form,
  globalSearch
});
