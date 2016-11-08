import { combineReducers } from 'redux';

import starred from './starred';
import refresh from './refresh';
import form from './form';
import globalSearch from './globalSearch';
import dashboard from './dashboard';
import slaves from './slaves';

export default combineReducers({
  starred,
  refresh,
  form,
  globalSearch,
  dashboard,
  slaves
});
