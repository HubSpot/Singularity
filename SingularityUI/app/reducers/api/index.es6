import { combineReducers } from 'redux';

import requests from './requests';
import user from './user';


export default combineReducers({
  requests,
  user
});
