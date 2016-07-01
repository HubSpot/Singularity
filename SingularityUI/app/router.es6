let NotFoundController;
let RequestDetailController;
let RequestsTableController;
let TaskSearchController;
let Utils;

const hasProp = {}.hasOwnProperty;

import DashboardController from 'controllers/Dashboard';

import StatusController from 'controllers/Status';

import RequestFormController from 'controllers/RequestForm';

import NewDeployFormController from 'controllers/NewDeployForm';

RequestDetailController = require('controllers/RequestDetail');

RequestsTableController = require('controllers/RequestsTable');

import TasksTableController from 'controllers/TasksTable';

import TaskDetailController from 'controllers/TaskDetail';

import RacksController from 'controllers/Racks';

import SlavesController from 'controllers/Slaves';

NotFoundController = require('controllers/NotFound');

import DeployDetailController from 'controllers/DeployDetail';

import LogViewerController from 'controllers/LogViewer';

TaskSearchController = require('controllers/TaskSearch');

import WebhooksController from 'controllers/Webhooks';

Utils = require('./utils').default;

class Router extends Backbone.Router {
  constructor(app) {
    super();
    this.app = app;
  }

  dashboard() {
    return this.app.bootstrapController(new DashboardController({
      store: this.app.store
    }));
  }

  status() {
    return this.app.bootstrapController(new StatusController({
      store: this.app.store
    }));
  }

  requestForm(requestId = null) {
    if (requestId == null) {
      requestId = '';
    }
    return this.app.bootstrapController(new RequestFormController({
      store: this.app.store,
      requestId
    }));
  }

  requestsTable(state, subFilter, searchFilter) {
    if (state == null) {
      state = 'all';
    }
    if (subFilter == null) {
      subFilter = 'all';
    }
    if (searchFilter == null) {
      searchFilter = '';
    }
    return this.app.bootstrapController(new RequestsTableController({
      state,
      subFilter,
      searchFilter
    }));
  }

  requestDetail(requestId) {
    return this.app.bootstrapController(new RequestDetailController({
      requestId
    }));
  }

  taskSearch(requestId) {
    return this.app.bootstrapController(new TaskSearchController({
      requestId
    }));
  }

  newDeploy(requestId) {
    return this.app.bootstrapController(new NewDeployFormController({
      store: this.app.store,
      requestId
    }));
  }

  tasksTable(state, requestsSubFilter, searchFilter) {
    if (state == null) {
      state = 'active';
    }
    if (requestsSubFilter == null) {
      requestsSubFilter = 'all';
    }
    if (searchFilter == null) {
      searchFilter = '';
    }
    return this.app.bootstrapController(new TasksTableController({
      store: this.app.store,
      state: state,
      requestsSubFilter: requestsSubFilter,
      searchFilter: searchFilter
    }));
  }

  taskDetail(taskId) {
    return this.app.bootstrapController(new TaskDetailController({
      store: this.app.store,
      taskId: taskId,
      filePath: taskId
    }));
  }

  taskFileBrowser(taskId, filePath) {
    if (filePath == null) {
      filePath = "";
    }
    return this.app.bootstrapController(new TaskDetailController({
      store: this.app.store,
      taskId: taskId,
      filePath: filePath
    }));
  }

  tail(taskId, path) {
    let initialOffset, params, requestId, search, splits;
    if (path == null) {
      path = '';
    }
    initialOffset = parseInt(window.location.hash.substr(1), 10) || null;
    splits = taskId.split('-');
    requestId = splits.slice(0, splits.length - 5).join('-');
    params = Utils.getQueryParams();
    search = params.search || '';
    path = path.replace(taskId, '$TASK_ID');
    return this.app.bootstrapController(new LogViewerController({
      store: this.app.store,
      requestId,
      path,
      initialOffset,
      taskIds: [taskId],
      search,
      viewMode: 'split'
    }));
  }

  racks(state) {
    // TODO: state
    return this.app.bootstrapController(new RacksController({store: this.app.store}));
  }

  slaves(state) {
    // TODO: state?
    return this.app.bootstrapController(new SlavesController({store: this.app.store}));
  }

  notFound() {
    return this.app.bootstrapController(new NotFoundController);
  }

  deployDetail(requestId, deployId) {
    return this.app.bootstrapController(new DeployDetailController({
      store: this.app.store,
      requestId: requestId,
      deployId: deployId
    }));
  }

  aggregateTail(requestId, path) {
    let initialOffset, params, search, taskIds, viewMode;
    if (path == null) {
      path = '';
    }
    initialOffset = parseInt(window.location.hash.substr(1), 10) || null;
    params = Utils.getQueryParams();
    if (params.taskIds) {
      taskIds = params.taskIds.split(',');
    } else {
      taskIds = [];
    }
    viewMode = params.viewMode || 'split';
    search = params.search || '';
    return this.app.bootstrapController(new LogViewerController({
      store: this.app.store,
      requestId,
      path,
      initialOffset,
      taskIds,
      viewMode,
      search
    }));
  }

  webhooks() {
    return this.app.bootstrapController(new WebhooksController({store: this.app.store}));
  }
}

Router.prototype.routes = {
  '(/)': 'dashboard',
  'status(/)': 'status',
  'requests/new(/)': 'requestForm',
  'requests/edit/:requestId': 'requestForm',
  'requests/:state/:subFilter/:searchFilter(/)': 'requestsTable',
  'requests/:state/:subFilter(/)': 'requestsTable',
  'requests/:state(/)': 'requestsTable',
  'requests(/)': 'requestsTable',
  'request/:requestId(/)': 'requestDetail',
  'request/:requestId/deploy/:deployId(/)': 'deployDetail',
  'request/:requestId/tail/*path': 'aggregateTail',
  'request/:requestId/taskSearch': 'taskSearch',
  'request/:requestId/deploy(/)': 'newDeploy',
  'tasks/:state/:requestsSubFilter/:searchFilter(/)': 'tasksTable',
  'tasks/:state/:requestsSubFilter(/)': 'tasksTable',
  'tasks/:state(/)': 'tasksTable',
  'tasks(/)': 'tasksTable',
  'task/:taskId(/)': 'taskDetail',
  'task/:taskId/files(/)*path': 'taskFileBrowser',
  'task/:taskId/tail/*path': 'tail',
  'taskSearch': 'taskSearch',
  'racks(/)': 'racks',
  'racks/:state(/)': 'racks',
  'slaves/:state(/)': 'slaves',
  'slaves(/)': 'slaves',
  'webhooks(/)': 'webhooks',
  '*anything': 'notFound'
};

export default Router;
