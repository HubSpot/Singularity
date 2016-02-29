DashboardController = require 'controllers/Dashboard'
StatusController    = require 'controllers/Status'

RequestFormNewController = require 'controllers/RequestFormNew'
RequestFormEditController = require 'controllers/RequestFormEdit'

NewDeployController  = require 'controllers/NewDeploy'

RequestDetailController = require 'controllers/RequestDetail'
RequestsTableController = require 'controllers/RequestsTable'

TasksTableController = require 'controllers/TasksTable'
TaskDetailController = require 'controllers/TaskDetail'
TailController = require 'controllers/Tail'

RacksController = require 'controllers/Racks'
SlavesController = require 'controllers/Slaves'

NotFoundController = require 'controllers/NotFound'

DeployDetailController = require 'controllers/DeployDetail'

AggregateTailController = require 'controllers/AggregateTail'

class Router extends Backbone.Router

    routes:
        '(/)': 'dashboard'
        'status(/)': 'status'

        'requests/new(/)': 'newRequest'
        'requests/edit/:requestId': 'editRequest'

        'requests/:state/:subFilter/:searchFilter(/)': 'requestsTable'
        'requests/:state/:subFilter(/)': 'requestsTable'
        'requests/:state(/)': 'requestsTable'
        'requests(/)': 'requestsTable'

        'request/:requestId(/)': 'requestDetail'
        'request/:requestId/deploy/:deployId(/)': 'deployDetail'
        'request/:requestId/tail/*path': 'aggregateTail'

        'request/:requestId/deploy(/)': 'newDeploy'

        'tasks/:state/:searchFilter(/)': 'tasksTable'
        'tasks/:state(/)': 'tasksTable'
        'tasks(/)': 'tasksTable'

        'task/:taskId(/)': 'taskDetail'
        'task/:taskId/files(/)*path': 'taskFileBrowser'
        'task/:taskId/tail/*path': 'tail'

        'racks(/)': 'racks'
        'racks/:state(/)': 'racks'

        'slaves/:state(/)': 'slaves'
        'slaves(/)': 'slaves'

        '*anything': 'notFound'

    dashboard: ->
        app.bootstrapController new DashboardController

    status: ->
        app.bootstrapController new StatusController

    newRequest: ->
        app.bootstrapController new RequestFormNewController

    editRequest: (requestId = '') ->
        app.bootstrapController new RequestFormEditController {requestId}

    requestsTable: (state = 'all', subFilter = 'all', searchFilter = '') ->
        app.bootstrapController new RequestsTableController {state, subFilter, searchFilter}

    requestDetail: (requestId) ->
        app.bootstrapController new RequestDetailController {requestId}

    newDeploy: (requestId) ->
        app.bootstrapController new NewDeployController {requestId}

    tasksTable: (state = 'active', searchFilter = '') ->
        app.bootstrapController new TasksTableController {state, searchFilter}

    taskDetail: (taskId) ->
        app.bootstrapController new TaskDetailController {taskId, filePath:null}

    taskFileBrowser: (taskId, filePath="") ->
        app.bootstrapController new TaskDetailController {taskId, filePath}

    tail: (taskId, path = '') ->
        offset = parseInt(window.location.hash.substr(1), 10) || null
        app.bootstrapController new TailController {taskId, path, offset}

    racks: (state = 'all') ->
        app.bootstrapController new RacksController {state}

    slaves: (state = 'all') ->
        app.bootstrapController new SlavesController {state}

    notFound: ->
        app.bootstrapController new NotFoundController

    deployDetail: (requestId, deployId) ->
        app.bootstrapController new DeployDetailController {requestId, deployId}

    aggregateTail: (requestId, path = '') ->
        offset = parseInt(window.location.hash.substr(1), 10) || null
        app.bootstrapController new AggregateTailController {requestId, path, offset}

module.exports = Router
