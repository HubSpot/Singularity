import { combineReducers } from 'redux';

import starred from './starred';
import form from './form';

export default combineReducers({
  starred,
  form
});
