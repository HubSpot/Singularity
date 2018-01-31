import _ from 'underscore';
import { combineReducers } from 'redux';
import buildApiActionReducer from './base';
import buildKeyedApiActionReducer from './keyed';

import { FetchUser } from '../../actions/api/auth';

import {
  AddStarredRequests,
  DeleteStarredRequests
} from '../../actions/api/users';

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
  FetchRequestHistory,
  FetchRequestArgHistory
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
  FetchUserRelevantRequests,
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
  ReactivateSlave,
  FetchExpiringSlaveStates,
  RemoveExpiringSlaveState,
  FetchSlaveUsages
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

import {
  FetchDisastersData,
  FetchDisabledActions,
  FetchPriorityFreeze,
  FetchTaskCredits
} from '../../actions/api/disasters';

import { FetchGroups } from '../../actions/api/requestGroups';

import { FetchInactiveHosts } from '../../actions/api/inactive';

import { FetchUtilization } from '../../actions/api/utilization';

const user = buildApiActionReducer(FetchUser);
const addStarredRequests = buildApiActionReducer(AddStarredRequests, []);
const deleteStarredRequests = buildApiActionReducer(DeleteStarredRequests, []);
const webhooks = buildApiActionReducer(FetchWebhooks, []);
const disabledActions = buildApiActionReducer(FetchDisabledActions, []);
const disastersData = buildApiActionReducer(FetchDisastersData, []);
const taskCredits = buildApiActionReducer(FetchTaskCredits, []);
const priorityFreeze = buildApiActionReducer(FetchPriorityFreeze, []);
const slaves = buildApiActionReducer(FetchSlaves, []);
const freezeSlave = buildApiActionReducer(FreezeSlave, []);
const decommissionSlave = buildApiActionReducer(DecommissionSlave, []);
const removeSlave = buildApiActionReducer(RemoveSlave, []);
const reactivateSlave = buildApiActionReducer(ReactivateSlave, []);
const expiringSlaveStates = buildApiActionReducer(FetchExpiringSlaveStates, []);
const removeExpiringSlaveState = buildApiActionReducer(RemoveExpiringSlaveState, []);
const slaveUsages = buildApiActionReducer(FetchSlaveUsages, []);
const racks = buildApiActionReducer(FetchRacks, []);
const freezeRack = buildApiActionReducer(FreezeRack, []);
const decommissionRack = buildApiActionReducer(DecommissionRack, []);
const removeRack = buildApiActionReducer(RemoveRack, []);
const reactivateRack = buildApiActionReducer(ReactivateRack, []);
const request = buildKeyedApiActionReducer(FetchRequest);
const saveRequest = buildApiActionReducer(SaveRequest);
const requests = buildApiActionReducer(FetchRequests, []);
const userRelevantRequests = buildApiActionReducer(FetchUserRelevantRequests, []);
const requestsInState = buildApiActionReducer(FetchRequestsInState, []);
const requestHistory = buildKeyedApiActionReducer(FetchRequestHistory, []);
const requestArgHistory = buildKeyedApiActionReducer(FetchRequestArgHistory, []);
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
const activeTasksForRequest = buildKeyedApiActionReducer(FetchActiveTasksForRequest, [], (tasks) => _.sortBy(tasks, (task) => task.taskId.instanceNo));
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
const inactiveHosts = buildApiActionReducer(FetchInactiveHosts, []);
const utilization = buildApiActionReducer(FetchUtilization, {});

export default combineReducers({
  user,
  addStarredRequests,
  deleteStarredRequests,
  webhooks,
  disabledActions,
  disastersData,
  taskCredits,
  priorityFreeze,
  slaves,
  freezeSlave,
  decommissionSlave,
  removeSlave,
  reactivateSlave,
  expiringSlaveStates,
  removeExpiringSlaveState,
  slaveUsages,
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
  userRelevantRequests,
  requestsInState,
  requestHistory,
  requestArgHistory,
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
  requestGroups,
  inactiveHosts,
  utilization
});
