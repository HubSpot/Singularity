import { combineReducers } from 'redux';

import starred from './starred';
import form from './form';
import globalSearch from './globalSearch';

export default combineReducers({
  starred,
  form,
  globalSearch
});
