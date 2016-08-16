import { combineReducers } from 'redux';
import buildApiActionReducer from './base';
import buildKeyedApiActionReducer from './keyed';

import {
  FetchUser,
  FetchUserSettings
} from '../../actions/api/user';

import {
  FetchPendingDeploys,
  SaveDeploy
} from '../../actions/api/deploys';

import {
  FetchTaskHistory,
  FetchActiveTasksForRequest,
  FetchTaskHistoryForRequest,
  FetchActiveTasksForDeploy,
  FetchTaskHistoryForDeploy,
  FetchDeployForRequest,
  FetchDeploysForRequest,
  FetchTaskSearchParams,
  FetchRequestHistory
} from '../../actions/api/history';

import { FetchTaskS3Logs } from '../../actions/api/logs';

import {
  FetchRacks,
  FreezeRack,
  DecommissionRack,
  RemoveRack,
  ReactivateRack
} from '../../actions/api/racks';

import {
  FetchRequests,
  FetchRequest,
  SaveRequest,
  RemoveRequest,
  PauseRequest,
  UnpauseRequest,
  ExitRequestCooldown,
  FetchRequestsInState
} from '../../actions/api/requests';

import { FetchTaskFiles } from '../../actions/api/sandbox';

import {
  FetchSlaves,
  FreezeSlave,
  DecommissionSlave,
  RemoveSlave,
  ReactivateSlave
} from '../../actions/api/slaves';

import {
  FetchSingularityStatus
} from '../../actions/api/state';

import {
  FetchTasksInState,
  FetchScheduledTasksForRequest,
  FetchTask, // currently FetchTaskHistory is used for `task` in the store
  KillTask,
  FetchTaskCleanups,
  FetchTaskStatistics,
  RunCommandOnTask
} from '../../actions/api/tasks';

import { FetchWebhooks } from '../../actions/api/webhooks';

import { FetchGroups } from '../../actions/api/requestGroups';

const user = buildApiActionReducer(FetchUser);
const userSettings = buildApiActionReducer(FetchUserSettings);
const webhooks = buildApiActionReducer(FetchWebhooks, []);
const slaves = buildApiActionReducer(FetchSlaves, []);
const freezeSlave = buildApiActionReducer(FreezeSlave, []);
const decommissionSlave = buildApiActionReducer(DecommissionSlave, []);
const removeSlave = buildApiActionReducer(RemoveSlave, []);
const reactivateSlave = buildApiActionReducer(ReactivateSlave, []);
const racks = buildApiActionReducer(FetchRacks, []);
const freezeRack = buildApiActionReducer(FreezeRack, []);
const decommissionRack = buildApiActionReducer(DecommissionRack, []);
const removeRack = buildApiActionReducer(RemoveRack, []);
const reactivateRack = buildApiActionReducer(ReactivateRack, []);
const request = buildKeyedApiActionReducer(FetchRequest);
const saveRequest = buildApiActionReducer(SaveRequest);
const requests = buildApiActionReducer(FetchRequests, []);
const requestsInState = buildApiActionReducer(FetchRequestsInState, []);
const requestHistory = buildKeyedApiActionReducer(FetchRequestHistory, []);
const removeRequest = buildKeyedApiActionReducer(RemoveRequest, []);
const pauseRequest = buildKeyedApiActionReducer(PauseRequest, []);
const unpauseRequest = buildKeyedApiActionReducer(UnpauseRequest, []);
const exitRequestCooldown = buildKeyedApiActionReducer(ExitRequestCooldown, []);
const status = buildApiActionReducer(FetchSingularityStatus);
const deploy = buildApiActionReducer(FetchDeployForRequest);
const deploys = buildApiActionReducer(FetchPendingDeploys, []);
const deploysForRequest = buildKeyedApiActionReducer(FetchDeploysForRequest, []);
const saveDeploy = buildApiActionReducer(SaveDeploy);
const activeTasksForDeploy = buildApiActionReducer(FetchActiveTasksForDeploy);
const activeTasksForRequest = buildKeyedApiActionReducer(FetchActiveTasksForRequest, []);
const scheduledTasksForRequest = buildKeyedApiActionReducer(FetchScheduledTasksForRequest, []);
const taskHistoryForDeploy = buildApiActionReducer(FetchTaskHistoryForDeploy, []);
const taskHistoryForRequest = buildKeyedApiActionReducer(FetchTaskHistoryForRequest, []);
const taskCleanups = buildApiActionReducer(FetchTaskCleanups, []);
const taskFiles = buildKeyedApiActionReducer(FetchTaskFiles, []);
const taskResourceUsage = buildApiActionReducer(FetchTaskStatistics);
const taskS3Logs = buildApiActionReducer(FetchTaskS3Logs, []);
const taskShellCommandResponse = buildApiActionReducer(RunCommandOnTask);
const runningTask = buildApiActionReducer(FetchTask);
const taskKill = buildApiActionReducer(KillTask);
const task = buildKeyedApiActionReducer(FetchTaskHistory);
const taskHistory = buildApiActionReducer(FetchTaskSearchParams, []);
const tasks = buildApiActionReducer(FetchTasksInState, []);
const requestGroups = buildApiActionReducer(FetchGroups, []);

export default combineReducers({
  user,
  userSettings,
  webhooks,
  slaves,
  freezeSlave,
  decommissionSlave,
  removeSlave,
  reactivateSlave,
  racks,
  freezeRack,
  decommissionRack,
  removeRack,
  reactivateRack,
  request,
  saveRequest,
  removeRequest,
  pauseRequest,
  unpauseRequest,
  exitRequestCooldown,
  requests,
  requestsInState,
  requestHistory,
  status,
  deploy,
  deploys,
  deploysForRequest,
  saveDeploy,
  task,
  tasks,
  activeTasksForDeploy,
  activeTasksForRequest,
  scheduledTasksForRequest,
  taskHistoryForDeploy,
  taskHistoryForRequest,
  taskCleanups,
  taskFiles,
  taskResourceUsage,
  taskS3Logs,
  taskShellCommandResponse,
  runningTask,
  taskKill,
  taskHistory,
  requestGroups
});
