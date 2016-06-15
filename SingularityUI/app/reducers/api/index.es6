import { combineReducers } from 'redux';
import buildApiActionReducer from './base';

import { FetchAction as UserFetchAction } from '../../actions/api/user';
import { FetchAction as WebhooksFetchAction } from '../../actions/api/webhooks';
import { FetchAction as SlavesFetchAction } from '../../actions/api/slaves';
import { FetchAction as RacksFetchAction } from '../../actions/api/racks';
import { FetchAction as RequestsFetchAction } from '../../actions/api/requests';
import { FetchAction as StatusFetchAction } from '../../actions/api/status';

const user = buildApiActionReducer(UserFetchAction);
const webhooks = buildApiActionReducer(WebhooksFetchAction);
const slaves = buildApiActionReducer(SlavesFetchAction);
const racks = buildApiActionReducer(RacksFetchAction);
const requests = buildApiActionReducer(RequestsFetchAction);
const status = buildApiActionReducer(StatusFetchAction);

export default combineReducers({
  user,
  webhooks,
  slaves,
  racks,
  requests,
  status
});
