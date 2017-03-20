import { combineReducers } from 'redux';

import files from './files';
import requests from './requests';
import config from './config';

export default combineReducers({
  files,
  requests,
  config
});
