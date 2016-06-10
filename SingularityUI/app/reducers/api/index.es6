import { combineReducers } from 'redux';

import user from './user';
import webhooks from './webhooks';
import slaves from './slaves';
import racks from './racks';

export default combineReducers({user, webhooks, slaves, racks});