import { combineReducers } from 'redux';

import starred from './starred';
import refresh from './refresh';
import form from './form';
import globalSearch from './globalSearch';

export default combineReducers({
  starred,
  refresh,
  form,
  globalSearch
});
