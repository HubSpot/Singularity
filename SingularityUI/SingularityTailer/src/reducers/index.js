import { combineReducers } from 'redux';
import files from './files';
import config from './config';

export default combineReducers({
  files,
  config
});
