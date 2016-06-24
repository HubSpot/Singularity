import { combineReducers } from 'redux';
import buildApiActionReducer from './base';
import task from './task';

import { FetchAction as UserFetchAction } from '../../actions/api/user';
import { FetchAction as WebhooksFetchAction } from '../../actions/api/webhooks';
import { FetchAction as SlavesFetchAction } from '../../actions/api/slaves';
import { FetchAction as RacksFetchAction } from '../../actions/api/racks';
import { FetchAction as RequestFetchAction, SaveAction as RequestSaveAction } from '../../actions/api/request';
import { FetchAction as RequestsFetchAction } from '../../actions/api/requests';
import { FetchAction as StatusFetchAction } from '../../actions/api/status';
import { FetchAction as DeployFetchAction } from '../../actions/api/deploy';
import { FetchForDeployAction as TasksFetchForDeployAction } from '../../actions/api/tasks';
import { FetchForDeploy as TaskHistoryFetchForDeploy } from '../../actions/api/taskHistory';

const user = buildApiActionReducer(UserFetchAction);
const webhooks = buildApiActionReducer(WebhooksFetchAction);
const slaves = buildApiActionReducer(SlavesFetchAction);
const racks = buildApiActionReducer(RacksFetchAction);
const request = buildApiActionReducer(RequestFetchAction);
const saveRequest = buildApiActionReducer(RequestSaveAction);
const requests = buildApiActionReducer(RequestsFetchAction);
const status = buildApiActionReducer(StatusFetchAction);
const deploy = buildApiActionReducer(DeployFetchAction);
const activeTasksForDeploy = buildApiActionReducer(TasksFetchForDeployAction);
const taskHistoryForDeploy = buildApiActionReducer(TaskHistoryFetchForDeploy);

export default combineReducers({
  user,
  webhooks,
  slaves,
  racks,
  request,
  saveRequest,
  requests,
  status,
  deploy,
  task,
  activeTasksForDeploy,
  taskHistoryForDeploy
});
