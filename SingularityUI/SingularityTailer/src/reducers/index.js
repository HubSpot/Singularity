import { combineReducers } from 'redux';
import files from './files';
import requests from './requests';
import config from './config';
import scroll from './scroll';

export default combineReducers({
  files,
  requests,
  config,
  scroll
});
