import { combineReducers } from 'redux';

import refresh from './refresh';
import form from './form';
import globalSearch from './globalSearch';
import dashboard from './dashboard';
import slaves from './slaves';

export default combineReducers({
  refresh,
  form,
  globalSearch,
  dashboard,
  slaves
});
