import { combineReducers } from 'redux';
import buildApiActionReducer from './base';
import task from './task';

import { FetchAction as UserFetchAction } from '../../actions/api/user';
import { FetchAction as WebhooksFetchAction } from '../../actions/api/webhooks';
import { FetchAction as SlavesFetchAction } from '../../actions/api/slaves';
import { FetchAction as RacksFetchAction } from '../../actions/api/racks';
import { FetchAction as StatusFetchAction } from '../../actions/api/status';
import { FetchAction as DeployFetchAction } from '../../actions/api/deploy';
import { FetchAction as DeploysFetchAction } from '../../actions/api/deploys';
import { FetchForDeployAction as TasksFetchForDeployAction } from '../../actions/api/tasks';
import { FetchForDeploy as TaskHistoryFetchForDeploy } from '../../actions/api/taskHistory';
import { FetchAction as TaskCleanupsFetchAction } from '../../actions/api/taskCleanups';
import { FetchAction as TaskFilesFetchAction } from '../../actions/api/taskFiles';
import { FetchAction as TaskResourceUsageFetchAction } from '../../actions/api/taskResourceUsage';
import { FetchAction as TaskS3LogsFetchAction } from '../../actions/api/taskS3Logs';

const user = buildApiActionReducer(UserFetchAction);
const webhooks = buildApiActionReducer(WebhooksFetchAction);
const slaves = buildApiActionReducer(SlavesFetchAction);
const racks = buildApiActionReducer(RacksFetchAction);
const status = buildApiActionReducer(StatusFetchAction);
const deploy = buildApiActionReducer(DeployFetchAction);
const deploys = buildApiActionReducer(DeploysFetchAction);
const activeTasksForDeploy = buildApiActionReducer(TasksFetchForDeployAction);
const taskHistoryForDeploy = buildApiActionReducer(TaskHistoryFetchForDeploy);
const taskCleanups = buildApiActionReducer(TaskCleanupsFetchAction);
const taskFiles = buildApiActionReducer(TaskFilesFetchAction);
const taskResourceUsage = buildApiActionReducer(TaskResourceUsageFetchAction);
const taskS3Logs = buildApiActionReducer(TaskS3LogsFetchAction);

export default combineReducers({
  user,
  webhooks,
  slaves,
  racks,
  status,
  deploy,
  task,
  activeTasksForDeploy,
  taskHistoryForDeploy,
  taskCleanups,
  taskFiles,
  taskResourceUsage,
  taskS3Logs,
  deploys
});
