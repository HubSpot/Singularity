let DashboardController;
let DeployDetailController;
let NewDeployController;
let NotFoundController;
let RacksController;
let RequestDetailController;
let RequestFormEditController;
let RequestFormNewController;
let RequestsTableController;
let SlavesController;
let TaskDetailController;
let TaskSearchController;
let TasksTableController;
let Utils;
let WebhooksController;

const hasProp = {}.hasOwnProperty;

DashboardController = require('controllers/Dashboard');

import StatusController from 'controllers/Status';

RequestFormNewController = require('controllers/RequestFormNew');

RequestFormEditController = require('controllers/RequestFormEdit');

NewDeployController = require('controllers/NewDeploy');

RequestDetailController = require('controllers/RequestDetail');

RequestsTableController = require('controllers/RequestsTable');

TasksTableController = require('controllers/TasksTable');

TaskDetailController = require('controllers/TaskDetail');

RacksController = require('controllers/Racks');

SlavesController = require('controllers/Slaves');

NotFoundController = require('controllers/NotFound');

DeployDetailController = require('controllers/DeployDetail');

import LogViewerController from 'controllers/LogViewer';

TaskSearchController = require('controllers/TaskSearch');

WebhooksController = require('controllers/Webhooks');

Utils = require('./utils').default;

class Router extends Backbone.Router {
  constructor(app) {
    super();
    this.app = app;
  }

  dashboard() {
    return this.app.bootstrapController(new DashboardController);
  }

  status() {
    return this.app.bootstrapController(new StatusController({
      store: this.app.store
    }));
  }

  newRequest() {
    return this.app.bootstrapController(new RequestFormNewController);
  }

  editRequest(requestId) {
    if (requestId == null) {
      requestId = '';
    }
    return this.app.bootstrapController(new RequestFormEditController({
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
    return this.app.bootstrapController(new NewDeployController({
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
      state,
      requestsSubFilter,
      searchFilter
    }));
  }

  taskDetail(taskId) {
    return this.app.bootstrapController(new TaskDetailController({
      taskId,
      filePath: null
    }));
  }

  taskFileBrowser(taskId, filePath) {
    if (filePath == null) {
      filePath = "";
    }
    return this.app.bootstrapController(new TaskDetailController({
      taskId,
      filePath
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
    if (state == null) {
      state = 'all';
    }
    return this.app.bootstrapController(new RacksController({
      state
    }));
  }

  slaves(state) {
    if (state == null) {
      state = 'all';
    }
    return this.app.bootstrapController(new SlavesController({
      state
    }));
  }

  notFound() {
    return this.app.bootstrapController(new NotFoundController);
  }

  deployDetail(requestId, deployId) {
    return this.app.bootstrapController(new DeployDetailController({
      requestId,
      deployId
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
    return this.app.bootstrapController(new WebhooksController);
  }
}

Router.prototype.routes = {
  '(/)': 'dashboard',
  'status(/)': 'status',
  'requests/new(/)': 'newRequest',
  'requests/edit/:requestId': 'editRequest',
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
