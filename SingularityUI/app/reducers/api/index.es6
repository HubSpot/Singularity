import { combineReducers } from 'redux';
import buildApiActionReducer from './base';
import buildKeyedApiActionReducer from './keyed';

import { FetchAction as TaskHistoryFetchAction } from '../../actions/api/task';
import { FetchAction as UserFetchAction } from '../../actions/api/user';
import { FetchAction as WebhooksFetchAction } from '../../actions/api/webhooks';
import { FetchAction as SlavesFetchAction } from '../../actions/api/slaves';
import { FetchAction as RacksFetchAction } from '../../actions/api/racks';
import { FetchAction as RequestFetchAction, SaveAction as RequestSaveAction } from '../../actions/api/request';
import { FetchAction as RequestsFetchAction } from '../../actions/api/requests';
import { FetchAction as StatusFetchAction } from '../../actions/api/status';
import { FetchAction as DeployFetchAction } from '../../actions/api/deploy';
import { FetchAction as DeploysFetchAction } from '../../actions/api/deploys';
import { FetchForDeployAction as TasksFetchForDeployAction } from '../../actions/api/tasks';
import { FetchForDeploy as TaskHistoryFetchForDeploy } from '../../actions/api/taskHistory';
import { FetchAction as TaskCleanupsFetchAction } from '../../actions/api/taskCleanups';
import { FetchAction as TaskFilesFetchAction } from '../../actions/api/taskFiles';
import { FetchAction as TaskResourceUsageFetchAction } from '../../actions/api/taskResourceUsage';
import { FetchAction as TaskS3LogsFetchAction } from '../../actions/api/taskS3Logs';
import { RunAction as TaskShellCommandRunAction } from '../../actions/api/taskShellCommand';

const user = buildApiActionReducer(UserFetchAction);
const webhooks = buildApiActionReducer(WebhooksFetchAction, []);
const slaves = buildApiActionReducer(SlavesFetchAction, []);
const racks = buildApiActionReducer(RacksFetchAction, []);
const request = buildApiActionReducer(RequestFetchAction);
const saveRequest = buildApiActionReducer(RequestSaveAction);
const requests = buildApiActionReducer(RequestsFetchAction, []);
const status = buildApiActionReducer(StatusFetchAction);
const deploy = buildApiActionReducer(DeployFetchAction);
const deploys = buildApiActionReducer(DeploysFetchAction, []);
const requests = buildApiActionReducer(RequestsFetchAction, []);
const activeTasksForDeploy = buildApiActionReducer(TasksFetchForDeployAction);
const taskHistoryForDeploy = buildApiActionReducer(TaskHistoryFetchForDeploy);
const taskCleanups = buildApiActionReducer(TaskCleanupsFetchAction, []);
const taskFiles = buildKeyedApiActionReducer(TaskFilesFetchAction, []);
const taskResourceUsage = buildApiActionReducer(TaskResourceUsageFetchAction);
const taskS3Logs = buildApiActionReducer(TaskS3LogsFetchAction, []);
const taskShellCommandResponse = buildApiActionReducer(TaskShellCommandRunAction);
const task = buildKeyedApiActionReducer(TaskHistoryFetchAction);

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
  taskHistoryForDeploy,
  taskCleanups,
  taskFiles,
  taskResourceUsage,
  taskS3Logs,
  deploys,
  taskShellCommandResponse
});
