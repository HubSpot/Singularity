import { combineReducers } from 'redux';

import refresh from './refresh';
import form from './form';
import globalSearch from './globalSearch';
import starred from './starred' ;

export default combineReducers({
  refresh,
  form,
  globalSearch,
  localStars: starred
});
